package com.hanan.mysavings.domain.model

import java.time.LocalDate

object DebtSchedule {
    /**
     * The installments of [debt], in order. The total is split evenly; the rupiah left over
     * by the division go to the last installment, so the installments always add up to the
     * total exactly (e.g. 1.000.000 in 3 -> 333.333, 333.333, 333.334).
     *
     * Every due date is counted from the first one, not from the previous one, so a debt due
     * on the 31st falls on the last day of shorter months (28/29 Feb, 30 Apr) and is back on
     * the 31st in March.
     */
    fun of(debt: Debt): List<Installment> {
        val count = debt.installmentCount
        val base = debt.totalAmount / count
        val remainder = debt.totalAmount % count
        return (0 until count).map { index ->
            Installment(
                number = index + 1,
                dueDate = debt.firstDueDate.plusMonths(index.toLong()),
                amount = if (index == count - 1) base + remainder else base,
            )
        }
    }
}

/** An installment and how much of it the payments so far have covered. */
data class InstallmentStatus(
    val installment: Installment,
    val paidAmount: Long,
    val isOverdue: Boolean,
) {
    val isPaid: Boolean get() = paidAmount >= installment.amount
    val remaining: Long get() = installment.amount - paidAmount
}

/** Where a debt stands on a given day. */
data class DebtStatus(
    val paid: Long,
    val remaining: Long,
    val installments: List<InstallmentStatus>,
) {
    val isPaidOff: Boolean get() = remaining <= 0

    /** The first installment not fully paid yet, or `null` once paid off. */
    val next: InstallmentStatus? get() = installments.firstOrNull { !it.isPaid }

    val overdueCount: Int get() = installments.count { it.isOverdue }

    companion object {
        fun of(debt: Debt, paid: Long, today: LocalDate): DebtStatus {
            // Pour the paid amount into the installments in order, like filling glasses.
            var left = paid
            val installments = DebtSchedule.of(debt).map { installment ->
                val covered = left.coerceIn(0, installment.amount)
                left -= covered
                InstallmentStatus(
                    installment = installment,
                    paidAmount = covered,
                    // Due today is not late yet; only the days after it are.
                    isOverdue = covered < installment.amount && installment.dueDate.isBefore(today),
                )
            }
            return DebtStatus(
                paid = paid,
                remaining = (debt.totalAmount - paid).coerceAtLeast(0),
                installments = installments,
            )
        }
    }
}
