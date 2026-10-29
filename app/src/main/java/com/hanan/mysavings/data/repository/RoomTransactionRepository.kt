package com.hanan.mysavings.data.repository

import com.hanan.mysavings.data.local.dao.TransactionDao
import com.hanan.mysavings.domain.model.Transaction
import com.hanan.mysavings.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class RoomTransactionRepository @Inject constructor(
    private val dao: TransactionDao,
) : TransactionRepository {

    override suspend fun add(transaction: Transaction): Long = dao.insert(transaction.toEntity())

    override fun observeSpendingByCategory(from: LocalDate, to: LocalDate): Flow<Map<Long, Long>> =
        dao.observeTotalsByCategory(from.toEpochDay(), to.toEpochDay())
            .map { rows -> rows.associate { it.categoryId to it.total } }

    override fun observeMonthlyTotals(from: LocalDate, to: LocalDate): Flow<Map<YearMonth, Long>> =
        dao.observeMonthlyTotals(from.toEpochDay(), to.toEpochDay())
            .map { rows -> rows.associate { it.yearMonth.toYearMonth() to it.total } }

    override fun observeCount(from: LocalDate, to: LocalDate): Flow<Int> =
        dao.observeCount(from.toEpochDay(), to.toEpochDay())

    override suspend fun getInRange(from: LocalDate, to: LocalDate): List<Transaction> =
        dao.getInRange(from.toEpochDay(), to.toEpochDay()).map { it.toDomain() }

    override fun observeRecent(limit: Int): Flow<List<Transaction>> =
        dao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }
}
