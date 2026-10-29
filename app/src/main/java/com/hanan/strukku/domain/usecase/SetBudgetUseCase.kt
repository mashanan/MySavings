package com.hanan.strukku.domain.usecase

import com.hanan.strukku.domain.model.Budget
import com.hanan.strukku.domain.repository.BudgetAlertRepository
import com.hanan.strukku.domain.repository.BudgetCheckScheduler
import com.hanan.strukku.domain.repository.BudgetRepository
import java.time.YearMonth
import javax.inject.Inject

/** Sets (or replaces) the overall budget or a per-category budget for a month. */
class SetBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val alertRepository: BudgetAlertRepository,
    private val budgetCheckScheduler: BudgetCheckScheduler,
) {
    /**
     * @param categoryId `null` to set the overall monthly budget.
     * @return `false` if [limitAmount] is not positive; nothing is saved in that case.
     */
    suspend operator fun invoke(yearMonth: YearMonth, categoryId: Long?, limitAmount: Long): Boolean {
        if (limitAmount <= 0) return false
        budgetRepository.upsert(Budget(yearMonth = yearMonth, categoryId = categoryId, limitAmount = limitAmount))
        if (categoryId == null) {
            // Alerts sent for the old limit no longer apply: start the month's alerts afresh
            // and re-check right away, since a lower budget may already be (nearly) used up.
            alertRepository.clear(yearMonth)
            budgetCheckScheduler.requestCheck()
        }
        return true
    }
}
