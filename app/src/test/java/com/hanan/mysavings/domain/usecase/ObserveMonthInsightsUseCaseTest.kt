package com.hanan.mysavings.domain.usecase

import app.cash.turbine.test
import com.hanan.mysavings.domain.model.Category
import com.hanan.mysavings.domain.model.CategorySpending
import com.hanan.mysavings.domain.model.DailyAllowance
import com.hanan.mysavings.domain.model.MonthInsights
import com.hanan.mysavings.domain.model.MonthlySummary
import com.hanan.mysavings.domain.model.PeriodComparison
import com.hanan.mysavings.domain.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ObserveMonthInsightsUseCaseTest {

    private val october = YearMonth.of(2026, 10)
    private val food = Category(1, "Makan & Minum", "food", "#000000", emptyList(), true)
    private val groceries = Category(2, "Belanja Harian", "groceries", "#000000", emptyList(), true)

    private val previousSpending = MutableStateFlow(mapOf(1L to 300_000L, 2L to 200_000L))
    private val transactionRepository = mockk<TransactionRepository>()
    private val observeInsights = ObserveMonthInsightsUseCase(transactionRepository)

    @Before
    fun setUp() {
        every { transactionRepository.observeCount(any(), any()) } returns flowOf(7)
        every { transactionRepository.observeSpendingByCategory(any(), any()) } returns previousSpending
    }

    @Test
    fun `counts the whole month and compares with the same days of last month`() = runTest {
        insights(summary(totalSpent = 580_000), today = LocalDate.of(2026, 10, 29))

        verify {
            transactionRepository.observeCount(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
            transactionRepository.observeSpendingByCategory(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 29))
        }
    }

    @Test
    fun `mid-month insights use the days so far`() = runTest {
        val summary = summary(totalSpent = 580_000, budget = 1_000_000)

        val insights = insights(summary, today = LocalDate.of(2026, 10, 29))

        assertEquals(7, insights.transactionCount)
        assertEquals(20_000L, insights.averagePerDay) // 580.000 / 29 days
        assertEquals(groceries, insights.topCategory?.category)
        // 420.000 left for 29, 30 and 31 October, rounded down.
        assertEquals(DailyAllowance(amountPerDay = 140_000, daysLeft = 3), insights.dailyAllowance)
        assertEquals(PeriodComparison(previousTotal = 500_000, changePercent = 16), insights.comparison)
    }

    @Test
    fun `on the last day the whole remaining budget is for today`() = runTest {
        val insights = insights(summary(totalSpent = 900_000, budget = 1_000_000), today = LocalDate.of(2026, 10, 31))

        assertEquals(DailyAllowance(amountPerDay = 100_000, daysLeft = 1), insights.dailyAllowance)
    }

    @Test
    fun `allowance is rounded down so it never exceeds the budget`() = runTest {
        // 100.000 over 3 days is 33.333,33...
        val insights = insights(summary(totalSpent = 900_000, budget = 1_000_000), today = LocalDate.of(2026, 10, 29))

        assertEquals(33_333L, insights.dailyAllowance?.amountPerDay)
    }

    @Test
    fun `no allowance without a budget or once the budget is used up`() = runTest {
        val today = LocalDate.of(2026, 10, 29)

        assertNull(insights(summary(totalSpent = 580_000, budget = null), today).dailyAllowance)
        assertNull(insights(summary(totalSpent = 1_000_000, budget = 1_000_000), today).dailyAllowance)
        assertNull(insights(summary(totalSpent = 1_200_000, budget = 1_000_000), today).dailyAllowance)
    }

    @Test
    fun `a past month has no allowance and compares full months`() = runTest {
        val september = summary(month = YearMonth.of(2026, 9), totalSpent = 600_000, budget = 1_000_000)

        val insights = insights(september, today = LocalDate.of(2026, 10, 29))

        assertNull(insights.dailyAllowance)
        assertEquals(20_000L, insights.averagePerDay) // 600.000 / 30 days
        verify { transactionRepository.observeSpendingByCategory(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)) }
    }

    @Test
    fun `comparison stops at the end of a shorter previous month`() = runTest {
        val march = summary(month = YearMonth.of(2028, 3), totalSpent = 100_000)

        insights(march, today = LocalDate.of(2028, 3, 31))

        // 2028 is a leap year, so February ends on the 29th.
        verify { transactionRepository.observeSpendingByCategory(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29)) }
    }

    @Test
    fun `on the last day of a short month the whole previous month is compared`() = runTest {
        val september = summary(month = YearMonth.of(2026, 9), totalSpent = 100_000)

        insights(september, today = LocalDate.of(2026, 9, 30))

        verify { transactionRepository.observeSpendingByCategory(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)) }
    }

    @Test
    fun `no comparison when last month had no spending`() = runTest {
        previousSpending.value = emptyMap()

        assertNull(insights(summary(totalSpent = 580_000), today = LocalDate.of(2026, 10, 29)).comparison)
    }

    @Test
    fun `spending less than last month gives a negative change`() = runTest {
        val insights = insights(summary(totalSpent = 410_000), today = LocalDate.of(2026, 10, 29))

        assertEquals(-18, insights.comparison?.changePercent)
    }

    @Test
    fun `an empty month has no top category and an average of zero`() = runTest {
        every { transactionRepository.observeCount(any(), any()) } returns flowOf(0)
        val empty = MonthlySummary(october, 0, null, emptyList(), emptyList())

        val insights = insights(empty, today = LocalDate.of(2026, 10, 1))

        assertEquals(0, insights.transactionCount)
        assertEquals(0L, insights.averagePerDay)
        assertNull(insights.topCategory)
    }

    @Test
    fun `emits again when last month's spending changes`() = runTest {
        observeInsights(summary(totalSpent = 500_000), LocalDate.of(2026, 10, 29)).test {
            assertEquals(0, awaitItem().comparison?.changePercent)

            previousSpending.value = mapOf(1L to 1_000_000L)

            assertEquals(-50, awaitItem().comparison?.changePercent)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun insights(summary: MonthlySummary, today: LocalDate): MonthInsights {
        var result: MonthInsights? = null
        observeInsights(summary, today).test {
            result = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        return result!!
    }

    /** Splits [totalSpent] 3:1 between groceries and food, so groceries is always the top category. */
    private fun summary(month: YearMonth = october, totalSpent: Long, budget: Long? = null) = MonthlySummary(
        yearMonth = month,
        totalSpent = totalSpent,
        budgetLimit = budget,
        spendingByCategory = listOf(
            CategorySpending(groceries, totalSpent - totalSpent / 4, null),
            CategorySpending(food, totalSpent / 4, null),
        ),
        recentTransactions = emptyList(),
    )
}
