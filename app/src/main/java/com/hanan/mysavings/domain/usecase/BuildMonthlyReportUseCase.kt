package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.MonthlyReport
import com.hanan.mysavings.domain.model.TransactionWithCategory
import com.hanan.mysavings.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import javax.inject.Inject

/**
 * Collects a month's data for the PDF report. Totals, budget and categories come from the
 * same use case as the Home and Stats screens, so the report always agrees with the app.
 */
class BuildMonthlyReportUseCase @Inject constructor(
    private val observeMonthlySummary: ObserveMonthlySummaryUseCase,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(yearMonth: YearMonth): MonthlyReport {
        val summary = observeMonthlySummary(yearMonth).first()
        // Every transaction of the month belongs to a category with spending this month.
        val categoriesById = summary.spendingByCategory.associate { it.category.id to it.category }
        val transactions = transactionRepository
            .getInRange(yearMonth.atDay(1), yearMonth.atEndOfMonth())
            .mapNotNull { transaction ->
                categoriesById[transaction.categoryId]?.let { TransactionWithCategory(transaction, it) }
            }

        return MonthlyReport(
            yearMonth = yearMonth,
            totalSpent = summary.totalSpent,
            budgetLimit = summary.budgetLimit,
            spendingByCategory = summary.spendingByCategory,
            transactions = transactions,
        )
    }
}
