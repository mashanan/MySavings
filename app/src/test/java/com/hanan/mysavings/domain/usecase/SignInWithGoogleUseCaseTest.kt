package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.AccountUser
import com.hanan.mysavings.domain.model.ProfileRecord
import com.hanan.mysavings.domain.repository.AuthRepository
import com.hanan.mysavings.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SignInWithGoogleUseCaseTest {

    private val googleUser = AccountUser(
        uid = "uid-1",
        email = "louise@example.com",
        displayName = "Louise Hanan",
        photoUrl = "https://lh3.googleusercontent.com/a/abc=s96-c",
    )
    private val record = MutableStateFlow(ProfileRecord(name = null, photoUpdatedAt = null))

    private val authRepository = mockk<AuthRepository> { coEvery { signInWithGoogle("token") } returns googleUser }
    private val profileRepository = mockk<ProfileRepository> { every { observe() } returns record }
    private val updateName = mockk<UpdateProfileNameUseCase> { coEvery { this@mockk.invoke(any()) } just runs }
    private val changePhoto = mockk<ChangeProfilePhotoUseCase> { coEvery { this@mockk.invoke(any()) } returns true }

    private val signIn = SignInWithGoogleUseCase(authRepository, profileRepository, updateName, changePhoto)

    @Test
    fun `an empty profile takes the Google name and a larger Google photo`() = runTest {
        assertTrue(signIn("token"))

        coVerify { updateName("Louise Hanan") }
        coVerify { changePhoto("https://lh3.googleusercontent.com/a/abc=s512-c") }
    }

    @Test
    fun `what the user set themselves is kept`() = runTest {
        record.value = ProfileRecord(name = "Lou", photoUpdatedAt = Instant.EPOCH)

        assertTrue(signIn("token"))

        coVerify(exactly = 0) { updateName(any()) }
        coVerify(exactly = 0) { changePhoto(any()) }
    }

    @Test
    fun `a failed photo download does not fail the sign-in`() = runTest {
        coEvery { changePhoto(any()) } returns false

        assertTrue(signIn("token"))
    }

    @Test
    fun `a rejected token signs nobody in and leaves the profile alone`() = runTest {
        coEvery { authRepository.signInWithGoogle("bad") } returns null

        assertFalse(signIn("bad"))
        coVerify(exactly = 0) { updateName(any()) }
    }

    @Test
    fun `only Google's size suffix is rewritten`() {
        assertEquals("https://x/a=s512-c", SignInWithGoogleUseCase.largerGooglePhoto("https://x/a=s96-c"))
        assertEquals("https://x/photo.jpg", SignInWithGoogleUseCase.largerGooglePhoto("https://x/photo.jpg"))
    }
}
