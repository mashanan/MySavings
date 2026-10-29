package com.hanan.strukku.ui.confirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.strukku.domain.model.Category
import com.hanan.strukku.domain.model.TransactionSource
import com.hanan.strukku.domain.usecase.AddTransactionResult
import com.hanan.strukku.domain.usecase.AddTransactionUseCase
import com.hanan.strukku.domain.usecase.ObserveCategoriesUseCase
import com.hanan.strukku.domain.usecase.TransactionError
import com.hanan.strukku.ui.util.parseAmount
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
import javax.inject.Inject

/**
 * Holds the transaction form. In phase 3 the form starts empty (manual entry);
 * phase 5 pre-fills it from a scanned receipt.
 */
@HiltViewModel
class ConfirmViewModel @Inject constructor(
    observeCategories: ObserveCategoriesUseCase,
    private val addTransaction: AddTransactionUseCase,
    clock: Clock,
) : ViewModel() {

    private val today = LocalDate.now(clock)
    private val form = MutableStateFlow(ConfirmForm(date = today))

    val uiState: StateFlow<ConfirmUiState> = combine(form, observeCategories()) { form, categories ->
        ConfirmUiState(form = form, categories = categories, maxDate = today)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConfirmUiState(form = form.value, categories = emptyList(), maxDate = today),
    )

    // Editing a field clears that field's error, so stale messages don't linger.
    fun onMerchantChange(value: String) = form.update { it.copy(merchant = value) }

    fun onAmountChange(digits: String) = form.update { it.copy(amountDigits = digits, amountError = false) }

    fun onDateChange(date: LocalDate) = form.update { it.copy(date = date, dateError = false) }

    fun onCategoryChange(categoryId: Long) = form.update { it.copy(categoryId = categoryId, categoryError = false) }

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
                source = TransactionSource.MANUAL,
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
    val amountError: Boolean = false,
    val categoryError: Boolean = false,
    val dateError: Boolean = false,
    val isSaving: Boolean = false,
    /** Becomes true once saved; the screen reacts by navigating back. */
    val isSaved: Boolean = false,
)

data class ConfirmUiState(
    val form: ConfirmForm,
    val categories: List<Category>,
    /** Latest selectable date (today). */
    val maxDate: LocalDate,
) {
    val selectedCategory: Category? get() = categories.firstOrNull { it.id == form.categoryId }
}
