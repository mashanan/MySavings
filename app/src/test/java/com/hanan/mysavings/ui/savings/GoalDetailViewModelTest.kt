package com.hanan.mysavings.ui.savings

import app.cash.turbine.test
import com.hanan.mysavings.domain.model.GoalCategory
import com.hanan.mysavings.domain.model.GoalProgress
import com.hanan.mysavings.domain.model.SavingsGoal
import com.hanan.mysavings.domain.usecase.AddDepositResult
import com.hanan.mysavings.domain.usecase.AddDepositUseCase
import com.hanan.mysavings.domain.usecase.DeleteDepositUseCase
import com.hanan.mysavings.domain.usecase.DeleteGoalUseCase
import com.hanan.mysavings.domain.usecase.DepositError
import com.hanan.mysavings.domain.usecase.GoalDetail
import com.hanan.mysavings.domain.usecase.ObserveGoalDetailUseCase
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

class GoalDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 29)
    private val clock = Clock.fixed(Instant.parse("2026-10-29T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private val goal = SavingsGoal(1, "Laptop", 1_000_000, GoalCategory.GADGET, null, Instant.EPOCH)
    private val detail = MutableStateFlow<GoalDetail?>(GoalDetail(goal, GoalProgress.of(goal, 300_000, today), emptyList()))

    private val observeDetail = mockk<ObserveGoalDetailUseCase> {
        every { this@mockk.invoke(1) } returns detail
    }
    private val addDeposit = mockk<AddDepositUseCase>()
    private val deleteGoal = mockk<DeleteGoalUseCase> { coEvery { this@mockk.invoke(any()) } just runs }
    private val deleteDeposit = mockk<DeleteDepositUseCase>()

    private fun viewModel() = GoalDetailViewModel(1, observeDetail, addDeposit, deleteGoal, deleteDeposit, clock)

    @Test
    fun `shows the goal and switches to Deleted when it disappears`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertTrue(awaitItem() is GoalDetailUiState.Content)

            detail.value = null

            assertEquals(GoalDetailUiState.Deleted, awaitItem())
        }
    }

    @Test
    fun `the sheet opens on today in the chosen direction`() {
        val viewModel = viewModel()

        viewModel.onOpenWithdraw()

        assertEquals(DepositSheetState(isWithdrawal = true, date = today, maxDate = today), viewModel.sheet.value)
    }

    @Test
    fun `a successful save closes the sheet and reports it once`() = runTest {
        coEvery { addDeposit(any(), any(), any(), any(), any()) } returns AddDepositResult.Success(5)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onOpenDeposit()
            viewModel.onSheetAmountChange("250000")
            viewModel.onSheetNoteChange("Gaji")
            viewModel.onSheetSave()

            assertEquals(GoalDetailEvent.DepositSaved, awaitItem())
        }
        coVerify { addDeposit(1, 250_000, false, today, "Gaji") }
        assertNull(viewModel.sheet.value)
    }

    @Test
    fun `a rejected withdrawal keeps the sheet open with the reason, cleared on the next edit`() = runTest {
        coEvery { addDeposit(any(), any(), any(), any(), any()) } returns AddDepositResult.Invalid(DepositError.EXCEEDS_BALANCE)
        val viewModel = viewModel()

        viewModel.onOpenWithdraw()
        viewModel.onSheetAmountChange("900000")
        viewModel.onSheetSave()

        assertEquals(DepositError.EXCEEDS_BALANCE, viewModel.sheet.value?.error)
        assertEquals(false, viewModel.sheet.value?.isSaving)

        viewModel.onSheetAmountChange("300000")
        assertNull(viewModel.sheet.value?.error)
    }

    @Test
    fun `deleting an entry reports whether it was allowed`() = runTest {
        coEvery { deleteDeposit(7) } returns false
        coEvery { deleteDeposit(8) } returns true
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onDeleteDeposit(7)
            assertEquals(GoalDetailEvent.DepositDeleteBlocked, awaitItem())

            viewModel.onDeleteDeposit(8)
            assertEquals(GoalDetailEvent.DepositDeleted, awaitItem())
        }
    }

    @Test
    fun `deleting the goal calls the use case`() = runTest {
        viewModel().onDeleteGoal()

        coVerify { deleteGoal(1) }
    }
}
