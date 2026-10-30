package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.AccountDataManager
import com.hanan.mysavings.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Deletes the account and everything stored for it in the cloud. The data on this phone
 * stays: the app simply works offline again, as before signing in.
 */
class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountData: AccountDataManager,
) {
    /** @param idToken a fresh token from the account picker, which Firebase requires for deleting. */
    suspend operator fun invoke(idToken: String): DeleteAccountResult {
        if (!authRepository.reauthenticateWithGoogle(idToken)) return DeleteAccountResult.NOT_CONFIRMED
        // Cloud data first: once the account is gone, its data could no longer be reached.
        if (!accountData.deleteCloudData()) return DeleteAccountResult.FAILED
        return if (authRepository.deleteAccount()) DeleteAccountResult.DELETED else DeleteAccountResult.FAILED
    }
}

enum class DeleteAccountResult {
    DELETED,

    /** The account picker returned a different account, or the token was rejected. Nothing was deleted. */
    NOT_CONFIRMED,

    /** Usually no connection. Trying again is safe: deleting what is already gone changes nothing. */
    FAILED,
}
