package com.hanan.mysavings.ui.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.mysavings.domain.model.GoalCategory
import com.hanan.mysavings.domain.usecase.GoalError
import com.hanan.mysavings.domain.usecase.ObserveGoalDetailUseCase
import com.hanan.mysavings.domain.usecase.SaveGoalResult
import com.hanan.mysavings.domain.usecase.SaveGoalUseCase
import com.hanan.mysavings.ui.util.parseAmount
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/**
 * Create (`goalId == null`) or edit a savings goal. The id arrives through assisted
 * injection, like the prefill of ConfirmViewModel, so tests can simply call the constructor.
 */
@HiltViewModel(assistedFactory = GoalFormViewModel.Factory::class)
class GoalFormViewModel @AssistedInject constructor(
    @Assisted private val goalId: Long?,
    observeGoalDetail: ObserveGoalDetailUseCase,
    private val saveGoal: SaveGoalUseCase,
    clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(goalId: Long?): GoalFormViewModel
    }

    private val _uiState = MutableStateFlow(
        GoalFormUiState(isEditing = goalId != null, isLoading = goalId != null, minDate = LocalDate.now(clock)),
    )
    val uiState: StateFlow<GoalFormUiState> = _uiState.asStateFlow()

    init {
        if (goalId != null) {
            viewModelScope.launch {
                // One snapshot is enough: the form edits a copy, it should not jump while typing.
                val goal = observeGoalDetail(goalId).first()?.goal
                _uiState.update {
                    if (goal == null) {
                        it.copy(isLoading = false, errors = setOf(GoalError.NOT_FOUND))
                    } else {
                        it.copy(
                            isLoading = false,
                            name = goal.name,
                            targetDigits = goal.targetAmount.toString(),
                            category = goal.category,
                            targetDate = goal.targetDate,
                        )
                    }
                }
            }
        }
    }

    // Editing a field clears its error: the user is fixing it.
    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, errors = it.errors - GoalError.NAME_BLANK) }

    fun onTargetChange(digits: String) =
        _uiState.update { it.copy(targetDigits = digits, errors = it.errors - GoalError.TARGET_NOT_POSITIVE) }

    fun onCategoryChange(category: GoalCategory) = _uiState.update { it.copy(category = category) }

    fun onTargetDateChange(date: LocalDate?) =
        _uiState.update { it.copy(targetDate = date, errors = it.errors - GoalError.TARGET_DATE_IN_PAST) }

    fun onSave() {
        val current = _uiState.value
        if (current.isLoading || current.isSaving || current.isSaved) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = saveGoal(
                id = goalId,
                name = current.name,
                // Empty or zero input reaches the use case as 0, which it reports as an error.
                targetAmount = parseAmount(current.targetDigits) ?: 0,
                category = current.category,
                targetDate = current.targetDate,
            )
            _uiState.update {
                when (result) {
                    is SaveGoalResult.Success -> it.copy(isSaving = false, isSaved = true)
                    is SaveGoalResult.Invalid -> it.copy(isSaving = false, errors = result.errors)
                }
            }
        }
    }
}

data class GoalFormUiState(
    val isEditing: Boolean,
    /** True while an existing goal is being read; the form is not editable yet. */
    val isLoading: Boolean = false,
    val name: String = "",
    /** Digits only, e.g. "8000000"; formatted visually by AmountTextField. */
    val targetDigits: String = "",
    val category: GoalCategory = GoalCategory.OTHER,
    val targetDate: LocalDate? = null,
    /** Earliest selectable deadline (today). */
    val minDate: LocalDate,
    val errors: Set<GoalError> = emptySet(),
    val isSaving: Boolean = false,
    /** Becomes true once saved; the screen reacts by navigating back. */
    val isSaved: Boolean = false,
)
