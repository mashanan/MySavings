package com.hanan.mysavings.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hanan.mysavings.domain.usecase.CheckDebtRemindersUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Daily check for debt payments that are coming up. All decisions live in
 * [CheckDebtRemindersUseCase]; the worker only connects it to WorkManager and notifications.
 */
@HiltWorker
class DebtReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val checkDebtReminders: CheckDebtRemindersUseCase,
    private val notifier: DebtReminderNotifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        checkDebtReminders().forEach(notifier::show)
        return Result.success()
    }
}
