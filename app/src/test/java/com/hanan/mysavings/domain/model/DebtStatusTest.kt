package com.hanan.mysavings.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DebtStatusTest {

    private val today = LocalDate.of(2026, 10, 29)

    private fun debt(total: Long = 1_200_000, firstDue: LocalDate = LocalDate.of(2026, 11, 5), count: Int = 3) =
        Debt(1, "Leasing", total, firstDue, count, null, Instant.EPOCH)

    @Test
    fun `installments split the total evenly and the last one takes the leftover rupiah`() {
        val amounts = DebtSchedule.of(debt(total = 1_000_000, count = 3)).map { it.amount }

        assertEquals(listOf(333_333L, 333_333L, 333_334L), amounts)
        assertEquals(1_000_000L, amounts.sum())
    }

    @Test
    fun `due dates are monthly, counted from the first, and clamp to short months`() {
        val dates = DebtSchedule.of(debt(firstDue = LocalDate.of(2027, 1, 31), count = 4)).map { it.dueDate }

        assertEquals(
            listOf(LocalDate.of(2027, 1, 31), LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 31), LocalDate.of(2027, 4, 30)),
            dates,
        )
    }

    @Test
    fun `a single payment is one installment for the whole amount`() {
        val schedule = DebtSchedule.of(debt(count = 1))

        assertEquals(listOf(Installment(1, LocalDate.of(2026, 11, 5), 1_200_000)), schedule)
    }

    @Test
    fun `payments fill the installments in order, a part payment leaves the rest open`() {
        val status = DebtStatus.of(debt(), paid = 600_000, today)

        assertEquals(listOf(400_000L, 200_000L, 0L), status.installments.map { it.paidAmount })
        assertTrue(status.installments[0].isPaid)
        assertEquals(2, status.next?.installment?.number)
        assertEquals(200_000L, status.next?.remaining)
        assertEquals(600_000L, status.remaining)
        assertFalse(status.isPaidOff)
    }

    @Test
    fun `unpaid installments before today are overdue, today's is not yet`() {
        val started = debt(firstDue = LocalDate.of(2026, 9, 29), count = 3) // 29 Sep, 29 Oct, 29 Nov

        val status = DebtStatus.of(started, paid = 0, today)

        assertEquals(listOf(true, false, false), status.installments.map { it.isOverdue })
        assertEquals(1, status.overdueCount)
    }

    @Test
    fun `a paid installment is never overdue`() {
        val started = debt(firstDue = LocalDate.of(2026, 9, 29), count = 3)

        assertEquals(0, DebtStatus.of(started, paid = 400_000, today).overdueCount)
    }

    @Test
    fun `paying everything marks the debt paid off`() {
        val status = DebtStatus.of(debt(), paid = 1_200_000, today)

        assertTrue(status.isPaidOff)
        assertNull(status.next)
        assertEquals(0L, status.remaining)
    }
}
