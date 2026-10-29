package com.hanan.strukku.domain.repository

import com.hanan.strukku.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface TransactionRepository {
    /** Saves a new transaction and returns its generated id. */
    suspend fun add(transaction: Transaction): Long

    /** Total spent per category id between [from] and [to], both inclusive. */
    fun observeSpendingByCategory(from: LocalDate, to: LocalDate): Flow<Map<Long, Long>>

    /** Total spent per calendar month between [from] and [to], both inclusive. Months without spending are absent. */
    fun observeMonthlyTotals(from: LocalDate, to: LocalDate): Flow<Map<YearMonth, Long>>

    /** All transactions between [from] and [to], both inclusive, oldest first. */
    suspend fun getInRange(from: LocalDate, to: LocalDate): List<Transaction>

    /** The [limit] most recent transactions, newest first. */
    fun observeRecent(limit: Int): Flow<List<Transaction>>
}
