package com.hanan.strukku.domain.usecase

import com.hanan.strukku.domain.model.Budget
import com.hanan.strukku.domain.repository.BudgetAlertRepository
import com.hanan.strukku.domain.repository.BudgetCheckScheduler
import com.hanan.strukku.domain.repository.BudgetRepository
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class SetBudgetUseCaseTest {

    private val october = YearMonth.of(2026, 10)
    private val repository = mockk<BudgetRepository>(relaxed = true)
    private val alertRepository = mockk<BudgetAlertRepository>(relaxed = true)
    private val scheduler = mockk<BudgetCheckScheduler>(relaxed = true)
    private val setBudget = SetBudgetUseCase(repository, alertRepository, scheduler)

    @Test
    fun `positive limit is upserted`() = runTest {
        val ok = setBudget(october, categoryId = null, limitAmount = 3_000_000)

        assertTrue(ok)
        coVerify { repository.upsert(Budget(yearMonth = october, categoryId = null, limitAmount = 3_000_000)) }
    }

    @Test
    fun `zero or negative limit is rejected`() = runTest {
        assertFalse(setBudget(october, categoryId = 1, limitAmount = 0))
        assertFalse(setBudget(october, categoryId = 1, limitAmount = -1))
        coVerify(exactly = 0) { repository.upsert(any()) }
        verify(exactly = 0) { scheduler.requestCheck() }
    }

    @Test
    fun `changing the overall budget resets the month's alerts and re-checks`() = runTest {
        setBudget(october, categoryId = null, limitAmount = 1_000_000)

        coVerifyOrder {
            repository.upsert(any())
            alertRepository.clear(october)
            scheduler.requestCheck()
        }
    }

    @Test
    fun `a per-category budget does not touch overall alerts`() = runTest {
        setBudget(october, categoryId = 2, limitAmount = 500_000)

        coVerify(exactly = 0) { alertRepository.clear(any()) }
        verify(exactly = 0) { scheduler.requestCheck() }
    }
}
