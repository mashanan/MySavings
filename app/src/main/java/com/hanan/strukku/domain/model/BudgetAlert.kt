package com.hanan.strukku.domain.model

import java.time.YearMonth

/** Budget usage levels that trigger a notification, lowest first. */
enum class BudgetAlertLevel(val percent: Int) {
    NEARLY_USED(80),
    USED_UP(100),
}

/** A notification-worthy budget state for one month. */
data class BudgetAlert(
    val level: BudgetAlertLevel,
    val yearMonth: YearMonth,
    val spent: Long,
    val limit: Long,
) {
    /** Negative when overspent. */
    val remaining: Long get() = limit - spent

    /** Rounded down, so 79.9% never shows as "80%". */
    val usedPercent: Int get() = (spent * 100 / limit).toInt()
}
