package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.SavingsGoalWithSaved
import com.hanan.mysavings.domain.repository.SavingsRepository
import com.hanan.mysavings.domain.usecase.SavingsTestData.goal
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ObserveSavingsGoalsUseCaseTest {

    private val repository = mockk<SavingsRepository>()
    private val observeGoals = ObserveSavingsGoalsUseCase(repository, SavingsTestData.clock())

    @Test
    fun `totals every balance and counts reached goals`() = runTest {
        every { repository.observeGoalsWithSaved() } returns flowOf(
            listOf(
                SavingsGoalWithSaved(goal(id = 1, target = 1_000_000), saved = 1_000_000),
                SavingsGoalWithSaved(goal(id = 2, target = 5_000_000), saved = 750_000),
                SavingsGoalWithSaved(goal(id = 3, target = 2_000_000), saved = 0),
            ),
        )

        val overview = observeGoals().first()

        assertEquals(1_750_000L, overview.totalSaved)
        assertEquals(1, overview.reachedCount)
        assertEquals(3, overview.goals.size)
    }

    @Test
    fun `open goals by nearest deadline, then without deadline (newest first), reached ones last`() = runTest {
        every { repository.observeGoalsWithSaved() } returns flowOf(
            listOf(
                SavingsGoalWithSaved(goal(id = 1, target = 100), saved = 100), // reached
                SavingsGoalWithSaved(goal(id = 2, createdAt = Instant.ofEpochSecond(1)), saved = 0),
                SavingsGoalWithSaved(goal(id = 3, deadline = LocalDate.of(2027, 6, 1)), saved = 0),
                SavingsGoalWithSaved(goal(id = 4, createdAt = Instant.ofEpochSecond(2)), saved = 0),
                SavingsGoalWithSaved(goal(id = 5, deadline = LocalDate.of(2026, 12, 1)), saved = 0),
            ),
        )

        val order = observeGoals().first().goals.map { it.goal.id }

        assertEquals(listOf(5L, 3L, 4L, 2L, 1L), order)
    }

    @Test
    fun `no goals gives an empty overview`() = runTest {
        every { repository.observeGoalsWithSaved() } returns flowOf(emptyList())

        val overview = observeGoals().first()

        assertEquals(0L, overview.totalSaved)
        assertEquals(0, overview.reachedCount)
        assertEquals(emptyList<GoalWithProgress>(), overview.goals)
    }
}
