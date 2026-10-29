package com.hanan.mysavings.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.hanan.mysavings.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    /** `null` until something is saved for the first time. */
    @Query("SELECT * FROM user_profile WHERE id = ${UserProfileEntity.SINGLE_ROW_ID}")
    fun observe(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = ${UserProfileEntity.SINGLE_ROW_ID}")
    suspend fun get(): UserProfileEntity?

    @Upsert
    suspend fun upsert(profile: UserProfileEntity)

    /**
     * Changes one part of the profile and keeps the rest. Read and write run in one
     * transaction, so two quick edits cannot overwrite each other.
     */
    @Transaction
    suspend fun update(change: (UserProfileEntity) -> UserProfileEntity) {
        upsert(change(get() ?: UserProfileEntity(name = null, photoUpdatedAt = null)))
    }
}
