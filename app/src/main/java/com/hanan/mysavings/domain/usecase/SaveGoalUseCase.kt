package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.GoalCategory
import com.hanan.mysavings.domain.model.SavingsGoal
import com.hanan.mysavings.domain.repository.SavingsRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** Creates a goal (`id == null`) or updates an existing one. */
class SaveGoalUseCase @Inject constructor(
    private val savingsRepository: SavingsRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        id: Long?,
        name: String,
        targetAmount: Long,
        category: GoalCategory,
        targetDate: LocalDate?,
    ): SaveGoalResult {
        val trimmedName = name.trim()
        val existing = id?.let { savingsRepository.getGoal(it) ?: return SaveGoalResult.Invalid(GoalError.NOT_FOUND) }

        val errors = buildSet {
            if (trimmedName.isEmpty()) add(GoalError.NAME_BLANK)
            if (targetAmount <= 0) add(GoalError.TARGET_NOT_POSITIVE)
            // A deadline in the past is a typo for a new goal, but an overdue goal being edited
            // (e.g. renamed) may keep the date it already had.
            if (targetDate != null && targetDate.isBefore(LocalDate.now(clock)) && targetDate != existing?.targetDate) {
                add(GoalError.TARGET_DATE_IN_PAST)
            }
        }
        if (errors.isNotEmpty()) return SaveGoalResult.Invalid(errors)

        val savedId = savingsRepository.saveGoal(
            SavingsGoal(
                id = existing?.id ?: 0,
                name = trimmedName,
                targetAmount = targetAmount,
                category = category,
                targetDate = targetDate,
                createdAt = existing?.createdAt ?: Instant.now(clock),
            ),
        )
        return SaveGoalResult.Success(savedId)
    }
}

sealed interface SaveGoalResult {
    data class Success(val id: Long) : SaveGoalResult

    /** All problems at once, so the form can mark every field in one go. */
    data class Invalid(val errors: Set<GoalError>) : SaveGoalResult {
        constructor(error: GoalError) : this(setOf(error))
    }
}

enum class GoalError { NAME_BLANK, TARGET_NOT_POSITIVE, TARGET_DATE_IN_PAST, NOT_FOUND }
