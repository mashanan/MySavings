package com.hanan.mysavings.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

/** One line from each tab, for the profile overview. */
data class FinancialSummary(
    val spentThisMonth: Long,
    val totalSaved: Long,
    val debtRemaining: Long,
)

/**
 * Puts the three tabs' headline numbers side by side. It reuses their use cases instead of
 * querying again, so each number always matches what its own tab shows.
 */
class ObserveFinancialSummaryUseCase @Inject constructor(
    private val observeMonthlySummary: ObserveMonthlySummaryUseCase,
    private val observeSavingsGoals: ObserveSavingsGoalsUseCase,
    private val observeDebts: ObserveDebtsUseCase,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<FinancialSummary> = combine(
        observeMonthlySummary(YearMonth.now(clock)),
        observeSavingsGoals(),
        observeDebts(),
    ) { month, savings, debts ->
        FinancialSummary(
            spentThisMonth = month.totalSpent,
            totalSaved = savings.totalSaved,
            debtRemaining = debts.totalRemaining,
        )
    }
}
