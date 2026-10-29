package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.GoalProgress
import com.hanan.mysavings.domain.model.SavingsGoal
import com.hanan.mysavings.domain.repository.SavingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class GoalWithProgress(
    val goal: SavingsGoal,
    val progress: GoalProgress,
)

/** Everything the Savings tab shows. */
data class SavingsOverview(
    /** Sum of all goal balances. */
    val totalSaved: Long,
    val reachedCount: Int,
    /** Open goals first (nearest deadline first), reached ones at the bottom. */
    val goals: List<GoalWithProgress>,
)

class ObserveSavingsGoalsUseCase @Inject constructor(
    private val savingsRepository: SavingsRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<SavingsOverview> = savingsRepository.observeGoalsWithSaved().map { rows ->
        // Read per emission: a goal can turn overdue while the app stays open.
        val today = LocalDate.now(clock)
        val goals = rows
            .map { GoalWithProgress(it.goal, GoalProgress.of(it.goal, it.saved, today)) }
            .sortedWith(GOAL_ORDER)
        SavingsOverview(
            totalSaved = rows.sumOf { it.saved },
            reachedCount = goals.count { it.progress.isReached },
            goals = goals,
        )
    }

    private companion object {
        // Open goals before reached ones; among open goals the nearest deadline first and goals
        // without a deadline after those; ties go to the newest goal.
        val GOAL_ORDER = compareBy<GoalWithProgress> { it.progress.isReached }
            .thenBy(nullsLast()) { it.goal.targetDate }
            .thenByDescending { it.goal.createdAt }
    }
}
