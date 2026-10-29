package com.hanan.strukku.domain.usecase

import com.hanan.strukku.domain.model.CategorySpending
import com.hanan.strukku.domain.model.MonthlySummary
import com.hanan.strukku.domain.model.TransactionWithCategory
import com.hanan.strukku.domain.repository.BudgetRepository
import com.hanan.strukku.domain.repository.CategoryRepository
import com.hanan.strukku.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth
import javax.inject.Inject

/**
 * Builds the Home screen summary for a month. Each source is a Room Flow, so the summary
 * re-emits automatically whenever a transaction, budget or category changes.
 */
class ObserveMonthlySummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
) {
    operator fun invoke(yearMonth: YearMonth): Flow<MonthlySummary> = combine(
        categoryRepository.observeAll(),
        transactionRepository.observeSpendingByCategory(yearMonth.atDay(1), yearMonth.atEndOfMonth()),
        budgetRepository.observeForMonth(yearMonth),
        transactionRepository.observeRecent(RECENT_LIMIT),
    ) { categories, spendingByCategoryId, budgets, recent ->
        val categoriesById = categories.associateBy { it.id }
        val categoryBudgets = budgets
            .mapNotNull { budget -> budget.categoryId?.let { it to budget.limitAmount } }
            .toMap()

        MonthlySummary(
            yearMonth = yearMonth,
            totalSpent = spendingByCategoryId.values.sum(),
            budgetLimit = budgets.firstOrNull { it.categoryId == null }?.limitAmount,
            spendingByCategory = spendingByCategoryId
                .mapNotNull { (categoryId, amount) ->
                    categoriesById[categoryId]?.let { CategorySpending(it, amount, categoryBudgets[categoryId]) }
                }
                .sortedByDescending { it.amount },
            recentTransactions = recent.mapNotNull { transaction ->
                categoriesById[transaction.categoryId]?.let { TransactionWithCategory(transaction, it) }
            },
        )
    }

    companion object {
        const val RECENT_LIMIT = 10
    }
}
