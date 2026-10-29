package com.hanan.strukku.ui.confirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.strukku.domain.model.Category
import com.hanan.strukku.domain.model.ReceiptField
import com.hanan.strukku.domain.model.TransactionSource
import com.hanan.strukku.domain.usecase.AddTransactionResult
import com.hanan.strukku.domain.usecase.AddTransactionUseCase
import com.hanan.strukku.domain.usecase.ObserveCategoriesUseCase
import com.hanan.strukku.domain.usecase.TransactionError
import com.hanan.strukku.ui.util.parseAmount
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/**
 * Holds the transaction form, starting either empty (manual entry) or pre-filled from a
 * scanned receipt.
 *
 * The prefill arrives through assisted injection: Hilt supplies the use cases and the
 * screen supplies the navigation argument. Tests simply call the constructor.
 */
@HiltViewModel(assistedFactory = ConfirmViewModel.Factory::class)
class ConfirmViewModel @AssistedInject constructor(
    @Assisted private val prefill: ConfirmPrefill,
    observeCategories: ObserveCategoriesUseCase,
    private val addTransaction: AddTransactionUseCase,
    clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(prefill: ConfirmPrefill): ConfirmViewModel
    }

    private val today = LocalDate.now(clock)
    private val initialForm = ConfirmForm(
        merchant = prefill.merchant.orEmpty(),
        amountDigits = prefill.total?.toString().orEmpty(),
        // A future date can only come from a misread receipt; today is the safer default.
        date = prefill.date?.takeIf { !it.isAfter(today) } ?: today,
        categoryId = prefill.categoryId,
        needsCheck = prefill.needsCheck,
    )
    private val form = MutableStateFlow(initialForm)

    val uiState: StateFlow<ConfirmUiState> = combine(form, observeCategories()) { form, categories ->
        uiState(form, categories)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = uiState(form.value, emptyList()),
    )

    private fun uiState(form: ConfirmForm, categories: List<Category>) = ConfirmUiState(
        form = form,
        categories = categories,
        maxDate = today,
        isFromScan = prefill.fromScan,
        hasChanges = !form.hasSameContentAs(initialForm),
    )

    // Editing a field clears its error and its "check this" hint: the user has looked at it.
    fun onMerchantChange(value: String) = form.update {
        it.copy(merchant = value, needsCheck = it.needsCheck - ReceiptField.MERCHANT)
    }

    fun onAmountChange(digits: String) = form.update {
        it.copy(amountDigits = digits, amountError = false, needsCheck = it.needsCheck - ReceiptField.TOTAL)
    }

    fun onDateChange(date: LocalDate) = form.update {
        it.copy(date = date, dateError = false, needsCheck = it.needsCheck - ReceiptField.DATE)
    }

    fun onCategoryChange(categoryId: Long) = form.update {
        it.copy(categoryId = categoryId, categoryError = false, needsCheck = it.needsCheck - ReceiptField.CATEGORY)
    }

    fun onNoteChange(value: String) = form.update { it.copy(note = value) }

    fun onSave() {
        val current = form.value
        if (current.isSaving || current.isSaved) return

        // Check every field at once so the user sees all problems in one go.
        val amount = parseAmount(current.amountDigits)?.takeIf { it > 0 }
        val categoryId = current.categoryId
        if (amount == null || categoryId == null) {
            form.update { it.copy(amountError = amount == null, categoryError = categoryId == null) }
            return
        }

        form.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = addTransaction(
                amount = amount,
                date = current.date,
                merchant = current.merchant,
                note = current.note,
                categoryId = categoryId,
                source = if (prefill.fromScan) TransactionSource.SCAN else TransactionSource.MANUAL,
            )
            form.update {
                when (result) {
                    is AddTransactionResult.Success -> it.copy(isSaving = false, isSaved = true)
                    is AddTransactionResult.Invalid -> it.copy(
                        isSaving = false,
                        amountError = result.error == TransactionError.AMOUNT_NOT_POSITIVE,
                        dateError = result.error == TransactionError.DATE_IN_FUTURE,
                    )
                }
            }
        }
    }
}

/** What the user has typed. Amount stays as digits until save, so half-typed input is never an error. */
data class ConfirmForm(
    val merchant: String = "",
    val amountDigits: String = "",
    val date: LocalDate,
    val categoryId: Long? = null,
    val note: String = "",
    /** Scanned fields the user has not reviewed yet. */
    val needsCheck: Set<ReceiptField> = emptySet(),
    val amountError: Boolean = false,
    val categoryError: Boolean = false,
    val dateError: Boolean = false,
    val isSaving: Boolean = false,
    /** Becomes true once saved; the screen reacts by navigating back. */
    val isSaved: Boolean = false,
) {
    /** Same values entered, ignoring errors and progress flags. */
    fun hasSameContentAs(other: ConfirmForm): Boolean =
        merchant == other.merchant &&
            amountDigits == other.amountDigits &&
            date == other.date &&
            categoryId == other.categoryId &&
            note == other.note
}

data class ConfirmUiState(
    val form: ConfirmForm,
    val categories: List<Category>,
    /** Latest selectable date (today). */
    val maxDate: LocalDate,
    val isFromScan: Boolean = false,
    /** True when leaving now would throw away something the user typed. */
    val hasChanges: Boolean = false,
) {
    val selectedCategory: Category? get() = categories.firstOrNull { it.id == form.categoryId }
}
