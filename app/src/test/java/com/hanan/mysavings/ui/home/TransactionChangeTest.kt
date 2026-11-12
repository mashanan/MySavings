package com.hanan.mysavings.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class TransactionChangeTest {

    private val october = YearMonth.of(2026, 10)
    private val september = YearMonth.of(2026, 9)

    @Test
    fun `a save into the month on screen needs no month in the message`() {
        assertNull(TransactionChange.Saved(october).monthToMention(displayedMonth = october))
    }

    @Test
    fun `a save into another month names that month`() {
        assertEquals(september, TransactionChange.Saved(september).monthToMention(displayedMonth = october))
    }

    @Test
    fun `while Home is still loading no month is named`() {
        assertNull(TransactionChange.Saved(september).monthToMention(displayedMonth = null))
    }
}
