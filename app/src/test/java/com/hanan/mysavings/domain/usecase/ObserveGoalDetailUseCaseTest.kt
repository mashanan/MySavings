package com.hanan.mysavings.domain.usecase

import app.cash.turbine.test
import com.hanan.mysavings.domain.model.SavingsGoal
import com.hanan.mysavings.domain.repository.SavingsRepository
import com.hanan.mysavings.domain.usecase.SavingsTestData.deposit
import com.hanan.mysavings.domain.usecase.SavingsTestData.goal
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveGoalDetailUseCaseTest {

    private val goalFlow = MutableStateFlow<SavingsGoal?>(goal())
    private val repository = mockk<SavingsRepository> {
        every { observeGoal(1) } returns goalFlow
        every { observeDeposits(1) } returns flowOf(listOf(deposit(3, -100_000), deposit(2, 300_000), deposit(1, 200_000)))
    }
    private val observeDetail = ObserveGoalDetailUseCase(repository, SavingsTestData.clock())

    @Test
    fun `balance is the sum of deposits minus withdrawals`() = runTest {
        observeDetail(1).test {
            val detail = awaitItem()!!

            assertEquals(400_000L, detail.progress.saved)
            assertEquals(3, detail.deposits.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits null once the goal is deleted`() = runTest {
        observeDetail(1).test {
            awaitItem()

            goalFlow.value = null

            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
