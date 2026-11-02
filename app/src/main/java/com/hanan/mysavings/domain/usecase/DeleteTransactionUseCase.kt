package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Deletes a transaction. No budget check afterwards: spending can only go down, and budget
 * alerts are never taken back.
 */
class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(id: Long) = transactionRepository.delete(id)
}
