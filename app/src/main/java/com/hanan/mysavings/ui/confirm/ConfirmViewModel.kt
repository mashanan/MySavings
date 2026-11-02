package com.hanan.mysavings.ui.confirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.mysavings.domain.model.Category
import com.hanan.mysavings.domain.model.ReceiptField
import com.hanan.mysavings.domain.model.Transaction
import com.hanan.mysavings.domain.model.TransactionSource
import com.hanan.mysavings.domain.usecase.AddTransactionUseCase
import com.hanan.mysavings.domain.usecase.DeleteTransactionUseCase
import com.hanan.mysavings.domain.usecase.GetTransactionUseCase
import com.hanan.mysavings.domain.usecase.ObserveCategoriesUseCase
import com.hanan.mysavings.domain.usecase.SaveTransactionResult
import com.hanan.mysavings.domain.usecase.TransactionError
import com.hanan.mysavings.domain.usecase.UpdateTransactionUseCase
import com.hanan.mysavings.ui.util.parseAmount
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
 * Holds the transaction form in one of three modes:
 * - new, manual entry: starts empty;
 * - new, from a scan: starts pre-filled from the receipt ([prefill]);
 * - edit ([transactionId] set): starts with the saved transaction, and can also delete it.
 *
 * One form for all three keeps fields, validation and layout in a single place.
 * The arguments arrive through assisted injection: Hilt supplies the use cases and the
 * screen supplies the navigation arguments. Tests simply call the constructor.
 */
@HiltViewModel(assistedFactory = ConfirmViewModel.Factory::class)
class ConfirmViewModel @AssistedInject constructor(
    @Assisted private val prefill: ConfirmPrefill,
    @Assisted private val transactionId: Long?,
    observeCategories: ObserveCategoriesUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val getTransaction: GetTransactionUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(prefill: ConfirmPrefill, transactionId: Long?): ConfirmViewModel
    }

    private val isEditing = transactionId != null
    private val today = LocalDate.now(clock)
    private val prefilledForm = ConfirmForm(
        merchant = prefill.merchant.orEmpty(),
        amountDigits = prefill.total?.toString().orEmpty(),
        // A future date can only come from a misread receipt; today is the safer default.
        date = prefill.date?.takeIf { !it.isAfter(today) } ?: today,
        categoryId = prefill.categoryId,
        needsCheck = prefill.needsCheck,
    )
    private val form = MutableStateFlow(prefilledForm)

    /**
     * The form as it was when it opened; leaving with different content asks first.
     * Null while the transaction to edit is still being read.
     */
    private val baseline = MutableStateFlow(prefilledForm.takeUnless { isEditing })

    val uiState: StateFlow<ConfirmUiState> = combine(form, baseline, observeCategories()) { form, baseline, categories ->
        uiState(form, baseline, categories)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = uiState(form.value, baseline.value, emptyList()),
    )

    init {
        if (transactionId != null) {
            viewModelScope.launch {
                // One snapshot is enough: the form edits a copy, it should not jump while typing.
                val transaction = getTransaction(transactionId)
                if (transaction == null) {
                    form.update { it.copy(isNotFound = true) }
                } else {
                    val loaded = transaction.toForm()
                    form.value = loaded
                    baseline.value = loaded
                }
            }
        }
    }

    private fun uiState(form: ConfirmForm, baseline: ConfirmForm?, categories: List<Category>) = ConfirmUiState(
        form = form,
        categories = categories,
        maxDate = today,
        isFromScan = prefill.fromScan,
        isEditing = isEditing,
        isLoading = baseline == null,
        hasChanges = baseline != null && !form.hasSameContentAs(baseline),
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
        if (baseline.value == null || current.isBusy) return

        // Check every field at once so the user sees all problems in one go.
        val amount = parseAmount(current.amountDigits)?.takeIf { it > 0 }
        val categoryId = current.categoryId
        if (amount == null || categoryId == null) {
            form.update { it.copy(amountError = amount == null, categoryError = categoryId == null) }
            return
        }

        form.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = if (transactionId != null) {
                updateTransaction(transactionId, amount, current.date, current.merchant, current.note, categoryId)
            } else {
                addTransaction(
                    amount = amount,
                    date = current.date,
                    merchant = current.merchant,
                    note = current.note,
                    categoryId = categoryId,
                    source = if (prefill.fromScan) TransactionSource.SCAN else TransactionSource.MANUAL,
                )
            }
            form.update {
                when (result) {
                    is SaveTransactionResult.Success -> it.copy(isSaving = false, isSaved = true)
                    is SaveTransactionResult.Invalid -> it.copy(
                        isSaving = false,
                        amountError = result.error == TransactionError.AMOUNT_NOT_POSITIVE,
                        dateError = result.error == TransactionError.DATE_IN_FUTURE,
                        isNotFound = result.error == TransactionError.NOT_FOUND,
                    )
                }
            }
        }
    }

    /** Only reachable in edit mode; the screen asks for confirmation before calling it. */
    fun onDelete() {
        if (transactionId == null || baseline.value == null || form.value.isBusy) return

        form.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            deleteTransaction(transactionId)
            form.update { it.copy(isDeleting = false, isDeleted = true) }
        }
    }
}

private fun Transaction.toForm() = ConfirmForm(
    merchant = merchant.orEmpty(),
    amountDigits = amount.toString(),
    date = date,
    categoryId = categoryId,
    note = note.orEmpty(),
)

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
    val isDeleting: Boolean = false,
    // The three flags below end the screen; it reacts by navigating back.
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    /** The transaction to edit no longer exists (e.g. deleted on another phone and synced). */
    val isNotFound: Boolean = false,
) {
    /** Saving, deleting or already finished: ignore further taps on Save and Delete. */
    val isBusy: Boolean get() = isSaving || isDeleting || isSaved || isDeleted || isNotFound

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
    /** Editing a saved transaction: different title, and a delete action. */
    val isEditing: Boolean = false,
    /** True while the transaction to edit is being read; Save and Delete wait for it. */
    val isLoading: Boolean = false,
    /** True when leaving now would throw away something the user typed. */
    val hasChanges: Boolean = false,
) {
    val selectedCategory: Category? get() = categories.firstOrNull { it.id == form.categoryId }
}
