package com.hanan.mysavings.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class FormattersTest {

    private val english = Locale.ENGLISH
    private val indonesian = Locale.forLanguageTag("id")

    @Test
    fun `formatRupiah uses dots as thousands separator in every language`() {
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
    fun `dates follow the UI language`() {
        val date = LocalDate.of(2026, 10, 29)
        val month = YearMonth.of(2026, 10)

        assertEquals("29 Oct 2026", formatShortDate(date, english))
        assertEquals("October 2026", formatMonthYear(month, english))
        assertEquals("Oct", formatMonthShort(month, english))

        assertEquals("29 Okt 2026", formatShortDate(date, indonesian))
        assertEquals("Oktober 2026", formatMonthYear(month, indonesian))
        assertEquals("Okt", formatMonthShort(month, indonesian))
    }

    @Test
    fun `short weekday follows the language`() {
        val thursday = LocalDate.of(2026, 10, 29)

        assertEquals("Thu", formatDayOfWeekShort(thursday, english))
        assertEquals("Kam", formatDayOfWeekShort(thursday, indonesian))
    }

    @Test
    fun `day header shows weekday and date without the year`() {
        val tuesday = LocalDate.of(2026, 10, 27)

        assertEquals("Tue, 27 Oct", formatDayHeader(tuesday, english))
        assertEquals("Sel, 27 Okt", formatDayHeader(tuesday, indonesian))
    }

    @Test
    fun `compact amounts use English units and decimal point`() {
        assertEquals("Rp500", formatRupiahShort(500, english))
        assertEquals("Rp250K", formatRupiahShort(250_000, english))
        assertEquals("Rp1.2M", formatRupiahShort(1_234_567, english))
        assertEquals("Rp15M", formatRupiahShort(15_000_000, english))
        assertEquals("Rp2.5B", formatRupiahShort(2_500_000_000, english))
    }

    @Test
    fun `compact amounts use Indonesian units and decimal comma`() {
        assertEquals("Rp250 rb", formatRupiahShort(250_000, indonesian))
        assertEquals("Rp1,2 jt", formatRupiahShort(1_234_567, indonesian))
        assertEquals("Rp15 jt", formatRupiahShort(15_000_000, indonesian))
        assertEquals("Rp2,5 M", formatRupiahShort(2_500_000_000, indonesian))
    }

    @Test
    fun `Indonesian is recognised by both its legacy and modern code`() {
        assertTrue(Locale.forLanguageTag("id").isIndonesian())
        assertTrue(Locale.forLanguageTag("in").isIndonesian())
        assertFalse(english.isIndonesian())
    }
}
