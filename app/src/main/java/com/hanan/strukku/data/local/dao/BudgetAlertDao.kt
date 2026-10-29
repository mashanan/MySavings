package com.hanan.strukku.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hanan.strukku.data.local.entity.BudgetAlertEntity

@Dao
interface BudgetAlertDao {
    @Query("SELECT thresholdPercent FROM budget_alerts WHERE yearMonth = :yearMonth")
    suspend fun sentThresholds(yearMonth: Int): List<Int>

    // IGNORE: marking an already-sent threshold again is harmless and keeps the original time.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(alerts: List<BudgetAlertEntity>)

    @Query("DELETE FROM budget_alerts WHERE yearMonth = :yearMonth")
    suspend fun clear(yearMonth: Int)
}
