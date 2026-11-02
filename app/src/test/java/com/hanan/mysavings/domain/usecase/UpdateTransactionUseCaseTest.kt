package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.Transaction
import com.hanan.mysavings.domain.model.TransactionSource
import com.hanan.mysavings.domain.repository.BudgetCheckScheduler
import com.hanan.mysavings.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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

class UpdateTransactionUseCaseTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-29T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private val today = LocalDate.of(2026, 10, 29)

    private val existing = Transaction(
        id = 7,
        amount = 87_500,
        date = LocalDate.of(2026, 10, 12),
        merchant = "Indomaret",
        note = "Belanja mingguan",
        categoryId = 2,
        source = TransactionSource.SCAN,
        createdAt = Instant.parse("2026-10-12T05:00:00Z"),
    )

    private val repository = mockk<TransactionRepository> {
        coEvery { get(7) } returns existing
        coEvery { update(any()) } just runs
    }
    private val scheduler = mockk<BudgetCheckScheduler>(relaxed = true)
    private val updateTransaction = UpdateTransactionUseCase(repository, scheduler, clock)

    @Test
    fun `edited fields are saved while source and createdAt stay the same`() = runTest {
        val updated = slot<Transaction>()
        coEvery { repository.update(capture(updated)) } just runs

        val result = updateTransaction(
            id = 7,
            amount = 90_000,
            date = today,
            merchant = "  Alfamart ",
            note = "  ",
            categoryId = 3,
        )

        assertEquals(SaveTransactionResult.Success(7), result)
        with(updated.captured) {
            assertEquals(7L, id)
            assertEquals(90_000L, amount)
            assertEquals(today, date)
            assertEquals("Alfamart", merchant)
            assertNull(note)
            assertEquals(3L, categoryId)
            assertEquals(TransactionSource.SCAN, source)
            assertEquals(existing.createdAt, createdAt)
        }
        verify(exactly = 1) { scheduler.requestCheck() }
    }

    @Test
    fun `invalid values are rejected before anything is read or written`() = runTest {
        assertEquals(
            SaveTransactionResult.Invalid(TransactionError.AMOUNT_NOT_POSITIVE),
            updateTransaction(7, 0, today, null, null, 2),
        )
        assertEquals(
            SaveTransactionResult.Invalid(TransactionError.DATE_IN_FUTURE),
            updateTransaction(7, 10_000, today.plusDays(1), null, null, 2),
        )
        coVerify(exactly = 0) { repository.get(any()) }
        coVerify(exactly = 0) { repository.update(any()) }
        verify(exactly = 0) { scheduler.requestCheck() }
    }

    @Test
    fun `a transaction deleted meanwhile is reported as not found`() = runTest {
        coEvery { repository.get(7) } returns null

        val result = updateTransaction(7, 10_000, today, null, null, 2)

        assertEquals(SaveTransactionResult.Invalid(TransactionError.NOT_FOUND), result)
        coVerify(exactly = 0) { repository.update(any()) }
    }
}
