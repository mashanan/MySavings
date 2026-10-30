package com.hanan.mysavings.data.repository

import com.hanan.mysavings.data.local.dao.ProfileDao
import com.hanan.mysavings.domain.model.ProfileRecord
import com.hanan.mysavings.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class RoomProfileRepository @Inject constructor(
    private val dao: ProfileDao,
    private val clock: Clock,
) : ProfileRepository {

    override fun observe(): Flow<ProfileRecord> =
        dao.observe().map { it?.toDomain() ?: ProfileRecord(name = null, photoUpdatedAt = null) }

    override suspend fun saveName(name: String?) = dao.update { it.copy(name = name, sync = it.sync.touched(clock.millis())) }

    override suspend fun savePhotoUpdatedAt(updatedAt: Instant?) =
        dao.update { it.copy(photoUpdatedAt = updatedAt?.toEpochMilli(), sync = it.sync.touched(clock.millis())) }
}
