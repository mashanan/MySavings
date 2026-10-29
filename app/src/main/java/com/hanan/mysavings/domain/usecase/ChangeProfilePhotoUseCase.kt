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
        if (!photoStore.import(sourceUri)) return false
        // A new timestamp is the photo's new version, which makes every screen reload it.
        profileRepository.savePhotoUpdatedAt(Instant.now(clock))
        return true
    }
}
