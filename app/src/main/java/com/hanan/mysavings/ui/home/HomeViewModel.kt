package com.hanan.mysavings.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.mysavings.domain.model.MonthInsights
import com.hanan.mysavings.domain.model.MonthlySummary
import com.hanan.mysavings.domain.usecase.ObserveMonthInsightsUseCase
import com.hanan.mysavings.domain.usecase.ObserveMonthlySummaryUseCase
import com.hanan.mysavings.domain.usecase.SetBudgetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeMonthlySummary: ObserveMonthlySummaryUseCase,
    observeMonthInsights: ObserveMonthInsightsUseCase,
    private val setBudget: SetBudgetUseCase,
    private val clock: Clock,
) : ViewModel() {

    // The date is state, not a constant: the app can stay open across midnight, and the
    // daily allowance changes every day while the month (and its summary) changes rarely.
    private val today = MutableStateFlow(LocalDate.now(clock))

    // flatMapLatest switches to the new month's query and cancels the old one; distinctUntilChanged
    // keeps a new day in the same month from restarting it.
    // WhileSubscribed(5s) keeps the query alive across rotation but stops it in the background.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = today
        .map { YearMonth.from(it) }
        .distinctUntilChanged()
        .flatMapLatest { observeMonthlySummary(it) }
        .combine(today) { summary, date -> summary to date }
        // Insights build on the summary, so they are recomputed whenever it changes.
        .flatMapLatest { (summary, date) ->
            observeMonthInsights(summary, date).map<MonthInsights, HomeUiState> { HomeUiState.Content(summary, it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    /** Called when the screen comes back to the foreground. A no-op unless the date changed. */
    fun refreshDate() {
        today.value = LocalDate.now(clock)
    }

    fun onSaveBudget(limitAmount: Long) {
        viewModelScope.launch { setBudget(YearMonth.from(today.value), categoryId = null, limitAmount = limitAmount) }
    }
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(val summary: MonthlySummary, val insights: MonthInsights) : HomeUiState
}
