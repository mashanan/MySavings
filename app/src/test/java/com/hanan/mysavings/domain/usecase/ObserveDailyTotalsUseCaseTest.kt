package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ObserveDailyTotalsUseCaseTest {

    private val repository = mockk<TransactionRepository>()
    private val observeTotals = ObserveDailyTotalsUseCase(repository)

    @Test
    fun `returns seven days oldest first and fills gaps with zero`() = runTest {
        every { repository.observeDailyTotals(any(), any()) } returns flowOf(
            mapOf(LocalDate.of(2026, 10, 24) to 45_000L, LocalDate.of(2026, 10, 29) to 87_500L),
        )

        val totals = observeTotals(LocalDate.of(2026, 10, 29)).first()

        assertEquals(
            listOf(
                DayTotal(LocalDate.of(2026, 10, 23), 0),
                DayTotal(LocalDate.of(2026, 10, 24), 45_000),
                DayTotal(LocalDate.of(2026, 10, 25), 0),
                DayTotal(LocalDate.of(2026, 10, 26), 0),
                DayTotal(LocalDate.of(2026, 10, 27), 0),
                DayTotal(LocalDate.of(2026, 10, 28), 0),
                DayTotal(LocalDate.of(2026, 10, 29), 87_500),
            ),
            totals,
        )
    }

    @Test
    fun `queries from six days before the end date up to it`() = runTest {
        every { repository.observeDailyTotals(any(), any()) } returns flowOf(emptyMap())

        observeTotals(LocalDate.of(2026, 10, 29)).first()

        verify { repository.observeDailyTotals(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 29)) }
    }

    @Test
    fun `week early in the month reaches into the previous month`() = runTest {
        every { repository.observeDailyTotals(any(), any()) } returns flowOf(mapOf(LocalDate.of(2026, 2, 27) to 10_000L))

        val totals = observeTotals(LocalDate.of(2026, 3, 2)).first()

        assertEquals((24..28).map { LocalDate.of(2026, 2, it) } + (1..2).map { LocalDate.of(2026, 3, it) }, totals.map { it.date })
        assertEquals(10_000L, totals.single { it.date == LocalDate.of(2026, 2, 27) }.total)
    }
}
