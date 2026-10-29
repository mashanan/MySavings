package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.Debt
import com.hanan.mysavings.domain.repository.DebtReminderRepository
import com.hanan.mysavings.domain.repository.DebtRepository
import com.hanan.mysavings.domain.usecase.DebtTestData.debt
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SaveDebtUseCaseTest {

    private val saved = slot<Debt>()
    private val debtRepository = mockk<DebtRepository> {
        coEvery { saveDebt(capture(saved)) } answers { saved.captured.id.takeIf { it != 0L } ?: 5L }
    }
    private val reminderRepository = mockk<DebtReminderRepository> {
        coEvery { clear(any()) } just runs
    }
    private val saveDebt = SaveDebtUseCase(debtRepository, reminderRepository, DebtTestData.clock())
    private val due = LocalDate.of(2026, 11, 5)

    @Test
    fun `a new debt is trimmed, stamped and stored`() = runTest {
        val result = saveDebt(null, "  Leasing motor ", 12_000_000, due, 12, "  ")

        assertEquals(SaveDebtResult.Success(5), result)
        assertEquals("Leasing motor", saved.captured.creditor)
        assertEquals(12, saved.captured.installmentCount)
        assertEquals(null, saved.captured.note)
        assertEquals(Instant.parse("2026-10-29T03:00:00Z"), saved.captured.createdAt)
        coVerify(exactly = 0) { reminderRepository.clear(any()) }
    }

    @Test
    fun `a first due date in the past is fine`() = runTest {
        assertEquals(SaveDebtResult.Success(5), saveDebt(null, "KPR", 300_000_000, LocalDate.of(2024, 1, 10), 240, null))
    }

    @Test
    fun `reports every invalid field at once`() = runTest {
        val result = saveDebt(null, " ", 0, due, 0, null)

        assertEquals(
            SaveDebtResult.Invalid(setOf(DebtError.CREDITOR_BLANK, DebtError.AMOUNT_NOT_POSITIVE, DebtError.INSTALLMENTS_OUT_OF_RANGE)),
            result,
        )
        coVerify(exactly = 0) { debtRepository.saveDebt(any()) }
    }

    @Test
    fun `more than 360 installments is refused`() = runTest {
        assertEquals(
            SaveDebtResult.Invalid(DebtError.INSTALLMENTS_OUT_OF_RANGE),
            saveDebt(null, "Bank", 1_000_000, due, 361, null),
        )
    }

    @Test
    fun `editing keeps id and creation time and resets reminders`() = runTest {
        val createdAt = Instant.parse("2026-01-01T00:00:00Z")
        coEvery { debtRepository.getDebt(3) } returns debt(id = 3, createdAt = createdAt)
        coEvery { debtRepository.getPaid(3) } returns 400_000

        val result = saveDebt(3, "Leasing", 1_500_000, due, 3, null)

        assertEquals(SaveDebtResult.Success(3), result)
        assertEquals(createdAt, saved.captured.createdAt)
        coVerify { reminderRepository.clear(3) }
    }

    @Test
    fun `the total cannot drop below what was already paid`() = runTest {
        coEvery { debtRepository.getDebt(3) } returns debt(id = 3)
        coEvery { debtRepository.getPaid(3) } returns 400_000

        assertEquals(SaveDebtResult.Invalid(DebtError.AMOUNT_BELOW_PAID), saveDebt(3, "Leasing", 300_000, due, 1, null))
    }

    @Test
    fun `editing a debt that is gone fails`() = runTest {
        coEvery { debtRepository.getDebt(9) } returns null

        assertEquals(SaveDebtResult.Invalid(DebtError.NOT_FOUND), saveDebt(9, "X", 1, due, 1, null))
    }
}
