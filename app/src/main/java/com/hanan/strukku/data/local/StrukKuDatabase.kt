package com.hanan.strukku.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hanan.strukku.data.local.dao.BudgetAlertDao
import com.hanan.strukku.data.local.dao.BudgetDao
import com.hanan.strukku.data.local.dao.CategoryDao
import com.hanan.strukku.data.local.dao.TransactionDao
import com.hanan.strukku.data.local.entity.BudgetAlertEntity
import com.hanan.strukku.data.local.entity.BudgetEntity
import com.hanan.strukku.data.local.entity.CategoryEntity
import com.hanan.strukku.data.local.entity.TransactionEntity

// Version history:
// 1: categories, transactions, budgets
// 2: budget_alerts (phase 6b). A new table only, so Room can generate the migration.
@Database(
    entities = [CategoryEntity::class, TransactionEntity::class, BudgetEntity::class, BudgetAlertEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class StrukKuDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun budgetAlertDao(): BudgetAlertDao

    /**
     * Seeds the default categories once, when the database file is first created.
     * Runs inside Room's creation transaction, before any DAO can read the table.
     */
    object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            DefaultCategories.all.forEach { category ->
                val values = ContentValues().apply {
                    put("name", category.name)
                    put("iconKey", category.iconKey)
                    put("colorHex", category.colorHex)
                    put("keywords", category.keywords)
                    put("isDefault", category.isDefault)
                }
                db.insert("categories", SQLiteDatabase.CONFLICT_ABORT, values)
            }
        }
    }

    companion object {
        const val NAME = "strukku.db"
    }
}
