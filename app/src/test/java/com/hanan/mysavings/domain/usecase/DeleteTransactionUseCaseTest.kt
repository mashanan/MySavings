package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.TransactionRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteTransactionUseCaseTest {

    private val repository = mockk<TransactionRepository>(relaxed = true)

    @Test
    fun `deletes the transaction by id`() = runTest {
        DeleteTransactionUseCase(repository)(7)

        coVerify(exactly = 1) { repository.delete(7) }
    }
}
