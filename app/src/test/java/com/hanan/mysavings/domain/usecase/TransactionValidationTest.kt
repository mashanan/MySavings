package com.hanan.mysavings.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TransactionValidationTest {

    private val today = LocalDate.of(2026, 10, 29)

    @Test
    fun `positive amount up to today is valid`() {
        assertNull(validateTransaction(1, today, today))
        assertNull(validateTransaction(25_000, today.minusYears(1), today))
    }

    @Test
    fun `zero or negative amount is invalid`() {
        assertEquals(TransactionError.AMOUNT_NOT_POSITIVE, validateTransaction(0, today, today))
        assertEquals(TransactionError.AMOUNT_NOT_POSITIVE, validateTransaction(-1, today, today))
    }

    @Test
    fun `date after today is invalid`() {
        assertEquals(TransactionError.DATE_IN_FUTURE, validateTransaction(1, today.plusDays(1), today))
    }
}
