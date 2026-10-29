package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.DebtPayment
import com.hanan.mysavings.domain.repository.DebtRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** Records a payment towards a debt. */
class AddDebtPaymentUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(debtId: Long, amount: Long, date: LocalDate, note: String?): AddDebtPaymentResult {
        if (amount <= 0) return AddDebtPaymentResult.Invalid(PaymentError.AMOUNT_NOT_POSITIVE)
        if (date.isAfter(LocalDate.now(clock))) return AddDebtPaymentResult.Invalid(PaymentError.DATE_IN_FUTURE)
        val debt = debtRepository.getDebt(debtId) ?: return AddDebtPaymentResult.Invalid(PaymentError.NOT_FOUND)
        // Paying more than is owed is almost always a typo (an extra zero).
        if (amount > debt.totalAmount - debtRepository.getPaid(debtId)) {
            return AddDebtPaymentResult.Invalid(PaymentError.EXCEEDS_REMAINING)
        }

        val id = debtRepository.addPayment(
            DebtPayment(
                debtId = debtId,
                amount = amount,
                date = date,
                note = note?.trim()?.ifBlank { null },
                createdAt = Instant.now(clock),
            ),
        )
        return AddDebtPaymentResult.Success(id)
    }
}

sealed interface AddDebtPaymentResult {
    data class Success(val id: Long) : AddDebtPaymentResult
    data class Invalid(val error: PaymentError) : AddDebtPaymentResult
}

enum class PaymentError { AMOUNT_NOT_POSITIVE, DATE_IN_FUTURE, EXCEEDS_REMAINING, NOT_FOUND }
