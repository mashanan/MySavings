package com.hanan.mysavings.data.local

import com.hanan.mysavings.data.local.entity.SyncColumns
import com.hanan.mysavings.data.local.entity.budgetUuid
import com.hanan.mysavings.data.repository.toEntity
import com.hanan.mysavings.domain.model.Transaction
import com.hanan.mysavings.domain.model.TransactionSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SyncColumnsTest {

    @Test
    fun `a budget's uuid comes from month and category, so phones agree on it`() {
        assertEquals("budget-202610-total", budgetUuid(202610, null))
        assertEquals("budget-202610-food", budgetUuid(202610, "food"))
        assertNotEquals(budgetUuid(202610, "food"), budgetUuid(202611, "food"))
    }

    @Test
    fun `a local edit keeps the uuid, moves the time and waits for upload`() {
        val uploaded = SyncColumns(uuid = "abc", updatedAt = 100, dirty = false)

        assertEquals(SyncColumns(uuid = "abc", updatedAt = 250, dirty = true), uploaded.touched(250))
    }

    @Test
    fun `a new row gets its own uuid, its creation time as first change, and is not uploaded yet`() {
        fun newTransaction() = Transaction(
            amount = 25_000,
            date = LocalDate.of(2026, 10, 29),
            merchant = null,
            note = null,
            categoryId = 1,
            source = TransactionSource.MANUAL,
            createdAt = Instant.ofEpochMilli(1_700_000_000_000),
        ).toEntity()

        val first = newTransaction()
        val second = newTransaction()

        assertEquals(1_700_000_000_000, first.sync.updatedAt)
        assertTrue(first.sync.dirty)
        assertNotEquals(first.sync.uuid, second.sync.uuid)
    }
}
