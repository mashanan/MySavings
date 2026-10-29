package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.DailyAllowance
import com.hanan.mysavings.domain.model.MonthInsights
import com.hanan.mysavings.domain.model.MonthlySummary
import com.hanan.mysavings.domain.model.PeriodComparison
import com.hanan.mysavings.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Derives the Home insights for the month of a [MonthlySummary]. Only the transaction count and
 * the previous month's spending need extra queries; the rest comes from the summary itself.
 *
 * `today` is a parameter rather than read from a clock here, so the caller decides when a
 * new day starts and the calculation stays a plain function of its inputs.
 */
class ObserveMonthInsightsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(summary: MonthlySummary, today: LocalDate): Flow<MonthInsights> {
        val month = summary.yearMonth
        val progress = MonthProgress.of(month, today)
        val previousMonth = month.minusMonths(1)
        // Same days of last month, so mid-month numbers are not compared with a full month.
        // A complete month is compared with the complete previous one, whatever their lengths
        // (all of September against all 31 days of August, 31 March against 28 February).
        val previousEnd = if (progress.daysElapsed >= month.lengthOfMonth()) {
            previousMonth.atEndOfMonth()
        } else {
            previousMonth.atDay(progress.daysElapsed.coerceIn(1, previousMonth.lengthOfMonth()))
        }

        return combine(
            transactionRepository.observeCount(month.atDay(1), month.atEndOfMonth()),
            transactionRepository.observeSpendingByCategory(previousMonth.atDay(1), previousEnd)
                .map { it.values.sum() },
        ) { count, previousTotal ->
            MonthInsights(
                transactionCount = count,
                averagePerDay = if (progress.daysElapsed > 0) summary.totalSpent / progress.daysElapsed else null,
                topCategory = summary.spendingByCategory.firstOrNull(),
                dailyAllowance = dailyAllowance(summary.remaining, progress.daysLeft),
                comparison = comparison(summary.totalSpent, previousTotal, progress.daysElapsed),
            )
        }
    }

    private fun dailyAllowance(remaining: Long?, daysLeft: Int): DailyAllowance? {
        if (remaining == null || remaining <= 0 || daysLeft == 0) return null
        // Rounded down: suggesting a little less is safer than overshooting the budget.
        return DailyAllowance(amountPerDay = remaining / daysLeft, daysLeft = daysLeft)
    }

    private fun comparison(currentTotal: Long, previousTotal: Long, daysElapsed: Int): PeriodComparison? {
        // A percentage of zero means nothing, and before the month starts there is nothing to compare.
        if (previousTotal <= 0 || daysElapsed == 0) return null
        // Double is fine here: this is a percentage for display, not a money amount.
        val changePercent = ((currentTotal - previousTotal) * 100.0 / previousTotal).roundToInt()
        return PeriodComparison(previousTotal, changePercent)
    }

    /**
     * Where [today] falls in a month. Today counts as elapsed (its spending is in the total)
     * and also as left (it can still be spent), so the two add up to one more than the month length.
     */
    private data class MonthProgress(val daysElapsed: Int, val daysLeft: Int) {
        companion object {
            fun of(month: YearMonth, today: LocalDate): MonthProgress {
                val current = YearMonth.from(today)
                val length = month.lengthOfMonth()
                return when {
                    month < current -> MonthProgress(daysElapsed = length, daysLeft = 0)
                    month > current -> MonthProgress(daysElapsed = 0, daysLeft = length)
                    else -> MonthProgress(daysElapsed = today.dayOfMonth, daysLeft = length - today.dayOfMonth + 1)
                }
            }
        }
    }
}
