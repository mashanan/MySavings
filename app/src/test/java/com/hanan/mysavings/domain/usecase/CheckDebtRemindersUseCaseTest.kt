package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.DebtReminderKind
import com.hanan.mysavings.domain.model.DebtWithPaid
import com.hanan.mysavings.domain.repository.DebtReminderRepository
import com.hanan.mysavings.domain.repository.DebtRepository
import com.hanan.mysavings.domain.usecase.DebtTestData.debt
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CheckDebtRemindersUseCaseTest {

    private val debts = mutableListOf<DebtWithPaid>()
    private val debtRepository = mockk<DebtRepository> {
        coEvery { getDebtsWithPaid() } answers { debts.toList() }
    }

    /** In-memory stand-in for the sent-reminders table. */
    private val sent = mutableSetOf<Triple<Long, Int, DebtReminderKind>>()
    private val reminderRepository = object : DebtReminderRepository {
        override suspend fun wasSent(debtId: Long, installmentNumber: Int, kind: DebtReminderKind) =
            Triple(debtId, installmentNumber, kind) in sent

        override suspend fun markSent(debtId: Long, installmentNumber: Int, kind: DebtReminderKind) {
            sent += Triple(debtId, installmentNumber, kind)
        }

        override suspend fun clear(debtId: Long) {
            sent.removeAll { it.first == debtId }
        }
    }

    private val clock = DebtTestData.clock()
    private val checkReminders = CheckDebtRemindersUseCase(debtRepository, reminderRepository, clock)

    private fun dueIn(days: Long, id: Long = 1, paid: Long = 0) =
        DebtWithPaid(debt(id = id, firstDue = DebtTestData.today.plusDays(days)), paid)

    private fun moveTo(date: LocalDate) {
        clock.instant = Instant.parse("${date}T03:00:00Z")
    }

    @Test
    fun `three days before the due date sends a due-soon reminder with what is left`() = runTest {
        debts += DebtWithPaid(debt(firstDue = DebtTestData.today.plusDays(3)), paid = 200_000)

        val reminders = checkReminders()

        assertEquals(1, reminders.size)
        assertEquals(DebtReminderKind.DUE_SOON, reminders.single().kind)
        assertEquals(3, reminders.single().daysLeft)
        assertEquals(1_000_000L, reminders.single().amountDue)
    }

    @Test
    fun `four days before is too early`() = runTest {
        debts += dueIn(4)

        assertTrue(checkReminders().isEmpty())
    }

    @Test
    fun `each reminder is sent once, then the due-day reminder follows`() = runTest {
        debts += dueIn(3)

        assertEquals(1, checkReminders().size)
        // The daily check runs again the next days: nothing new until the due date.
        moveTo(DebtTestData.today.plusDays(1))
        assertTrue(checkReminders().isEmpty())
        moveTo(DebtTestData.today.plusDays(3))
        assertEquals(listOf(DebtReminderKind.DUE_TODAY), checkReminders().map { it.kind })
        assertTrue(checkReminders().isEmpty())
    }

    @Test
    fun `a missed three-day reminder still goes out on a later day`() = runTest {
        debts += dueIn(1) // the phone was off on days -3 and -2

        assertEquals(listOf(DebtReminderKind.DUE_SOON), checkReminders().map { it.kind })
    }

    @Test
    fun `paid and overdue installments get no reminder`() = runTest {
        debts += dueIn(2, id = 1, paid = 1_200_000) // already paid
        debts += dueIn(-1, id = 2) // overdue: the app shows it in red instead

        assertTrue(checkReminders().isEmpty())
    }
}
