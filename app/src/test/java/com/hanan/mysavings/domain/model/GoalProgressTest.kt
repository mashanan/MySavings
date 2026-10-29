package com.hanan.mysavings.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class GoalProgressTest {

    private val today = LocalDate.of(2026, 10, 29)

    private fun goal(target: Long = 1_000_000, deadline: LocalDate? = null) =
        SavingsGoal(1, "Laptop", target, GoalCategory.GADGET, deadline, Instant.EPOCH)

    @Test
    fun `part way there`() {
        val progress = GoalProgress.of(goal(), saved = 250_000, today)

        assertEquals(750_000L, progress.remaining)
        assertEquals(0.25f, progress.fraction, 0.0001f)
        assertFalse(progress.isReached)
        assertFalse(progress.isOverdue)
    }

    @Test
    fun `exactly the target counts as reached`() {
        val progress = GoalProgress.of(goal(), saved = 1_000_000, today)

        assertTrue(progress.isReached)
        assertEquals(0L, progress.remaining)
    }

    @Test
    fun `saving more than the target keeps remaining at zero and the fraction above one`() {
        val progress = GoalProgress.of(goal(), saved = 1_200_000, today)

        assertTrue(progress.isReached)
        assertEquals(0L, progress.remaining)
        assertEquals(1.2f, progress.fraction, 0.0001f)
    }

    @Test
    fun `monthly suggestion spreads the rest over this month up to the deadline month, rounded up`() {
        // October through March is 6 months; 1.000.000 / 6 = 166.666,67 -> 166.667.
        val progress = GoalProgress.of(goal(deadline = LocalDate.of(2027, 3, 1)), saved = 0, today)

        assertEquals(166_667L, progress.monthlySuggestion)
    }

    @Test
    fun `a deadline later this month asks for the whole rest now`() {
        val progress = GoalProgress.of(goal(deadline = LocalDate.of(2026, 10, 31)), saved = 400_000, today)

        assertEquals(600_000L, progress.monthlySuggestion)
    }

    @Test
    fun `a deadline of today is not overdue yet`() {
        val progress = GoalProgress.of(goal(deadline = today), saved = 0, today)

        assertFalse(progress.isOverdue)
        assertEquals(1_000_000L, progress.monthlySuggestion)
    }

    @Test
    fun `a passed deadline is overdue and gets no suggestion`() {
        val progress = GoalProgress.of(goal(deadline = LocalDate.of(2026, 10, 28)), saved = 0, today)

        assertTrue(progress.isOverdue)
        assertNull(progress.monthlySuggestion)
    }

    @Test
    fun `a reached goal is never overdue and needs no suggestion`() {
        val progress = GoalProgress.of(goal(deadline = LocalDate.of(2026, 1, 1)), saved = 1_000_000, today)

        assertFalse(progress.isOverdue)
        assertNull(progress.monthlySuggestion)
    }

    @Test
    fun `no deadline means no suggestion`() {
        assertNull(GoalProgress.of(goal(), saved = 0, today).monthlySuggestion)
    }
}
