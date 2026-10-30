package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Ends the session. Local data stays on the phone; the choice to also wipe it comes with
 * sync in a later phase, when there is a cloud copy to fall back on.
 */
class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() = authRepository.signOut()
}
