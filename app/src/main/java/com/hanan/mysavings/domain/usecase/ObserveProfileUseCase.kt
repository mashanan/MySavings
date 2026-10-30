package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.ProfilePhoto
import com.hanan.mysavings.domain.model.UserProfile
import com.hanan.mysavings.domain.repository.ProfilePhotoStore
import com.hanan.mysavings.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val photoStore: ProfilePhotoStore,
) {
    // Also watches the photo on disk: sync may store the row's new photo version a moment
    // before (or after) the file itself arrives, and the screen must catch up either way.
    operator fun invoke(): Flow<UserProfile> = combine(profileRepository.observe(), photoStore.observeStoredVersion(), ::Pair)
        // Re-read the photo file only when the row or the file changed, not on every emission.
        .distinctUntilChanged()
        .map { (record, _) ->
            val photo = record.photoUpdatedAt?.let { version -> photoStore.load(version)?.let { ProfilePhoto(it, version) } }
            UserProfile(name = record.name, photo = photo)
        }
}
