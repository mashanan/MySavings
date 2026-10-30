package com.hanan.mysavings.ui.profile

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.hanan.mysavings.R

/** What the Google account picker came back with. */
sealed interface GoogleSignInOutcome {
    data class Token(val idToken: String) : GoogleSignInOutcome

    /** The user closed the picker: not an error, nothing to show. */
    data object Cancelled : GoogleSignInOutcome

    /** No Google account on the phone. */
    data object NoAccount : GoogleSignInOutcome

    data object Failed : GoogleSignInOutcome
}

/**
 * Shows the system "Sign in with Google" sheet (Credential Manager, the successor of the old
 * GoogleSignIn API) and returns an ID token for Firebase. It needs an Activity [context] to
 * show the sheet, which is why it lives in the UI layer rather than in a repository.
 *
 * `default_web_client_id` is generated from google-services.json once Google sign-in is
 * enabled in the Firebase console: the token is issued for that client, so Firebase accepts it.
 */
suspend fun requestGoogleIdToken(context: Context): GoogleSignInOutcome {
    val option = GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    return try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            GoogleSignInOutcome.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            GoogleSignInOutcome.Failed
        }
    } catch (e: GetCredentialCancellationException) {
        GoogleSignInOutcome.Cancelled
    } catch (e: NoCredentialException) {
        GoogleSignInOutcome.NoAccount
    } catch (e: GetCredentialException) {
        GoogleSignInOutcome.Failed
    } catch (e: GoogleIdTokenParsingException) {
        GoogleSignInOutcome.Failed
    }
}

/** Forgets the account chosen in the picker, so the next sign-in asks again instead of reusing it. */
suspend fun clearGoogleCredential(context: Context) {
    try {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    } catch (e: ClearCredentialException) {
        // Nothing stored to clear; signing out of Firebase still happens.
    }
}
