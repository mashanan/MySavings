package com.hanan.mysavings.ui.debts

import com.hanan.mysavings.domain.usecase.DueItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** How urgent a calendar day is; the most urgent installment of the day decides. */
enum class DueDot {
    /** Every installment that day is paid. */
    PAID,

    /** Unpaid, more than [CalendarLogic.SOON_DAYS] days away. */
    UPCOMING,

    /** Unpaid and due today or in at most [CalendarLogic.SOON_DAYS] days. */
    SOON,

    /** Unpaid and past its due date. */
    OVERDUE,
}

/** The numbers behind the calendar, kept out of the composables so they can be unit tested. */
object CalendarLogic {
    const val SOON_DAYS = 7

    /**
     * The grid for [month]: whole weeks starting on [firstDayOfWeek], with `null` for the
     * empty cells before the 1st and after the last day.
     */
    fun monthCells(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
        // Days from the start of the week to the 1st; floorMod keeps it in 0..6.
        val leading = Math.floorMod(month.atDay(1).dayOfWeek.value - firstDayOfWeek.value, 7)
        val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
        val cells = List(leading) { null } + days
        val trailing = (7 - cells.size % 7) % 7
        return cells + List(trailing) { null }
    }

    /** The weekdays in display order, starting on [firstDayOfWeek]. */
    fun weekdays(firstDayOfWeek: DayOfWeek): List<DayOfWeek> = (0L until 7L).map { firstDayOfWeek.plus(it) }

    /** One dot per day of [month] that has installments. */
    fun dots(dueByDate: Map<LocalDate, List<DueItem>>, month: YearMonth, today: LocalDate): Map<LocalDate, DueDot> =
        dueByDate
            .filterKeys { YearMonth.from(it) == month }
            .mapValues { (date, items) -> items.maxOf { dotFor(it, date, today) } }

    private fun dotFor(item: DueItem, date: LocalDate, today: LocalDate): DueDot {
        val status = item.installment
        return when {
            status.isPaid -> DueDot.PAID
            status.isOverdue -> DueDot.OVERDUE
            // Inclusive, to match the "≤ 7 days" legend.
            ChronoUnit.DAYS.between(today, date) <= SOON_DAYS -> DueDot.SOON
            else -> DueDot.UPCOMING
        }
    }
}
