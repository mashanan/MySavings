package com.hanan.mysavings.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.mysavings.domain.model.MonthlySummary
import com.hanan.mysavings.domain.repository.ReportExporter
import com.hanan.mysavings.domain.usecase.BuildMonthlyReportUseCase
import com.hanan.mysavings.domain.usecase.MonthTotal
import com.hanan.mysavings.domain.usecase.ObserveMonthlySummaryUseCase
import com.hanan.mysavings.domain.usecase.ObserveMonthlyTotalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    observeMonthlyTotals: ObserveMonthlyTotalsUseCase,
    observeMonthlySummary: ObserveMonthlySummaryUseCase,
    private val buildMonthlyReport: BuildMonthlyReportUseCase,
    private val reportExporter: ReportExporter,
    clock: Clock,
) : ViewModel() {

    private val currentMonth = YearMonth.now(clock)
    private val selection = MutableStateFlow(Selection(month = currentMonth, windowEnd = currentMonth))

    // Two independent queries: the trend only re-runs when the window moves, the
    // category breakdown re-runs for every selected month.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsUiState> = combine(
        selection.map { it.windowEnd }.distinctUntilChanged().flatMapLatest { observeMonthlyTotals(it) },
        selection.map { it.month }.distinctUntilChanged().flatMapLatest { observeMonthlySummary(it) },
        selection,
    ) { trend, summary, selection ->
        // While switching months, briefly skip the pair where the summary is still the old month.
        if (summary.yearMonth != selection.month) return@combine null
        StatsUiState.Content(
            selectedMonth = selection.month,
            canGoNext = selection.month < currentMonth,
            trend = trend,
            summary = summary,
        )
    }
        .filterNotNull()
        .stateIn<StatsUiState>(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState.Loading)

    fun onSelectMonth(month: YearMonth) {
        if (month > currentMonth) return
        selection.update { Selection(month, chartWindowEnd(month, it.windowEnd, currentMonth)) }
    }

    fun onPreviousMonth() = onSelectMonth(selection.value.month.minusMonths(1))

    fun onNextMonth() = onSelectMonth(selection.value.month.plusMonths(1))

    // Export progress is separate from the screen content so the charts don't re-render for it.
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    /** Builds the PDF for the selected month; the screen shares it once [ExportState.Ready]. */
    fun onExport() {
        if (_exportState.value == ExportState.Exporting) return
        val month = selection.value.month
        _exportState.value = ExportState.Exporting
        viewModelScope.launch {
            _exportState.value = try {
                ExportState.Ready(reportExporter.export(buildMonthlyReport(month)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ExportState.Failed
            }
        }
    }

    /** Called after the share sheet opened or the error was shown, so neither repeats. */
    fun onExportHandled() {
        _exportState.value = ExportState.Idle
    }

    private data class Selection(val month: YearMonth, val windowEnd: YearMonth)

    companion object {
        private const val WINDOW = ObserveMonthlyTotalsUseCase.DEFAULT_MONTHS.toLong()

        /**
         * Last month shown in the trend chart. The chart stays put while the selection moves
         * inside it (so tapping a bar doesn't make the bars jump), and only scrolls when the
         * selection leaves the visible six months. It never shows months after [current].
         */
        fun chartWindowEnd(selected: YearMonth, previousEnd: YearMonth, current: YearMonth): YearMonth {
            val previousStart = previousEnd.minusMonths(WINDOW - 1)
            return when {
                selected in previousStart..previousEnd -> previousEnd
                selected > previousEnd -> selected
                else -> minOf(selected.plusMonths(WINDOW - 1), current)
            }
        }
    }
}

sealed interface StatsUiState {
    data object Loading : StatsUiState

    data class Content(
        val selectedMonth: YearMonth,
        /** False on the current month: there is nothing to show in the future. */
        val canGoNext: Boolean,
        /** Six months, oldest first; the selected month is always one of them. */
        val trend: List<MonthTotal>,
        val summary: MonthlySummary,
    ) : StatsUiState
}

sealed interface ExportState {
    data object Idle : ExportState
    data object Exporting : ExportState
    data class Ready(val file: File) : ExportState
    data object Failed : ExportState
}
