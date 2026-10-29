package com.hanan.mysavings.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Something the user is saving up for.
 *
 * @property targetAmount whole rupiah, always positive.
 * @property targetDate optional deadline; with it the app can suggest a monthly amount.
 */
data class SavingsGoal(
    val id: Long = 0,
    val name: String,
    val targetAmount: Long,
    val category: GoalCategory,
    val targetDate: LocalDate?,
    val createdAt: Instant,
)

/**
 * Money put into (positive [amount]) or taken out of (negative [amount]) a goal.
 * One signed column keeps the balance a plain SUM.
 */
data class SavingsDeposit(
    val id: Long = 0,
    val goalId: Long,
    val amount: Long,
    val date: LocalDate,
    val note: String?,
    val createdAt: Instant,
) {
    val isWithdrawal: Boolean get() = amount < 0
}

/**
 * Kinds of things to save for. A fixed list (not a table) so each has its own icon and a
 * translated name. Stored by [key], never by name or ordinal, so renaming or reordering an
 * entry cannot change what existing rows mean.
 */
enum class GoalCategory(val key: String) {
    GADGET("gadget"),
    VEHICLE("vehicle"),
    TRAVEL("travel"),
    HOME("home"),
    EDUCATION("education"),
    EMERGENCY("emergency"),
    OTHER("other"),
    ;

    companion object {
        /** Unknown keys (e.g. from a newer app version) fall back to [OTHER] instead of crashing. */
        fun fromKey(key: String): GoalCategory = entries.firstOrNull { it.key == key } ?: OTHER
    }
}

/** A goal together with how much has been saved for it. */
data class SavingsGoalWithSaved(
    val goal: SavingsGoal,
    val saved: Long,
)
