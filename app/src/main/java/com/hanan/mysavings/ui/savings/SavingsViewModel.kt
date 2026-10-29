package com.hanan.mysavings.ui.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.mysavings.domain.model.GoalCategory
import com.hanan.mysavings.domain.usecase.GoalWithProgress
import com.hanan.mysavings.domain.usecase.ObserveSavingsGoalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SavingsViewModel @Inject constructor(
    observeSavingsGoals: ObserveSavingsGoalsUseCase,
) : ViewModel() {

    /** null = show every category. */
    private val filter = MutableStateFlow<GoalCategory?>(null)

    val uiState: StateFlow<SavingsUiState> = combine(observeSavingsGoals(), filter) { overview, filter ->
        // Chips only for categories that have goals, in the enum's (stable) order.
        val categories = overview.goals.map { it.goal.category }.distinct().sortedBy { it.ordinal }
        // A filter whose last goal was deleted would show an empty list; fall back to "all".
        val activeFilter = filter?.takeIf { it in categories }
        SavingsUiState.Content(
            totalSaved = overview.totalSaved,
            reachedCount = overview.reachedCount,
            goalCount = overview.goals.size,
            categories = categories,
            filter = activeFilter,
            goals = if (activeFilter == null) overview.goals else overview.goals.filter { it.goal.category == activeFilter },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavingsUiState.Loading)

    fun onFilterChange(category: GoalCategory?) {
        filter.value = category
    }
}

sealed interface SavingsUiState {
    data object Loading : SavingsUiState

    data class Content(
        /** Across all goals, whatever the filter. */
        val totalSaved: Long,
        val reachedCount: Int,
        val goalCount: Int,
        /** Categories that have at least one goal. */
        val categories: List<GoalCategory>,
        val filter: GoalCategory?,
        /** Goals shown under the current filter. */
        val goals: List<GoalWithProgress>,
    ) : SavingsUiState
}
