package com.hanan.mysavings.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Money the user owes. Paid in one go ([installmentCount] = 1) or in monthly installments,
 * the first one due on [firstDueDate].
 *
 * @property totalAmount whole rupiah, always positive.
 */
data class Debt(
    val id: Long = 0,
    val creditor: String,
    val totalAmount: Long,
    val firstDueDate: LocalDate,
    val installmentCount: Int,
    val note: String?,
    val createdAt: Instant,
) {
    val isInstallmentPlan: Boolean get() = installmentCount > 1
}

/**
 * A payment towards a debt. Payments are not tied to a specific installment: they are
 * added up and fill the installments in order, so paying early or paying part of an
 * installment both just work.
 */
data class DebtPayment(
    val id: Long = 0,
    val debtId: Long,
    val amount: Long,
    val date: LocalDate,
    val note: String?,
    val createdAt: Instant,
)

/** A debt together with how much has been paid so far. */
data class DebtWithPaid(
    val debt: Debt,
    val paid: Long,
)

/** One due date of a debt. [number] starts at 1. */
data class Installment(
    val number: Int,
    val dueDate: LocalDate,
    val amount: Long,
)
