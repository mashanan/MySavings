package com.hanan.mysavings.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.hanan.mysavings.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    fun observeForMonth(yearMonth: Int): Flow<List<BudgetEntity>>

    // `IS` (not `=`) so that a NULL categoryId matches the overall budget row.
    @Query("SELECT id FROM budgets WHERE yearMonth = :yearMonth AND categoryId IS :categoryId LIMIT 1")
    suspend fun findId(yearMonth: Int, categoryId: Long?): Long?

    @Insert
    suspend fun insert(budget: BudgetEntity): Long

    @Query("UPDATE budgets SET limitAmount = :limitAmount WHERE id = :id")
    suspend fun updateLimit(id: Long, limitAmount: Long)

    /**
     * Manual upsert instead of `@Upsert`: the unique index cannot detect a duplicate overall
     * budget because NULL categoryIds never conflict. Running inside one transaction keeps
     * the read-then-write atomic.
     */
    @Transaction
    suspend fun upsert(budget: BudgetEntity) {
        val existingId = findId(budget.yearMonth, budget.categoryId)
        if (existingId == null) insert(budget) else updateLimit(existingId, budget.limitAmount)
    }
}
