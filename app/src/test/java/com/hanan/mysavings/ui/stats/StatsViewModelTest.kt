package com.hanan.mysavings.ui.stats

import app.cash.turbine.test
import com.hanan.mysavings.domain.model.MonthlyReport
import com.hanan.mysavings.domain.model.MonthlySummary
import com.hanan.mysavings.domain.repository.ReportExporter
import com.hanan.mysavings.domain.usecase.BuildMonthlyReportUseCase
import com.hanan.mysavings.domain.usecase.MonthTotal
import com.hanan.mysavings.domain.usecase.ObserveMonthlySummaryUseCase
import com.hanan.mysavings.domain.usecase.ObserveMonthlyTotalsUseCase
import com.hanan.mysavings.testutil.MainDispatcherRule
import com.hanan.mysavings.testutil.MutableClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
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
            flowOf(MonthlySummary(firstArg(), 0, null, emptyList()))
        }
    }

    private val buildReport = mockk<BuildMonthlyReportUseCase>()
    private val exporter = mockk<ReportExporter>()

    private fun createViewModel() = StatsViewModel(observeTotals, observeSummary, buildReport, exporter, clock)

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

    // ---------------------------------------------------------------- PDF export

    @Test
    fun `export builds the report for the selected month and hands over the file`() = runTest {
        val september = YearMonth.of(2026, 9)
        val report = MonthlyReport(september, 0, null, emptyList(), emptyList())
        val file = File("MySavings-Report-2026-09.pdf")
        coEvery { buildReport(september) } returns report
        coEvery { exporter.export(report) } returns file
        val viewModel = createViewModel()

        viewModel.onPreviousMonth()
        viewModel.onExport()

        assertEquals(ExportState.Ready(file), viewModel.exportState.value)

        viewModel.onExportHandled()
        assertEquals(ExportState.Idle, viewModel.exportState.value)
    }

    @Test
    fun `a failing export reports failure instead of crashing`() = runTest {
        coEvery { buildReport(any()) } throws IllegalStateException("disk full")
        val viewModel = createViewModel()

        viewModel.onExport()

        assertEquals(ExportState.Failed, viewModel.exportState.value)
    }

    @Test
    fun `pressing export again while exporting does nothing`() = runTest {
        val pending = CompletableDeferred<File>()
        coEvery { buildReport(any()) } returns MonthlyReport(october, 0, null, emptyList(), emptyList())
        coEvery { exporter.export(any()) } coAnswers { pending.await() }
        val viewModel = createViewModel()

        viewModel.onExport()
        viewModel.onExport()
        assertEquals(ExportState.Exporting, viewModel.exportState.value)
        pending.complete(File("x.pdf"))

        coVerify(exactly = 1) { exporter.export(any()) }
    }
}
