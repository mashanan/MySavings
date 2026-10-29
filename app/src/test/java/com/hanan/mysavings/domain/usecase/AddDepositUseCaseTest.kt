package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.SavingsDeposit
import com.hanan.mysavings.domain.repository.SavingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddDepositUseCaseTest {

    private val added = slot<SavingsDeposit>()
    private val repository = mockk<SavingsRepository> {
        coEvery { getSaved(1) } returns 300_000
        coEvery { addDeposit(capture(added)) } returns 11
    }
    private val addDeposit = AddDepositUseCase(repository, SavingsTestData.clock())
    private val today = SavingsTestData.today

    @Test
    fun `a deposit is stored as a positive amount`() = runTest {
        val result = addDeposit(1, 500_000, isWithdrawal = false, today, note = " Gaji ")

        assertEquals(AddDepositResult.Success(11), result)
        assertEquals(500_000L, added.captured.amount)
        assertEquals("Gaji", added.captured.note)
    }

    @Test
    fun `a withdrawal is stored as a negative amount`() = runTest {
        addDeposit(1, 200_000, isWithdrawal = true, today, note = "  ")

        assertEquals(-200_000L, added.captured.amount)
        assertNull(added.captured.note)
    }

    @Test
    fun `the whole balance can be withdrawn but not more`() = runTest {
        assertEquals(AddDepositResult.Success(11), addDeposit(1, 300_000, isWithdrawal = true, today, null))
        assertEquals(
            AddDepositResult.Invalid(DepositError.EXCEEDS_BALANCE),
            addDeposit(1, 300_001, isWithdrawal = true, today, null),
        )
    }

    @Test
    fun `rejects a zero amount and a future date`() = runTest {
        assertEquals(AddDepositResult.Invalid(DepositError.AMOUNT_NOT_POSITIVE), addDeposit(1, 0, false, today, null))
        assertEquals(
            AddDepositResult.Invalid(DepositError.DATE_IN_FUTURE),
            addDeposit(1, 10_000, false, today.plusDays(1), null),
        )
        coVerify(exactly = 0) { repository.addDeposit(any()) }
    }
}
