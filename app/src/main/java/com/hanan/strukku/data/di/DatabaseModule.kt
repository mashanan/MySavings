package com.hanan.strukku.data.di

import android.content.Context
import androidx.room.Room
import com.hanan.strukku.data.local.StrukKuDatabase
import com.hanan.strukku.data.local.dao.BudgetDao
import com.hanan.strukku.data.local.dao.CategoryDao
import com.hanan.strukku.data.local.dao.TransactionDao
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
    fun provideDatabase(@ApplicationContext context: Context): StrukKuDatabase =
        Room.databaseBuilder(context, StrukKuDatabase::class.java, StrukKuDatabase.NAME)
            .addCallback(StrukKuDatabase.SeedCallback)
            .build()

    @Provides
    fun provideCategoryDao(db: StrukKuDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTransactionDao(db: StrukKuDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideBudgetDao(db: StrukKuDatabase): BudgetDao = db.budgetDao()

    // Use cases read "now" through this Clock; tests pass a fixed one instead.
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
