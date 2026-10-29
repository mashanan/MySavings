package com.hanan.strukku

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.hanan.strukku.notification.WorkManagerBudgetCheckScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. [HiltAndroidApp] generates the app-level DI container
 * that every other Hilt component (activities, ViewModels, workers) hangs off.
 *
 * It also configures WorkManager with Hilt's worker factory, so workers can receive
 * use cases through their constructors like any other class.
 */
@HiltAndroidApp
class StrukKuApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var budgetCheckScheduler: WorkManagerBudgetCheckScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        budgetCheckScheduler.scheduleDailyCheck()
    }
}
