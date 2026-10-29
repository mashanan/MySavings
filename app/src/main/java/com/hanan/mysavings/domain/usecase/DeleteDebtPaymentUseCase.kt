package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.DebtRepository
import javax.inject.Inject

/**
 * Removes a payment, e.g. one entered by mistake. Always allowed: it only raises what is
 * still owed, which can never become invalid (unlike a savings withdrawal).
 */
class DeleteDebtPaymentUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
) {
    suspend operator fun invoke(paymentId: Long) = debtRepository.deletePayment(paymentId)
}
