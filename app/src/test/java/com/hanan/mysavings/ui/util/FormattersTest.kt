package com.hanan.mysavings.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FormattersTest {

    @Test
    fun `formatRupiah uses dots as thousands separator`() {
        assertEquals("Rp0", formatRupiah(0))
        assertEquals("Rp500", formatRupiah(500))
        assertEquals("Rp25.000", formatRupiah(25_000))
        assertEquals("Rp1.250.000", formatRupiah(1_250_000))
    }

    @Test
    fun `formatRupiah puts the minus sign before Rp`() {
        assertEquals("-Rp50.000", formatRupiah(-50_000))
    }

    @Test
    fun `groupThousands inserts a dot every three digits from the right`() {
        assertEquals("", groupThousands(""))
        assertEquals("999", groupThousands("999"))
        assertEquals("1.000", groupThousands("1000"))
        assertEquals("123.456.789", groupThousands("123456789"))
    }

    @Test
    fun `sanitizeAmountInput keeps digits, drops leading zeros and caps length`() {
        assertEquals("2500", sanitizeAmountInput("Rp 02.500"))
        assertEquals("", sanitizeAmountInput("000"))
        assertEquals("", sanitizeAmountInput("abc"))
        assertEquals("123456789012", sanitizeAmountInput("1234567890123456"))
    }

    @Test
    fun `parseAmount returns null for empty input`() {
        assertNull(parseAmount(""))
        assertEquals(25_000L, parseAmount("25000"))
    }

    @Test
    fun `dates use Indonesian month names`() {
        assertEquals("29 Okt 2026", formatShortDate(LocalDate.of(2026, 10, 29)))
        assertEquals("Oktober 2026", formatMonthYear(YearMonth.of(2026, 10)))
    }

    @Test
    fun `short month and compact amounts for charts`() {
        assertEquals("Okt", formatMonthShort(YearMonth.of(2026, 10)))
        assertEquals("Rp500", formatRupiahShort(500))
        assertEquals("Rp250 rb", formatRupiahShort(250_000))
        assertEquals("Rp1,2 jt", formatRupiahShort(1_234_567))
        assertEquals("Rp15 jt", formatRupiahShort(15_000_000))
    }
}
