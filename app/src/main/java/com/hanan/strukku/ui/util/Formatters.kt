package com.hanan.strukku.ui.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val indonesian: Locale = Locale.forLanguageTag("id-ID")
private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", indonesian)
private val monthYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", indonesian)

/** Longest amount a user can type: 12 digits (up to Rp999 miliar), far below Long.MAX_VALUE. */
const val MAX_AMOUNT_DIGITS = 12

/** 25000 -> "Rp25.000", -50000 -> "-Rp50.000". */
fun formatRupiah(amount: Long): String {
    val digits = NumberFormat.getIntegerInstance(indonesian).format(abs(amount))
    return if (amount < 0) "-Rp$digits" else "Rp$digits"
}

/** "25000" -> "25.000". Input must contain digits only. */
fun groupThousands(digits: String): String =
    digits.reversed().chunked(3).joinToString(".").reversed()

/** Keeps only digits, drops leading zeros and caps the length, e.g. "Rp 02.500" -> "2500". */
fun sanitizeAmountInput(text: String): String =
    text.filter { it.isDigit() }.trimStart('0').take(MAX_AMOUNT_DIGITS)

/** Digits-only text to an amount, or `null` when empty. */
fun parseAmount(digits: String): Long? = digits.toLongOrNull()

/** 2026-10-29 -> "29 Okt 2026". */
fun formatShortDate(date: LocalDate): String = shortDateFormatter.format(date)

/** 2026-10 -> "Oktober 2026". */
fun formatMonthYear(yearMonth: YearMonth): String = monthYearFormatter.format(yearMonth)
