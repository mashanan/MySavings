package com.hanan.mysavings.data.sync

import com.hanan.mysavings.data.di.ApplicationScope
import com.hanan.mysavings.data.local.dao.SyncDao
import com.hanan.mysavings.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides *when* to sync, for the whole app, from two facts: who is signed in, and whether
 * something waits for upload. Repositories and screens never have to remember to sync.
 */
@Singleton
class SyncTrigger @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncDao: SyncDao,
    private val controller: WorkManagerSyncController,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    /** Called once from Application.onCreate. */
    fun start() {
        // Signing in (or opening the app while signed in) syncs right away and keeps the
        // periodic sync scheduled; signing out stops both.
        scope.launch {
            authRepository.observeUser().map { it?.uid }.distinctUntilChanged().collect { uid ->
                if (uid != null) {
                    controller.schedulePeriodic()
                    controller.requestSync()
                } else {
                    controller.cancelAll()
                }
            }
        }
        // A local change while signed in: sync a few seconds later.
        scope.launch {
            combine(authRepository.observeUser(), syncDao.observePendingCount()) { user, pending -> user != null && pending > 0 }
                .filter { it }
                .collect { controller.requestSyncSoon() }
        }
    }
}
