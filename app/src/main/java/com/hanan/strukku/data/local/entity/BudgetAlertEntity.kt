package com.hanan.strukku.data.local.entity

import androidx.room.Entity

/**
 * Records that a budget notification was sent, so each threshold fires at most once a month.
 * The composite primary key makes "already sent" a simple existence check.
 */
@Entity(tableName = "budget_alerts", primaryKeys = ["yearMonth", "thresholdPercent"])
data class BudgetAlertEntity(
    /** yyyyMM, same encoding as [BudgetEntity.yearMonth]. */
    val yearMonth: Int,
    val thresholdPercent: Int,
    /** Epoch millis, kept for debugging and a possible notification history later. */
    val sentAt: Long,
)
