package com.hanan.mysavings.ui.savings

import com.hanan.mysavings.domain.model.GoalCategory
import com.hanan.mysavings.domain.model.GoalProgress
import com.hanan.mysavings.domain.model.SavingsGoal
import com.hanan.mysavings.domain.usecase.GoalDetail
import com.hanan.mysavings.domain.usecase.GoalError
import com.hanan.mysavings.domain.usecase.ObserveGoalDetailUseCase
import com.hanan.mysavings.domain.usecase.SaveGoalResult
import com.hanan.mysavings.domain.usecase.SaveGoalUseCase
import com.hanan.mysavings.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GoalFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 29)
    private val clock = Clock.fixed(Instant.parse("2026-10-29T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private val deadline = LocalDate.of(2027, 3, 1)
    private val existing = SavingsGoal(4, "Motor", 20_000_000, GoalCategory.VEHICLE, deadline, Instant.EPOCH)

    private val observeDetail = mockk<ObserveGoalDetailUseCase> {
        every { this@mockk.invoke(4) } returns flowOf(GoalDetail(existing, GoalProgress.of(existing, 0, today), emptyList()))
        every { this@mockk.invoke(9) } returns flowOf(null)
    }
    private val saveGoal = mockk<SaveGoalUseCase>()

    private fun viewModel(goalId: Long?) = GoalFormViewModel(goalId, observeDetail, saveGoal, clock)

    @Test
    fun `a new goal starts empty with today as the earliest deadline`() {
        val state = viewModel(null).uiState.value

        assertFalse(state.isEditing)
        assertFalse(state.isLoading)
        assertEquals("", state.name)
        assertEquals(GoalCategory.OTHER, state.category)
        assertEquals(today, state.minDate)
    }

    @Test
    fun `editing loads the existing goal into the form`() {
        val state = viewModel(4).uiState.value

        assertTrue(state.isEditing)
        assertFalse(state.isLoading)
        assertEquals("Motor", state.name)
        assertEquals("20000000", state.targetDigits)
        assertEquals(GoalCategory.VEHICLE, state.category)
        assertEquals(deadline, state.targetDate)
    }

    @Test
    fun `editing a goal that is gone shows not found`() {
        assertEquals(setOf(GoalError.NOT_FOUND), viewModel(9).uiState.value.errors)
    }

    @Test
    fun `saving sends the typed values and marks the form saved`() = runTest {
        coEvery { saveGoal(any(), any(), any(), any(), any()) } returns SaveGoalResult.Success(1)
        val viewModel = viewModel(null)

        viewModel.onNameChange("Laptop")
        viewModel.onTargetChange("12000000")
        viewModel.onCategoryChange(GoalCategory.GADGET)
        viewModel.onTargetDateChange(deadline)
        viewModel.onSave()

        coVerify { saveGoal(null, "Laptop", 12_000_000, GoalCategory.GADGET, deadline) }
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `errors from the use case are shown and cleared when the field is edited`() = runTest {
        coEvery { saveGoal(any(), any(), any(), any(), any()) } returns
            SaveGoalResult.Invalid(setOf(GoalError.NAME_BLANK, GoalError.TARGET_NOT_POSITIVE))
        val viewModel = viewModel(null)

        viewModel.onSave()
        coVerify { saveGoal(null, "", 0, GoalCategory.OTHER, null) } // empty amount arrives as 0
        assertEquals(setOf(GoalError.NAME_BLANK, GoalError.TARGET_NOT_POSITIVE), viewModel.uiState.value.errors)
        assertFalse(viewModel.uiState.value.isSaving)

        viewModel.onNameChange("Laptop")
        assertEquals(setOf(GoalError.TARGET_NOT_POSITIVE), viewModel.uiState.value.errors)
    }
}
