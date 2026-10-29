package com.hanan.mysavings.domain.model

import java.time.Instant

/** What is stored about the user. Both parts are optional: the app works without a profile. */
data class ProfileRecord(
    val name: String?,
    /** When the current photo was saved; `null` without a photo. Doubles as its version. */
    val photoUpdatedAt: Instant?,
)

/** The profile as screens show it, photo included. */
data class UserProfile(
    val name: String?,
    val photo: ProfilePhoto?,
)

/**
 * The profile photo as JPEG bytes. Compared by [version] rather than by content: two loads
 * of the same photo are equal without comparing every byte, and a new photo always differs.
 */
class ProfilePhoto(
    val bytes: ByteArray,
    val version: Instant,
) {
    override fun equals(other: Any?): Boolean = other is ProfilePhoto && other.version == version

    override fun hashCode(): Int = version.hashCode()
}
