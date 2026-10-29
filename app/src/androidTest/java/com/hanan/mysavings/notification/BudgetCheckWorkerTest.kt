package com.hanan.mysavings.notification

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.hanan.mysavings.data.local.MySavingsDatabase
import com.hanan.mysavings.data.local.entity.BudgetEntity
import com.hanan.mysavings.data.local.entity.TransactionEntity
import com.hanan.mysavings.data.repository.RoomBudgetAlertRepository
import com.hanan.mysavings.data.repository.RoomBudgetRepository
import com.hanan.mysavings.data.repository.RoomTransactionRepository
import com.hanan.mysavings.domain.model.BudgetAlert
import com.hanan.mysavings.domain.model.BudgetAlertLevel
import com.hanan.mysavings.domain.usecase.CheckBudgetAlertUseCase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Runs the real worker against a real (in-memory) database and the real use case; only the
 * notifier is replaced, so the test checks the whole background path without posting
 * anything to the phone's notification shade.
 */
@RunWith(AndroidJUnit4::class)
class BudgetCheckWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = Clock.fixed(Instant.parse("2026-10-29T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private val shown = mutableListOf<BudgetAlert>()
    private lateinit var db: MySavingsDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, MySavingsDatabase::class.java)
            .addCallback(MySavingsDatabase.SeedCallback)
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun worker(): BudgetCheckWorker {
        val checkAlert = CheckBudgetAlertUseCase(
            RoomTransactionRepository(db.transactionDao()),
            RoomBudgetRepository(db.budgetDao()),
            RoomBudgetAlertRepository(db.budgetAlertDao(), clock),
        )
        return TestListenableWorkerBuilder<BudgetCheckWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    BudgetCheckWorker(appContext, workerParameters, checkAlert, { shown += it }, clock)
            })
            .build()
    }

    private suspend fun spend(amount: Long) {
        db.transactionDao().insert(
            TransactionEntity(
                amount = amount,
                date = LocalDate.of(2026, 10, 15).toEpochDay(),
                merchant = null,
                note = null,
                categoryId = 1,
                source = "MANUAL",
                createdAt = 0,
            ),
        )
    }

    @Test
    fun notifiesOncePerThresholdAcrossRuns() = runTest {
        db.budgetDao().upsert(BudgetEntity(yearMonth = 202610, categoryId = null, limitAmount = 1_000_000))
        spend(850_000)

        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertEquals(listOf(BudgetAlertLevel.NEARLY_USED), shown.map { it.level })

        // Daily run without new spending: nothing new to say.
        worker().doWork()
        assertEquals(1, shown.size)

        spend(200_000)
        worker().doWork()
        assertEquals(listOf(BudgetAlertLevel.NEARLY_USED, BudgetAlertLevel.USED_UP), shown.map { it.level })
    }

    @Test
    fun withoutBudgetTheWorkerSucceedsQuietly() = runTest {
        spend(5_000_000)

        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertEquals(emptyList<BudgetAlert>(), shown)
    }
}
