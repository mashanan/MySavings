package com.hanan.mysavings.domain.usecase

import app.cash.turbine.test
import com.hanan.mysavings.domain.model.Budget
import com.hanan.mysavings.domain.model.Category
import com.hanan.mysavings.domain.model.Transaction
import com.hanan.mysavings.domain.model.TransactionSource
import com.hanan.mysavings.domain.repository.BudgetRepository
import com.hanan.mysavings.domain.repository.CategoryRepository
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
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class ObserveMonthlySummaryUseCaseTest {

    private val october = YearMonth.of(2026, 10)
    private val food = category(1, "Makan & Minum")
    private val groceries = category(2, "Belanja Harian")
    private val transport = category(3, "Transportasi")

    private val spending = MutableStateFlow(mapOf(1L to 150_000L, 2L to 400_000L))
    private val budgets = MutableStateFlow<List<Budget>>(emptyList())

    private val transactionRepository = mockk<TransactionRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val budgetRepository = mockk<BudgetRepository>()

    private val observeSummary =
        ObserveMonthlySummaryUseCase(transactionRepository, categoryRepository, budgetRepository)

    @Before
    fun setUp() {
        every { categoryRepository.observeAll() } returns flowOf(listOf(food, groceries, transport))
        every { transactionRepository.observeSpendingByCategory(any(), any()) } returns spending
        every { budgetRepository.observeForMonth(october) } returns budgets
        every { transactionRepository.observeRecent(any()) } returns flowOf(
            listOf(transaction(id = 9, categoryId = 2), transaction(id = 8, categoryId = 1)),
        )
    }

    @Test
    fun `queries the full calendar month and the recent limit`() = runTest {
        observeSummary(october).test { awaitItem(); cancelAndIgnoreRemainingEvents() }

        verify {
            transactionRepository.observeSpendingByCategory(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
            transactionRepository.observeRecent(ObserveMonthlySummaryUseCase.RECENT_LIMIT)
        }
    }

    @Test
    fun `sums spending, sorts categories largest first and joins recent transactions`() = runTest {
        observeSummary(october).test {
            val summary = awaitItem()

            assertEquals(550_000L, summary.totalSpent)
            assertEquals(listOf(groceries, food), summary.spendingByCategory.map { it.category })
            assertEquals(listOf(400_000L, 150_000L), summary.spendingByCategory.map { it.amount })
            assertEquals(listOf(9L to groceries, 8L to food), summary.recentTransactions.map { it.transaction.id to it.category })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `without a budget remaining and usedFraction are null`() = runTest {
        observeSummary(october).test {
            val summary = awaitItem()

            assertNull(summary.budgetLimit)
            assertNull(summary.remaining)
            assertNull(summary.usedFraction)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `overall and per-category budgets are applied`() = runTest {
        budgets.value = listOf(
            Budget(yearMonth = october, categoryId = null, limitAmount = 2_000_000),
            Budget(yearMonth = october, categoryId = 2, limitAmount = 500_000),
        )

        observeSummary(october).test {
            val summary = awaitItem()

            assertEquals(2_000_000L, summary.budgetLimit)
            assertEquals(1_450_000L, summary.remaining)
            assertEquals(0.275f, summary.usedFraction!!, 0.0001f)
            assertEquals(500_000L, summary.spendingByCategory.first { it.category == groceries }.budgetLimit)
            assertNull(summary.spendingByCategory.first { it.category == food }.budgetLimit)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `remaining goes negative when overspent`() = runTest {
        budgets.value = listOf(Budget(yearMonth = october, categoryId = null, limitAmount = 500_000))

        observeSummary(october).test {
            assertEquals(-50_000L, awaitItem().remaining)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits a new summary when spending changes`() = runTest {
        observeSummary(october).test {
            assertEquals(550_000L, awaitItem().totalSpent)

            spending.value = spending.value + (3L to 50_000L)

            val updated = awaitItem()
            assertEquals(600_000L, updated.totalSpent)
            assertEquals(transport, updated.spendingByCategory.last().category)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun category(id: Long, name: String) = Category(
        id = id,
        name = name,
        iconKey = "icon$id",
        colorHex = "#000000",
        keywords = emptyList(),
        isDefault = true,
    )

    private fun transaction(id: Long, categoryId: Long) = Transaction(
        id = id,
        amount = 10_000,
        date = LocalDate.of(2026, 10, 20),
        merchant = null,
        note = null,
        categoryId = categoryId,
        source = TransactionSource.MANUAL,
        createdAt = Instant.EPOCH,
    )
}
