package com.hanan.strukku.domain.parser

import java.time.LocalDate

/**
 * Extracts merchant, total and date from the text rows of an Indonesian receipt.
 *
 * Plain heuristics instead of a trained model: they run instantly on-device, are easy to
 * read, and every rule is pinned down by a unit test. Anything they miss is corrected
 * by the user on the Confirm screen.
 */
object ReceiptParser {

    data class Result(
        val merchant: String?,
        val total: Long?,
        val date: LocalDate?,
    )

    fun parse(rows: List<String>, today: LocalDate): Result = Result(
        merchant = findMerchant(rows, today),
        total = findTotal(rows),
        date = findDate(rows, today),
    )

    // ---------------------------------------------------------------- Total

    /** Labels that mark the amount to pay, with a priority: higher wins when several rows match. */
    private val totalLabels = listOf(
        "GRAND TOTAL" to 3, "GRANDTOTAL" to 3,
        "TOTAL BAYAR" to 2, "TOTAL BELANJA" to 2, "TOTAL TAGIHAN" to 2,
        "TOTAL" to 1, "JUMLAH" to 1,
    )

    /** Words on rows that hold some other amount: subtotal, tax, discount, cash given, change... */
    private val nonTotalWords = setOf(
        "SUB", "SUBTOTAL", "DISKON", "DISC", "DISCOUNT", "POTONGAN", "HEMAT", "VOUCHER",
        "PPN", "PAJAK", "TAX", "DPP", "SERVICE", "SERVIS",
        "TUNAI", "CASH", "KEMBALI", "KEMBALIAN", "CHANGE", "DEBIT", "KREDIT", "CREDIT", "EDC", "QRIS",
    )

    /** Below this an "amount" is almost certainly a quantity or an item count. */
    private const val MIN_AMOUNT = 100L
    private const val MAX_AMOUNT = 999_999_999L

    private fun findTotal(rows: List<String>): Long? {
        val labelled = rows.mapIndexedNotNull { index, row ->
            val words = words(row)
            val priority = totalLabels.filter { (label, _) -> words.containsPhrase(label) }.maxOfOrNull { it.second }
            if (priority == null || words.any { it in nonTotalWords }) return@mapIndexedNotNull null

            // The amount is normally on the same row; if OCR split it off, it is on the next row on its own.
            val amount = lastAmount(row, groupedOnly = false)
                ?: rows.getOrNull(index + 1)?.takeIf { words(it).isEmpty() }?.let { lastAmount(it, groupedOnly = false) }
            amount?.let { priority to it }
        }
        if (labelled.isNotEmpty()) {
            return labelled.maxWith(compareBy<Pair<Int, Long>> { it.first }.thenBy { it.second }).second
        }

        // No usable label: fall back to the largest money-looking amount that isn't cash or change.
        // Only "25.000"-style amounts count here, so phone and receipt numbers are ignored.
        return rows
            .filter { row -> words(row).none { it in nonTotalWords } }
            .flatMap { amounts(it, groupedOnly = true) }
            .maxOrNull()
    }

    private val numberToken = Regex("""\d[\d.,]*\d|\d""")
    private val groupedNumber = Regex("""\d{1,3}(?:[.,]\d{3})+([.,]\d{2})?""")
    private val plainNumber = Regex("""\d+([.,]\d{2})?""")

    private fun lastAmount(row: String, groupedOnly: Boolean): Long? = amounts(row, groupedOnly).lastOrNull()

    private fun amounts(row: String, groupedOnly: Boolean): List<Long> =
        numberToken.findAll(row).mapNotNull { parseAmount(it.value, groupedOnly) }.toList()

    /**
     * "25.000", "25,000", "25.000,00" and "1.250.000" are grouped amounts; "25000" is plain.
     * A trailing two-digit group is treated as cents and dropped. Dates and times such as
     * "29.10.2026" or "12.30" do not fit either shape or fall below [MIN_AMOUNT].
     */
    private fun parseAmount(token: String, groupedOnly: Boolean): Long? {
        val integerPart = groupedNumber.matchEntire(token)?.let { match ->
            if (match.groupValues[1].isEmpty()) token else token.dropLast(3)
        } ?: plainNumber.matchEntire(token)?.takeIf { !groupedOnly }?.let { match ->
            if (match.groupValues[1].isEmpty()) token else token.dropLast(3)
        } ?: return null

        return integerPart.filter { it.isDigit() }.toLongOrNull()?.takeIf { it in MIN_AMOUNT..MAX_AMOUNT }
    }

    // ---------------------------------------------------------------- Date

    /** dd/MM/yyyy, dd-MM-yy, dd.MM.yyyy */
    private val dayFirstDate = Regex("""(?<!\d)(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4}|\d{2})(?!\d)""")

    /** yyyy-MM-dd */
    private val yearFirstDate = Regex("""(?<!\d)(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})(?!\d)""")

    /** 29 Okt 2026, 29-OCT-26, 29 Oktober 2026 */
    private val textMonthDate = Regex(
        """(?<!\d)(\d{1,2})[\s\-./]*(JAN|FEB|MAR|APR|MEI|MAY|JUN|JUL|AGU|AGT|AUG|SEP|OKT|OCT|NOV|DES|DEC)[A-Z]*[\s\-./,]*(\d{4}|\d{2})(?!\d)""",
    )

    private val monthNumbers = mapOf(
        "JAN" to 1, "FEB" to 2, "MAR" to 3, "APR" to 4, "MEI" to 5, "MAY" to 5, "JUN" to 6, "JUL" to 7,
        "AGU" to 8, "AGT" to 8, "AUG" to 8, "SEP" to 9, "OKT" to 10, "OCT" to 10, "NOV" to 11, "DES" to 12, "DEC" to 12,
    )

    private fun findDate(rows: List<String>, today: LocalDate): LocalDate? =
        rows.asSequence().mapNotNull { findDateInRow(it.uppercase(), today) }.firstOrNull()

    private fun findDateInRow(row: String, today: LocalDate): LocalDate? {
        val candidates = sequence {
            yearFirstDate.findAll(row).forEach { yield(Triple(it.int(3), it.int(2), it.int(1))) }
            dayFirstDate.findAll(row).forEach { yield(Triple(it.int(1), it.int(2), it.int(3))) }
            textMonthDate.findAll(row).forEach { yield(Triple(it.int(1), monthNumbers.getValue(it.groupValues[2]), it.int(3))) }
        }
        return candidates.mapNotNull { (day, month, year) -> toDate(day, month, year, today) }.firstOrNull()
    }

    private fun MatchResult.int(group: Int) = groupValues[group].toInt()

    /** Builds a valid date, expanding two-digit years and rejecting the future (not a real receipt date). */
    private fun toDate(day: Int, month: Int, year: Int, today: LocalDate): LocalDate? {
        val fullYear = if (year < 100) 2000 + year else year
        val date = runCatching { LocalDate.of(fullYear, month, day) }.getOrNull() ?: return null
        return date.takeIf { fullYear >= 2000 && !it.isAfter(today) }
    }

    // ---------------------------------------------------------------- Merchant

    /** The store name is printed at the very top; only look there. */
    private const val MERCHANT_SEARCH_ROWS = 8

    /** Rows with these words are addresses, contact details or receipt metadata, not the store name. */
    private val nonMerchantWords = setOf(
        "JL", "JLN", "JALAN", "RT", "RW", "KEL", "KEC", "KAB", "BLOK", "RUKO", "NO",
        "TELP", "TLP", "TEL", "HP", "FAX", "WA", "NPWP", "WWW", "COM", "ID",
        "KASIR", "STRUK", "NOTA", "BON", "INVOICE", "FAKTUR", "BILL", "ORDER", "MEJA", "TABLE",
        "TANGGAL", "TGL", "JAM", "WAKTU", "SELAMAT", "DATANG", "TERIMA", "KASIH",
    )
    private val legalEntityWords = setOf("PT", "CV", "TBK")

    private fun findMerchant(rows: List<String>, today: LocalDate): String? {
        val candidates = rows.take(MERCHANT_SEARCH_ROWS).filter { looksLikeName(it, today) }
        // Prefer a brand name ("INDOMARET") over the legal company name ("PT INDOMARCO PRISMATAMA").
        val name = candidates.firstOrNull { row -> words(row).none { it in legalEntityWords } } ?: candidates.firstOrNull()
        return name?.let(::cleanName)
    }

    private fun looksLikeName(row: String, today: LocalDate): Boolean {
        val compact = row.filterNot { it.isWhitespace() }
        val letters = compact.count { it.isLetter() }
        return letters >= 3 &&
            letters >= compact.length * 0.6 &&
            words(row).none { it in nonMerchantWords } &&
            findDateInRow(row.uppercase(), today) == null
    }

    /** Trims stray symbols and turns ALL-CAPS receipt text into "Title Case". */
    private fun cleanName(row: String): String {
        val trimmed = row.trim { !it.isLetterOrDigit() }.replace(Regex("""\s+"""), " ")
        if (trimmed.any { it.isLowerCase() }) return trimmed
        return trimmed.lowercase().split(' ').joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
    }

    // ---------------------------------------------------------------- Helpers

    private val wordPattern = Regex("""[A-Z]+""")

    private fun words(row: String): List<String> = wordPattern.findAll(row.uppercase()).map { it.value }.toList()

    private fun List<String>.containsPhrase(phrase: String): Boolean {
        val target = phrase.split(' ')
        return windowed(target.size).any { it == target }
    }
}
