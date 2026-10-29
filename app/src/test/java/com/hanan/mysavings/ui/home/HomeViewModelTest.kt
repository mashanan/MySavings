package com.hanan.mysavings.ui.home

import app.cash.turbine.test
import com.hanan.mysavings.domain.model.MonthInsights
import com.hanan.mysavings.domain.model.MonthlySummary
import com.hanan.mysavings.domain.usecase.DayTotal
import com.hanan.mysavings.domain.usecase.ObserveDailyTotalsUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val september = YearMonth.of(2026, 9)
    private val october = YearMonth.of(2026, 10)
    private val november = YearMonth.of(2026, 11)
    private val today = LocalDate.of(2026, 10, 29)
    private val clock = MutableClock(Instant.parse("2026-10-29T03:00:00Z"))

    private val octoberSummaries = MutableSharedFlow<MonthlySummary>()
    private val observeSummary = mockk<ObserveMonthlySummaryUseCase> {
        every { this@mockk.invoke(october) } returns octoberSummaries
        every { this@mockk.invoke(september) } returns flowOf(emptySummary(september))
        every { this@mockk.invoke(november) } returns flowOf(emptySummary(november))
    }

    // Both fakes echo the date they were asked about, so tests can see which date reached them.
    private val observeInsights = mockk<ObserveMonthInsightsUseCase> {
        every { this@mockk.invoke(any(), any()) } answers { flowOf(insightsFor(secondArg())) }
    }
    private val observeDaily = mockk<ObserveDailyTotalsUseCase> {
        every { this@mockk.invoke(any(), any()) } answers { flowOf(weekEndingOn(firstArg())) }
    }
    private val setBudget = mockk<SetBudgetUseCase>()

    private fun createViewModel() = HomeViewModel(observeSummary, observeInsights, observeDaily, setBudget, clock)

    private fun emptySummary(month: YearMonth) = MonthlySummary(month, 0, null, emptyList(), emptyList())

    private fun insightsFor(day: LocalDate) = MonthInsights(day.dayOfMonth, null, null, null, null)

    private fun weekEndingOn(end: LocalDate) = (6L downTo 0L).map { DayTotal(end.minusDays(it), 0) }

    private fun content(summary: MonthlySummary, today: LocalDate, chartEnd: LocalDate, canGoNext: Boolean) =
        HomeUiState.Content(summary, insightsFor(today), weekEndingOn(chartEnd), today, canGoNext)

    @Test
    fun `starts loading then shows the current month with insights and the week up to today`() = runTest {
        val viewModel = createViewModel()
        val summary = MonthlySummary(october, 100_000, 1_000_000, emptyList(), emptyList())

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())

            octoberSummaries.emit(summary)

            assertEquals(content(summary, today, chartEnd = today, canGoNext = false), awaitItem())
        }
        verify { observeInsights(summary, today) }
    }

    @Test
    fun `saving a budget sets the overall budget for the current month`() = runTest {
        coEvery { setBudget(any(), any(), any()) } returns true

        createViewModel().onSaveBudget(2_500_000)

        coVerify { setBudget(october, null, 2_500_000) }
    }

    @Test
    fun `previous month shows its last week and can go forward again`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())

            viewModel.onPreviousMonth()

            val state = awaitItem() as HomeUiState.Content
            assertEquals(emptySummary(september), state.summary)
            assertEquals(LocalDate.of(2026, 9, 30), state.dailyTotals.last().date)
            assertTrue(state.canGoNext)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `budget saved while browsing goes to the month on screen`() = runTest {
        coEvery { setBudget(any(), any(), any()) } returns true
        val viewModel = createViewModel()

        viewModel.onPreviousMonth()
        viewModel.onSaveBudget(1_500_000)

        coVerify { setBudget(september, null, 1_500_000) }
    }

    @Test
    fun `next month stops at the current month`() = runTest {
        coEvery { setBudget(any(), any(), any()) } returns true
        val viewModel = createViewModel()

        viewModel.onPreviousMonth()
        viewModel.onNextMonth()
        viewModel.onNextMonth() // already on the current month: no further

        viewModel.onSaveBudget(1_000_000)
        coVerify { setBudget(october, null, 1_000_000) }
    }

    @Test
    fun `returning to the app in a new month switches to that month`() = runTest {
        coEvery { setBudget(any(), any(), any()) } returns true
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())

            // Midnight passes on 31 October (Jakarta time) while the app is in the background.
            clock.instant = Instant.parse("2026-10-31T17:30:00Z")
            viewModel.refreshDate()

            val firstOfNovember = LocalDate.of(2026, 11, 1)
            assertEquals(content(emptySummary(november), firstOfNovember, firstOfNovember, canGoNext = false), awaitItem())
        }
        viewModel.onSaveBudget(1_000_000)
        coVerify { setBudget(november, null, 1_000_000) }
    }

    @Test
    fun `a month chosen by the user stays when the current month changes`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            viewModel.onPreviousMonth()
            assertEquals(september, (awaitItem() as HomeUiState.Content).summary.yearMonth)

            clock.instant = Instant.parse("2026-10-31T17:30:00Z")
            viewModel.refreshDate()

            // Only the date inside the insights moves on; the month on screen is still September.
            val state = awaitItem() as HomeUiState.Content
            assertEquals(september, state.summary.yearMonth)
            assertEquals(LocalDate.of(2026, 11, 1), state.today)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a new day in the same month updates insights and chart without restarting the summary query`() = runTest {
        val viewModel = createViewModel()
        val summary = emptySummary(october)

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            octoberSummaries.emit(summary)
            assertEquals(content(summary, today, chartEnd = today, canGoNext = false), awaitItem())

            clock.instant = Instant.parse("2026-10-29T17:30:00Z") // 00:30 on 30 October in Jakarta
            viewModel.refreshDate()

            val nextDay = LocalDate.of(2026, 10, 30)
            assertEquals(content(summary, nextDay, chartEnd = nextDay, canGoNext = false), expectMostRecentItem())
        }
        verify(exactly = 1) { observeSummary(october) }
    }

    @Test
    fun `refreshing within the same day does not restart the queries`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem()
            viewModel.refreshDate()
            viewModel.refreshDate()
            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) { observeSummary(october) }
        verify(exactly = 1) { observeDaily(today, any()) }
    }

    @Test
    fun `the current month cannot go further forward`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()
            octoberSummaries.emit(emptySummary(october))
            assertFalse((awaitItem() as HomeUiState.Content).canGoNext)
        }
    }
}
