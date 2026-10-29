package com.hanan.strukku.domain.usecase

import com.hanan.strukku.domain.model.BudgetAlert
import com.hanan.strukku.domain.model.BudgetAlertLevel
import com.hanan.strukku.domain.repository.BudgetAlertRepository
import com.hanan.strukku.domain.repository.BudgetRepository
import com.hanan.strukku.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import javax.inject.Inject

/**
 * Decides whether the user should be told about their budget for a month, and records
 * the decision so the same alert is never sent twice in a month.
 */
class CheckBudgetAlertUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val alertRepository: BudgetAlertRepository,
) {
    /** @return the alert to show, or `null` when there is nothing new to say. */
    suspend operator fun invoke(yearMonth: YearMonth): BudgetAlert? {
        val limit = budgetRepository.observeForMonth(yearMonth).first()
            .firstOrNull { it.categoryId == null }?.limitAmount ?: return null
        val spent = transactionRepository
            .observeSpendingByCategory(yearMonth.atDay(1), yearMonth.atEndOfMonth())
            .first().values.sum()

        // Integer maths: spent / limit >= percent / 100, without rounding surprises.
        val reached = BudgetAlertLevel.entries.filter { spent * 100 >= limit * it.percent }.toSet()
        val newLevels = reached - alertRepository.sentLevels(yearMonth)
        if (newLevels.isEmpty()) return null

        // Mark every reached level, not just the highest: jumping from 50% to 120% in one
        // purchase sends only "used up", and "nearly used" must not follow it afterwards.
        alertRepository.markSent(yearMonth, reached)
        return BudgetAlert(newLevels.maxBy { it.percent }, yearMonth, spent, limit)
    }
}
