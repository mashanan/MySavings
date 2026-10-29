package com.hanan.strukku.domain.repository

import com.hanan.strukku.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

interface BudgetRepository {
    /** All budgets (overall and per category) for [yearMonth]. */
    fun observeForMonth(yearMonth: YearMonth): Flow<List<Budget>>

    /** Creates the budget, or replaces the limit if one already exists for the same month and category. */
    suspend fun upsert(budget: Budget)
}
