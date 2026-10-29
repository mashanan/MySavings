package com.hanan.strukku.data.di

import com.hanan.strukku.data.repository.RoomBudgetAlertRepository
import com.hanan.strukku.data.repository.RoomBudgetRepository
import com.hanan.strukku.data.repository.RoomCategoryRepository
import com.hanan.strukku.data.repository.RoomTransactionRepository
import com.hanan.strukku.domain.repository.BudgetAlertRepository
import com.hanan.strukku.domain.repository.BudgetRepository
import com.hanan.strukku.domain.repository.CategoryRepository
import com.hanan.strukku.domain.repository.TransactionRepository
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
}
