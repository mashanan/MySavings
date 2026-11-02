package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.Transaction
import com.hanan.mysavings.domain.model.TransactionSource
import com.hanan.mysavings.domain.repository.BudgetCheckScheduler
import com.hanan.mysavings.domain.repository.TransactionRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Validates and saves a new expense. Validation lives in the domain (not in the ViewModel) so
 * the same rules apply to manual entry, scanned receipts and edits.
 */
class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetCheckScheduler: BudgetCheckScheduler,
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
    ): SaveTransactionResult {
        validateTransaction(amount, date, LocalDate.now(clock))?.let { return SaveTransactionResult.Invalid(it) }

        val id = transactionRepository.add(
            Transaction(
                amount = amount,
                date = date,
                merchant = merchant.cleaned(),
                note = note.cleaned(),
                categoryId = categoryId,
                source = source,
                createdAt = Instant.now(clock),
            ),
        )
        // A new expense may push the month over a budget threshold.
        budgetCheckScheduler.requestCheck()
        return SaveTransactionResult.Success(id)
    }
}

/** Trimmed text, with blank input stored as "no value". Shared with [UpdateTransactionUseCase]. */
internal fun String?.cleaned(): String? = this?.trim()?.ifBlank { null }
