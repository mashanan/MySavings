package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.AuthRepository
import com.hanan.mysavings.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Signs in with the ID token from the Google account picker, then fills in the parts of the
 * local profile the user has not set yet. What the user chose themselves is never replaced.
 */
class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val updateProfileName: UpdateProfileNameUseCase,
    private val changeProfilePhoto: ChangeProfilePhotoUseCase,
) {
    /** @return `false` if signing in failed; the profile is left alone then. */
    suspend operator fun invoke(idToken: String): Boolean {
        val user = authRepository.signInWithGoogle(idToken) ?: return false

        val profile = profileRepository.observe().first()
        if (profile.name == null) user.displayName?.let { updateProfileName(it) }
        // A failed photo download is not worth failing the sign-in for: the user can pick one later.
        if (profile.photoUpdatedAt == null) user.photoUrl?.let { changeProfilePhoto(largerGooglePhoto(it)) }
        return true
    }

    companion object {
        /**
         * Google returns a 96 px avatar ("...=s96-c"); the same address serves any size, so ask
         * for the 512 px the profile photo is stored at. Other addresses are left as they are.
         */
        fun largerGooglePhoto(url: String): String = url.replace(Regex("=s\\d+-c$"), "=s512-c")
    }
}
