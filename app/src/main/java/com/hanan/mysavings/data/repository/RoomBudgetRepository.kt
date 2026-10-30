package com.hanan.mysavings.data.repository

import com.hanan.mysavings.data.local.dao.BudgetDao
import com.hanan.mysavings.data.local.entity.SyncColumns
import com.hanan.mysavings.data.local.entity.budgetUuid
import com.hanan.mysavings.domain.model.Budget
import com.hanan.mysavings.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

class RoomBudgetRepository @Inject constructor(
    private val dao: BudgetDao,
    private val clock: Clock,
) : BudgetRepository {

    override fun observeForMonth(yearMonth: YearMonth): Flow<List<Budget>> =
        dao.observeForMonth(yearMonth.toInt()).map { rows -> rows.map { it.toDomain() } }

    override suspend fun upsert(budget: Budget) {
        val month = budget.yearMonth.toInt()
        val categoryKey = budget.categoryId?.let { dao.categoryKey(it) }
        dao.upsert(budget.toEntity(SyncColumns(uuid = budgetUuid(month, categoryKey), updatedAt = clock.millis())))
    }
}
