package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.ProfilePhotoStore
import com.hanan.mysavings.domain.repository.ProfileRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** Replaces the profile photo with the image the user picked. */
class ChangeProfilePhotoUseCase @Inject constructor(
    private val photoStore: ProfilePhotoStore,
    private val profileRepository: ProfileRepository,
    private val clock: Clock,
) {
    /** @return `false` if the picked file could not be used; nothing changes then. */
    suspend operator fun invoke(sourceUri: String): Boolean {
        // One timestamp for both: it names the file and becomes the profile's photo version,
        // which makes every screen (and, through sync, every phone) load this photo.
        val version = Instant.now(clock)
        if (!photoStore.import(sourceUri, version)) return false
        profileRepository.savePhotoUpdatedAt(version)
        return true
    }
}
