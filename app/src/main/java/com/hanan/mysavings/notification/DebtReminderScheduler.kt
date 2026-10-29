package com.hanan.mysavings.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebtReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /**
     * Runs the reminder check about once a day. WorkManager cannot promise an exact time,
     * which is why the use case also sends a missed "3 days before" reminder on a later day.
     * KEEP leaves an already scheduled job untouched, so calling this on every app start is cheap.
     */
    fun scheduleDailyCheck() {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CHECK_DAILY,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DebtReminderWorker>(1, TimeUnit.DAYS).build(),
        )
    }

    private companion object {
        const val CHECK_DAILY = "debt-reminder-daily"
    }
}
