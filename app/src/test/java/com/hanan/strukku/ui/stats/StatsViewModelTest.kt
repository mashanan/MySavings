package com.hanan.strukku.ui.stats

import app.cash.turbine.test
import com.hanan.strukku.domain.model.MonthlySummary
import com.hanan.strukku.domain.usecase.MonthTotal
import com.hanan.strukku.domain.usecase.ObserveMonthlySummaryUseCase
import com.hanan.strukku.domain.usecase.ObserveMonthlyTotalsUseCase
import com.hanan.strukku.testutil.MainDispatcherRule
import com.hanan.strukku.testutil.MutableClock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val october = YearMonth.of(2026, 10)
    private val clock = MutableClock(Instant.parse("2026-10-29T03:00:00Z"))

    private val observeTotals = mockk<ObserveMonthlyTotalsUseCase>()
    private val observeSummary = mockk<ObserveMonthlySummaryUseCase>()

    @Before
    fun setUp() {
        // Any month: six zero totals ending there, and an empty summary for it.
        every { observeTotals(any(), any()) } answers {
            val end = firstArg<YearMonth>()
            flowOf((5L downTo 0L).map { MonthTotal(end.minusMonths(it), 0) })
        }
        every { observeSummary(any()) } answers {
            flowOf(MonthlySummary(firstArg(), 0, null, emptyList(), emptyList()))
        }
    }

    private fun createViewModel() = StatsViewModel(observeTotals, observeSummary, clock)

    private fun StatsUiState.content() = this as StatsUiState.Content

    @Test
    fun `opens on the current month with no way forward`() = runTest {
        createViewModel().uiState.test {
            val state = expectMostRecentItem().content()
            assertEquals(october, state.selectedMonth)
            assertEquals(october, state.summary.yearMonth)
            assertEquals(october, state.trend.last().yearMonth)
            assertFalse(state.canGoNext)
        }
    }

    @Test
    fun `previous month selects it and keeps the chart window in place`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            viewModel.onPreviousMonth()

            val state = expectMostRecentItem().content()
            assertEquals(YearMonth.of(2026, 9), state.selectedMonth)
            assertEquals(YearMonth.of(2026, 9), state.summary.yearMonth)
            assertEquals(october, state.trend.last().yearMonth)
            assertTrue(state.canGoNext)
        }
    }

    @Test
    fun `next month cannot go past the current month`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            viewModel.onNextMonth()
            viewModel.onSelectMonth(YearMonth.of(2027, 1))

            assertEquals(october, expectMostRecentItem().content().selectedMonth)
        }
    }

    @Test
    fun `selecting a bar does not re-query the trend`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            viewModel.onSelectMonth(YearMonth.of(2026, 7))
            viewModel.onSelectMonth(YearMonth.of(2026, 5))
            expectMostRecentItem()
        }

        verify(exactly = 1) { observeTotals(any(), any()) }
    }

    @Test
    fun `chart window stays while the selection is inside it`() {
        val end = StatsViewModel.chartWindowEnd(YearMonth.of(2026, 5), previousEnd = october, current = october)
        assertEquals(october, end)
    }

    @Test
    fun `chart window scrolls back when the selection leaves it on the left`() {
        val end = StatsViewModel.chartWindowEnd(YearMonth.of(2026, 4), previousEnd = october, current = october)
        // April becomes the oldest visible month: Apr..Sep.
        assertEquals(YearMonth.of(2026, 9), end)
    }

    @Test
    fun `chart window follows the selection forward but never past the current month`() {
        val march = YearMonth.of(2026, 3)
        assertEquals(YearMonth.of(2026, 6), StatsViewModel.chartWindowEnd(YearMonth.of(2026, 6), previousEnd = march, current = october))
        assertEquals(october, StatsViewModel.chartWindowEnd(YearMonth.of(2026, 9), previousEnd = october, current = october))
    }
}
