package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.ProfilePhotoStore
import com.hanan.mysavings.domain.repository.ProfileRepository
import javax.inject.Inject

class RemoveProfilePhotoUseCase @Inject constructor(
    private val photoStore: ProfilePhotoStore,
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke() {
        // Record first: if deleting the file failed, the app would still show no photo.
        profileRepository.savePhotoUpdatedAt(null)
        photoStore.delete()
    }
}
