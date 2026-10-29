package com.hanan.mysavings.domain.usecase

import app.cash.turbine.test
import com.hanan.mysavings.domain.model.Category
import com.hanan.mysavings.domain.model.Transaction
import com.hanan.mysavings.domain.model.TransactionSource
import com.hanan.mysavings.domain.repository.CategoryRepository
import com.hanan.mysavings.domain.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class ObserveTransactionsByDayUseCaseTest {

    private val food = Category(1, "Makan & Minum", "food", "#000000", emptyList(), true)
    private val groceries = Category(2, "Belanja Harian", "groceries", "#000000", emptyList(), true)
    private val categories = listOf(food, groceries)

    private val transactionRepository = mockk<TransactionRepository>()
    private val categoryRepository = mockk<CategoryRepository> {
        every { observeAll() } returns flowOf(categories)
    }
    private val observeByDay = ObserveTransactionsByDayUseCase(transactionRepository, categoryRepository)

    @Test
    fun `groups newest-first transactions into days with their totals`() {
        val days = ObserveTransactionsByDayUseCase.groupByDay(
            listOf(
                transaction(id = 4, day = 29, amount = 35_000, categoryId = 1),
                transaction(id = 3, day = 29, amount = 87_500, categoryId = 2),
                transaction(id = 2, day = 27, amount = 20_000, categoryId = 1),
                transaction(id = 1, day = 3, amount = 120_000, categoryId = 2),
            ),
            categories,
        )

        assertEquals(listOf(29, 27, 3), days.map { it.date.dayOfMonth })
        assertEquals(listOf(122_500L, 20_000L, 120_000L), days.map { it.total })
        assertEquals(listOf(4L, 3L), days.first().transactions.map { it.transaction.id })
        assertEquals(listOf(food, groceries), days.first().transactions.map { it.category })
    }

    @Test
    fun `a transaction with an unknown category is left out, also from the total`() {
        val days = ObserveTransactionsByDayUseCase.groupByDay(
            listOf(
                transaction(id = 2, day = 29, amount = 35_000, categoryId = 1),
                transaction(id = 1, day = 29, amount = 99_000, categoryId = 999),
            ),
            categories,
        )

        assertEquals(35_000L, days.single().total)
        assertEquals(listOf(2L), days.single().transactions.map { it.transaction.id })
    }

    @Test
    fun `no transactions gives no days`() {
        assertTrue(ObserveTransactionsByDayUseCase.groupByDay(emptyList(), categories).isEmpty())
    }

    @Test
    fun `observes the full calendar month and updates on change`() = runTest {
        val transactions = MutableStateFlow(listOf(transaction(id = 1, day = 3, amount = 10_000, categoryId = 1)))
        every { transactionRepository.observeInRange(any(), any()) } returns transactions

        observeByDay(YearMonth.of(2026, 10)).test {
            assertEquals(1, awaitItem().size)

            transactions.value = listOf(transaction(id = 2, day = 5, amount = 5_000, categoryId = 2)) + transactions.value

            assertEquals(listOf(5, 3), awaitItem().map { it.date.dayOfMonth })
            cancelAndIgnoreRemainingEvents()
        }
        verify { transactionRepository.observeInRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)) }
    }

    private fun transaction(id: Long, day: Int, amount: Long, categoryId: Long) = Transaction(
        id = id,
        amount = amount,
        date = LocalDate.of(2026, 10, day),
        merchant = null,
        note = null,
        categoryId = categoryId,
        source = TransactionSource.MANUAL,
        createdAt = Instant.EPOCH,
    )
}
