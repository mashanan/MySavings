package com.hanan.mysavings.data.sync

import com.hanan.mysavings.data.local.dao.SyncDao
import com.hanan.mysavings.data.profile.AndroidProfilePhotoStore
import com.hanan.mysavings.domain.repository.AccountDataManager
import com.hanan.mysavings.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SyncAccountDataManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val engine: SyncEngine,
    private val local: LocalSyncStore,
    private val syncDao: SyncDao,
    private val photoStore: AndroidProfilePhotoStore,
    private val state: SyncStateStore,
    private val remote: RemoteStore,
    private val controller: WorkManagerSyncController,
) : AccountDataManager {

    override suspend fun syncBeforeErase(): Int {
        authRepository.observeUser().first()?.uid?.let { uid ->
            try {
                engine.sync(uid)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Offline, most likely: the count below tells the user what would be lost.
            }
        }
        return syncDao.observePendingCount().first()
    }

    override suspend fun eraseLocalData() {
        controller.cancelAll()
        local.eraseAll()
        photoStore.delete()
        // Also forgets which account this phone synced with: a later sign-in starts afresh.
        state.clear()
    }

    override suspend fun deleteCloudData(): Boolean {
        val uid = authRepository.observeUser().first()?.uid ?: return false
        return try {
            remote.deleteAll(uid)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }
}
