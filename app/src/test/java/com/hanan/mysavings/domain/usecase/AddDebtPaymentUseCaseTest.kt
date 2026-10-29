package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.DebtPayment
import com.hanan.mysavings.domain.repository.DebtRepository
import com.hanan.mysavings.domain.usecase.DebtTestData.debt
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddDebtPaymentUseCaseTest {

    private val added = slot<DebtPayment>()
    private val repository = mockk<DebtRepository> {
        coEvery { getDebt(1) } returns debt(total = 1_200_000)
        coEvery { getDebt(9) } returns null
        coEvery { getPaid(1) } returns 1_000_000
        coEvery { addPayment(capture(added)) } returns 21
    }
    private val addPayment = AddDebtPaymentUseCase(repository, DebtTestData.clock())
    private val today = DebtTestData.today

    @Test
    fun `stores the payment with a trimmed note`() = runTest {
        assertEquals(AddDebtPaymentResult.Success(21), addPayment(1, 150_000, today, " Transfer BCA "))
        assertEquals(150_000L, added.captured.amount)
        assertEquals("Transfer BCA", added.captured.note)
    }

    @Test
    fun `the exact remaining amount is allowed but not more`() = runTest {
        assertEquals(AddDebtPaymentResult.Success(21), addPayment(1, 200_000, today, null))
        assertEquals(AddDebtPaymentResult.Invalid(PaymentError.EXCEEDS_REMAINING), addPayment(1, 200_001, today, null))
    }

    @Test
    fun `rejects zero, a future date and an unknown debt`() = runTest {
        assertEquals(AddDebtPaymentResult.Invalid(PaymentError.AMOUNT_NOT_POSITIVE), addPayment(1, 0, today, null))
        assertEquals(AddDebtPaymentResult.Invalid(PaymentError.DATE_IN_FUTURE), addPayment(1, 10_000, today.plusDays(1), null))
        assertEquals(AddDebtPaymentResult.Invalid(PaymentError.NOT_FOUND), addPayment(9, 10_000, today, null))
        coVerify(exactly = 0) { repository.addPayment(any()) }
    }
}
