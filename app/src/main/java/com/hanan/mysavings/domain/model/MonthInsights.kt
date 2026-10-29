package com.hanan.mysavings.domain.model

/** Quick facts about a month shown on Home, next to the [MonthlySummary] they are derived from. */
data class MonthInsights(
    val transactionCount: Int,
    /** Spending divided by the days that have passed, `null` before the month starts. */
    val averagePerDay: Long?,
    /** The category with the most spending, `null` when nothing was spent. */
    val topCategory: CategorySpending?,
    /** How much can still be spent per day, `null` without a budget, when it is used up, or for a past month. */
    val dailyAllowance: DailyAllowance?,
    /** Compared with the same days of the previous month, `null` when that period had no spending. */
    val comparison: PeriodComparison?,
)

data class DailyAllowance(
    val amountPerDay: Long,
    /** Days left in the month, today included. */
    val daysLeft: Int,
)

data class PeriodComparison(
    val previousTotal: Long,
    /** Change against [previousTotal] in percent: negative means less was spent this time. */
    val changePercent: Int,
)
