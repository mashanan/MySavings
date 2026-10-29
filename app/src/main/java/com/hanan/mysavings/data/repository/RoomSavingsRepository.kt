package com.hanan.mysavings.data.repository

import com.hanan.mysavings.data.local.dao.SavingsDao
import com.hanan.mysavings.domain.model.SavingsDeposit
import com.hanan.mysavings.domain.model.SavingsGoal
import com.hanan.mysavings.domain.model.SavingsGoalWithSaved
import com.hanan.mysavings.domain.repository.SavingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomSavingsRepository @Inject constructor(
    private val dao: SavingsDao,
) : SavingsRepository {

    override fun observeGoalsWithSaved(): Flow<List<SavingsGoalWithSaved>> =
        dao.observeGoalsWithSaved().map { rows -> rows.map { SavingsGoalWithSaved(it.goal.toDomain(), it.saved) } }

    override fun observeGoal(id: Long): Flow<SavingsGoal?> = dao.observeGoal(id).map { it?.toDomain() }

    override fun observeDeposits(goalId: Long): Flow<List<SavingsDeposit>> =
        dao.observeDeposits(goalId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getGoal(id: Long): SavingsGoal? = dao.getGoal(id)?.toDomain()

    override suspend fun getSaved(goalId: Long): Long = dao.getSaved(goalId)

    override suspend fun getDeposit(id: Long): SavingsDeposit? = dao.getDeposit(id)?.toDomain()

    override suspend fun saveGoal(goal: SavingsGoal): Long =
        if (goal.id == 0L) {
            dao.insertGoal(goal.toEntity())
        } else {
            dao.updateGoal(goal.toEntity())
            goal.id
        }

    override suspend fun addDeposit(deposit: SavingsDeposit): Long = dao.insertDeposit(deposit.toEntity())

    override suspend fun deleteGoal(id: Long) = dao.deleteGoal(id)

    override suspend fun deleteDeposit(id: Long) = dao.deleteDeposit(id)
}
