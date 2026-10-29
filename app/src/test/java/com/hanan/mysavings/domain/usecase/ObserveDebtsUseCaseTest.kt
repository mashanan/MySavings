package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.DebtWithPaid
import com.hanan.mysavings.domain.repository.DebtRepository
import com.hanan.mysavings.domain.usecase.DebtTestData.debt
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ObserveDebtsUseCaseTest {

    private val repository = mockk<DebtRepository>()
    private val observeDebts = ObserveDebtsUseCase(repository, DebtTestData.clock())

    @Test
    fun `splits active and paid-off debts, most urgent first, and totals what is left`() = runTest {
        every { repository.observeDebtsWithPaid() } returns flowOf(
            listOf(
                DebtWithPaid(debt(id = 1, total = 500_000, firstDue = LocalDate.of(2026, 12, 1)), paid = 0),
                DebtWithPaid(debt(id = 2, total = 300_000, firstDue = LocalDate.of(2026, 9, 1)), paid = 300_000),
                // 3 installments of 200.000, the first one overdue.
                DebtWithPaid(debt(id = 3, total = 600_000, firstDue = LocalDate.of(2026, 10, 10), count = 3), paid = 0),
            ),
        )

        val overview = observeDebts().first()

        assertEquals(listOf(3L, 1L), overview.active.map { it.debt.id })
        assertEquals(listOf(2L), overview.paidOff.map { it.debt.id })
        assertEquals(1_100_000L, overview.totalRemaining)
        assertEquals(1, overview.overdueCount)
    }

    @Test
    fun `every installment lands on its due date for the calendar`() = runTest {
        every { repository.observeDebtsWithPaid() } returns flowOf(
            listOf(
                DebtWithPaid(debt(id = 1, firstDue = LocalDate.of(2026, 11, 5), count = 2), paid = 0),
                DebtWithPaid(debt(id = 2, firstDue = LocalDate.of(2026, 12, 5)), paid = 0),
            ),
        )

        val dueByDate = observeDebts().first().dueByDate

        assertEquals(listOf(1L), dueByDate[LocalDate.of(2026, 11, 5)]?.map { it.debtId })
        assertEquals(listOf(1L, 2L), dueByDate[LocalDate.of(2026, 12, 5)]?.map { it.debtId })
        assertEquals(2, dueByDate.getValue(LocalDate.of(2026, 12, 5)).first().installment.installment.number)
    }
}
