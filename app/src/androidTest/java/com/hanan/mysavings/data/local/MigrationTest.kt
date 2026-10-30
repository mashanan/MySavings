package com.hanan.mysavings.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Upgrades real older databases (built from the committed schema files 1.json, 2.json) and
 * checks that nothing the user saved is lost. This is what protects existing installs
 * when the app updates.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MySavingsDatabase::class.java)

    @Test
    fun migrate1To2KeepsExistingDataAndAddsAlertsTable() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO categories (id, name, iconKey, colorHex, keywords, isDefault) VALUES (1, 'Makan & Minum', 'food', '#E57373', 'warung', 1)")
            execSQL("INSERT INTO transactions (amount, date, merchant, note, categoryId, source, createdAt) VALUES (25000, 20390, 'Warung Bu Siti', NULL, 1, 'MANUAL', 0)")
            execSQL("INSERT INTO budgets (yearMonth, categoryId, limitAmount) VALUES (202610, NULL, 1000000)")
            close()
        }

        // Applies the AutoMigration and validates the result against schema 2.json.
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true)

        db.query("SELECT amount, merchant, categoryId FROM transactions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(25_000L, cursor.getLong(0))
            assertEquals("Warung Bu Siti", cursor.getString(1))
            assertEquals(1L, cursor.getLong(2))
        }
        db.query("SELECT limitAmount FROM budgets WHERE yearMonth = 202610").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1_000_000L, cursor.getLong(0))
        }
        db.query("SELECT COUNT(*) FROM budget_alerts").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate2To3KeepsExpensesAndAddsEmptySavingsTables() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL("INSERT INTO categories (id, name, iconKey, colorHex, keywords, isDefault) VALUES (1, 'Makan & Minum', 'food', '#E57373', 'warung', 1)")
            execSQL("INSERT INTO transactions (amount, date, merchant, note, categoryId, source, createdAt) VALUES (25000, 20390, 'Warung Bu Siti', NULL, 1, 'MANUAL', 0)")
            close()
        }

        // Validates the result against schema 3.json, including the foreign key on savings_deposits.
        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true)

        db.query("SELECT amount FROM transactions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(25_000L, cursor.getLong(0))
        }
        db.query("SELECT (SELECT COUNT(*) FROM savings_goals) + (SELECT COUNT(*) FROM savings_deposits)").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate3To4KeepsSavingsAndAddsEmptyDebtTables() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL("INSERT INTO savings_goals (id, name, targetAmount, categoryKey, targetDate, createdAt) VALUES (1, 'Laptop', 12000000, 'gadget', NULL, 0)")
            execSQL("INSERT INTO savings_deposits (goalId, amount, date, note, createdAt) VALUES (1, 500000, 20390, NULL, 0)")
            close()
        }

        // Validates the result against schema 4.json, including the debt foreign keys.
        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true)

        db.query("SELECT SUM(amount) FROM savings_deposits WHERE goalId = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(500_000L, cursor.getLong(0))
        }
        db.query(
            "SELECT (SELECT COUNT(*) FROM debts) + (SELECT COUNT(*) FROM debt_payments) + (SELECT COUNT(*) FROM debt_reminders_sent)",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate4To5KeepsDebtsAndAddsAnEmptyProfileTable() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL("INSERT INTO debts (id, creditor, totalAmount, firstDueDate, installmentCount, note, createdAt) VALUES (1, 'Budi', 500000, 20400, 1, NULL, 0)")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true)

        db.query("SELECT creditor FROM debts WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Budi", cursor.getString(0))
        }
        db.query("SELECT COUNT(*) FROM user_profile").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate5To6GivesEveryRowSyncColumnsAndKeepsAllData() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL("INSERT INTO categories (id, name, iconKey, colorHex, keywords, isDefault) VALUES (1, 'Makan & Minum', 'food', '#E57373', '', 1)")
            execSQL("INSERT INTO transactions (amount, date, merchant, note, categoryId, source, createdAt) VALUES (25000, 20390, 'Warung', NULL, 1, 'MANUAL', 111)")
            execSQL("INSERT INTO transactions (amount, date, merchant, note, categoryId, source, createdAt) VALUES (10000, 20391, NULL, NULL, 1, 'SCAN', 222)")
            execSQL("INSERT INTO budgets (yearMonth, categoryId, limitAmount) VALUES (202610, NULL, 1000000)")
            // A duplicate overall budget, which the old unique index could not prevent.
            execSQL("INSERT INTO budgets (yearMonth, categoryId, limitAmount) VALUES (202610, NULL, 2000000)")
            execSQL("INSERT INTO budgets (yearMonth, categoryId, limitAmount) VALUES (202610, 1, 300000)")
            execSQL("INSERT INTO savings_goals (id, name, targetAmount, categoryKey, targetDate, createdAt) VALUES (1, 'Laptop', 12000000, 'gadget', NULL, 333)")
            execSQL("INSERT INTO savings_deposits (goalId, amount, date, note, createdAt) VALUES (1, 500000, 20390, NULL, 444)")
            execSQL("INSERT INTO debts (id, creditor, totalAmount, firstDueDate, installmentCount, note, createdAt) VALUES (1, 'Budi', 500000, 20400, 1, NULL, 555)")
            execSQL("INSERT INTO debt_payments (debtId, amount, date, note, createdAt) VALUES (1, 100000, 20390, NULL, 666)")
            execSQL("INSERT INTO user_profile (id, name, photoUpdatedAt) VALUES (1, 'Louise', NULL)")
            close()
        }

        // Validates against schema 6.json: columns, defaults and the unique uuid indexes.
        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Every row of every synced table: a uuid, its creation time as first change, waiting for upload.
        db.query("SELECT uuid, updatedAt, dirty FROM transactions ORDER BY id").use { cursor ->
            val uuids = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                assertEquals(32, cursor.getString(0).length)
                uuids += cursor.getString(0)
                assertEquals(1, cursor.getInt(2))
            }
            assertEquals(2, uuids.size)
        }
        db.query("SELECT updatedAt FROM transactions ORDER BY id").use { cursor ->
            cursor.moveToNext()
            assertEquals(111L, cursor.getLong(0))
        }
        listOf("savings_goals", "savings_deposits", "debts", "debt_payments").forEach { table ->
            db.query("SELECT uuid, dirty FROM $table").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(32, cursor.getString(0).length)
                assertEquals(1, cursor.getInt(1))
            }
        }
        // Budgets: duplicate overall budget dropped (newest kept), uuids from month + category.
        db.query("SELECT uuid, limitAmount FROM budgets ORDER BY uuid").use { cursor ->
            assertTrue(cursor.moveToNext())
            assertEquals("budget-202610-food", cursor.getString(0))
            assertTrue(cursor.moveToNext())
            assertEquals("budget-202610-total", cursor.getString(0))
            assertEquals(2_000_000L, cursor.getLong(1))
            assertFalse(cursor.moveToNext())
        }
        db.query("SELECT uuid, name FROM user_profile").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("profile", cursor.getString(0))
            assertEquals("Louise", cursor.getString(1))
        }
        db.query("SELECT COUNT(*) FROM sync_tombstones").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        db.close()
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
