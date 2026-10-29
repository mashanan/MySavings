package com.hanan.mysavings.domain.repository

/**
 * Keeps the profile photo as a file of its own. The domain only passes a picked image's
 * address as a String; decoding, cropping and storing it is the implementation's business.
 */
interface ProfilePhotoStore {
    /**
     * Copies the image at [sourceUri] into the app as the profile photo.
     * @return `false` if it could not be read as an image; the old photo is kept then.
     */
    suspend fun import(sourceUri: String): Boolean

    /** The stored photo as JPEG bytes, or `null` if there is none. */
    suspend fun load(): ByteArray?

    suspend fun delete()
}
