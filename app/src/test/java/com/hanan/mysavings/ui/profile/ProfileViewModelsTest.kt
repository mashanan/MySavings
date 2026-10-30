package com.hanan.mysavings.ui.profile

import app.cash.turbine.test
import androidx.compose.ui.graphics.ImageBitmap
import com.hanan.mysavings.domain.model.AccountUser
import com.hanan.mysavings.domain.model.ProfilePhoto
import com.hanan.mysavings.domain.model.UserProfile
import com.hanan.mysavings.domain.usecase.ChangeProfilePhotoUseCase
import com.hanan.mysavings.domain.usecase.ObserveAccountUseCase
import com.hanan.mysavings.domain.usecase.FinancialSummary
import com.hanan.mysavings.domain.usecase.ObserveFinancialSummaryUseCase
import com.hanan.mysavings.domain.usecase.ObserveProfileUseCase
import com.hanan.mysavings.domain.usecase.RemoveProfilePhotoUseCase
import com.hanan.mysavings.domain.usecase.SignInWithGoogleUseCase
import com.hanan.mysavings.domain.usecase.SignOutUseCase
import com.hanan.mysavings.domain.usecase.UpdateProfileNameUseCase
import com.hanan.mysavings.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class ProfileViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val photo = ProfilePhoto(byteArrayOf(1), Instant.EPOCH)
    private val profile = MutableStateFlow(UserProfile(name = "Louise Hanan", photo = photo))
    private val observeProfile = mockk<ObserveProfileUseCase> { every { this@mockk.invoke() } returns profile }

    // ImageBitmap cannot be created on the JVM, so the decoder hands out a mock.
    private val decoded = mockk<ImageBitmap>()
    private val decoder = ProfilePhotoDecoder { decoded }

    private val summary = FinancialSummary(1_650_000, 4_500_000, 8_500_000)
    private val observeSummary = mockk<ObserveFinancialSummaryUseCase> { every { this@mockk.invoke() } returns flowOf(summary) }

    private val updateName = mockk<UpdateProfileNameUseCase> { coEvery { this@mockk.invoke(any()) } just runs }
    private val changePhoto = mockk<ChangeProfilePhotoUseCase>()
    private val removePhoto = mockk<RemoveProfilePhotoUseCase> { coEvery { this@mockk.invoke() } just runs }

    private val account = MutableStateFlow<AccountUser?>(null)
    private val observeAccount = mockk<ObserveAccountUseCase> { every { this@mockk.invoke() } returns account }
    private val signInWithGoogle = mockk<SignInWithGoogleUseCase>()
    private val signOut = mockk<SignOutUseCase> { coEvery { this@mockk.invoke() } just runs }

    private fun profileViewModel() =
        ProfileViewModel(observeProfile, observeSummary, observeAccount, signInWithGoogle, signOut, decoder)

    private fun editViewModel() = EditProfileViewModel(observeProfile, updateName, changePhoto, removePhoto, decoder)

    @Test
    fun `the profile tab shows name, initials, the decoded photo and the summary`() = runTest {
        profileViewModel().uiState.test {
            val state = awaitItem().takeIf { it.summary != null } ?: awaitItem()

            assertEquals("Louise Hanan", state.name)
            assertEquals("LH", state.initials)
            assertSame(decoded, state.photo)
            assertEquals(summary, state.summary)
        }
    }

    @Test
    fun `a token from the account picker signs in and clears the busy state`() = runTest {
        coEvery { signInWithGoogle("token") } answers {
            account.value = AccountUser("uid", "louise@example.com", "Louise", null)
            true
        }
        val viewModel = profileViewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onSignInStarted()
            assertTrue(expectMostRecentItem().isSigningIn)

            viewModel.onGoogleSignInOutcome(GoogleSignInOutcome.Token("token"))

            val state = expectMostRecentItem()
            assertEquals("louise@example.com", state.account?.email)
            assertFalse(state.isSigningIn)
            assertNull(state.signInError)
        }
    }

    @Test
    fun `sign-in problems are shown, a cancelled picker is not`() = runTest {
        coEvery { signInWithGoogle(any()) } returns false
        val viewModel = profileViewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onGoogleSignInOutcome(GoogleSignInOutcome.Token("rejected"))
            assertEquals(SignInError.FAILED, expectMostRecentItem().signInError)

            viewModel.onGoogleSignInOutcome(GoogleSignInOutcome.NoAccount)
            assertEquals(SignInError.NO_ACCOUNT, expectMostRecentItem().signInError)

            viewModel.onGoogleSignInOutcome(GoogleSignInOutcome.Cancelled)
            val state = expectMostRecentItem()
            assertNull(state.signInError)
            assertFalse(state.isSigningIn)
        }
    }

    @Test
    fun `signing out calls the use case`() = runTest {
        profileViewModel().onSignOut()

        coVerify { signOut() }
    }

    @Test
    fun `editing starts from the stored name and photo`() {
        val state = editViewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals("Louise Hanan", state.name)
        assertSame(decoded, state.photo)
    }

    @Test
    fun `saving stores the typed name and finishes`() = runTest {
        val viewModel = editViewModel()

        viewModel.onNameChange("Louise")
        viewModel.onSave()

        coVerify { updateName("Louise") }
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `an unusable photo shows an error and keeps the old one`() = runTest {
        coEvery { changePhoto(any()) } returns false
        val viewModel = editViewModel()

        viewModel.onPhotoPicked("content://broken")

        assertTrue(viewModel.uiState.value.photoError)
        assertFalse(viewModel.uiState.value.isPhotoBusy)
        assertSame(decoded, viewModel.uiState.value.photo)
    }

    @Test
    fun `removing the photo clears it right away`() = runTest {
        val viewModel = editViewModel()

        viewModel.onRemovePhoto()

        coVerify { removePhoto() }
        assertNull(viewModel.uiState.value.photo)
    }

    @Test
    fun `initials come from the first two words`() {
        assertEquals("LH", initialsOf("  louise   hanan putri "))
        assertEquals("B", initialsOf("budi"))
        assertNull(initialsOf("   "))
        assertNull(initialsOf(null))
    }
}
