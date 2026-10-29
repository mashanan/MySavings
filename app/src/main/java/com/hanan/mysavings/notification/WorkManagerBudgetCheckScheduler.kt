package com.hanan.mysavings.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hanan.mysavings.domain.repository.BudgetCheckScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerBudgetCheckScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : BudgetCheckScheduler {

    private val workManager get() = WorkManager.getInstance(context)

    /**
     * Unique work with REPLACE: saving several transactions in a row leaves a single pending
     * check, which then sees all of them at once.
     */
    override fun requestCheck() {
        workManager.enqueueUniqueWork(
            CHECK_NOW,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<BudgetCheckWorker>().build(),
        )
    }

    /**
     * Daily safety net, e.g. for a month that starts while the app is closed. KEEP leaves an
     * already scheduled job untouched, so calling this on every app start is cheap.
     */
    fun scheduleDailyCheck() {
        workManager.enqueueUniquePeriodicWork(
            CHECK_DAILY,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BudgetCheckWorker>(1, TimeUnit.DAYS).build(),
        )
    }

    private companion object {
        const val CHECK_NOW = "budget-check-now"
        const val CHECK_DAILY = "budget-check-daily"
    }
}
