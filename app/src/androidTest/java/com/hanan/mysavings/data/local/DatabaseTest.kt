package com.hanan.mysavings.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hanan.mysavings.data.local.entity.BudgetAlertEntity
import com.hanan.mysavings.data.local.entity.BudgetEntity
import com.hanan.mysavings.data.local.entity.TransactionEntity
import com.hanan.mysavings.domain.model.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Runs real SQL against an in-memory database, including the seed callback used in production. */
@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: MySavingsDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MySavingsDatabase::class.java)
            .addCallback(MySavingsDatabase.SeedCallback)
            .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun defaultCategoriesAreSeededOnCreate() = runTest {
        val categories = db.categoryDao().observeAll().first()

        assertEquals(DefaultCategories.all.map { it.name }, categories.map { it.name })
        assertTrue(categories.any { it.iconKey == Category.OTHER_ICON_KEY })
    }

    @Test
    fun totalsByCategoryOnlyIncludeTheRequestedRange() = runTest {
        val dao = db.transactionDao()
        dao.insert(transaction(amount = 10_000, date = LocalDate.of(2026, 10, 1), categoryId = 1))
        dao.insert(transaction(amount = 15_000, date = LocalDate.of(2026, 10, 31), categoryId = 1))
        dao.insert(transaction(amount = 40_000, date = LocalDate.of(2026, 10, 15), categoryId = 2))
        dao.insert(transaction(amount = 99_000, date = LocalDate.of(2026, 9, 30), categoryId = 1))
        dao.insert(transaction(amount = 99_000, date = LocalDate.of(2026, 11, 1), categoryId = 2))

        val totals = dao.observeTotalsByCategory(
            LocalDate.of(2026, 10, 1).toEpochDay(),
            LocalDate.of(2026, 10, 31).toEpochDay(),
        ).first()

        assertEquals(mapOf(1L to 25_000L, 2L to 40_000L), totals.associate { it.categoryId to it.total })
    }

    @Test
    fun monthlyTotalsGroupByCalendarMonth() = runTest {
        val dao = db.transactionDao()
        dao.insert(transaction(amount = 10_000, date = LocalDate.of(2026, 9, 1)))
        dao.insert(transaction(amount = 15_000, date = LocalDate.of(2026, 9, 30)))
        dao.insert(transaction(amount = 40_000, date = LocalDate.of(2026, 10, 1)))
        dao.insert(transaction(amount = 7_000, date = LocalDate.of(2025, 12, 31)))
        dao.insert(transaction(amount = 99_000, date = LocalDate.of(2026, 11, 1))) // outside the range

        val totals = dao.observeMonthlyTotals(
            LocalDate.of(2025, 12, 1).toEpochDay(),
            LocalDate.of(2026, 10, 31).toEpochDay(),
        ).first()

        assertEquals(
            mapOf(202512 to 7_000L, 202609 to 25_000L, 202610 to 40_000L),
            totals.associate { it.yearMonth to it.total },
        )
    }

    @Test
    fun dailyTotalsGroupByDayWithinTheRange() = runTest {
        val dao = db.transactionDao()
        dao.insert(transaction(amount = 10_000, date = LocalDate.of(2026, 10, 23)))
        dao.insert(transaction(amount = 15_000, date = LocalDate.of(2026, 10, 29)))
        dao.insert(transaction(amount = 5_000, date = LocalDate.of(2026, 10, 29), categoryId = 2))
        dao.insert(transaction(amount = 99_000, date = LocalDate.of(2026, 10, 22))) // outside the range

        val totals = dao.observeDailyTotals(
            LocalDate.of(2026, 10, 23).toEpochDay(),
            LocalDate.of(2026, 10, 29).toEpochDay(),
        ).first()

        assertEquals(
            mapOf(LocalDate.of(2026, 10, 23).toEpochDay() to 10_000L, LocalDate.of(2026, 10, 29).toEpochDay() to 20_000L),
            totals.associate { it.epochDay to it.total },
        )
    }

    @Test
    fun countIncludesBothEndsOfTheRange() = runTest {
        val dao = db.transactionDao()
        dao.insert(transaction(date = LocalDate.of(2026, 10, 1)))
        dao.insert(transaction(date = LocalDate.of(2026, 10, 31)))
        dao.insert(transaction(date = LocalDate.of(2026, 10, 15), categoryId = 2))
        dao.insert(transaction(date = LocalDate.of(2026, 9, 30)))
        dao.insert(transaction(date = LocalDate.of(2026, 11, 1)))

        val count = dao.observeCount(
            LocalDate.of(2026, 10, 1).toEpochDay(),
            LocalDate.of(2026, 10, 31).toEpochDay(),
        ).first()

        assertEquals(3, count)
    }

    @Test
    fun inRangeIsNewestFirstAndLimitedToTheRange() = runTest {
        val dao = db.transactionDao()
        val older = dao.insert(transaction(date = LocalDate.of(2026, 10, 1), createdAt = 3))
        val sameDayFirst = dao.insert(transaction(date = LocalDate.of(2026, 10, 5), createdAt = 1))
        val sameDaySecond = dao.insert(transaction(date = LocalDate.of(2026, 10, 5), createdAt = 2))
        dao.insert(transaction(date = LocalDate.of(2026, 9, 30))) // outside the range

        val inRange = dao.observeInRange(
            LocalDate.of(2026, 10, 1).toEpochDay(),
            LocalDate.of(2026, 10, 31).toEpochDay(),
        ).first()

        assertEquals(listOf(sameDaySecond, sameDayFirst, older), inRange.map { it.id })
    }

    @Test(expected = Exception::class)
    fun transactionWithUnknownCategoryIsRejected() = runTest {
        db.transactionDao().insert(transaction(categoryId = 999))
    }

    @Test
    fun upsertReplacesOverallBudgetInsteadOfDuplicatingIt() = runTest {
        val dao = db.budgetDao()
        dao.upsert(BudgetEntity(yearMonth = 202610, categoryId = null, limitAmount = 1_000_000))
        dao.upsert(BudgetEntity(yearMonth = 202610, categoryId = null, limitAmount = 2_000_000))

        val budgets = dao.observeForMonth(202610).first()

        assertEquals(1, budgets.size)
        assertEquals(2_000_000L, budgets.single().limitAmount)
    }

    @Test
    fun budgetsAreSeparatePerCategoryAndPerMonth() = runTest {
        val dao = db.budgetDao()
        dao.upsert(BudgetEntity(yearMonth = 202610, categoryId = null, limitAmount = 3_000_000))
        dao.upsert(BudgetEntity(yearMonth = 202610, categoryId = 1, limitAmount = 800_000))
        dao.upsert(BudgetEntity(yearMonth = 202610, categoryId = 1, limitAmount = 900_000))
        dao.upsert(BudgetEntity(yearMonth = 202611, categoryId = null, limitAmount = 2_500_000))

        val october = dao.observeForMonth(202610).first().associate { it.categoryId to it.limitAmount }

        assertEquals(mapOf(null to 3_000_000L, 1L to 900_000L), october)
    }

    @Test
    fun budgetAlertsAreRecordedOncePerMonthAndClearedPerMonth() = runTest {
        val dao = db.budgetAlertDao()
        dao.insertAll(listOf(BudgetAlertEntity(202610, 80, sentAt = 1)))
        dao.insertAll(listOf(BudgetAlertEntity(202610, 80, sentAt = 2), BudgetAlertEntity(202610, 100, sentAt = 2)))
        dao.insertAll(listOf(BudgetAlertEntity(202609, 80, sentAt = 3)))

        assertEquals(setOf(80, 100), dao.sentThresholds(202610).toSet())

        dao.clear(202610)

        assertEquals(emptyList<Int>(), dao.sentThresholds(202610))
        assertEquals(listOf(80), dao.sentThresholds(202609))
    }

    private fun transaction(
        amount: Long = 10_000,
        date: LocalDate = LocalDate.of(2026, 10, 10),
        categoryId: Long = 1,
        createdAt: Long = 0,
    ) = TransactionEntity(
        amount = amount,
        date = date.toEpochDay(),
        merchant = null,
        note = null,
        categoryId = categoryId,
        source = "MANUAL",
        createdAt = createdAt,
    )
}
