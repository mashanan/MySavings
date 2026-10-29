package com.hanan.mysavings.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hanan.mysavings.data.local.entity.DebtEntity
import com.hanan.mysavings.data.local.entity.DebtPaymentEntity
import com.hanan.mysavings.data.local.entity.DebtReminderEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real SQL for debts: the paid sum, the reminder bookkeeping and the CASCADE deletes. */
@RunWith(AndroidJUnit4::class)
class DebtDaoTest {

    private lateinit var db: MySavingsDatabase
    private val dao get() = db.debtDao()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MySavingsDatabase::class.java).build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun debtsCarryWhatWasPaidAndDebtsWithoutPaymentsShowZero() = runTest {
        val leasing = dao.insertDebt(debt("Leasing"))
        val friend = dao.insertDebt(debt("Budi"))
        dao.insertPayment(payment(leasing, 400_000))
        dao.insertPayment(payment(leasing, 100_000))

        val paid = dao.observeDebtsWithPaid().first().associate { it.debt.id to it.paid }

        assertEquals(mapOf(leasing to 500_000L, friend to 0L), paid)
        assertEquals(paid, dao.getDebtsWithPaid().associate { it.debt.id to it.paid })
        assertEquals(500_000L, dao.getPaid(leasing))
    }

    @Test
    fun aReminderIsRememberedPerInstallmentAndKind() = runTest {
        val id = dao.insertDebt(debt("Leasing"))

        dao.insertReminder(DebtReminderEntity(id, 2, "soon", 0))
        dao.insertReminder(DebtReminderEntity(id, 2, "soon", 99)) // a repeat is ignored, not an error

        assertTrue(dao.reminderSent(id, 2, "soon"))
        assertFalse(dao.reminderSent(id, 2, "today"))
        assertFalse(dao.reminderSent(id, 3, "soon"))

        dao.clearReminders(id)
        assertFalse(dao.reminderSent(id, 2, "soon"))
    }

    @Test
    fun deletingADebtDeletesItsPaymentsAndReminders() = runTest {
        val id = dao.insertDebt(debt("Leasing"))
        dao.insertPayment(payment(id, 100_000))
        dao.insertReminder(DebtReminderEntity(id, 1, "soon", 0))

        dao.deleteDebt(id)

        assertNull(dao.getDebt(id))
        assertEquals(0L, dao.getPaid(id))
        assertFalse(dao.reminderSent(id, 1, "soon"))
    }

    private fun debt(creditor: String) = DebtEntity(
        creditor = creditor,
        totalAmount = 1_200_000,
        firstDueDate = 20_400,
        installmentCount = 3,
        note = null,
        createdAt = 0,
    )

    private fun payment(debtId: Long, amount: Long) =
        DebtPaymentEntity(debtId = debtId, amount = amount, date = 20_390, note = null, createdAt = 0)
}
