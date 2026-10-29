package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.ProfilePhoto
import com.hanan.mysavings.domain.model.UserProfile
import com.hanan.mysavings.domain.repository.ProfilePhotoStore
import com.hanan.mysavings.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val photoStore: ProfilePhotoStore,
) {
    operator fun invoke(): Flow<UserProfile> = profileRepository.observe()
        // Re-read the photo file only when the record changed, not on every emission.
        .distinctUntilChanged()
        .map { record ->
            val photo = record.photoUpdatedAt?.let { version -> photoStore.load()?.let { ProfilePhoto(it, version) } }
            UserProfile(name = record.name, photo = photo)
        }
}
