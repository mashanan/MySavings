package com.hanan.mysavings.ui.debts

import app.cash.turbine.test
import com.hanan.mysavings.domain.model.Debt
import com.hanan.mysavings.domain.model.DebtStatus
import com.hanan.mysavings.domain.usecase.AddDebtPaymentResult
import com.hanan.mysavings.domain.usecase.AddDebtPaymentUseCase
import com.hanan.mysavings.domain.usecase.DebtDetail
import com.hanan.mysavings.domain.usecase.DeleteDebtPaymentUseCase
import com.hanan.mysavings.domain.usecase.DeleteDebtUseCase
import com.hanan.mysavings.domain.usecase.ObserveDebtDetailUseCase
import com.hanan.mysavings.domain.usecase.PaymentError
import com.hanan.mysavings.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DebtDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 29)
    private val clock = Clock.fixed(Instant.parse("2026-10-29T03:00:00Z"), ZoneId.of("Asia/Jakarta"))

    // 3 installments of 400.000; 500.000 paid, so 300.000 is left of the second one.
    private val debt = Debt(1, "Leasing", 1_200_000, LocalDate.of(2026, 11, 5), 3, null, Instant.EPOCH)
    private val detail = MutableStateFlow<DebtDetail?>(DebtDetail(debt, DebtStatus.of(debt, 500_000, today), emptyList()))

    private val observeDetail = mockk<ObserveDebtDetailUseCase> { every { this@mockk.invoke(1) } returns detail }
    private val addPayment = mockk<AddDebtPaymentUseCase>()
    private val deleteDebt = mockk<DeleteDebtUseCase> { coEvery { this@mockk.invoke(any()) } just runs }
    private val deletePayment = mockk<DeleteDebtPaymentUseCase> { coEvery { this@mockk.invoke(any()) } just runs }

    private fun viewModel() = DebtDetailViewModel(1, observeDetail, addPayment, deleteDebt, deletePayment, clock)

    @Test
    fun `pay is pre-filled with the rest of the next installment, pay off with everything`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()

            viewModel.onOpenPay()
            assertEquals("300000", viewModel.sheet.value?.amountDigits)

            viewModel.onOpenPayOff()
            assertEquals("700000", viewModel.sheet.value?.amountDigits)
            assertTrue(viewModel.sheet.value!!.isPayOff)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `paying the whole rest reports paid off and closes the sheet`() = runTest {
        coEvery { addPayment(any(), any(), any(), any()) } returns AddDebtPaymentResult.Success(9)
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()
            viewModel.events.test {
                viewModel.onOpenPayOff()
                viewModel.onSheetSave()

                assertEquals(DebtDetailEvent.PaidOff, awaitItem())
            }
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { addPayment(1, 700_000, today, "") }
        assertNull(viewModel.sheet.value)
    }

    @Test
    fun `a part payment reports a saved payment`() = runTest {
        coEvery { addPayment(any(), any(), any(), any()) } returns AddDebtPaymentResult.Success(9)
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()
            viewModel.events.test {
                viewModel.onOpenPay()
                viewModel.onSheetSave()

                assertEquals(DebtDetailEvent.PaymentSaved, awaitItem())
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a rejected payment keeps the sheet open with the reason`() = runTest {
        coEvery { addPayment(any(), any(), any(), any()) } returns AddDebtPaymentResult.Invalid(PaymentError.EXCEEDS_REMAINING)
        val viewModel = viewModel()

        viewModel.onOpenPay()
        viewModel.onSheetAmountChange("9000000")
        viewModel.onSheetSave()

        assertEquals(PaymentError.EXCEEDS_REMAINING, viewModel.sheet.value?.error)
    }

    @Test
    fun `the screen learns when the debt is gone`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertTrue(awaitItem() is DebtDetailUiState.Content)

            viewModel.onDeleteDebt()
            detail.value = null

            assertEquals(DebtDetailUiState.Deleted, awaitItem())
        }
        coVerify { deleteDebt(1) }
    }
}
