package com.hanan.mysavings.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Rupiah keeps its Indonesian format in every UI language: "Rp25.000" is how the currency
 * is written, just as "$25,000.00" stays American in a French app. Dates and words follow
 * the user's language and take a [Locale].
 */
private val rupiahLocale: Locale = Locale.forLanguageTag("id-ID")

/** Longest amount a user can type: 12 digits (up to Rp999 billion), far below Long.MAX_VALUE. */
const val MAX_AMOUNT_DIGITS = 12

/** The language the UI is currently shown in (follows the per-app language setting). */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** 25000 -> "Rp25.000", -50000 -> "-Rp50.000". */
fun formatRupiah(amount: Long): String {
    val digits = NumberFormat.getIntegerInstance(rupiahLocale).format(abs(amount))
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

/** 2026-10-29 -> "29 Oct 2026" (en) / "29 Okt 2026" (id). */
fun formatShortDate(date: LocalDate, locale: Locale): String =
    DateTimeFormatter.ofPattern("d MMM yyyy", locale).format(date)

/** 2026-10 -> "October 2026" (en) / "Oktober 2026" (id). */
fun formatMonthYear(yearMonth: YearMonth, locale: Locale): String =
    DateTimeFormatter.ofPattern("MMMM yyyy", locale).format(yearMonth)

/** 2026-10 -> "Oct" (en) / "Okt" (id). */
fun formatMonthShort(yearMonth: YearMonth, locale: Locale): String =
    DateTimeFormatter.ofPattern("MMM", locale).format(yearMonth)

/** 2026-10-29 -> "Thu" (en) / "Kam" (id). */
fun formatDayOfWeekShort(date: LocalDate, locale: Locale): String =
    DateTimeFormatter.ofPattern("EEE", locale).format(date)

/**
 * Compact amount for charts: "Rp250K", "Rp1.2M" in English; "Rp250 rb", "Rp1,2 jt" in
 * Indonesian (ribu, juta, miliar). One decimal at most, with the language's decimal mark.
 */
fun formatRupiahShort(amount: Long, locale: Locale): String {
    val units = if (locale.isIndonesian()) listOf(" rb", " jt", " M") else listOf("K", "M", "B")
    return when {
        amount < 1_000 -> formatRupiah(amount)
        amount < 1_000_000 -> "Rp${oneDecimal(amount / 1_000.0, locale)}${units[0]}"
        amount < 1_000_000_000 -> "Rp${oneDecimal(amount / 1_000_000.0, locale)}${units[1]}"
        else -> "Rp${oneDecimal(amount / 1_000_000_000.0, locale)}${units[2]}"
    }
}

/** Java reports Indonesian as "in" (legacy code) or "id" depending on the version. */
fun Locale.isIndonesian(): Boolean = language == "in" || language == "id"

private fun oneDecimal(value: Double, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(value)
