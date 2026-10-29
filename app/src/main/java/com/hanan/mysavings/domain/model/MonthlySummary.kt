package com.hanan.mysavings.domain.model

import java.time.YearMonth

/** Everything the Home screen needs for one month, computed in the domain layer. */
data class MonthlySummary(
    val yearMonth: YearMonth,
    val totalSpent: Long,
    /** Overall budget for the month, or `null` if the user has not set one. */
    val budgetLimit: Long?,
    /** Categories with spending this month, largest first. */
    val spendingByCategory: List<CategorySpending>,
) {
    /** Budget left this month. Negative when overspent, `null` without a budget. */
    val remaining: Long? get() = budgetLimit?.let { it - totalSpent }

    /** Share of the budget already used (1.0 = 100%), `null` without a budget. */
    val usedFraction: Float? get() = budgetLimit?.let { totalSpent.toFloat() / it }
}

data class CategorySpending(
    val category: Category,
    val amount: Long,
    /** Per-category budget, or `null` if none is set. */
    val budgetLimit: Long?,
)

data class TransactionWithCategory(
    val transaction: Transaction,
    val category: Category,
)
