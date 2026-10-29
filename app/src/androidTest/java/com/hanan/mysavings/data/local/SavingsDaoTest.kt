package com.hanan.mysavings.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hanan.mysavings.data.local.entity.SavingsDepositEntity
import com.hanan.mysavings.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real SQL for savings: the LEFT JOIN sum, the newest-first history and the CASCADE delete. */
@RunWith(AndroidJUnit4::class)
class SavingsDaoTest {

    private lateinit var db: MySavingsDatabase
    private val dao get() = db.savingsDao()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MySavingsDatabase::class.java).build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun goalsCarryTheirBalanceAndGoalsWithoutDepositsShowZero() = runTest {
        val laptop = dao.insertGoal(goal("Laptop"))
        val bali = dao.insertGoal(goal("Bali"))
        dao.insertDeposit(deposit(laptop, 500_000))
        dao.insertDeposit(deposit(laptop, -100_000))

        val saved = dao.observeGoalsWithSaved().first().associate { it.goal.id to it.saved }

        assertEquals(mapOf(laptop to 400_000L, bali to 0L), saved)
        assertEquals(400_000L, dao.getSaved(laptop))
        assertEquals(0L, dao.getSaved(bali))
    }

    @Test
    fun historyIsNewestFirst() = runTest {
        val goal = dao.insertGoal(goal("Laptop"))
        val older = dao.insertDeposit(deposit(goal, 100, date = 20_000, createdAt = 5))
        val sameDayFirst = dao.insertDeposit(deposit(goal, 200, date = 20_010, createdAt = 1))
        val sameDaySecond = dao.insertDeposit(deposit(goal, 300, date = 20_010, createdAt = 2))

        val history = dao.observeDeposits(goal).first()

        assertEquals(listOf(sameDaySecond, sameDayFirst, older), history.map { it.id })
    }

    @Test
    fun deletingAGoalDeletesItsDeposits() = runTest {
        val goal = dao.insertGoal(goal("Laptop"))
        val depositId = dao.insertDeposit(deposit(goal, 100_000))

        dao.deleteGoal(goal)

        assertNull(dao.getGoal(goal))
        assertNull(dao.getDeposit(depositId))
    }

    private fun goal(name: String) =
        SavingsGoalEntity(name = name, targetAmount = 1_000_000, categoryKey = "gadget", targetDate = null, createdAt = 0)

    private fun deposit(goalId: Long, amount: Long, date: Long = 20_000, createdAt: Long = 0) =
        SavingsDepositEntity(goalId = goalId, amount = amount, date = date, note = null, createdAt = createdAt)
}
