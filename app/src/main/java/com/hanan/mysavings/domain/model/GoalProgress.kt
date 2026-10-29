package com.hanan.mysavings.domain.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** How far a goal has come, derived from what was saved and what "today" is. */
data class GoalProgress(
    val saved: Long,
    val target: Long,
    /** Still needed; 0 once reached, never negative. */
    val remaining: Long,
    /** saved / target. Can go above 1 when the user saved more than planned. */
    val fraction: Float,
    val isReached: Boolean,
    /** Amount to set aside each month to reach the target on time; `null` without a (future) deadline. */
    val monthlySuggestion: Long?,
    /** The deadline has passed and the goal is not reached. */
    val isOverdue: Boolean,
) {
    companion object {
        fun of(goal: SavingsGoal, saved: Long, today: LocalDate): GoalProgress {
            val remaining = (goal.targetAmount - saved).coerceAtLeast(0)
            val isReached = remaining == 0L
            val deadline = goal.targetDate
            val isOverdue = !isReached && deadline != null && deadline.isBefore(today)
            return GoalProgress(
                saved = saved,
                target = goal.targetAmount,
                remaining = remaining,
                fraction = saved.coerceAtLeast(0).toFloat() / goal.targetAmount,
                isReached = isReached,
                monthlySuggestion = if (isReached || isOverdue || deadline == null) null else monthlySuggestion(remaining, today, deadline),
                isOverdue = isOverdue,
            )
        }

        /**
         * Spread over the months from now up to and including the deadline's month, so a
         * deadline in March counts this month through March. Rounded up: rounding down would
         * leave the user a few rupiah short at the end.
         */
        private fun monthlySuggestion(remaining: Long, today: LocalDate, deadline: LocalDate): Long {
            val months = ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(deadline)) + 1
            return (remaining + months - 1) / months
        }
    }
}
