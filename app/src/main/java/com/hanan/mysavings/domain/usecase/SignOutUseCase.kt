package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.AccountDataManager
import com.hanan.mysavings.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Ends the session. With [eraseLocalData] the account's data also leaves this phone, for
 * example before handing it to someone else; it stays in the cloud and on other phones.
 */
class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountData: AccountDataManager,
) {
    suspend operator fun invoke(eraseLocalData: Boolean) {
        // Sign out first: that stops sync, so nothing runs against data being erased.
        authRepository.signOut()
        if (eraseLocalData) accountData.eraseLocalData()
    }
}
