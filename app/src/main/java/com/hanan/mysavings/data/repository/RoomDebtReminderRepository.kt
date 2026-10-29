package com.hanan.mysavings.data.repository

import com.hanan.mysavings.data.local.dao.DebtDao
import com.hanan.mysavings.data.local.entity.DebtReminderEntity
import com.hanan.mysavings.domain.model.DebtReminderKind
import com.hanan.mysavings.domain.repository.DebtReminderRepository
import java.time.Clock
import javax.inject.Inject

class RoomDebtReminderRepository @Inject constructor(
    private val dao: DebtDao,
    private val clock: Clock,
) : DebtReminderRepository {

    override suspend fun wasSent(debtId: Long, installmentNumber: Int, kind: DebtReminderKind): Boolean =
        dao.reminderSent(debtId, installmentNumber, kind.key)

    override suspend fun markSent(debtId: Long, installmentNumber: Int, kind: DebtReminderKind) =
        dao.insertReminder(DebtReminderEntity(debtId, installmentNumber, kind.key, clock.millis()))

    override suspend fun clear(debtId: Long) = dao.clearReminders(debtId)
}
