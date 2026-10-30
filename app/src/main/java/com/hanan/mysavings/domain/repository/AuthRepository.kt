package com.hanan.mysavings.domain.repository

import com.hanan.mysavings.domain.model.AccountUser
import kotlinx.coroutines.flow.Flow

/**
 * Who is signed in. The Google account picker itself needs an Activity, so it lives in the
 * UI; this only exchanges the ID token it returns for a session.
 */
interface AuthRepository {
    /** The signed-in account, or `null`. Emits again on sign-in and sign-out. */
    fun observeUser(): Flow<AccountUser?>

    /** @return the account, or `null` if the token was rejected or the network failed. */
    suspend fun signInWithGoogle(idToken: String): AccountUser?

    suspend fun signOut()
}
