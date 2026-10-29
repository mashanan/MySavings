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
import java.time.YearMonth

class ObserveMonthlyTotalsUseCaseTest {

    private val repository = mockk<TransactionRepository>()
    private val observeTotals = ObserveMonthlyTotalsUseCase(repository)

    @Test
    fun `returns six months oldest first and fills gaps with zero`() = runTest {
        every { repository.observeMonthlyTotals(any(), any()) } returns flowOf(
            mapOf(YearMonth.of(2026, 6) to 500_000L, YearMonth.of(2026, 10) to 1_200_000L),
        )

        val totals = observeTotals(YearMonth.of(2026, 10)).first()

        assertEquals(
            listOf(
                MonthTotal(YearMonth.of(2026, 5), 0),
                MonthTotal(YearMonth.of(2026, 6), 500_000),
                MonthTotal(YearMonth.of(2026, 7), 0),
                MonthTotal(YearMonth.of(2026, 8), 0),
                MonthTotal(YearMonth.of(2026, 9), 0),
                MonthTotal(YearMonth.of(2026, 10), 1_200_000),
            ),
            totals,
        )
    }

    @Test
    fun `queries from the first day of the oldest month to the last day of the end month`() = runTest {
        every { repository.observeMonthlyTotals(any(), any()) } returns flowOf(emptyMap())

        observeTotals(YearMonth.of(2026, 10)).first()

        verify { repository.observeMonthlyTotals(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 10, 31)) }
    }

    @Test
    fun `window spans the turn of the year`() = runTest {
        every { repository.observeMonthlyTotals(any(), any()) } returns flowOf(emptyMap())

        val months = observeTotals(YearMonth.of(2027, 2)).first().map { it.yearMonth }

        assertEquals((9..12).map { YearMonth.of(2026, it) } + (1..2).map { YearMonth.of(2027, it) }, months)
    }
}
