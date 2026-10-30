package com.hanan.mysavings.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.hanan.mysavings.domain.model.AccountUser
import com.hanan.mysavings.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
) : AuthRepository {

    // Firebase reports sign-in changes through a listener; callbackFlow turns it into a Flow
    // and removes the listener when nobody collects any more.
    override fun observeUser(): Flow<AccountUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAccountUser()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithGoogle(idToken: String): AccountUser? = try {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user?.toAccountUser()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Rejected token or no network: the caller shows one "could not sign in" message.
        null
    }

    override suspend fun signOut() = auth.signOut()

    // A token of another Google account fails here ("user mismatch"), so nobody can confirm
    // the deletion of an account that is not theirs.
    override suspend fun reauthenticateWithGoogle(idToken: String): Boolean = runCatchingTask {
        val user = auth.currentUser ?: return@runCatchingTask false
        user.reauthenticate(GoogleAuthProvider.getCredential(idToken, null)).await()
        true
    }

    override suspend fun deleteAccount(): Boolean = runCatchingTask {
        val user = auth.currentUser ?: return@runCatchingTask false
        user.delete().await()
        true
    }

    /** Firebase reports failures as exceptions; here they become `false`, cancellation still propagates. */
    private inline fun runCatchingTask(block: () -> Boolean): Boolean = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    private fun FirebaseUser.toAccountUser() = AccountUser(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString(),
    )
}
