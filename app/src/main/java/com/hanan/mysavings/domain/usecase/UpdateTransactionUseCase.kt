package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.BudgetCheckScheduler
import com.hanan.mysavings.domain.repository.TransactionRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Validates and saves changes to an existing expense.
 *
 * Only what the user can edit changes. `source` (manual or scan) and `createdAt` stay as they
 * were: they describe how and when the expense was first recorded, which an edit does not change.
 */
class UpdateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetCheckScheduler: BudgetCheckScheduler,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        id: Long,
        amount: Long,
        date: LocalDate,
        merchant: String?,
        note: String?,
        categoryId: Long,
    ): SaveTransactionResult {
        validateTransaction(amount, date, LocalDate.now(clock))?.let { return SaveTransactionResult.Invalid(it) }
        val existing = transactionRepository.get(id) ?: return SaveTransactionResult.Invalid(TransactionError.NOT_FOUND)

        transactionRepository.update(
            existing.copy(
                amount = amount,
                date = date,
                merchant = merchant.cleaned(),
                note = note.cleaned(),
                categoryId = categoryId,
            ),
        )
        // A higher amount, another category or another month can cross a budget threshold.
        budgetCheckScheduler.requestCheck()
        return SaveTransactionResult.Success(id)
    }
}
