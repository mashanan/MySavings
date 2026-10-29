package com.hanan.strukku.export

import kotlin.math.max

/**
 * Page geometry of the PDF report, in PostScript points (1/72 inch), and the pagination
 * that follows from it. Pure Kotlin: the PDF writer draws with these numbers, and unit tests
 * check the page breaks without rendering anything.
 */
object ReportLayout {
    // A4
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842
    const val MARGIN = 40f

    const val ROW = 18f
    const val TABLE_HEADER = 22f
    const val SECTION_TITLE = 28f
    const val SECTION_GAP = 16f
    const val FOOTER = 30f

    /** "Laporan Pengeluaran", the month and the "generated on" line. */
    const val TITLE_BLOCK = 64f

    /** Total, budget and remaining, plus spacing below. */
    const val SUMMARY_BLOCK = 3 * ROW + 16f

    /** Title line on pages 2+. */
    const val CONTINUATION_HEADER = 36f

    /** Rows stop here so they never run into the page footer. */
    const val BODY_BOTTOM = PAGE_HEIGHT - MARGIN - FOOTER

    /**
     * Height of everything above the first transaction row on page 1. The category table
     * always takes at least one row (its "no transactions" line when empty).
     */
    fun firstPageHeaderHeight(categoryCount: Int): Float =
        TITLE_BLOCK + SUMMARY_BLOCK +
            SECTION_TITLE + TABLE_HEADER + max(categoryCount, 1) * ROW + SECTION_GAP +
            SECTION_TITLE + TABLE_HEADER

    /** Height of everything above the first transaction row on pages 2+. */
    const val OTHER_PAGE_HEADER = CONTINUATION_HEADER + TABLE_HEADER

    fun rowsThatFit(headerHeight: Float): Int = ((BODY_BOTTOM - MARGIN - headerHeight) / ROW).toInt().coerceAtLeast(0)

    /** Transaction index ranges, one per page, for a report with these sizes. */
    fun pages(categoryCount: Int, transactionCount: Int): List<IntRange> = paginate(
        count = transactionCount,
        firstCapacity = rowsThatFit(firstPageHeaderHeight(categoryCount)),
        otherCapacity = rowsThatFit(OTHER_PAGE_HEADER),
    )

    /**
     * Splits [count] rows into pages: up to [firstCapacity] on page 1, then [otherCapacity]
     * per page. There is always at least one page, even for zero rows.
     */
    fun paginate(count: Int, firstCapacity: Int, otherCapacity: Int): List<IntRange> {
        require(otherCapacity > 0) { "Pages after the first must fit at least one row" }
        val pages = mutableListOf(0 until minOf(count, firstCapacity.coerceAtLeast(0)))
        var next = pages.first().last + 1
        while (next < count) {
            val end = minOf(count, next + otherCapacity)
            pages += next until end
            next = end
        }
        return pages
    }
}
