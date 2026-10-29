package com.hanan.strukku.domain.usecase

import com.hanan.strukku.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

/** Total spent in one month, for the trend chart. */
data class MonthTotal(
    val yearMonth: YearMonth,
    val total: Long,
)

/**
 * Spending for [count] consecutive months ending at `endMonth`, oldest first. Months
 * without transactions are included as 0, so the chart always has [count] bars in order.
 */
class ObserveMonthlyTotalsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(endMonth: YearMonth, count: Int = DEFAULT_MONTHS): Flow<List<MonthTotal>> {
        val months = (count - 1 downTo 0).map { endMonth.minusMonths(it.toLong()) }
        return transactionRepository
            .observeMonthlyTotals(from = months.first().atDay(1), to = endMonth.atEndOfMonth())
            .map { totals -> months.map { MonthTotal(it, totals[it] ?: 0L) } }
    }

    companion object {
        const val DEFAULT_MONTHS = 6
    }
}
