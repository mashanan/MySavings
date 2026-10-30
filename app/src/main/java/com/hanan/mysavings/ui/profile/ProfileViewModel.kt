package com.hanan.mysavings.ui.profile

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.mysavings.domain.model.AccountUser
import com.hanan.mysavings.domain.model.SyncStatus
import com.hanan.mysavings.domain.usecase.FinancialSummary
import com.hanan.mysavings.domain.usecase.ObserveAccountUseCase
import com.hanan.mysavings.domain.usecase.ObserveFinancialSummaryUseCase
import com.hanan.mysavings.domain.usecase.ObserveProfileUseCase
import com.hanan.mysavings.domain.usecase.ObserveSyncStatusUseCase
import com.hanan.mysavings.domain.usecase.RequestSyncUseCase
import com.hanan.mysavings.domain.usecase.SignInWithGoogleUseCase
import com.hanan.mysavings.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    private val photoDecoder: ProfilePhotoDecoder,
) : ViewModel() {

    /** Sign-in progress and its last problem; the account itself comes from [observeAccount]. */
    private val signIn = MutableStateFlow(SignInState())

    val uiState: StateFlow<ProfileUiState> = combine(
        observeProfile(),
        observeFinancialSummary(),
        observeAccount(),
        observeSyncStatus(),
        signIn,
    ) { profile, summary, account, syncStatus, signIn ->
        ProfileUiState(
            name = profile.name,
            initials = initialsOf(profile.name),
            photo = profile.photo?.let { photoDecoder.decode(it.bytes) },
            summary = summary,
            account = account,
            isSigningIn = signIn.isBusy,
            signInError = signIn.error,
            syncStatus = syncStatus.takeIf { account != null },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    /** The account picker is about to open. */
    fun onSignInStarted() {
        signIn.value = SignInState(isBusy = true)
    }

    /** What the account picker returned (see requestGoogleIdToken). */
    fun onGoogleSignInOutcome(outcome: GoogleSignInOutcome) {
        when (outcome) {
            is GoogleSignInOutcome.Token -> viewModelScope.launch {
                val ok = signInWithGoogle(outcome.idToken)
                signIn.value = SignInState(error = SignInError.FAILED.takeUnless { ok })
            }
            GoogleSignInOutcome.Cancelled -> signIn.value = SignInState()
            GoogleSignInOutcome.NoAccount -> signIn.value = SignInState(error = SignInError.NO_ACCOUNT)
            GoogleSignInOutcome.Failed -> signIn.value = SignInState(error = SignInError.FAILED)
        }
    }

    fun onSyncNow() = requestSync()

    fun onSignOut() {
        viewModelScope.launch { signOut() }
    }

    private data class SignInState(val isBusy: Boolean = false, val error: SignInError? = null)
}

enum class SignInError { NO_ACCOUNT, FAILED }

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
)
