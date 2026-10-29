package com.hanan.mysavings.ui.home

import app.cash.turbine.test
import com.hanan.mysavings.domain.model.MonthInsights
import com.hanan.mysavings.domain.model.MonthlySummary
import com.hanan.mysavings.domain.usecase.ObserveMonthInsightsUseCase
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
import java.time.LocalDate
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

    // Returns the day it was asked about, so tests can see which date reached the use case.
    private val observeInsights = mockk<ObserveMonthInsightsUseCase> {
        every { this@mockk.invoke(any(), any()) } answers { flowOf(insightsFor(secondArg())) }
    }
    private val setBudget = mockk<SetBudgetUseCase>()

    private fun createViewModel() = HomeViewModel(observeSummary, observeInsights, setBudget, clock)

    private fun emptySummary(month: YearMonth) = MonthlySummary(month, 0, null, emptyList(), emptyList())

    private fun insightsFor(day: LocalDate) = MonthInsights(day.dayOfMonth, null, null, null, null)

    @Test
    fun `starts loading then shows the current month summary with its insights`() = runTest {
        val viewModel = createViewModel()
        val summary = MonthlySummary(october, 100_000, 1_000_000, emptyList(), emptyList())

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())

            octoberSummaries.emit(summary)

            assertEquals(HomeUiState.Content(summary, insightsFor(LocalDate.of(2026, 10, 29))), awaitItem())
        }
        verify { observeInsights(summary, LocalDate.of(2026, 10, 29)) }
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
            viewModel.refreshDate()

            assertEquals(HomeUiState.Content(emptySummary(november), insightsFor(LocalDate.of(2026, 11, 1))), awaitItem())
        }
        viewModel.onSaveBudget(1_000_000)
        coVerify { setBudget(november, null, 1_000_000) }
    }

    @Test
    fun `a new day in the same month updates insights without restarting the summary query`() = runTest {
        val viewModel = createViewModel()
        val summary = emptySummary(october)

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            octoberSummaries.emit(summary)
            assertEquals(HomeUiState.Content(summary, insightsFor(LocalDate.of(2026, 10, 29))), awaitItem())

            clock.instant = Instant.parse("2026-10-29T17:30:00Z") // 00:30 on 30 October in Jakarta
            viewModel.refreshDate()

            assertEquals(HomeUiState.Content(summary, insightsFor(LocalDate.of(2026, 10, 30))), awaitItem())
        }
        verify(exactly = 1) { observeSummary(october) }
    }

    @Test
    fun `refreshing within the same day does not restart the query`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem()
            viewModel.refreshDate()
            viewModel.refreshDate()
            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) { observeSummary(october) }
    }
}
