package com.hanan.mysavings.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.mysavings.domain.usecase.DebtError
import com.hanan.mysavings.domain.usecase.ObserveDebtDetailUseCase
import com.hanan.mysavings.domain.usecase.SaveDebtResult
import com.hanan.mysavings.domain.usecase.SaveDebtUseCase
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

/** Create (`debtId == null`) or edit a debt. Same assisted-injection pattern as GoalFormViewModel. */
@HiltViewModel(assistedFactory = DebtFormViewModel.Factory::class)
class DebtFormViewModel @AssistedInject constructor(
    @Assisted private val debtId: Long?,
    observeDebtDetail: ObserveDebtDetailUseCase,
    private val saveDebt: SaveDebtUseCase,
    clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(debtId: Long?): DebtFormViewModel
    }

    private val _uiState = MutableStateFlow(
        DebtFormUiState(isEditing = debtId != null, isLoading = debtId != null, firstDueDate = LocalDate.now(clock)),
    )
    val uiState: StateFlow<DebtFormUiState> = _uiState.asStateFlow()

    init {
        if (debtId != null) {
            viewModelScope.launch {
                // One snapshot is enough: the form edits a copy, it should not jump while typing.
                val debt = observeDebtDetail(debtId).first()?.debt
                _uiState.update {
                    if (debt == null) {
                        it.copy(isLoading = false, errors = setOf(DebtError.NOT_FOUND))
                    } else {
                        it.copy(
                            isLoading = false,
                            creditor = debt.creditor,
                            amountDigits = debt.totalAmount.toString(),
                            isInstallmentPlan = debt.isInstallmentPlan,
                            installmentDigits = if (debt.isInstallmentPlan) debt.installmentCount.toString() else "",
                            firstDueDate = debt.firstDueDate,
                            note = debt.note.orEmpty(),
                        )
                    }
                }
            }
        }
    }

    // Editing a field clears its error: the user is fixing it.
    fun onCreditorChange(value: String) =
        _uiState.update { it.copy(creditor = value, errors = it.errors - DebtError.CREDITOR_BLANK) }

    fun onAmountChange(digits: String) = _uiState.update {
        it.copy(amountDigits = digits, errors = it.errors - DebtError.AMOUNT_NOT_POSITIVE - DebtError.AMOUNT_BELOW_PAID)
    }

    fun onInstallmentPlanChange(isPlan: Boolean) = _uiState.update {
        it.copy(isInstallmentPlan = isPlan, errors = it.errors - DebtError.INSTALLMENTS_OUT_OF_RANGE)
    }

    fun onInstallmentsChange(digits: String) = _uiState.update {
        // Three digits are enough for the 360-month maximum.
        it.copy(installmentDigits = digits.filter(Char::isDigit).take(3), errors = it.errors - DebtError.INSTALLMENTS_OUT_OF_RANGE)
    }

    fun onFirstDueDateChange(date: LocalDate) = _uiState.update { it.copy(firstDueDate = date) }

    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }

    fun onSave() {
        val current = _uiState.value
        if (current.isLoading || current.isSaving || current.isSaved) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = saveDebt(
                id = debtId,
                creditor = current.creditor,
                // Empty input reaches the use case as 0, which it reports as an error.
                totalAmount = parseAmount(current.amountDigits) ?: 0,
                firstDueDate = current.firstDueDate,
                installmentCount = current.installmentCount,
                note = current.note,
            )
            _uiState.update {
                when (result) {
                    is SaveDebtResult.Success -> it.copy(isSaving = false, isSaved = true)
                    is SaveDebtResult.Invalid -> it.copy(isSaving = false, errors = result.errors)
                }
            }
        }
    }
}

data class DebtFormUiState(
    val isEditing: Boolean,
    /** True while an existing debt is being read; the form is not editable yet. */
    val isLoading: Boolean = false,
    val creditor: String = "",
    /** Digits only; formatted visually by AmountTextField. */
    val amountDigits: String = "",
    val isInstallmentPlan: Boolean = false,
    val installmentDigits: String = "",
    val firstDueDate: LocalDate,
    val note: String = "",
    val errors: Set<DebtError> = emptySet(),
    val isSaving: Boolean = false,
    /** Becomes true once saved; the screen reacts by navigating back. */
    val isSaved: Boolean = false,
) {
    /** 1 for a single payment; 0 (invalid) while an installment plan has no count yet. */
    val installmentCount: Int get() = if (isInstallmentPlan) installmentDigits.toIntOrNull() ?: 0 else 1

    /** Preview of a monthly installment, or `null` while amount or count is missing. */
    val perInstallment: Long?
        get() {
            val amount = parseAmount(amountDigits) ?: return null
            val count = installmentCount.takeIf { isInstallmentPlan && it > 1 } ?: return null
            return amount / count
        }
}
