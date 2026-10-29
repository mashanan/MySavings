package com.hanan.mysavings.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.mysavings.domain.usecase.DebtWithStatus
import com.hanan.mysavings.domain.usecase.DueItem
import com.hanan.mysavings.domain.usecase.ObserveDebtsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class DebtsViewModel @Inject constructor(
    observeDebts: ObserveDebtsUseCase,
    clock: Clock,
) : ViewModel() {

    private val today = LocalDate.now(clock)

    // Month and selected day live in one state so they always change together; two separate
    // flows would briefly show the new month with the old month's selection.
    // The selection starts on today so today's payments show without a tap.
    private val calendar = MutableStateFlow(CalendarState(month = YearMonth.from(today), selectedDate = today))

    val uiState: StateFlow<DebtsUiState> = combine(observeDebts(), calendar) { overview, (month, selected) ->
        DebtsUiState.Content(
            totalRemaining = overview.totalRemaining,
            overdueCount = overview.overdueCount,
            hasDebts = overview.active.isNotEmpty() || overview.paidOff.isNotEmpty(),
            month = month,
            today = today,
            dots = CalendarLogic.dots(overview.dueByDate, month, today),
            selectedDate = selected,
            dueOnSelectedDate = selected?.let { overview.dueByDate[it] }.orEmpty(),
            active = overview.active,
            paidOff = overview.paidOff,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebtsUiState.Loading)

    // Debts reach into the future (installment plans), so the calendar can move both ways.
    fun onPreviousMonth() = showMonth(calendar.value.month.minusMonths(1))

    fun onNextMonth() = showMonth(calendar.value.month.plusMonths(1))

    private fun showMonth(newMonth: YearMonth) {
        // A selection from another month would list payments that are not on screen.
        calendar.value = CalendarState(newMonth, today.takeIf { YearMonth.from(it) == newMonth })
    }

    /** Tapping the selected day again clears the selection. */
    fun onSelectDate(date: LocalDate) {
        calendar.update { state -> state.copy(selectedDate = date.takeIf { it != state.selectedDate }) }
    }

    private data class CalendarState(val month: YearMonth, val selectedDate: LocalDate?)
}

sealed interface DebtsUiState {
    data object Loading : DebtsUiState

    data class Content(
        val totalRemaining: Long,
        val overdueCount: Int,
        val hasDebts: Boolean,
        val month: YearMonth,
        val today: LocalDate,
        /** Days of [month] with installments, colored by urgency. */
        val dots: Map<LocalDate, DueDot>,
        val selectedDate: LocalDate?,
        val dueOnSelectedDate: List<DueItem>,
        val active: List<DebtWithStatus>,
        val paidOff: List<DebtWithStatus>,
    ) : DebtsUiState
}
