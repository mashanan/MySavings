package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.SavingsRepository
import javax.inject.Inject

/** Removes one deposit or withdrawal, e.g. one entered by mistake. */
class DeleteDepositUseCase @Inject constructor(
    private val savingsRepository: SavingsRepository,
) {
    /**
     * @return `false` if removing it would leave the goal below zero; nothing is deleted then.
     * Example: deposit 100.000, withdraw 100.000, then delete the deposit: the balance would
     * become -100.000, so the withdrawal has to be deleted first.
     */
    suspend operator fun invoke(depositId: Long): Boolean {
        val deposit = savingsRepository.getDeposit(depositId) ?: return true
        if (savingsRepository.getSaved(deposit.goalId) - deposit.amount < 0) return false
        savingsRepository.deleteDeposit(depositId)
        return true
    }
}
