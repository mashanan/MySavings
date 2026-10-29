package com.hanan.mysavings.domain.model

import java.time.YearMonth

/** Everything printed in a monthly PDF report: a snapshot, not a live view. */
data class MonthlyReport(
    val yearMonth: YearMonth,
    val totalSpent: Long,
    /** Overall budget, or `null` when none was set for the month. */
    val budgetLimit: Long?,
    /** Largest first. */
    val spendingByCategory: List<CategorySpending>,
    /** Every transaction of the month, oldest first. */
    val transactions: List<TransactionWithCategory>,
) {
    val remaining: Long? get() = budgetLimit?.let { it - totalSpent }
}
