package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** Total spent on one day, for the Home chart. */
data class DayTotal(
    val date: LocalDate,
    val total: Long,
)

/**
 * Spending for [days] consecutive days ending at `endDate`, oldest first. Days without
 * transactions are included as 0, so the chart always has [days] bars in order.
 */
class ObserveDailyTotalsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(endDate: LocalDate, days: Int = DEFAULT_DAYS): Flow<List<DayTotal>> {
        val dates = (days - 1 downTo 0).map { endDate.minusDays(it.toLong()) }
        return transactionRepository
            .observeDailyTotals(from = dates.first(), to = endDate)
            .map { totals -> dates.map { DayTotal(it, totals[it] ?: 0L) } }
    }

    companion object {
        const val DEFAULT_DAYS = 7
    }
}
