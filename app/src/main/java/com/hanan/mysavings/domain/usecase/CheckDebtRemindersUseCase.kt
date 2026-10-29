package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.DebtReminder
import com.hanan.mysavings.domain.model.DebtReminderKind
import com.hanan.mysavings.domain.model.DebtStatus
import com.hanan.mysavings.domain.repository.DebtReminderRepository
import com.hanan.mysavings.domain.repository.DebtRepository
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Decides which upcoming installments the user should be reminded of today, and records
 * them so no reminder is sent twice.
 *
 * Each unpaid installment gets at most two reminders: one when it is [DAYS_BEFORE] days
 * away or closer, and one on the due date. "Or closer" matters: the background check runs
 * roughly once a day and may skip a day (phone off), so a reminder missed on day -3 still
 * goes out on day -2 or -1 instead of never.
 */
class CheckDebtRemindersUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
    private val reminderRepository: DebtReminderRepository,
    private val clock: Clock,
) {
    /** @return the reminders to show now; empty when there is nothing new. */
    suspend operator fun invoke(): List<DebtReminder> {
        val today = LocalDate.now(clock)
        val reminders = debtRepository.getDebtsWithPaid().flatMap { (debt, paid) ->
            DebtStatus.of(debt, paid, today).installments
                .filter { !it.isPaid }
                .mapNotNull { status ->
                    val daysLeft = ChronoUnit.DAYS.between(today, status.installment.dueDate).toInt()
                    val kind = when (daysLeft) {
                        0 -> DebtReminderKind.DUE_TODAY
                        in 1..DAYS_BEFORE -> DebtReminderKind.DUE_SOON
                        // Overdue installments are shown in the app in red; nagging about them
                        // every day would just train the user to ignore the notifications.
                        else -> return@mapNotNull null
                    }
                    DebtReminder(debt.id, debt.creditor, status.installment, status.remaining, kind, daysLeft)
                }
        }

        return reminders.filter { reminder ->
            val sent = reminderRepository.wasSent(reminder.debtId, reminder.installment.number, reminder.kind)
            if (!sent) reminderRepository.markSent(reminder.debtId, reminder.installment.number, reminder.kind)
            !sent
        }
    }

    companion object {
        /** The owner asked for reminders three days ahead (H-3). */
        const val DAYS_BEFORE = 3
    }
}
