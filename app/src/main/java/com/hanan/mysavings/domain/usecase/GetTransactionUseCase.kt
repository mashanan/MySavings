package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.Transaction
import com.hanan.mysavings.domain.repository.TransactionRepository
import javax.inject.Inject

/** Reads one transaction, e.g. to fill the edit form. Null when it no longer exists. */
class GetTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(id: Long): Transaction? = transactionRepository.get(id)
}
