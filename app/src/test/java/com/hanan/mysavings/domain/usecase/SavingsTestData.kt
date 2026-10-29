package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.GoalCategory
import com.hanan.mysavings.domain.model.SavingsDeposit
import com.hanan.mysavings.domain.model.SavingsGoal
import com.hanan.mysavings.testutil.MutableClock
import java.time.Instant
import java.time.LocalDate

/** Shared fixtures for the savings use case tests. "Today" is 29 October 2026 in Jakarta. */
internal object SavingsTestData {
    val today: LocalDate = LocalDate.of(2026, 10, 29)

    fun clock() = MutableClock(Instant.parse("2026-10-29T03:00:00Z"))

    fun goal(
        id: Long = 1,
        name: String = "Laptop",
        target: Long = 1_000_000,
        category: GoalCategory = GoalCategory.GADGET,
        deadline: LocalDate? = null,
        createdAt: Instant = Instant.EPOCH,
    ) = SavingsGoal(id, name, target, category, deadline, createdAt)

    fun deposit(id: Long, amount: Long, goalId: Long = 1, date: LocalDate = today) =
        SavingsDeposit(id, goalId, amount, date, null, Instant.EPOCH)
}
