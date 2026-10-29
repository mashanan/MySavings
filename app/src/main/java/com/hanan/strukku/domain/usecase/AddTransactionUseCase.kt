package com.hanan.strukku.domain.usecase

import com.hanan.strukku.domain.model.Transaction
import com.hanan.strukku.domain.model.TransactionSource
import com.hanan.strukku.domain.repository.TransactionRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Validates and saves a new expense. Validation lives here (not in the ViewModel) so the
 * same rules apply to manual entry and scanned receipts.
 */
class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    // Injected instead of calling LocalDate.now() so tests control "today".
    private val clock: Clock,
) {
    suspend operator fun invoke(
        amount: Long,
        date: LocalDate,
        merchant: String?,
        note: String?,
        categoryId: Long,
        source: TransactionSource,
    ): AddTransactionResult {
        if (amount <= 0) return AddTransactionResult.Invalid(TransactionError.AMOUNT_NOT_POSITIVE)
        if (date.isAfter(LocalDate.now(clock))) return AddTransactionResult.Invalid(TransactionError.DATE_IN_FUTURE)

        val id = transactionRepository.add(
            Transaction(
                amount = amount,
                date = date,
                merchant = merchant?.trim()?.ifBlank { null },
                note = note?.trim()?.ifBlank { null },
                categoryId = categoryId,
                source = source,
                createdAt = Instant.now(clock),
            ),
        )
        return AddTransactionResult.Success(id)
    }
}

sealed interface AddTransactionResult {
    data class Success(val id: Long) : AddTransactionResult
    data class Invalid(val error: TransactionError) : AddTransactionResult
}

// The UI maps each value to an Indonesian message from strings.xml.
enum class TransactionError { AMOUNT_NOT_POSITIVE, DATE_IN_FUTURE }
