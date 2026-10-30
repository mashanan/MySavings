package com.hanan.mysavings.domain.repository

import com.hanan.mysavings.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * Starting and watching sync. The domain only states the need; that it runs in WorkManager
 * against Firestore is the data layer's business (like BudgetCheckScheduler).
 */
interface SyncController {
    fun observeStatus(): Flow<SyncStatus>

    /** Syncs as soon as there is a connection. Does nothing while signed out. */
    fun requestSync()
}
