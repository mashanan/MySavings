package com.hanan.mysavings.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hanan.mysavings.data.local.entity.PROFILE_UUID

/**
 * Version 6 (phase 11b) prepares every synced table for sync: a uuid that is the same on
 * every phone, the time of the last change, and a "not uploaded yet" flag.
 *
 * Written by hand because the order matters and Room cannot generate it: the unique index on
 * `uuid` can only be created after every existing row got its own uuid.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {

    /** Tables whose rows already know when they were created; that time is their first updatedAt. */
    private val tablesWithCreatedAt = listOf(
        "transactions",
        "savings_goals",
        "savings_deposits",
        "debts",
        "debt_payments",
    )

    override fun migrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        tablesWithCreatedAt.forEach { table ->
            addSyncColumns(db, table)
            // 32 random hex characters: unique without needing Kotlin, inside one UPDATE.
            db.execSQL("UPDATE `$table` SET uuid = lower(hex(randomblob(16))), updatedAt = createdAt")
            createUuidIndex(db, table)
        }

        addSyncColumns(db, "budgets")
        // An old overall budget could exist twice for one month (NULLs never clash in the unique
        // index); the uuid below would then clash. Keep the newest row of such a pair.
        db.execSQL(
            """
            DELETE FROM budgets WHERE categoryId IS NULL
            AND id NOT IN (SELECT MAX(id) FROM budgets WHERE categoryId IS NULL GROUP BY yearMonth)
            """.trimIndent(),
        )
        // Same formula as budgetUuid(): month plus the category's icon key, or "total".
        db.execSQL(
            """
            UPDATE budgets SET updatedAt = $now, uuid = 'budget-' || yearMonth || '-' ||
                COALESCE((SELECT iconKey FROM categories WHERE categories.id = budgets.categoryId), 'total')
            """.trimIndent(),
        )
        createUuidIndex(db, "budgets")

        addSyncColumns(db, "user_profile")
        db.execSQL("UPDATE user_profile SET uuid = '$PROFILE_UUID', updatedAt = $now")
        createUuidIndex(db, "user_profile")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sync_tombstones` (`collection` TEXT NOT NULL, `uuid` TEXT NOT NULL, " +
                "`deletedAt` INTEGER NOT NULL, PRIMARY KEY(`collection`, `uuid`))",
        )
    }

    /** Defaults match SyncColumns; dirty = 1 so the first sync uploads everything that exists. */
    private fun addSyncColumns(db: SupportSQLiteDatabase, table: String) {
        db.execSQL("ALTER TABLE `$table` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `$table` ADD COLUMN `dirty` INTEGER NOT NULL DEFAULT 1")
    }

    // Same name Room gives the index declared on the entity, so the schemas match.
    private fun createUuidIndex(db: SupportSQLiteDatabase, table: String) {
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_${table}_uuid` ON `$table` (`uuid`)")
    }
}
