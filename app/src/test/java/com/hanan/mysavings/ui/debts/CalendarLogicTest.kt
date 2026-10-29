package com.hanan.mysavings.ui.debts

import com.hanan.mysavings.domain.model.Debt
import com.hanan.mysavings.domain.model.DebtStatus
import com.hanan.mysavings.domain.usecase.DueItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class CalendarLogicTest {

    private val october = YearMonth.of(2026, 10) // 1 October 2026 is a Thursday
    private val today = LocalDate.of(2026, 10, 29)

    @Test
    fun `a Monday-first grid has three empty cells before Thursday the 1st`() {
        val cells = CalendarLogic.monthCells(october, DayOfWeek.MONDAY)

        assertEquals(listOf(null, null, null), cells.take(3))
        assertEquals(LocalDate.of(2026, 10, 1), cells[3])
        assertEquals(0, cells.size % 7)
        assertEquals(31, cells.count { it != null })
    }

    @Test
    fun `a Sunday-first grid has four empty cells before Thursday the 1st`() {
        val cells = CalendarLogic.monthCells(october, DayOfWeek.SUNDAY)

        assertEquals(LocalDate.of(2026, 10, 1), cells[4])
        assertEquals(0, cells.size % 7)
    }

    @Test
    fun `a month starting on the first weekday has no leading gap`() {
        // 1 February 2027 is a Monday, and 28 days fill exactly four weeks.
        val cells = CalendarLogic.monthCells(YearMonth.of(2027, 2), DayOfWeek.MONDAY)

        assertEquals(LocalDate.of(2027, 2, 1), cells.first())
        assertEquals(28, cells.size)
    }

    @Test
    fun `weekdays start on the given day`() {
        assertEquals(DayOfWeek.SUNDAY, CalendarLogic.weekdays(DayOfWeek.SUNDAY).first())
        assertEquals(DayOfWeek.SATURDAY, CalendarLogic.weekdays(DayOfWeek.SUNDAY).last())
    }

    @Test
    fun `dots show the most urgent installment of each day in the month`() {
        val overdueDate = LocalDate.of(2026, 10, 10)
        val soonDate = LocalDate.of(2026, 11, 2)
        val laterDate = LocalDate.of(2026, 10, 31)
        val dueByDate = mapOf(
            // Same day: one paid, one overdue -> overdue wins.
            overdueDate to listOf(item(overdueDate, paid = true), item(overdueDate, paid = false)),
            laterDate to listOf(item(laterDate, paid = false)),
            soonDate to listOf(item(soonDate, paid = false)), // November: not on October's grid
        )

        val dots = CalendarLogic.dots(dueByDate, october, today)

        assertEquals(DueDot.OVERDUE, dots[overdueDate])
        assertEquals(DueDot.SOON, dots[laterDate])
        assertNull(dots[soonDate])
    }

    @Test
    fun `more than a week away is upcoming, a fully paid day is paid`() {
        val far = LocalDate.of(2026, 11, 20)
        val dots = CalendarLogic.dots(
            mapOf(far to listOf(item(far, paid = false)), today to listOf(item(today, paid = true))),
            YearMonth.of(2026, 11),
            today,
        ) + CalendarLogic.dots(mapOf(today to listOf(item(today, paid = true))), october, today)

        assertEquals(DueDot.UPCOMING, dots[far])
        assertEquals(DueDot.PAID, dots[today])
    }

    /** A single-payment debt due on [date], fully paid or not paid at all. */
    private fun item(date: LocalDate, paid: Boolean): DueItem {
        val debt = Debt(1, "Budi", 100_000, date, 1, null, Instant.EPOCH)
        val status = DebtStatus.of(debt, if (paid) 100_000 else 0, today)
        return DueItem(debt.id, debt.creditor, status.installments.single(), 1)
    }
}
