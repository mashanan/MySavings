package com.hanan.mysavings.data.repository

import com.hanan.mysavings.data.local.dao.ProfileDao
import com.hanan.mysavings.domain.model.ProfileRecord
import com.hanan.mysavings.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomProfileRepository @Inject constructor(
    private val dao: ProfileDao,
) : ProfileRepository {

    override fun observe(): Flow<ProfileRecord> =
        dao.observe().map { it?.toDomain() ?: ProfileRecord(name = null, photoUpdatedAt = null) }

    override suspend fun saveName(name: String?) = dao.update { it.copy(name = name) }

    override suspend fun savePhotoUpdatedAt(updatedAt: Instant?) =
        dao.update { it.copy(photoUpdatedAt = updatedAt?.toEpochMilli()) }
}
