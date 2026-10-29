package com.hanan.mysavings.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hanan.mysavings.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    /** Sums per category in SQL, so only a few rows reach Kotlin no matter how many transactions exist. */
    @Query(
        """
        SELECT categoryId, SUM(amount) AS total
        FROM transactions
        WHERE date BETWEEN :fromEpochDay AND :toEpochDay
        GROUP BY categoryId
        """,
    )
    fun observeTotalsByCategory(fromEpochDay: Long, toEpochDay: Long): Flow<List<CategoryTotal>>

    /**
     * Totals per calendar month. `date` is an epoch day, so `date * 86400` is a Unix timestamp
     * that SQLite can format as yyyyMM (e.g. 202610), the same encoding budgets use.
     */
    @Query(
        """
        SELECT CAST(strftime('%Y%m', date * 86400, 'unixepoch') AS INTEGER) AS yearMonth, SUM(amount) AS total
        FROM transactions
        WHERE date BETWEEN :fromEpochDay AND :toEpochDay
        GROUP BY yearMonth
        """,
    )
    fun observeMonthlyTotals(fromEpochDay: Long, toEpochDay: Long): Flow<List<MonthTotalRow>>

    // One-shot (suspend, not Flow): a report is a snapshot, it should not change while being written.
    @Query("SELECT * FROM transactions WHERE date BETWEEN :fromEpochDay AND :toEpochDay ORDER BY date, createdAt")
    suspend fun getInRange(fromEpochDay: Long, toEpochDay: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>
}

/** Result row of [TransactionDao.observeTotalsByCategory]. */
data class CategoryTotal(
    val categoryId: Long,
    val total: Long,
)

/** Result row of [TransactionDao.observeMonthlyTotals]. */
data class MonthTotalRow(
    /** yyyyMM, e.g. 202610. */
    val yearMonth: Int,
    val total: Long,
)
