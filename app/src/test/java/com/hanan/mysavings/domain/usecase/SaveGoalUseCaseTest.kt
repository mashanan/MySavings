package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.GoalCategory
import com.hanan.mysavings.domain.model.SavingsGoal
import com.hanan.mysavings.domain.repository.SavingsRepository
import com.hanan.mysavings.domain.usecase.SavingsTestData.goal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SaveGoalUseCaseTest {

    private val saved = slot<SavingsGoal>()
    private val repository = mockk<SavingsRepository> {
        coEvery { saveGoal(capture(saved)) } answers { saved.captured.id.takeIf { it != 0L } ?: 7L }
    }
    private val saveGoal = SaveGoalUseCase(repository, SavingsTestData.clock())

    @Test
    fun `a new goal is trimmed and stamped with the current time`() = runTest {
        val result = saveGoal(null, "  Laptop baru ", 12_000_000, GoalCategory.GADGET, LocalDate.of(2027, 3, 1))

        assertEquals(SaveGoalResult.Success(7), result)
        assertEquals(0L, saved.captured.id)
        assertEquals("Laptop baru", saved.captured.name)
        assertEquals(Instant.parse("2026-10-29T03:00:00Z"), saved.captured.createdAt)
    }

    @Test
    fun `reports every invalid field at once and saves nothing`() = runTest {
        val result = saveGoal(null, "   ", 0, GoalCategory.OTHER, LocalDate.of(2026, 10, 28))

        assertEquals(
            SaveGoalResult.Invalid(setOf(GoalError.NAME_BLANK, GoalError.TARGET_NOT_POSITIVE, GoalError.TARGET_DATE_IN_PAST)),
            result,
        )
        coVerify(exactly = 0) { repository.saveGoal(any()) }
    }

    @Test
    fun `today is an allowed deadline`() = runTest {
        val result = saveGoal(null, "Kado", 100_000, GoalCategory.OTHER, SavingsTestData.today)

        assertEquals(SaveGoalResult.Success(7), result)
    }

    @Test
    fun `editing keeps the id and the original creation time`() = runTest {
        val createdAt = Instant.parse("2026-01-05T00:00:00Z")
        coEvery { repository.getGoal(3) } returns goal(id = 3, createdAt = createdAt)

        val result = saveGoal(3, "Laptop gaming", 15_000_000, GoalCategory.GADGET, null)

        assertEquals(SaveGoalResult.Success(3), result)
        assertEquals(3L, saved.captured.id)
        assertEquals(createdAt, saved.captured.createdAt)
        assertEquals(15_000_000L, saved.captured.targetAmount)
    }

    @Test
    fun `an overdue goal can be edited without changing its past deadline`() = runTest {
        val pastDeadline = LocalDate.of(2026, 9, 1)
        coEvery { repository.getGoal(3) } returns goal(id = 3, deadline = pastDeadline)

        assertEquals(SaveGoalResult.Success(3), saveGoal(3, "Renamed", 1_000_000, GoalCategory.GADGET, pastDeadline))
        // But moving it to another past date is still a mistake.
        assertEquals(
            SaveGoalResult.Invalid(GoalError.TARGET_DATE_IN_PAST),
            saveGoal(3, "Renamed", 1_000_000, GoalCategory.GADGET, LocalDate.of(2026, 9, 2)),
        )
    }

    @Test
    fun `editing a goal that no longer exists fails`() = runTest {
        coEvery { repository.getGoal(9) } returns null

        assertEquals(SaveGoalResult.Invalid(GoalError.NOT_FOUND), saveGoal(9, "X", 1, GoalCategory.OTHER, null))
    }
}
