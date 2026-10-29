package com.hanan.mysavings.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The single profile row. A fixed id keeps "the" profile easy to find and impossible to duplicate. */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val name: String?,
    /** Epoch millis of the last photo change; null without a photo. */
    val photoUpdatedAt: Long?,
) {
    companion object {
        const val SINGLE_ROW_ID = 1
    }
}
