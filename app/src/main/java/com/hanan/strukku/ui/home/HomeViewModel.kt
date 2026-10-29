package com.hanan.strukku.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.strukku.domain.model.MonthlySummary
import com.hanan.strukku.domain.usecase.ObserveMonthlySummaryUseCase
import com.hanan.strukku.domain.usecase.SetBudgetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    clock: Clock,
) : ViewModel() {

    private val month: YearMonth = YearMonth.now(clock)

    // WhileSubscribed(5s) keeps the Room query alive across configuration changes
    // (rotation) but stops it when the screen has been in the background for a while.
    val uiState: StateFlow<HomeUiState> = observeMonthlySummary(month)
        .map<MonthlySummary, HomeUiState> { HomeUiState.Content(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun onSaveBudget(limitAmount: Long) {
        viewModelScope.launch { setBudget(month, categoryId = null, limitAmount = limitAmount) }
    }
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(val summary: MonthlySummary) : HomeUiState
}
