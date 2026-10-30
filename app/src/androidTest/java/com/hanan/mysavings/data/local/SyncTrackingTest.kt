package com.hanan.mysavings.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hanan.mysavings.data.local.entity.SyncCollection
import com.hanan.mysavings.data.repository.RoomBudgetRepository
import com.hanan.mysavings.data.repository.RoomDebtRepository
import com.hanan.mysavings.data.repository.RoomSavingsRepository
import com.hanan.mysavings.domain.model.Budget
import com.hanan.mysavings.domain.model.Debt
import com.hanan.mysavings.domain.model.DebtPayment
import com.hanan.mysavings.domain.model.GoalCategory
import com.hanan.mysavings.domain.model.SavingsDeposit
import com.hanan.mysavings.domain.model.SavingsGoal
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * The repositories keep the sync bookkeeping right without any sync running yet: edits keep
 * the uuid and mark the row for upload, deletes leave tombstones, budgets get stable uuids.
 */
@RunWith(AndroidJUnit4::class)
class SyncTrackingTest {

    private lateinit var db: MySavingsDatabase
    private val clock = Clock.fixed(Instant.ofEpochMilli(5_000), ZoneOffset.UTC)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MySavingsDatabase::class.java)
            .addCallback(MySavingsDatabase.SeedCallback)
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun tombstones(): Set<Pair<String, String>> =
        db.query("SELECT collection, uuid FROM sync_tombstones", null).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getString(1)) }
        }

    @Test
    fun editingAGoalKeepsItsUuidAndMarksItForUpload() = runTest {
        val repository = RoomSavingsRepository(db.savingsDao(), clock)
        val goal = SavingsGoal(name = "Laptop", targetAmount = 1_000, category = GoalCategory.GADGET, targetDate = null, createdAt = Instant.EPOCH)
        val id = repository.saveGoal(goal)
        val before = db.savingsDao().getGoal(id)!!.sync

        repository.saveGoal(goal.copy(id = id, name = "Laptop gaming"))

        val after = db.savingsDao().getGoal(id)!!.sync
        assertEquals(before.uuid, after.uuid)
        assertEquals(5_000L, after.updatedAt)
        assertTrue(after.dirty)
    }

    @Test
    fun deletingAGoalLeavesTombstonesForItAndItsDeposits() = runTest {
        val repository = RoomSavingsRepository(db.savingsDao(), clock)
        val goalId = repository.saveGoal(
            SavingsGoal(name = "Laptop", targetAmount = 1_000, category = GoalCategory.GADGET, targetDate = null, createdAt = Instant.EPOCH),
        )
        val depositId = repository.addDeposit(SavingsDeposit(goalId = goalId, amount = 100, date = LocalDate.of(2026, 10, 1), note = null, createdAt = Instant.EPOCH))
        val goalUuid = db.savingsDao().getGoal(goalId)!!.sync.uuid
        val depositUuid = db.savingsDao().getDeposit(depositId)!!.sync.uuid

        repository.deleteGoal(goalId)

        assertEquals(
            setOf(SyncCollection.SAVINGS_GOALS to goalUuid, SyncCollection.SAVINGS_DEPOSITS to depositUuid),
            tombstones(),
        )
    }

    @Test
    fun deletingAPaymentLeavesOneTombstoneAndKeepsTheDebt() = runTest {
        val repository = RoomDebtRepository(db.debtDao(), clock)
        val debtId = repository.saveDebt(Debt(creditor = "Budi", totalAmount = 500, firstDueDate = LocalDate.of(2026, 11, 5), installmentCount = 1, note = null, createdAt = Instant.EPOCH))
        val paymentId = repository.addPayment(DebtPayment(debtId = debtId, amount = 100, date = LocalDate.of(2026, 10, 1), note = null, createdAt = Instant.EPOCH))
        val paymentUuid = db.debtDao().getPayment(paymentId)!!.sync.uuid

        repository.deletePayment(paymentId)

        assertEquals(setOf(SyncCollection.DEBT_PAYMENTS to paymentUuid), tombstones())
    }

    @Test
    fun budgetsGetAStableUuidAndAChangedLimitIsMarkedForUpload() = runTest {
        val repository = RoomBudgetRepository(db.budgetDao(), clock)
        val october = YearMonth.of(2026, 10)

        repository.upsert(Budget(yearMonth = october, categoryId = null, limitAmount = 1_000))
        repository.upsert(Budget(yearMonth = october, categoryId = 1, limitAmount = 300)) // category 1 = "food"
        db.openHelper.writableDatabase.execSQL("UPDATE budgets SET dirty = 0") // as if uploaded
        repository.upsert(Budget(yearMonth = october, categoryId = null, limitAmount = 2_000))

        db.query("SELECT uuid, dirty FROM budgets ORDER BY id", null).use { cursor ->
            cursor.moveToNext()
            assertEquals("budget-202610-total", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
            cursor.moveToNext()
            assertEquals("budget-202610-food", cursor.getString(0))
        }
    }
}
