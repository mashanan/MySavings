package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.Category
import com.hanan.mysavings.domain.model.CategorySpending
import com.hanan.mysavings.domain.model.MonthlySummary
import com.hanan.mysavings.domain.model.Transaction
import com.hanan.mysavings.domain.model.TransactionSource
import com.hanan.mysavings.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class BuildMonthlyReportUseCaseTest {

    private val october = YearMonth.of(2026, 10)
    private val food = Category(1, "Makan & Minum", "food", "#E57373", emptyList(), true)
    private val groceries = Category(2, "Belanja Harian", "groceries", "#4DB6AC", emptyList(), true)

    private val summary = MonthlySummary(
        yearMonth = october,
        totalSpent = 130_000,
        budgetLimit = 100_000,
        spendingByCategory = listOf(CategorySpending(groceries, 100_000, null), CategorySpending(food, 30_000, null)),
    )
    private val observeSummary = mockk<ObserveMonthlySummaryUseCase> {
        every { this@mockk.invoke(october) } returns flowOf(summary)
    }
    private val transactionRepository = mockk<TransactionRepository>()
    private val buildReport = BuildMonthlyReportUseCase(observeSummary, transactionRepository)

    private fun transaction(id: Long, day: Int, amount: Long, categoryId: Long) = Transaction(
        id, amount, LocalDate.of(2026, 10, day), "Toko $id", null, categoryId, TransactionSource.MANUAL, Instant.EPOCH,
    )

    @Test
    fun `report combines the month summary with every transaction of the month`() = runTest {
        coEvery { transactionRepository.getInRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)) } returns listOf(
            transaction(1, day = 3, amount = 30_000, categoryId = 1),
            transaction(2, day = 20, amount = 100_000, categoryId = 2),
        )

        val report = buildReport(october)

        assertEquals(october, report.yearMonth)
        assertEquals(130_000L, report.totalSpent)
        assertEquals(-30_000L, report.remaining)
        assertEquals(summary.spendingByCategory, report.spendingByCategory)
        assertEquals(listOf(1L to food, 2L to groceries), report.transactions.map { it.transaction.id to it.category })
    }

    @Test
    fun `empty month gives an empty but valid report`() = runTest {
        every { observeSummary(october) } returns flowOf(MonthlySummary(october, 0, null, emptyList()))
        coEvery { transactionRepository.getInRange(any(), any()) } returns emptyList()

        val report = buildReport(october)

        assertEquals(0L, report.totalSpent)
        assertEquals(null, report.remaining)
        assertEquals(emptyList<Any>(), report.transactions)
    }
}
