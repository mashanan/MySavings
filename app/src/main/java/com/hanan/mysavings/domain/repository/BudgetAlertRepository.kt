package com.hanan.mysavings.domain.repository

import com.hanan.mysavings.domain.model.BudgetAlertLevel
import java.time.YearMonth

/** Remembers which budget notifications were already sent, so none repeats within a month. */
interface BudgetAlertRepository {
    suspend fun sentLevels(yearMonth: YearMonth): Set<BudgetAlertLevel>

    suspend fun markSent(yearMonth: YearMonth, levels: Set<BudgetAlertLevel>)

    /** Forget sent alerts for [yearMonth], e.g. after the budget changed. */
    suspend fun clear(yearMonth: YearMonth)
}
