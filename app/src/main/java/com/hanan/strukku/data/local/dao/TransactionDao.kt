package com.hanan.strukku.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hanan.strukku.data.local.entity.TransactionEntity
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

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>
}

/** Result row of [TransactionDao.observeTotalsByCategory]. */
data class CategoryTotal(
    val categoryId: Long,
    val total: Long,
)
