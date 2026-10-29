package com.hanan.strukku.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hanan.strukku.domain.usecase.CheckBudgetAlertUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.YearMonth

/**
 * Checks this month's budget in the background and shows a notification when a threshold
 * is reached. All decisions live in [CheckBudgetAlertUseCase]; the worker only connects it
 * to WorkManager and the notification system.
 */
@HiltWorker
class BudgetCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val checkBudgetAlert: CheckBudgetAlertUseCase,
    private val notifier: BudgetAlertNotifier,
    private val clock: Clock,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        checkBudgetAlert(YearMonth.now(clock))?.let(notifier::show)
        return Result.success()
    }
}
