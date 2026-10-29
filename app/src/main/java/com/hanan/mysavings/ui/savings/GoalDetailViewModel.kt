package com.hanan.mysavings.ui.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.mysavings.domain.usecase.AddDepositResult
import com.hanan.mysavings.domain.usecase.AddDepositUseCase
import com.hanan.mysavings.domain.usecase.DeleteDepositUseCase
import com.hanan.mysavings.domain.usecase.DeleteGoalUseCase
import com.hanan.mysavings.domain.usecase.DepositError
import com.hanan.mysavings.domain.usecase.GoalDetail
import com.hanan.mysavings.domain.usecase.ObserveGoalDetailUseCase
import com.hanan.mysavings.ui.util.parseAmount
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

@HiltViewModel(assistedFactory = GoalDetailViewModel.Factory::class)
class GoalDetailViewModel @AssistedInject constructor(
    @Assisted private val goalId: Long,
    observeGoalDetail: ObserveGoalDetailUseCase,
    private val addDeposit: AddDepositUseCase,
    private val deleteGoal: DeleteGoalUseCase,
    private val deleteDeposit: DeleteDepositUseCase,
    private val clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(goalId: Long): GoalDetailViewModel
    }

    val uiState: StateFlow<GoalDetailUiState> = observeGoalDetail(goalId)
        .map { detail -> if (detail == null) GoalDetailUiState.Deleted else GoalDetailUiState.Content(detail) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalDetailUiState.Loading)

    // The sheet's input lives here, not in the composable, because the use case can reject it
    // (e.g. withdrawing more than the balance) and the sheet must stay open to show why.
    private val _sheet = MutableStateFlow<DepositSheetState?>(null)
    val sheet: StateFlow<DepositSheetState?> = _sheet.asStateFlow()

    // One-off messages for a snackbar. A Channel delivers each one once, even across rotation.
    private val _events = Channel<GoalDetailEvent>(Channel.BUFFERED)
    val events: Flow<GoalDetailEvent> = _events.receiveAsFlow()

    fun onOpenDeposit() = openSheet(isWithdrawal = false)

    fun onOpenWithdraw() = openSheet(isWithdrawal = true)

    private fun openSheet(isWithdrawal: Boolean) {
        val today = LocalDate.now(clock)
        _sheet.value = DepositSheetState(isWithdrawal = isWithdrawal, date = today, maxDate = today)
    }

    fun onSheetDismiss() {
        _sheet.value = null
    }

    fun onSheetAmountChange(digits: String) = _sheet.update { it?.copy(amountDigits = digits, error = null) }

    fun onSheetDateChange(date: LocalDate) = _sheet.update { it?.copy(date = date, error = null) }

    fun onSheetNoteChange(note: String) = _sheet.update { it?.copy(note = note) }

    fun onSheetSave() {
        val current = _sheet.value ?: return
        if (current.isSaving) return
        _sheet.value = current.copy(isSaving = true)
        viewModelScope.launch {
            val result = addDeposit(
                goalId = goalId,
                amount = parseAmount(current.amountDigits) ?: 0,
                isWithdrawal = current.isWithdrawal,
                date = current.date,
                note = current.note,
            )
            when (result) {
                is AddDepositResult.Success -> {
                    _sheet.value = null
                    _events.send(if (current.isWithdrawal) GoalDetailEvent.WithdrawalSaved else GoalDetailEvent.DepositSaved)
                }
                is AddDepositResult.Invalid -> _sheet.update { it?.copy(isSaving = false, error = result.error) }
            }
        }
    }

    fun onDeleteDeposit(depositId: Long) {
        viewModelScope.launch {
            _events.send(if (deleteDeposit(depositId)) GoalDetailEvent.DepositDeleted else GoalDetailEvent.DepositDeleteBlocked)
        }
    }

    /** The detail flow then emits Deleted, and the screen closes itself. */
    fun onDeleteGoal() {
        viewModelScope.launch { deleteGoal(goalId) }
    }
}

sealed interface GoalDetailUiState {
    data object Loading : GoalDetailUiState
    data class Content(val detail: GoalDetail) : GoalDetailUiState

    /** The goal no longer exists (just deleted here, or gone already). */
    data object Deleted : GoalDetailUiState
}

/** The open "add money" / "withdraw" sheet. */
data class DepositSheetState(
    val isWithdrawal: Boolean,
    val date: LocalDate,
    /** Latest selectable date (today). */
    val maxDate: LocalDate,
    val amountDigits: String = "",
    val note: String = "",
    val error: DepositError? = null,
    val isSaving: Boolean = false,
)

enum class GoalDetailEvent { DepositSaved, WithdrawalSaved, DepositDeleted, DepositDeleteBlocked }
