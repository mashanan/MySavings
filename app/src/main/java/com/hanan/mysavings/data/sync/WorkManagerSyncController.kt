package com.hanan.mysavings.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.hanan.mysavings.domain.model.SyncStatus
import com.hanan.mysavings.domain.repository.SyncController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerSyncController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val state: SyncStateStore,
) : SyncController {

    private val workManager get() = WorkManager.getInstance(context)

    // Any connection: a sync moves a few kilobytes, waiting for Wi-Fi is not worth it.
    private val needsNetwork = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    override fun observeStatus(): Flow<SyncStatus> = combine(
        state.lastResult,
        workManager.getWorkInfosForUniqueWorkFlow(SYNC_NOW),
        workManager.getWorkInfosForUniqueWorkFlow(SYNC_PERIODIC),
    ) { result, now, periodic ->
        SyncStatus(
            lastSyncedAt = result.lastSyncedAt?.let(Instant::ofEpochMilli),
            isSyncing = (now + periodic).any { it.state == WorkInfo.State.RUNNING },
            lastAttemptFailed = result.lastAttemptFailed,
        )
    }

    override fun requestSync() = enqueue(delaySeconds = 0)

    /**
     * After a local change: wait a few seconds so a burst of edits goes up as one sync.
     * KEEP: while a sync is already waiting, it will pick these changes up as well.
     */
    fun requestSyncSoon() = enqueue(delaySeconds = CHANGE_DELAY_SECONDS)

    private fun enqueue(delaySeconds: Long) {
        workManager.enqueueUniqueWork(
            SYNC_NOW,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(needsNetwork)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .build(),
        )
    }

    /** Catches changes made on other phones even when nothing changes here. */
    fun schedulePeriodic() {
        workManager.enqueueUniquePeriodicWork(
            SYNC_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_HOURS, TimeUnit.HOURS).setConstraints(needsNetwork).build(),
        )
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(SYNC_NOW)
        workManager.cancelUniqueWork(SYNC_PERIODIC)
    }

    private companion object {
        const val SYNC_NOW = "sync-now"
        const val SYNC_PERIODIC = "sync-periodic"
        const val CHANGE_DELAY_SECONDS = 10L
        const val PERIODIC_HOURS = 6L
    }
}
