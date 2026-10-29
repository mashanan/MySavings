package com.hanan.strukku.data.repository

import com.hanan.strukku.data.local.dao.TransactionDao
import com.hanan.strukku.domain.model.Transaction
import com.hanan.strukku.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class RoomTransactionRepository @Inject constructor(
    private val dao: TransactionDao,
) : TransactionRepository {

    override suspend fun add(transaction: Transaction): Long = dao.insert(transaction.toEntity())

    override fun observeSpendingByCategory(from: LocalDate, to: LocalDate): Flow<Map<Long, Long>> =
        dao.observeTotalsByCategory(from.toEpochDay(), to.toEpochDay())
            .map { rows -> rows.associate { it.categoryId to it.total } }

    override fun observeRecent(limit: Int): Flow<List<Transaction>> =
        dao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }
}
