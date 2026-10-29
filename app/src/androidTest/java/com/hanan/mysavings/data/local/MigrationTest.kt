package com.hanan.mysavings.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
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

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
