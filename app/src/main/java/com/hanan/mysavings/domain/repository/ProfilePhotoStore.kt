package com.hanan.mysavings.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Keeps the profile photo as a file of its own. The domain only passes a picked image's
 * address as a String; decoding, cropping and storing it is the implementation's business.
 *
 * Every photo has a version: the time it was set (the profile's photoUpdatedAt). Sync uses
 * it to tell whether this phone holds the same photo as the profile row says.
 */
interface ProfilePhotoStore {
    /**
     * Copies the image at [sourceUri] into the app as the profile photo with [version].
     * @return `false` if it could not be read as an image; the old photo is kept then.
     */
    suspend fun import(sourceUri: String, version: Instant): Boolean

    /** The stored photo of [version] as JPEG bytes, or `null` if this phone does not have it. */
    suspend fun load(version: Instant): ByteArray?

    suspend fun delete()

    /** Version of the photo on this phone; emits again when sync downloads a new one. */
    fun observeStoredVersion(): Flow<Instant?>
}
