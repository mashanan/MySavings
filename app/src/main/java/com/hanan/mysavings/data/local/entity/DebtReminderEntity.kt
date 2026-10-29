package com.hanan.mysavings.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * Records that a reminder was sent for one installment, like [BudgetAlertEntity] does for
 * budgets. The composite primary key makes "already sent" a simple existence check.
 */
@Entity(
    tableName = "debt_reminders_sent",
    primaryKeys = ["debtId", "installmentNumber", "kind"],
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DebtReminderEntity(
    val debtId: Long,
    val installmentNumber: Int,
    /** `DebtReminderKind.key`. */
    val kind: String,
    /** Epoch millis, kept for debugging. */
    val sentAt: Long,
)
