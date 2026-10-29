package com.hanan.mysavings.ui.debts

import com.hanan.mysavings.domain.model.Debt
import com.hanan.mysavings.domain.model.DebtStatus
import com.hanan.mysavings.domain.usecase.DebtDetail
import com.hanan.mysavings.domain.usecase.DebtError
import com.hanan.mysavings.domain.usecase.ObserveDebtDetailUseCase
import com.hanan.mysavings.domain.usecase.SaveDebtResult
import com.hanan.mysavings.domain.usecase.SaveDebtUseCase
import com.hanan.mysavings.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DebtFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 29)
    private val clock = Clock.fixed(Instant.parse("2026-10-29T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private val existing = Debt(4, "Leasing motor", 12_000_000, LocalDate.of(2026, 6, 5), 12, "Honda", Instant.EPOCH)

    private val observeDetail = mockk<ObserveDebtDetailUseCase> {
        every { this@mockk.invoke(4) } returns flowOf(DebtDetail(existing, DebtStatus.of(existing, 0, today), emptyList()))
    }
    private val saveDebt = mockk<SaveDebtUseCase>()

    private fun viewModel(debtId: Long?) = DebtFormViewModel(debtId, observeDetail, saveDebt, clock)

    @Test
    fun `a new debt starts as a single payment due today`() {
        val state = viewModel(null).uiState.value

        assertFalse(state.isInstallmentPlan)
        assertEquals(today, state.firstDueDate)
        assertEquals(1, state.installmentCount)
    }

    @Test
    fun `editing loads the existing installment plan`() {
        val state = viewModel(4).uiState.value

        assertEquals("Leasing motor", state.creditor)
        assertTrue(state.isInstallmentPlan)
        assertEquals("12", state.installmentDigits)
        assertEquals(1_000_000L, state.perInstallment)
        assertEquals("Honda", state.note)
    }

    @Test
    fun `the monthly preview needs both an amount and more than one month`() {
        val viewModel = viewModel(null)

        viewModel.onAmountChange("3000000")
        assertNull(viewModel.uiState.value.perInstallment) // still a single payment

        viewModel.onInstallmentPlanChange(true)
        viewModel.onInstallmentsChange("6x") // non-digits are dropped
        assertEquals("6", viewModel.uiState.value.installmentDigits)
        assertEquals(500_000L, viewModel.uiState.value.perInstallment)
    }

    @Test
    fun `an installment plan without a count is sent as 0 and the error is shown`() = runTest {
        coEvery { saveDebt(any(), any(), any(), any(), any(), any()) } returns
            SaveDebtResult.Invalid(DebtError.INSTALLMENTS_OUT_OF_RANGE)
        val viewModel = viewModel(null)

        viewModel.onCreditorChange("Bank")
        viewModel.onAmountChange("1000000")
        viewModel.onInstallmentPlanChange(true)
        viewModel.onSave()

        coVerify { saveDebt(null, "Bank", 1_000_000, today, 0, "") }
        assertEquals(setOf(DebtError.INSTALLMENTS_OUT_OF_RANGE), viewModel.uiState.value.errors)

        viewModel.onInstallmentsChange("12")
        assertTrue(viewModel.uiState.value.errors.isEmpty())
    }

    @Test
    fun `a successful save marks the form saved`() = runTest {
        coEvery { saveDebt(any(), any(), any(), any(), any(), any()) } returns SaveDebtResult.Success(4)
        val viewModel = viewModel(4)

        viewModel.onSave()

        coVerify { saveDebt(4, "Leasing motor", 12_000_000, LocalDate.of(2026, 6, 5), 12, "Honda") }
        assertTrue(viewModel.uiState.value.isSaved)
    }
}
