package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.ProfileRepository
import javax.inject.Inject

/** Saves the display name. A blank name is allowed and simply removes it. */
class UpdateProfileNameUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(name: String) {
        profileRepository.saveName(name.trim().take(MAX_LENGTH).ifBlank { null })
    }

    companion object {
        /** Long enough for any real name, short enough for the profile header. */
        const val MAX_LENGTH = 40
    }
}
