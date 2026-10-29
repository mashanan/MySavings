package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.MonthlySummary
import com.hanan.mysavings.testutil.MutableClock
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class ObserveFinancialSummaryUseCaseTest {

    // 17:30 UTC on 31 October is already 00:30 on 1 November in Jakarta: the month must
    // follow the clock's zone, not UTC.
    private val clock = MutableClock(Instant.parse("2026-10-31T17:30:00Z"))
    private val november = YearMonth.of(2026, 11)

    private val observeMonthlySummary = mockk<ObserveMonthlySummaryUseCase> {
        every { this@mockk.invoke(november) } returns flowOf(MonthlySummary(november, 1_650_000, null, emptyList()))
    }
    private val observeSavings = mockk<ObserveSavingsGoalsUseCase> {
        every { this@mockk.invoke() } returns flowOf(SavingsOverview(4_500_000, 1, emptyList()))
    }
    private val observeDebts = mockk<ObserveDebtsUseCase> {
        every { this@mockk.invoke() } returns flowOf(DebtsOverview(8_500_000, 0, emptyList(), emptyList(), emptyMap()))
    }

    @Test
    fun `takes one headline number from each tab for the current month`() = runTest {
        val summary = ObserveFinancialSummaryUseCase(observeMonthlySummary, observeSavings, observeDebts, clock)().first()

        assertEquals(FinancialSummary(spentThisMonth = 1_650_000, totalSaved = 4_500_000, debtRemaining = 8_500_000), summary)
    }
}
