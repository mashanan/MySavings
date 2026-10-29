package com.hanan.mysavings.ui.savings

import app.cash.turbine.test
import com.hanan.mysavings.domain.model.GoalCategory
import com.hanan.mysavings.domain.model.GoalProgress
import com.hanan.mysavings.domain.model.SavingsGoal
import com.hanan.mysavings.domain.usecase.GoalWithProgress
import com.hanan.mysavings.domain.usecase.ObserveSavingsGoalsUseCase
import com.hanan.mysavings.domain.usecase.SavingsOverview
import com.hanan.mysavings.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SavingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 29)
    private val laptop = item(1, GoalCategory.GADGET, saved = 500_000)
    private val phone = item(2, GoalCategory.GADGET, saved = 0)
    private val bali = item(3, GoalCategory.TRAVEL, saved = 200_000)

    private val overview = MutableStateFlow(overviewOf(laptop, phone, bali))
    private val observeGoals = mockk<ObserveSavingsGoalsUseCase> {
        every { this@mockk.invoke() } returns overview
    }

    private fun item(id: Long, category: GoalCategory, saved: Long): GoalWithProgress {
        val goal = SavingsGoal(id, "Goal $id", 1_000_000, category, null, Instant.EPOCH)
        return GoalWithProgress(goal, GoalProgress.of(goal, saved, today))
    }

    private fun overviewOf(vararg goals: GoalWithProgress) =
        SavingsOverview(goals.sumOf { it.progress.saved }, goals.count { it.progress.isReached }, goals.toList())

    @Test
    fun `shows all goals and the categories that have goals, in a stable order`() = runTest {
        val viewModel = SavingsViewModel(observeGoals)

        viewModel.uiState.test {
            val state = awaitItem() as SavingsUiState.Content

            assertEquals(700_000L, state.totalSaved)
            assertEquals(3, state.goalCount)
            assertEquals(listOf(GoalCategory.GADGET, GoalCategory.TRAVEL), state.categories)
            assertNull(state.filter)
            assertEquals(listOf(laptop, phone, bali), state.goals)
        }
    }

    @Test
    fun `a filter narrows the list but not the total`() = runTest {
        val viewModel = SavingsViewModel(observeGoals)

        viewModel.uiState.test {
            awaitItem()
            viewModel.onFilterChange(GoalCategory.TRAVEL)

            val state = awaitItem() as SavingsUiState.Content
            assertEquals(GoalCategory.TRAVEL, state.filter)
            assertEquals(listOf(bali), state.goals)
            assertEquals(700_000L, state.totalSaved)
        }
    }

    @Test
    fun `when the filtered category loses its last goal the filter falls back to all`() = runTest {
        val viewModel = SavingsViewModel(observeGoals)

        viewModel.uiState.test {
            awaitItem()
            viewModel.onFilterChange(GoalCategory.TRAVEL)
            awaitItem()

            overview.value = overviewOf(laptop, phone) // the Bali goal was deleted

            val state = awaitItem() as SavingsUiState.Content
            assertNull(state.filter)
            assertEquals(listOf(laptop, phone), state.goals)
        }
    }
}
