package com.hanan.mysavings.data.di

import android.content.Context
import androidx.room.Room
import com.hanan.mysavings.data.local.MIGRATION_5_6
import com.hanan.mysavings.data.local.MySavingsDatabase
import com.hanan.mysavings.data.local.dao.BudgetAlertDao
import com.hanan.mysavings.data.local.dao.BudgetDao
import com.hanan.mysavings.data.local.dao.CategoryDao
import com.hanan.mysavings.data.local.dao.DebtDao
import com.hanan.mysavings.data.local.dao.ProfileDao
import com.hanan.mysavings.data.local.dao.SavingsDao
import com.hanan.mysavings.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // One database instance for the whole app; opening it is expensive.
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MySavingsDatabase =
        Room.databaseBuilder(context, MySavingsDatabase::class.java, MySavingsDatabase.NAME)
            .addCallback(MySavingsDatabase.SeedCallback)
            // Versions 1-5 migrate automatically (AutoMigration); 5 -> 6 is written by hand.
            .addMigrations(MIGRATION_5_6)
            .build()

    @Provides
    fun provideCategoryDao(db: MySavingsDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTransactionDao(db: MySavingsDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideBudgetDao(db: MySavingsDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideBudgetAlertDao(db: MySavingsDatabase): BudgetAlertDao = db.budgetAlertDao()

    @Provides
    fun provideSavingsDao(db: MySavingsDatabase): SavingsDao = db.savingsDao()

    @Provides
    fun provideDebtDao(db: MySavingsDatabase): DebtDao = db.debtDao()

    @Provides
    fun provideProfileDao(db: MySavingsDatabase): ProfileDao = db.profileDao()

    // Use cases read "now" through this Clock; tests pass a fixed one instead.
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
