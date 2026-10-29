package com.hanan.strukku.domain.repository

import com.hanan.strukku.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TransactionRepository {
    /** Saves a new transaction and returns its generated id. */
    suspend fun add(transaction: Transaction): Long

    /** Total spent per category id between [from] and [to], both inclusive. */
    fun observeSpendingByCategory(from: LocalDate, to: LocalDate): Flow<Map<Long, Long>>

    /** The [limit] most recent transactions, newest first. */
    fun observeRecent(limit: Int): Flow<List<Transaction>>
}
