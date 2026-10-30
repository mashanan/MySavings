package com.hanan.mysavings.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hanan.mysavings.domain.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.Clock

/** Runs [SyncEngine] in the background; WorkManager only starts it with a connection. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val engine: SyncEngine,
    private val authRepository: AuthRepository,
    private val state: SyncStateStore,
    private val clock: Clock,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = authRepository.observeUser().first()?.uid ?: return Result.success() // signed out: nothing to sync
        return try {
            engine.sync(uid)
            state.recordSuccess(clock.millis())
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Typically a dropped connection. WorkManager retries with growing delays; the
            // changes stay marked as dirty, so nothing is lost meanwhile.
            state.recordFailure()
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}
