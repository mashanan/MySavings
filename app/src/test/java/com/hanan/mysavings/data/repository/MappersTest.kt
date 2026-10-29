package com.hanan.mysavings.data.repository

import com.hanan.mysavings.data.local.entity.BudgetEntity
import com.hanan.mysavings.data.local.entity.CategoryEntity
import com.hanan.mysavings.domain.model.Transaction
import com.hanan.mysavings.domain.model.TransactionSource
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class MappersTest {

    @Test
    fun `category keywords are split, trimmed and blanks dropped`() {
        val entity = CategoryEntity(1, "Belanja", "groceries", "#fff", "indomaret, alfamart,,  ", true)

        assertEquals(listOf("indomaret", "alfamart"), entity.toDomain().keywords)
    }

    @Test
    fun `empty keyword string maps to empty list`() {
        val entity = CategoryEntity(7, "Lainnya", "other", "#fff", "", true)

        assertEquals(emptyList<String>(), entity.toDomain().keywords)
    }

    @Test
    fun `transaction survives a round trip through the entity`() {
        val transaction = Transaction(
            id = 3,
            amount = 125_500,
            date = LocalDate.of(2026, 2, 28),
            merchant = "Kopi Kenangan",
            note = null,
            categoryId = 1,
            source = TransactionSource.SCAN,
            createdAt = Instant.parse("2026-02-28T10:15:30.123Z"),
        )

        assertEquals(transaction, transaction.toEntity().toDomain())
    }

    @Test
    fun `yearMonth is stored as yyyyMM`() {
        assertEquals(202610, YearMonth.of(2026, 10).toInt())
        assertEquals(YearMonth.of(2027, 1), 202701.toYearMonth())
        assertEquals(YearMonth.of(2026, 10), BudgetEntity(1, 202610, null, 1).toDomain().yearMonth)
    }
}
