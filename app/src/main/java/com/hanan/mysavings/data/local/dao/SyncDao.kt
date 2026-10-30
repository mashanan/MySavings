package com.hanan.mysavings.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {
    /**
     * How many changes wait for upload. Room re-runs it whenever one of these tables changes,
     * which is what starts a sync shortly after the user edits something. Downloads store rows
     * as clean, so they do not move this number and cannot trigger a sync loop.
     */
    @Query(
        """
        SELECT (SELECT COUNT(*) FROM transactions WHERE dirty = 1)
             + (SELECT COUNT(*) FROM budgets WHERE dirty = 1)
             + (SELECT COUNT(*) FROM savings_goals WHERE dirty = 1)
             + (SELECT COUNT(*) FROM savings_deposits WHERE dirty = 1)
             + (SELECT COUNT(*) FROM debts WHERE dirty = 1)
             + (SELECT COUNT(*) FROM debt_payments WHERE dirty = 1)
             + (SELECT COUNT(*) FROM user_profile WHERE dirty = 1)
             + (SELECT COUNT(*) FROM sync_tombstones)
        """,
    )
    fun observePendingCount(): Flow<Int>
}
