package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.AccountDataManager
import com.hanan.mysavings.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountUseCasesTest {

    private val auth = mockk<AuthRepository> {
        coEvery { signOut() } just runs
        coEvery { reauthenticateWithGoogle("token") } returns true
        coEvery { reauthenticateWithGoogle("someone-else") } returns false
        coEvery { deleteAccount() } returns true
    }
    private val accountData = mockk<AccountDataManager> {
        coEvery { eraseLocalData() } just runs
        coEvery { deleteCloudData() } returns true
    }

    @Test
    fun `signing out keeps the data unless asked to erase, and erases only after signing out`() = runTest {
        val signOut = SignOutUseCase(auth, accountData)

        signOut(eraseLocalData = false)
        coVerify(exactly = 0) { accountData.eraseLocalData() }

        signOut(eraseLocalData = true)
        coVerifyOrder {
            auth.signOut()
            accountData.eraseLocalData()
        }
    }

    @Test
    fun `deleting confirms the owner, then removes the cloud data, then the account`() = runTest {
        assertEquals(DeleteAccountResult.DELETED, DeleteAccountUseCase(auth, accountData)("token"))

        coVerifyOrder {
            auth.reauthenticateWithGoogle("token")
            accountData.deleteCloudData()
            auth.deleteAccount()
        }
    }

    @Test
    fun `another account cannot confirm the deletion and nothing is deleted`() = runTest {
        assertEquals(DeleteAccountResult.NOT_CONFIRMED, DeleteAccountUseCase(auth, accountData)("someone-else"))

        coVerify(exactly = 0) { accountData.deleteCloudData() }
        coVerify(exactly = 0) { auth.deleteAccount() }
    }

    @Test
    fun `if the cloud data cannot be removed the account stays, so it can be tried again`() = runTest {
        coEvery { accountData.deleteCloudData() } returns false

        assertEquals(DeleteAccountResult.FAILED, DeleteAccountUseCase(auth, accountData)("token"))
        coVerify(exactly = 0) { auth.deleteAccount() }
    }
}
