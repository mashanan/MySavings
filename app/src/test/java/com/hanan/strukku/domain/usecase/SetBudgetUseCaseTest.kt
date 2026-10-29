package com.hanan.strukku.domain.usecase

import com.hanan.strukku.domain.model.Budget
import com.hanan.strukku.domain.repository.BudgetRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class SetBudgetUseCaseTest {

    private val october = YearMonth.of(2026, 10)
    private val repository = mockk<BudgetRepository>()
    private val setBudget = SetBudgetUseCase(repository)

    @Test
    fun `positive limit is upserted`() = runTest {
        coEvery { repository.upsert(any()) } just runs

        val ok = setBudget(october, categoryId = null, limitAmount = 3_000_000)

        assertTrue(ok)
        coVerify { repository.upsert(Budget(yearMonth = october, categoryId = null, limitAmount = 3_000_000)) }
    }

    @Test
    fun `zero or negative limit is rejected`() = runTest {
        assertFalse(setBudget(october, categoryId = 1, limitAmount = 0))
        assertFalse(setBudget(october, categoryId = 1, limitAmount = -1))
        coVerify(exactly = 0) { repository.upsert(any()) }
    }
}
