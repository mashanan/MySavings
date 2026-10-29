package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.SavingsRepository
import javax.inject.Inject

/** Deletes a goal together with its whole history. */
class DeleteGoalUseCase @Inject constructor(
    private val savingsRepository: SavingsRepository,
) {
    suspend operator fun invoke(goalId: Long) = savingsRepository.deleteGoal(goalId)
}
