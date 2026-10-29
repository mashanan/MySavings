package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.Debt
import com.hanan.mysavings.domain.model.DebtStatus
import com.hanan.mysavings.domain.model.InstallmentStatus
import com.hanan.mysavings.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class DebtWithStatus(
    val debt: Debt,
    val status: DebtStatus,
)

/** One installment on the calendar. */
data class DueItem(
    val debtId: Long,
    val creditor: String,
    val installment: InstallmentStatus,
    /** Number of installments of the debt, to show "2/12". */
    val installmentCount: Int,
)

/** Everything the Debts tab shows. */
data class DebtsOverview(
    /** Still to pay across all debts. */
    val totalRemaining: Long,
    /** Installments past their due date and not fully paid, across all debts. */
    val overdueCount: Int,
    /** Not paid off, the most urgent (earliest next due date) first. */
    val active: List<DebtWithStatus>,
    /** Paid off, most recent due date first. */
    val paidOff: List<DebtWithStatus>,
    /** Every installment by its due date, paid or not, for the calendar. */
    val dueByDate: Map<LocalDate, List<DueItem>>,
)

class ObserveDebtsUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<DebtsOverview> = debtRepository.observeDebtsWithPaid().map { rows ->
        // Read per emission: an installment can turn overdue while the app stays open.
        val today = LocalDate.now(clock)
        val debts = rows.map { DebtWithStatus(it.debt, DebtStatus.of(it.debt, it.paid, today)) }
        val (paidOff, active) = debts.partition { it.status.isPaidOff }
        DebtsOverview(
            totalRemaining = active.sumOf { it.status.remaining },
            overdueCount = active.sumOf { it.status.overdueCount },
            active = active.sortedBy { it.status.next?.installment?.dueDate },
            paidOff = paidOff.sortedByDescending { it.status.installments.last().installment.dueDate },
            dueByDate = debts
                .flatMap { item ->
                    item.status.installments.map { DueItem(item.debt.id, item.debt.creditor, it, item.debt.installmentCount) }
                }
                .groupBy { it.installment.installment.dueDate },
        )
    }
}
