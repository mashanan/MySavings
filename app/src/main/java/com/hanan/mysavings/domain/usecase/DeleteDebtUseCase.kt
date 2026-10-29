package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.DebtRepository
import javax.inject.Inject

/** Deletes a debt together with its payments and reminders. */
class DeleteDebtUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
) {
    suspend operator fun invoke(debtId: Long) = debtRepository.deleteDebt(debtId)
}
