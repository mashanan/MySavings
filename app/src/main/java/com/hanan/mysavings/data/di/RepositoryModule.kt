package com.hanan.mysavings.data.di

import com.hanan.mysavings.data.repository.RoomBudgetAlertRepository
import com.hanan.mysavings.data.repository.RoomBudgetRepository
import com.hanan.mysavings.data.repository.RoomCategoryRepository
import com.hanan.mysavings.data.repository.RoomDebtReminderRepository
import com.hanan.mysavings.data.repository.RoomDebtRepository
import com.hanan.mysavings.data.repository.RoomSavingsRepository
import com.hanan.mysavings.data.repository.RoomTransactionRepository
import com.hanan.mysavings.domain.repository.BudgetAlertRepository
import com.hanan.mysavings.domain.repository.BudgetRepository
import com.hanan.mysavings.domain.repository.CategoryRepository
import com.hanan.mysavings.domain.repository.DebtReminderRepository
import com.hanan.mysavings.domain.repository.DebtRepository
import com.hanan.mysavings.domain.repository.SavingsRepository
import com.hanan.mysavings.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Tells Hilt which implementation to inject for each domain interface. `@Binds` generates
 * no extra code at runtime, unlike a `@Provides` method that calls the constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindTransactionRepository(impl: RoomTransactionRepository): TransactionRepository

    @Binds
    abstract fun bindCategoryRepository(impl: RoomCategoryRepository): CategoryRepository

    @Binds
    abstract fun bindBudgetRepository(impl: RoomBudgetRepository): BudgetRepository

    @Binds
    abstract fun bindBudgetAlertRepository(impl: RoomBudgetAlertRepository): BudgetAlertRepository

    @Binds
    abstract fun bindSavingsRepository(impl: RoomSavingsRepository): SavingsRepository

    @Binds
    abstract fun bindDebtRepository(impl: RoomDebtRepository): DebtRepository

    @Binds
    abstract fun bindDebtReminderRepository(impl: RoomDebtReminderRepository): DebtReminderRepository
}
