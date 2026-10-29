package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.Debt
import com.hanan.mysavings.domain.model.DebtPayment
import com.hanan.mysavings.domain.model.DebtStatus
import com.hanan.mysavings.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class DebtDetail(
    val debt: Debt,
    val status: DebtStatus,
    /** Newest first. */
    val payments: List<DebtPayment>,
)

/** One debt with its schedule and payments. Emits `null` once the debt is deleted. */
class ObserveDebtDetailUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
    private val clock: Clock,
) {
    operator fun invoke(debtId: Long): Flow<DebtDetail?> = combine(
        debtRepository.observeDebt(debtId),
        debtRepository.observePayments(debtId),
    ) { debt, payments ->
        // The payments are loaded anyway, so the paid total is summed here instead of queried again.
        debt?.let { DebtDetail(it, DebtStatus.of(it, payments.sumOf { p -> p.amount }, LocalDate.now(clock)), payments) }
    }
}
