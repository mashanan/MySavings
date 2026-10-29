package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.GoalProgress
import com.hanan.mysavings.domain.model.SavingsDeposit
import com.hanan.mysavings.domain.model.SavingsGoal
import com.hanan.mysavings.domain.repository.SavingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class GoalDetail(
    val goal: SavingsGoal,
    val progress: GoalProgress,
    /** Newest first. */
    val deposits: List<SavingsDeposit>,
)

/** One goal with its history. Emits `null` once the goal is deleted, so the screen can close. */
class ObserveGoalDetailUseCase @Inject constructor(
    private val savingsRepository: SavingsRepository,
    private val clock: Clock,
) {
    operator fun invoke(goalId: Long): Flow<GoalDetail?> = combine(
        savingsRepository.observeGoal(goalId),
        savingsRepository.observeDeposits(goalId),
    ) { goal, deposits ->
        // The history is already loaded, so the balance is summed here instead of queried again.
        goal?.let { GoalDetail(it, GoalProgress.of(it, deposits.sumOf { d -> d.amount }, LocalDate.now(clock)), deposits) }
    }
}
