package com.hanan.mysavings.ui.home

import app.cash.turbine.test
import com.hanan.mysavings.domain.model.MonthlySummary
import com.hanan.mysavings.domain.usecase.ObserveMonthlySummaryUseCase
import com.hanan.mysavings.domain.usecase.SetBudgetUseCase
import com.hanan.mysavings.testutil.MainDispatcherRule
import com.hanan.mysavings.testutil.MutableClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val october = YearMonth.of(2026, 10)
    private val november = YearMonth.of(2026, 11)
    private val clock = MutableClock(Instant.parse("2026-10-29T03:00:00Z"))

    private val octoberSummaries = MutableSharedFlow<MonthlySummary>()
    private val observeSummary = mockk<ObserveMonthlySummaryUseCase> {
        every { this@mockk.invoke(october) } returns octoberSummaries
    }
    private val setBudget = mockk<SetBudgetUseCase>()

    private fun createViewModel() = HomeViewModel(observeSummary, setBudget, clock)

    private fun emptySummary(month: YearMonth) = MonthlySummary(month, 0, null, emptyList(), emptyList())

    @Test
    fun `starts loading then shows the current month summary`() = runTest {
        val viewModel = createViewModel()
        val summary = MonthlySummary(october, 100_000, 1_000_000, emptyList(), emptyList())

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())

            octoberSummaries.emit(summary)

            assertEquals(HomeUiState.Content(summary), awaitItem())
        }
    }

    @Test
    fun `saving a budget sets the overall budget for the current month`() = runTest {
        coEvery { setBudget(any(), any(), any()) } returns true

        createViewModel().onSaveBudget(2_500_000)

        coVerify { setBudget(october, null, 2_500_000) }
    }

    @Test
    fun `returning to the app in a new month switches to that month`() = runTest {
        every { observeSummary(november) } returns flowOf(emptySummary(november))
        coEvery { setBudget(any(), any(), any()) } returns true
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())

            // Midnight passes on 31 October (Jakarta time) while the app is in the background.
            clock.instant = Instant.parse("2026-10-31T17:30:00Z")
            viewModel.refreshMonth()

            assertEquals(HomeUiState.Content(emptySummary(november)), awaitItem())
        }
        viewModel.onSaveBudget(1_000_000)
        coVerify { setBudget(november, null, 1_000_000) }
    }

    @Test
    fun `refreshing within the same month does not restart the query`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem()
            viewModel.refreshMonth()
            viewModel.refreshMonth()
            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) { observeSummary(october) }
    }
}
