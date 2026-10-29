package com.hanan.strukku.domain.usecase

import com.hanan.strukku.domain.model.Budget
import com.hanan.strukku.domain.model.BudgetAlert
import com.hanan.strukku.domain.model.BudgetAlertLevel
import com.hanan.strukku.domain.model.BudgetAlertLevel.NEARLY_USED
import com.hanan.strukku.domain.model.BudgetAlertLevel.USED_UP
import com.hanan.strukku.domain.repository.BudgetAlertRepository
import com.hanan.strukku.domain.repository.BudgetRepository
import com.hanan.strukku.domain.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class CheckBudgetAlertUseCaseTest {

    private val october = YearMonth.of(2026, 10)

    private val spending = MutableStateFlow<Map<Long, Long>>(emptyMap())
    private val budgets = MutableStateFlow<List<Budget>>(emptyList())

    private val transactionRepository = mockk<TransactionRepository> {
        every { observeSpendingByCategory(october.atDay(1), october.atEndOfMonth()) } returns spending
    }
    private val budgetRepository = mockk<BudgetRepository> {
        every { observeForMonth(october) } returns budgets
    }

    /** Behaves like the real table: remembers what was sent, per month. */
    private val alertRepository = object : BudgetAlertRepository {
        val sent = mutableMapOf<YearMonth, MutableSet<BudgetAlertLevel>>()
        override suspend fun sentLevels(yearMonth: YearMonth) = sent[yearMonth].orEmpty().toSet()
        override suspend fun markSent(yearMonth: YearMonth, levels: Set<BudgetAlertLevel>) {
            sent.getOrPut(yearMonth) { mutableSetOf() } += levels
        }
        override suspend fun clear(yearMonth: YearMonth) {
            sent.remove(yearMonth)
        }
    }

    private val checkAlert = CheckBudgetAlertUseCase(transactionRepository, budgetRepository, alertRepository)

    private fun givenBudget(limit: Long, spent: Long) {
        budgets.value = listOf(Budget(yearMonth = october, categoryId = null, limitAmount = limit))
        // Split over two categories to check that the total is used.
        spending.value = mapOf(1L to spent / 2, 2L to spent - spent / 2)
    }

    @Test
    fun `no budget means no alert`() = runTest {
        spending.value = mapOf(1L to 5_000_000L)

        assertNull(checkAlert(october))
    }

    @Test
    fun `per-category budgets alone do not trigger the overall alert`() = runTest {
        budgets.value = listOf(Budget(yearMonth = october, categoryId = 1, limitAmount = 100_000))
        spending.value = mapOf(1L to 500_000L)

        assertNull(checkAlert(october))
    }

    @Test
    fun `below 80 percent there is no alert`() = runTest {
        givenBudget(limit = 1_000_000, spent = 799_999)

        assertNull(checkAlert(october))
    }

    @Test
    fun `reaching exactly 80 percent sends nearly used`() = runTest {
        givenBudget(limit = 1_000_000, spent = 800_000)

        assertEquals(BudgetAlert(NEARLY_USED, october, 800_000, 1_000_000), checkAlert(october))
    }

    @Test
    fun `the same level is never sent twice in a month`() = runTest {
        givenBudget(limit = 1_000_000, spent = 850_000)
        checkAlert(october)

        givenBudget(limit = 1_000_000, spent = 900_000)

        assertNull(checkAlert(october))
    }

    @Test
    fun `going from nearly used to over budget sends used up`() = runTest {
        givenBudget(limit = 1_000_000, spent = 850_000)
        checkAlert(october)

        givenBudget(limit = 1_000_000, spent = 1_050_000)
        val alert = checkAlert(october)!!

        assertEquals(USED_UP, alert.level)
        assertEquals(-50_000L, alert.remaining)
    }

    @Test
    fun `jumping straight past 100 percent sends only used up, and nearly used never follows`() = runTest {
        givenBudget(limit = 1_000_000, spent = 1_200_000)

        assertEquals(USED_UP, checkAlert(october)!!.level)
        assertEquals(setOf(NEARLY_USED, USED_UP), alertRepository.sentLevels(october))
        assertNull(checkAlert(october))
    }

    @Test
    fun `after alerts are cleared, a lowered budget alerts again`() = runTest {
        givenBudget(limit = 1_000_000, spent = 850_000)
        checkAlert(october)

        alertRepository.clear(october) // what SetBudgetUseCase does
        givenBudget(limit = 800_000, spent = 850_000)

        assertEquals(USED_UP, checkAlert(october)!!.level)
    }

    @Test
    fun `used percent rounds down so 79,9 percent never reads as 80`() {
        assertEquals(79, BudgetAlert(NEARLY_USED, october, spent = 799_999, limit = 1_000_000).usedPercent)
        assertEquals(120, BudgetAlert(USED_UP, october, spent = 1_200_000, limit = 1_000_000).usedPercent)
    }
}
