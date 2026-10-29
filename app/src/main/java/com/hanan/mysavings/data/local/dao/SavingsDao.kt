package com.hanan.mysavings.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hanan.mysavings.data.local.entity.SavingsDepositEntity
import com.hanan.mysavings.data.local.entity.SavingsGoalEntity
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
}

/** Result row of [SavingsDao.observeGoalsWithSaved]. */
data class GoalWithSavedRow(
    @Embedded val goal: SavingsGoalEntity,
    val saved: Long,
)
