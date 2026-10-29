package com.hanan.mysavings.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.mysavings.domain.model.MonthlySummary
import com.hanan.mysavings.domain.usecase.ObserveMonthlySummaryUseCase
import com.hanan.mysavings.domain.usecase.SetBudgetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeMonthlySummary: ObserveMonthlySummaryUseCase,
    private val setBudget: SetBudgetUseCase,
    private val clock: Clock,
) : ViewModel() {

    // The month is state, not a constant: the app can stay open across midnight on the last day.
    private val month = MutableStateFlow(YearMonth.now(clock))

    // flatMapLatest switches to the new month's query and cancels the old one.
    // WhileSubscribed(5s) keeps the query alive across rotation but stops it in the background.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = month
        .flatMapLatest { observeMonthlySummary(it) }
        .map<MonthlySummary, HomeUiState> { HomeUiState.Content(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    /** Called when the screen comes back to the foreground. A no-op unless the month changed. */
    fun refreshMonth() {
        month.value = YearMonth.now(clock)
    }

    fun onSaveBudget(limitAmount: Long) {
        viewModelScope.launch { setBudget(month.value, categoryId = null, limitAmount = limitAmount) }
    }
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(val summary: MonthlySummary) : HomeUiState
}
