package com.hanan.mysavings.domain.model

/** When a reminder is sent, relative to an installment's due date. */
enum class DebtReminderKind(
    /** Stored in the database; never rename. */
    val key: String,
) {
    /** A few days before (see CheckDebtRemindersUseCase.DAYS_BEFORE). */
    DUE_SOON("soon"),

    /** On the due date itself. */
    DUE_TODAY("today"),
}

/** A notification-worthy upcoming payment. */
data class DebtReminder(
    val debtId: Long,
    val creditor: String,
    val installment: Installment,
    /** What is still to pay for this installment. */
    val amountDue: Long,
    val kind: DebtReminderKind,
    /** 0 for DUE_TODAY. */
    val daysLeft: Int,
)
