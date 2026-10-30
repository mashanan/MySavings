package com.hanan.mysavings.ui.profile

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.mysavings.domain.model.AccountUser
import com.hanan.mysavings.domain.model.SyncStatus
import com.hanan.mysavings.domain.usecase.DeleteAccountResult
import com.hanan.mysavings.domain.usecase.DeleteAccountUseCase
import com.hanan.mysavings.domain.usecase.FinancialSummary
import com.hanan.mysavings.domain.usecase.ObserveAccountUseCase
import com.hanan.mysavings.domain.usecase.ObserveFinancialSummaryUseCase
import com.hanan.mysavings.domain.usecase.ObserveProfileUseCase
import com.hanan.mysavings.domain.usecase.ObserveSyncStatusUseCase
import com.hanan.mysavings.domain.usecase.RequestSyncUseCase
import com.hanan.mysavings.domain.usecase.SignInWithGoogleUseCase
import com.hanan.mysavings.domain.usecase.SignOutUseCase
import com.hanan.mysavings.domain.usecase.SyncBeforeEraseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeProfile: ObserveProfileUseCase,
    observeFinancialSummary: ObserveFinancialSummaryUseCase,
    observeAccount: ObserveAccountUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    private val requestSync: RequestSyncUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val signOut: SignOutUseCase,
    private val syncBeforeErase: SyncBeforeEraseUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    private val photoDecoder: ProfilePhotoDecoder,
) : ViewModel() {

    /**
     * What the user is doing with the account right now: signing in, a dialog, work in
     * progress, the last message. The account itself comes from [observeAccount].
     */
    private val session = MutableStateFlow(SessionState())

    val uiState: StateFlow<ProfileUiState> = combine(
        observeProfile(),
        observeFinancialSummary(),
        observeAccount(),
        observeSyncStatus(),
        session,
    ) { profile, summary, account, syncStatus, session ->
        ProfileUiState(
            name = profile.name,
            initials = initialsOf(profile.name),
            photo = profile.photo?.let { photoDecoder.decode(it.bytes) },
            summary = summary,
            account = account,
            isSigningIn = session.isSigningIn,
            signInError = session.signInError,
            syncStatus = syncStatus.takeIf { account != null },
            accountDialog = session.dialog,
            isAccountBusy = session.isBusy,
            accountMessage = session.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    /** The account picker is about to open. */
    fun onSignInStarted() {
        session.value = SessionState(isSigningIn = true)
    }

    /** What the account picker returned (see requestGoogleIdToken). */
    fun onGoogleSignInOutcome(outcome: GoogleSignInOutcome) {
        when (outcome) {
            is GoogleSignInOutcome.Token -> viewModelScope.launch {
                val ok = signInWithGoogle(outcome.idToken)
                session.value = SessionState(signInError = SignInError.FAILED.takeUnless { ok })
            }
            GoogleSignInOutcome.Cancelled -> session.value = SessionState()
            GoogleSignInOutcome.NoAccount -> session.value = SessionState(signInError = SignInError.NO_ACCOUNT)
            GoogleSignInOutcome.Failed -> session.value = SessionState(signInError = SignInError.FAILED)
        }
    }

    fun onSyncNow() = requestSync()

    fun onSignOutClick() = session.update { SessionState(dialog = AccountDialog.SignOutChoice) }

    fun onDismissDialog() = session.update { it.copy(dialog = null) }

    fun onSignOutKeepData() {
        session.value = SessionState()
        viewModelScope.launch { signOut(eraseLocalData = false) }
    }

    /** Erasing first syncs one last time; only if something still could not go up, the user is asked again. */
    fun onSignOutEraseData() {
        session.value = SessionState(isBusy = true)
        viewModelScope.launch {
            val unsynced = syncBeforeErase()
            if (unsynced > 0) {
                session.value = SessionState(dialog = AccountDialog.EraseWarning(unsynced))
            } else {
                erase()
            }
        }
    }

    fun onConfirmErase() {
        session.value = SessionState(isBusy = true)
        viewModelScope.launch { erase() }
    }

    private suspend fun erase() {
        signOut(eraseLocalData = true)
        session.value = SessionState()
    }

    fun onDeleteAccountClick() = session.update { SessionState(dialog = AccountDialog.DeleteConfirm) }

    /** Confirmed in the dialog; the screen now opens the account picker to prove it is the owner. */
    fun onDeleteAccountConfirmed() {
        session.value = SessionState(isBusy = true)
    }

    fun onDeleteAccountReauth(outcome: GoogleSignInOutcome) {
        if (outcome !is GoogleSignInOutcome.Token) {
            // Closing the picker just cancels; any other outcome means it could not be confirmed.
            val message = AccountMessage.DELETE_NOT_CONFIRMED.takeUnless { outcome == GoogleSignInOutcome.Cancelled }
            session.value = SessionState(message = message)
            return
        }
        viewModelScope.launch {
            val message = when (deleteAccount(outcome.idToken)) {
                DeleteAccountResult.DELETED -> AccountMessage.ACCOUNT_DELETED
                DeleteAccountResult.NOT_CONFIRMED -> AccountMessage.DELETE_NOT_CONFIRMED
                DeleteAccountResult.FAILED -> AccountMessage.DELETE_FAILED
            }
            session.value = SessionState(message = message)
        }
    }

    private data class SessionState(
        val isSigningIn: Boolean = false,
        val signInError: SignInError? = null,
        val dialog: AccountDialog? = null,
        val isBusy: Boolean = false,
        val message: AccountMessage? = null,
    )
}

enum class SignInError { NO_ACCOUNT, FAILED }

/** The account dialogs, one at a time. */
sealed interface AccountDialog {
    /** Keep or erase this phone's data. */
    data object SignOutChoice : AccountDialog

    /** The last sync could not upload everything; erasing would lose [unsynced] changes. */
    data class EraseWarning(val unsynced: Int) : AccountDialog

    data object DeleteConfirm : AccountDialog
}

enum class AccountMessage { ACCOUNT_DELETED, DELETE_NOT_CONFIRMED, DELETE_FAILED }

data class ProfileUiState(
    val name: String? = null,
    val initials: String? = null,
    val photo: ImageBitmap? = null,
    /** `null` while loading. */
    val summary: FinancialSummary? = null,
    /** `null` when signed out. */
    val account: AccountUser? = null,
    val isSigningIn: Boolean = false,
    val signInError: SignInError? = null,
    /** `null` when signed out. */
    val syncStatus: SyncStatus? = null,
    val accountDialog: AccountDialog? = null,
    /** Erasing or deleting is under way; the account buttons wait. */
    val isAccountBusy: Boolean = false,
    val accountMessage: AccountMessage? = null,
)
