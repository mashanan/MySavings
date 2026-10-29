package com.hanan.mysavings.domain.repository

import com.hanan.mysavings.domain.model.SavingsDeposit
import com.hanan.mysavings.domain.model.SavingsGoal
import com.hanan.mysavings.domain.model.SavingsGoalWithSaved
import kotlinx.coroutines.flow.Flow

interface SavingsRepository {
    /** Every goal with its balance. */
    fun observeGoalsWithSaved(): Flow<List<SavingsGoalWithSaved>>

    /** The goal, or `null` once it is deleted. */
    fun observeGoal(id: Long): Flow<SavingsGoal?>

    /** Deposits and withdrawals of one goal, newest first. */
    fun observeDeposits(goalId: Long): Flow<List<SavingsDeposit>>

    suspend fun getGoal(id: Long): SavingsGoal?

    /** Current balance of a goal (deposits minus withdrawals). */
    suspend fun getSaved(goalId: Long): Long

    suspend fun getDeposit(id: Long): SavingsDeposit?

    /** Inserts when [SavingsGoal.id] is 0, otherwise updates. Returns the goal's id. */
    suspend fun saveGoal(goal: SavingsGoal): Long

    suspend fun addDeposit(deposit: SavingsDeposit): Long

    /** Deletes the goal and, with it, all of its deposits. */
    suspend fun deleteGoal(id: Long)

    suspend fun deleteDeposit(id: Long)
}
