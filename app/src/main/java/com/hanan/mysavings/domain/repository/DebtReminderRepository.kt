package com.hanan.mysavings.domain.repository

import com.hanan.mysavings.domain.model.DebtReminderKind

/** Remembers which debt reminders were sent, so none repeats. */
interface DebtReminderRepository {
    suspend fun wasSent(debtId: Long, installmentNumber: Int, kind: DebtReminderKind): Boolean

    suspend fun markSent(debtId: Long, installmentNumber: Int, kind: DebtReminderKind)

    /** Forget a debt's reminders, e.g. after its schedule was edited. */
    suspend fun clear(debtId: Long)
}
