package com.hanan.mysavings.domain.repository

import com.hanan.mysavings.domain.model.ProfileRecord
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ProfileRepository {
    /** The stored profile; an empty record until the user fills anything in. */
    fun observe(): Flow<ProfileRecord>

    suspend fun saveName(name: String?)

    /** Records that the photo changed (or, with `null`, was removed). */
    suspend fun savePhotoUpdatedAt(updatedAt: Instant?)
}
