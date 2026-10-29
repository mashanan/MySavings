package com.hanan.mysavings.notification

import com.hanan.mysavings.domain.repository.BudgetCheckScheduler
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

    @Binds
    abstract fun bindDebtReminderNotifier(impl: AndroidDebtReminderNotifier): DebtReminderNotifier
}
