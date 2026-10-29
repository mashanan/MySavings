package com.hanan.strukku.notification

import com.hanan.strukku.domain.repository.BudgetCheckScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds
    abstract fun bindBudgetCheckScheduler(impl: WorkManagerBudgetCheckScheduler): BudgetCheckScheduler

    @Binds
    abstract fun bindBudgetAlertNotifier(impl: AndroidBudgetAlertNotifier): BudgetAlertNotifier
}
