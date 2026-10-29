package com.hanan.mysavings.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.mysavings.domain.usecase.AddDebtPaymentResult
import com.hanan.mysavings.domain.usecase.AddDebtPaymentUseCase
import com.hanan.mysavings.domain.usecase.DebtDetail
import com.hanan.mysavings.domain.usecase.DeleteDebtPaymentUseCase
import com.hanan.mysavings.domain.usecase.DeleteDebtUseCase
import com.hanan.mysavings.domain.usecase.ObserveDebtDetailUseCase
import com.hanan.mysavings.domain.usecase.PaymentError
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

@HiltViewModel(assistedFactory = DebtDetailViewModel.Factory::class)
class DebtDetailViewModel @AssistedInject constructor(
    @Assisted private val debtId: Long,
    observeDebtDetail: ObserveDebtDetailUseCase,
    private val addPayment: AddDebtPaymentUseCase,
    private val deleteDebt: DeleteDebtUseCase,
    private val deletePayment: DeleteDebtPaymentUseCase,
    private val clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(debtId: Long): DebtDetailViewModel
    }

    val uiState: StateFlow<DebtDetailUiState> = observeDebtDetail(debtId)
        .map { detail -> if (detail == null) DebtDetailUiState.Deleted else DebtDetailUiState.Content(detail) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebtDetailUiState.Loading)

    // Kept here (not in the composable) because the use case can reject the input and the
    // sheet must stay open to say why; same reasoning as GoalDetailViewModel.
    private val _sheet = MutableStateFlow<PaymentSheetState?>(null)
    val sheet: StateFlow<PaymentSheetState?> = _sheet.asStateFlow()

    // One-off messages for a snackbar. A Channel delivers each one once, even across rotation.
    private val _events = Channel<DebtDetailEvent>(Channel.BUFFERED)
    val events: Flow<DebtDetailEvent> = _events.receiveAsFlow()

    private val currentDetail: DebtDetail? get() = (uiState.value as? DebtDetailUiState.Content)?.detail

    /** Pre-filled with what is left of the next installment: the usual payment. */
    fun onOpenPay() = openSheet(prefill = currentDetail?.status?.next?.remaining, isPayOff = false)

    /** Pre-filled with everything still owed. */
    fun onOpenPayOff() = openSheet(prefill = currentDetail?.status?.remaining, isPayOff = true)

    private fun openSheet(prefill: Long?, isPayOff: Boolean) {
        val today = LocalDate.now(clock)
        _sheet.value = PaymentSheetState(
            isPayOff = isPayOff,
            amountDigits = prefill?.takeIf { it > 0 }?.toString().orEmpty(),
            date = today,
            maxDate = today,
        )
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
        val amount = parseAmount(current.amountDigits) ?: 0
        // Read before saving: afterwards the detail flow already shows the new balance.
        val remainingBefore = currentDetail?.status?.remaining
        _sheet.value = current.copy(isSaving = true)
        viewModelScope.launch {
            when (val result = addPayment(debtId, amount, current.date, current.note)) {
                is AddDebtPaymentResult.Success -> {
                    _sheet.value = null
                    _events.send(if (amount == remainingBefore) DebtDetailEvent.PaidOff else DebtDetailEvent.PaymentSaved)
                }
                is AddDebtPaymentResult.Invalid -> _sheet.update { it?.copy(isSaving = false, error = result.error) }
            }
        }
    }

    fun onDeletePayment(paymentId: Long) {
        viewModelScope.launch {
            deletePayment(paymentId)
            _events.send(DebtDetailEvent.PaymentDeleted)
        }
    }

    /** The detail flow then emits Deleted, and the screen closes itself. */
    fun onDeleteDebt() {
        viewModelScope.launch { deleteDebt(debtId) }
    }
}

sealed interface DebtDetailUiState {
    data object Loading : DebtDetailUiState
    data class Content(val detail: DebtDetail) : DebtDetailUiState

    /** The debt no longer exists (just deleted here, or gone already). */
    data object Deleted : DebtDetailUiState
}

/** The open payment sheet. */
data class PaymentSheetState(
    /** Opened through "Pay off": only changes the title. */
    val isPayOff: Boolean,
    val amountDigits: String,
    val date: LocalDate,
    /** Latest selectable date (today). */
    val maxDate: LocalDate,
    val note: String = "",
    val error: PaymentError? = null,
    val isSaving: Boolean = false,
)

enum class DebtDetailEvent { PaymentSaved, PaidOff, PaymentDeleted }
