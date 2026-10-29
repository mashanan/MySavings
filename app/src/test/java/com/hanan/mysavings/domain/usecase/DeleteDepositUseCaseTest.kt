package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.SavingsRepository
import com.hanan.mysavings.domain.usecase.SavingsTestData.deposit
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteDepositUseCaseTest {

    // Deposit 100.000 (id 1), then withdraw 100.000 (id 2): balance 0.
    private val repository = mockk<SavingsRepository> {
        coEvery { getDeposit(1) } returns deposit(1, 100_000)
        coEvery { getDeposit(2) } returns deposit(2, -100_000)
        coEvery { getSaved(1) } returns 0
        coEvery { deleteDeposit(any()) } just runs
    }
    private val deleteDeposit = DeleteDepositUseCase(repository)

    @Test
    fun `deleting the deposit would leave the goal below zero, so it is refused`() = runTest {
        assertFalse(deleteDeposit(1))
        coVerify(exactly = 0) { repository.deleteDeposit(any()) }
    }

    @Test
    fun `deleting the withdrawal raises the balance, so it is allowed`() = runTest {
        assertTrue(deleteDeposit(2))
        coVerify { repository.deleteDeposit(2) }
    }

    @Test
    fun `an entry that is already gone counts as deleted`() = runTest {
        coEvery { repository.getDeposit(9) } returns null

        assertTrue(deleteDeposit(9))
    }
}
