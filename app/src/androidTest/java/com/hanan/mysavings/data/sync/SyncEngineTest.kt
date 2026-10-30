package com.hanan.mysavings.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hanan.mysavings.data.local.MySavingsDatabase
import com.hanan.mysavings.data.profile.AndroidProfilePhotoStore
import com.hanan.mysavings.data.repository.RoomBudgetRepository
import com.hanan.mysavings.data.repository.RoomSavingsRepository
import com.hanan.mysavings.data.repository.RoomTransactionRepository
import com.hanan.mysavings.domain.model.Budget
import com.hanan.mysavings.domain.model.GoalCategory
import com.hanan.mysavings.domain.model.SavingsDeposit
import com.hanan.mysavings.domain.model.SavingsGoal
import com.hanan.mysavings.domain.model.Transaction
import com.hanan.mysavings.domain.model.TransactionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Two phones, one account, one (fake, in-memory) cloud: the whole sync path except Firestore's
 * own network code. Each "phone" has its own Room database and its own sync state.
 */
@RunWith(AndroidJUnit4::class)
class SyncEngineTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val cloud = FakeRemoteStore()
    private val phoneA = Phone("a")
    private val phoneB = Phone("b")

    @After
    fun tearDown() {
        phoneA.close()
        phoneB.close()
    }

    @Test
    fun dataFromOnePhoneArrivesOnTheOtherWithItsRelations() = runTest {
        val goalId = phoneA.savings(at = 10).saveGoal(goal("Laptop"))
        phoneA.savings(at = 11).addDeposit(SavingsDeposit(goalId = goalId, amount = 500, date = LocalDate.of(2026, 10, 1), note = "Gaji", createdAt = Instant.ofEpochMilli(11)))
        phoneA.transactions().add(transaction(amount = 25_000)) // category 1 = "food"
        RoomBudgetRepository(phoneA.db.budgetDao(), clockAt(12)).upsert(Budget(yearMonth = YearMonth.of(2026, 10), categoryId = 1, limitAmount = 300_000))

        phoneA.sync()
        phoneB.sync()

        val goalOnB = phoneB.db.savingsDao().observeGoalsWithSaved().first().single()
        assertEquals("Laptop", goalOnB.goal.name)
        assertEquals(500L, goalOnB.saved) // the deposit found its goal by uuid
        val spending = phoneB.db.transactionDao().observeTotalsByCategory(0, Long.MAX_VALUE).first().single()
        assertEquals(1L to 25_000L, spending.categoryId to spending.total) // category found by its key
        assertEquals(300_000L, phoneB.db.budgetDao().observeForMonth(202610).first().single().limitAmount)
        // Nothing left to upload on either side.
        assertEquals(0, phoneA.db.syncDao().observePendingCount().first())
        assertEquals(0, phoneB.db.syncDao().observePendingCount().first())
    }

    @Test
    fun theNewerEditWinsWhicheverPhoneSyncsFirst() = runTest {
        val goalOnA = phoneA.savings(at = 10).saveGoal(goal("Laptop"))
        phoneA.sync()
        phoneB.sync()
        val goalOnB = phoneB.db.savingsDao().observeGoalsWithSaved().first().single().goal.id

        // Both edit offline; B's edit is the later one, but A uploads after B.
        phoneB.savings(at = 200).saveGoal(goal("Laptop gaming", id = goalOnB))
        phoneA.savings(at = 100).saveGoal(goal("Laptop kerja", id = goalOnA))
        phoneB.sync()
        phoneA.sync()
        phoneB.sync()

        assertEquals("Laptop gaming", phoneA.db.savingsDao().getGoal(goalOnA)!!.name)
        assertEquals("Laptop gaming", phoneB.db.savingsDao().getGoal(goalOnB)!!.name)
    }

    @Test
    fun aDeletionReachesTheOtherPhoneWithTheChildren() = runTest {
        val goalId = phoneA.savings(at = 10).saveGoal(goal("Laptop"))
        phoneA.savings(at = 11).addDeposit(SavingsDeposit(goalId = goalId, amount = 500, date = LocalDate.of(2026, 10, 1), note = null, createdAt = Instant.ofEpochMilli(11)))
        phoneA.sync()
        phoneB.sync()

        phoneA.savings(at = 50).deleteGoal(goalId)
        phoneA.sync()
        phoneB.sync()

        assertTrue(phoneB.db.savingsDao().observeGoalsWithSaved().first().isEmpty())
        assertNull(phoneB.db.query("SELECT 1 FROM savings_deposits", null).use { if (it.moveToFirst()) 1 else null })
        // The tombstones were uploaded, so phone A forgot them.
        assertEquals(0, phoneA.db.syncDao().observePendingCount().first())
    }

    @Test
    fun theProfileNameTravelsToo() = runTest {
        phoneA.db.profileDao().update { it.copy(name = "Louise", sync = it.sync.touched(10)) }

        phoneA.sync()
        phoneB.sync()

        assertEquals("Louise", phoneB.db.profileDao().observe().first()?.name)
    }

    @Test
    fun anotherAccountGetsEverythingUploadedAgain() = runTest {
        phoneA.savings(at = 10).saveGoal(goal("Laptop"))
        phoneA.sync(uid = "account-1")

        phoneA.sync(uid = "account-2")

        assertEquals(1, cloud.count("account-2", "savings_goals"))
    }

    @Test
    fun theProfilePhotoTravelsAndItsRemovalToo() = runTest {
        val jpeg = byteArrayOf(1, 2, 3, 4)
        phoneA.photos.write(jpeg, version = 77)
        phoneA.db.profileDao().update { it.copy(photoUpdatedAt = 77, sync = it.sync.touched(77)) }

        phoneA.sync()
        phoneB.sync()

        assertEquals(77L, phoneB.db.profileDao().observe().first()?.photoUpdatedAt)
        assertArrayEquals(jpeg, phoneB.photos.load(Instant.ofEpochMilli(77)))

        // Removed on B: gone from the cloud and, after its next sync, from A.
        phoneB.db.profileDao().update { it.copy(photoUpdatedAt = null, sync = it.sync.touched(200)) }
        phoneB.sync()
        phoneA.sync()

        assertFalse(cloud.hasPhoto("account-1"))
        assertNull(phoneA.photos.load(Instant.ofEpochMilli(77)))
    }

    @Test
    fun erasingAPhoneLeavesTheCloudAloneAndTheNextSignInBringsEverythingBack() = runTest {
        phoneA.savings(at = 10).saveGoal(goal("Laptop"))
        phoneA.sync()

        phoneA.local.eraseAll()
        phoneA.state.clear()
        assertTrue(phoneA.db.savingsDao().observeGoalsWithSaved().first().isEmpty())
        assertEquals(1, cloud.count("account-1", "savings_goals")) // no tombstone went up

        phoneA.sync()

        assertEquals("Laptop", phoneA.db.savingsDao().observeGoalsWithSaved().first().single().goal.name)
    }

    @Test
    fun deletingTheCloudDataOfOneAccountKeepsOtherAccounts() = runTest {
        phoneA.savings(at = 10).saveGoal(goal("Laptop"))
        phoneA.sync(uid = "account-1")
        phoneB.savings(at = 10).saveGoal(goal("Motor"))
        phoneB.sync(uid = "account-2")

        cloud.deleteAll("account-1")

        assertEquals(0, cloud.count("account-1", "savings_goals"))
        assertEquals(1, cloud.count("account-2", "savings_goals"))
    }

    private fun clockAt(millis: Long) = Clock.fixed(Instant.ofEpochMilli(millis), ZoneOffset.UTC)

    private fun goal(name: String, id: Long = 0) =
        SavingsGoal(id = id, name = name, targetAmount = 1_000, category = GoalCategory.GADGET, targetDate = null, createdAt = Instant.ofEpochMilli(10))

    private fun transaction(amount: Long) = Transaction(
        amount = amount,
        date = LocalDate.of(2026, 10, 2),
        merchant = "Warung",
        note = null,
        categoryId = 1,
        source = TransactionSource.MANUAL,
        createdAt = Instant.ofEpochMilli(12),
    )

    private inner class Phone(name: String) {
        val db: MySavingsDatabase = Room.inMemoryDatabaseBuilder(context, MySavingsDatabase::class.java)
            .addCallback(MySavingsDatabase.SeedCallback)
            .allowMainThreadQueries()
            .build()
        private val prefsName = "sync-test-$name"
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).apply { edit().clear().commit() }
        private val photoDir = File(context.cacheDir, "sync-test-photos-$name").apply { deleteRecursively() }
        val photos = AndroidProfilePhotoStore(context, photoDir, Dispatchers.IO)
        val local = LocalSyncStore(db)
        val state = SyncStateStore(prefs)
        private val engine = SyncEngine(local, cloud, state, photos, Dispatchers.IO)

        fun savings(at: Long) = RoomSavingsRepository(db.savingsDao(), clockAt(at))

        fun transactions() = RoomTransactionRepository(db.transactionDao())

        suspend fun sync(uid: String = "account-1") = engine.sync(uid)

        // The test runs inside the app's process: remove its files so none are left in the real app's data.
        fun close() {
            db.close()
            // commit() waits for every earlier apply() of SyncStateStore, so none can recreate the file afterwards.
            prefs.edit().clear().commit()
            context.deleteSharedPreferences(prefsName)
            photoDir.deleteRecursively()
        }
    }
}

/** The cloud in memory. It stamps each upload with its own increasing "server time", like Firestore does. */
private class FakeRemoteStore : RemoteStore {
    private var serverClock = 1_000L
    private val docs = mutableMapOf<Triple<String, String, String>, RemoteDoc>()
    private val photos = mutableMapOf<String, RemotePhoto>()

    override suspend fun push(uid: String, collection: String, docs: List<RemoteDoc>) {
        docs.forEach { this.docs[Triple(uid, collection, it.uuid)] = it.copy(serverTime = ++serverClock) }
    }

    override suspend fun pull(uid: String, collection: String, sinceServerTime: Long): List<RemoteDoc> =
        docs.filterKeys { it.first == uid && it.second == collection }.values
            .filter { it.serverTime!! >= sinceServerTime }
            .sortedBy { it.serverTime }

    override suspend fun uploadPhoto(uid: String, version: Long, bytes: ByteArray) {
        photos[uid] = RemotePhoto(version, bytes)
    }

    override suspend fun downloadPhoto(uid: String): RemotePhoto? = photos[uid]

    override suspend fun deletePhoto(uid: String) {
        photos.remove(uid)
    }

    override suspend fun deleteAll(uid: String) {
        docs.keys.removeAll { it.first == uid }
        photos.remove(uid)
    }

    fun count(uid: String, collection: String) = docs.keys.count { it.first == uid && it.second == collection }

    fun hasPhoto(uid: String) = uid in photos
}
