package com.hanan.mysavings.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hanan.mysavings.data.local.entity.SavingsDepositEntity
import com.hanan.mysavings.data.local.entity.SavingsGoalEntity
import com.hanan.mysavings.data.local.entity.SyncCollection
import com.hanan.mysavings.data.local.entity.SyncTombstoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {
    /**
     * Each goal with its balance, summed in SQL. LEFT JOIN keeps goals without deposits;
     * COALESCE turns their NULL sum into 0.
     */
    @Query(
        """
        SELECT savings_goals.*, COALESCE(SUM(savings_deposits.amount), 0) AS saved
        FROM savings_goals
        LEFT JOIN savings_deposits ON savings_deposits.goalId = savings_goals.id
        GROUP BY savings_goals.id
        """,
    )
    fun observeGoalsWithSaved(): Flow<List<GoalWithSavedRow>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    fun observeGoal(id: Long): Flow<SavingsGoalEntity?>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getGoal(id: Long): SavingsGoalEntity?

    @Query("SELECT * FROM savings_deposits WHERE goalId = :goalId ORDER BY date DESC, createdAt DESC")
    fun observeDeposits(goalId: Long): Flow<List<SavingsDepositEntity>>

    @Query("SELECT * FROM savings_deposits WHERE id = :id")
    suspend fun getDeposit(id: Long): SavingsDepositEntity?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM savings_deposits WHERE goalId = :goalId")
    suspend fun getSaved(goalId: Long): Long

    @Insert
    suspend fun insertGoal(goal: SavingsGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: SavingsGoalEntity)

    @Insert
    suspend fun insertDeposit(deposit: SavingsDepositEntity): Long

    // Deposits go with it through the CASCADE foreign key.
    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("DELETE FROM savings_deposits WHERE id = :id")
    suspend fun deleteDeposit(id: Long)

    @Query("SELECT uuid FROM savings_deposits WHERE goalId = :goalId")
    suspend fun depositUuids(goalId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTombstones(tombstones: List<SyncTombstoneEntity>)

    /**
     * Deletes a goal with its deposits and remembers all their uuids, so sync can tell the
     * other phones. The CASCADE removes the deposits here; listing them explicitly means the
     * cloud copies go too, instead of lingering without a goal.
     */
    @Transaction
    suspend fun deleteGoalTracked(id: Long, now: Long) {
        val goal = getGoal(id) ?: return
        insertTombstones(
            depositUuids(id).map { SyncTombstoneEntity(SyncCollection.SAVINGS_DEPOSITS, it, now) } +
                SyncTombstoneEntity(SyncCollection.SAVINGS_GOALS, goal.sync.uuid, now),
        )
        deleteGoal(id)
    }

    @Transaction
    suspend fun deleteDepositTracked(id: Long, now: Long) {
        val deposit = getDeposit(id) ?: return
        insertTombstones(listOf(SyncTombstoneEntity(SyncCollection.SAVINGS_DEPOSITS, deposit.sync.uuid, now)))
        deleteDeposit(id)
    }
}

/** Result row of [SavingsDao.observeGoalsWithSaved]. */
data class GoalWithSavedRow(
    @Embedded val goal: SavingsGoalEntity,
    val saved: Long,
)
