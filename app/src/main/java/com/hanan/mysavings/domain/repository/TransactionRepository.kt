package com.hanan.mysavings.domain.repository

import com.hanan.mysavings.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface TransactionRepository {
    /** Saves a new transaction and returns its generated id. */
    suspend fun add(transaction: Transaction): Long

    /** The transaction with this id, or null if it does not exist (anymore). */
    suspend fun get(id: Long): Transaction?

    /** Overwrites an existing transaction, matched by [Transaction.id]. */
    suspend fun update(transaction: Transaction)

    /** Deletes a transaction; does nothing if it is already gone. */
    suspend fun delete(id: Long)

    /** Total spent per category id between [from] and [to], both inclusive. */
    fun observeSpendingByCategory(from: LocalDate, to: LocalDate): Flow<Map<Long, Long>>

    /** Total spent per calendar month between [from] and [to], both inclusive. Months without spending are absent. */
    fun observeMonthlyTotals(from: LocalDate, to: LocalDate): Flow<Map<YearMonth, Long>>

    /** Total spent per day between [from] and [to], both inclusive. Days without spending are absent. */
    fun observeDailyTotals(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Long>>

    /** Number of transactions between [from] and [to], both inclusive. */
    fun observeCount(from: LocalDate, to: LocalDate): Flow<Int>

    /** Transactions between [from] and [to], both inclusive, newest first. Re-emits on every change. */
    fun observeInRange(from: LocalDate, to: LocalDate): Flow<List<Transaction>>

    /** All transactions between [from] and [to], both inclusive, oldest first. */
    suspend fun getInRange(from: LocalDate, to: LocalDate): List<Transaction>
}
