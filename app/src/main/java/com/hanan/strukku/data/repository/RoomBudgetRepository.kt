package com.hanan.strukku.data.repository

import com.hanan.strukku.data.local.dao.BudgetDao
import com.hanan.strukku.domain.model.Budget
import com.hanan.strukku.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

class RoomBudgetRepository @Inject constructor(
    private val dao: BudgetDao,
) : BudgetRepository {

    override fun observeForMonth(yearMonth: YearMonth): Flow<List<Budget>> =
        dao.observeForMonth(yearMonth.toInt()).map { rows -> rows.map { it.toDomain() } }

    override suspend fun upsert(budget: Budget) = dao.upsert(budget.toEntity())
}
