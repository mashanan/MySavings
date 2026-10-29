package com.hanan.strukku.domain.usecase

import com.hanan.strukku.domain.model.Transaction
import com.hanan.strukku.domain.model.TransactionSource
import com.hanan.strukku.domain.repository.BudgetCheckScheduler
import com.hanan.strukku.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AddTransactionUseCaseTest {

    private val now = Instant.parse("2026-10-29T03:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("Asia/Jakarta"))
    private val today = LocalDate.of(2026, 10, 29)

    private val repository = mockk<TransactionRepository>()
    private val scheduler = mockk<BudgetCheckScheduler>(relaxed = true)
    private val addTransaction = AddTransactionUseCase(repository, scheduler, clock)

    @Test
    fun `valid transaction is saved with trimmed text and createdAt from clock`() = runTest {
        val saved = slot<Transaction>()
        coEvery { repository.add(capture(saved)) } returns 42L

        val result = addTransaction(
            amount = 25_000,
            date = today,
            merchant = "  Indomaret  ",
            note = "   ",
            categoryId = 2,
            source = TransactionSource.SCAN,
        )

        assertEquals(AddTransactionResult.Success(42L), result)
        with(saved.captured) {
            assertEquals(25_000L, amount)
            assertEquals(today, date)
            assertEquals("Indomaret", merchant)
            assertNull(note)
            assertEquals(2L, categoryId)
            assertEquals(TransactionSource.SCAN, source)
            assertEquals(now, createdAt)
        }
        verify(exactly = 1) { scheduler.requestCheck() }
    }

    @Test
    fun `zero or negative amount is rejected and nothing is saved`() = runTest {
        listOf(0L, -5_000L).forEach { amount ->
            val result = addTransaction(amount, today, "Toko", null, 1, TransactionSource.MANUAL)
            assertEquals(AddTransactionResult.Invalid(TransactionError.AMOUNT_NOT_POSITIVE), result)
        }
        coVerify(exactly = 0) { repository.add(any()) }
        verify(exactly = 0) { scheduler.requestCheck() }
    }

    @Test
    fun `future date is rejected`() = runTest {
        val result = addTransaction(10_000, today.plusDays(1), null, null, 1, TransactionSource.MANUAL)

        assertEquals(AddTransactionResult.Invalid(TransactionError.DATE_IN_FUTURE), result)
        coVerify(exactly = 0) { repository.add(any()) }
    }

    @Test
    fun `past date is accepted`() = runTest {
        coEvery { repository.add(any()) } returns 1L

        val result = addTransaction(10_000, today.minusMonths(2), null, null, 1, TransactionSource.MANUAL)

        assertEquals(AddTransactionResult.Success(1L), result)
    }
}
