package com.hanan.mysavings.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.hanan.mysavings.R
import com.hanan.mysavings.data.di.IoDispatcher
import com.hanan.mysavings.domain.model.MonthlyReport
import com.hanan.mysavings.domain.model.TransactionWithCategory
import com.hanan.mysavings.domain.repository.ReportExporter
import com.hanan.mysavings.export.ReportLayout.MARGIN
import com.hanan.mysavings.export.ReportLayout.PAGE_HEIGHT
import com.hanan.mysavings.export.ReportLayout.PAGE_WIDTH
import com.hanan.mysavings.export.ReportLayout.ROW
import com.hanan.mysavings.ui.util.formatMonthYear
import com.hanan.mysavings.ui.util.formatRupiah
import com.hanan.mysavings.ui.util.formatShortDate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Writes a [MonthlyReport] as an A4 PDF with Android's built-in [PdfDocument]: no library,
 * no internet. Each draw step advances by the same heights [ReportLayout] uses for
 * pagination, so the page breaks computed up front match what is drawn.
 */
class PdfReportExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val clock: Clock,
) : ReportExporter {

    override suspend fun export(report: MonthlyReport): File = withContext(ioDispatcher) {
        // Cache, not app files: the PDF only exists to be shared, and Android may clear it.
        val dir = File(context.cacheDir, REPORTS_DIR).apply { mkdirs() }
        val file = File(dir, "MySavings-Report-${report.yearMonth}.pdf")

        val document = PdfDocument()
        try {
            val pages = ReportLayout.pages(report.spendingByCategory.size, report.transactions.size)
            pages.forEachIndexed { index, rows ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                val page = document.startPage(pageInfo)
                page.canvas.drawPage(report, index, pages.size, rows)
                document.finishPage(page)
            }
            file.outputStream().use(document::writeTo)
        } finally {
            document.close()
        }
        file
    }

    private fun Canvas.drawPage(report: MonthlyReport, pageIndex: Int, pageCount: Int, rows: IntRange) {
        var y = MARGIN
        if (pageIndex == 0) {
            y = drawTitle(report, y)
            y = drawSummary(report, y)
            y = drawCategoryTable(report, y)
            val count = report.transactions.size
            y = drawSectionTitle(context.resources.getQuantityString(R.plurals.report_transactions, count, count), y)
        } else {
            drawText(context.getString(R.string.report_continued, formatMonthYear(report.yearMonth)), MARGIN, y + 20, heading)
            y += ReportLayout.CONTINUATION_HEADER
        }
        y = drawTransactionHeader(y)
        if (report.transactions.isEmpty()) {
            drawText(context.getString(R.string.report_no_transactions), MARGIN, y + BASELINE, muted)
        }
        for (index in rows) {
            drawTransactionRow(report.transactions[index], y)
            y += ROW
        }
        drawFooter(pageIndex + 1, pageCount)
    }

    private fun Canvas.drawTitle(report: MonthlyReport, top: Float): Float {
        drawText(context.getString(R.string.report_title), MARGIN, top + 20, title)
        drawText(formatMonthYear(report.yearMonth), MARGIN, top + 40, heading)
        drawText(
            context.getString(R.string.report_generated, formatShortDate(LocalDate.now(clock))),
            MARGIN,
            top + 56,
            muted,
        )
        return top + ReportLayout.TITLE_BLOCK
    }

    private fun Canvas.drawSummary(report: MonthlyReport, top: Float): Float {
        val valueX = MARGIN + 150
        val remaining = report.remaining
        val lines = listOf(
            context.getString(R.string.report_total) to formatRupiah(report.totalSpent),
            context.getString(R.string.report_budget) to
                (report.budgetLimit?.let(::formatRupiah) ?: context.getString(R.string.report_budget_none)),
            when {
                remaining == null -> context.getString(R.string.report_remaining) to "-"
                remaining < 0 -> context.getString(R.string.report_over) to formatRupiah(-remaining)
                else -> context.getString(R.string.report_remaining) to formatRupiah(remaining)
            },
        )
        lines.forEachIndexed { i, (label, value) ->
            val baseline = top + i * ROW + BASELINE
            drawText(label, MARGIN, baseline, body)
            drawText(value, valueX, baseline, bodyBold)
        }
        return top + ReportLayout.SUMMARY_BLOCK
    }

    private fun Canvas.drawCategoryTable(report: MonthlyReport, top: Float): Float {
        var y = drawSectionTitle(context.getString(R.string.report_by_category), top)

        val amountRight = PAGE_WIDTH - MARGIN - 60
        val shareRight = PAGE_WIDTH - MARGIN
        drawText(context.getString(R.string.report_col_category), MARGIN, y + 14, headerText)
        drawText(context.getString(R.string.report_col_amount), amountRight, y + 14, headerTextRight)
        drawText(context.getString(R.string.report_col_share), shareRight, y + 14, headerTextRight)
        drawHeaderRule(y)
        y += ReportLayout.TABLE_HEADER

        if (report.spendingByCategory.isEmpty()) {
            drawText(context.getString(R.string.report_no_transactions), MARGIN, y + BASELINE, muted)
            y += ROW
        }
        report.spendingByCategory.forEach { spending ->
            val share = if (report.totalSpent > 0) (spending.amount * 100.0 / report.totalSpent).roundToInt() else 0
            drawText(body.fit(spending.category.name, amountRight - MARGIN - 100), MARGIN, y + BASELINE, body)
            drawText(formatRupiah(spending.amount), amountRight, y + BASELINE, bodyRight)
            drawText("$share%", shareRight, y + BASELINE, bodyRight)
            y += ROW
        }
        return y + ReportLayout.SECTION_GAP
    }

    private fun Canvas.drawSectionTitle(text: String, top: Float): Float {
        drawText(text, MARGIN, top + 18, heading)
        return top + ReportLayout.SECTION_TITLE
    }

    private fun Canvas.drawTransactionHeader(top: Float): Float {
        drawText(context.getString(R.string.report_col_date), COL_DATE, top + 14, headerText)
        drawText(context.getString(R.string.report_col_merchant), COL_MERCHANT, top + 14, headerText)
        drawText(context.getString(R.string.report_col_category), COL_CATEGORY, top + 14, headerText)
        drawText(context.getString(R.string.report_col_amount), COL_AMOUNT_RIGHT, top + 14, headerTextRight)
        drawHeaderRule(top)
        return top + ReportLayout.TABLE_HEADER
    }

    private fun Canvas.drawTransactionRow(item: TransactionWithCategory, top: Float) {
        val baseline = top + BASELINE
        val transaction = item.transaction
        val description = transaction.merchant ?: transaction.note ?: "-"
        drawText(formatShortDate(transaction.date), COL_DATE, baseline, body)
        drawText(body.fit(description, COL_CATEGORY - COL_MERCHANT - 8), COL_MERCHANT, baseline, body)
        drawText(body.fit(item.category.name, COL_AMOUNT_RIGHT - COL_CATEGORY - 80), COL_CATEGORY, baseline, body)
        drawText(formatRupiah(transaction.amount), COL_AMOUNT_RIGHT, baseline, bodyRight)
    }

    private fun Canvas.drawHeaderRule(top: Float) {
        val lineY = top + ReportLayout.TABLE_HEADER - 4
        drawLine(MARGIN, lineY, PAGE_WIDTH - MARGIN, lineY, rule)
    }

    private fun Canvas.drawFooter(page: Int, pageCount: Int) {
        val baseline = PAGE_HEIGHT - MARGIN
        drawText(context.getString(R.string.app_name), MARGIN, baseline, muted)
        drawText(context.getString(R.string.report_page, page, pageCount), PAGE_WIDTH - MARGIN, baseline, mutedRight)
    }

    /** Shortens [text] with "…" so it never spills into the next column. */
    private fun Paint.fit(text: String, maxWidth: Float): String {
        if (measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        val chars = breakText(text, true, maxWidth - measureText(ellipsis), null)
        return text.take(chars).trimEnd() + ellipsis
    }

    // Paints are created once per exporter; PdfDocument canvases are drawn on one thread.
    private val title = paint(18f, bold = true, color = BRAND_GREEN)
    private val heading = paint(12f, bold = true)
    private val body = paint(10f)
    private val bodyBold = paint(10f, bold = true)
    private val bodyRight = paint(10f, align = Paint.Align.RIGHT)
    private val headerText = paint(9f, bold = true, color = MUTED)
    private val headerTextRight = paint(9f, bold = true, color = MUTED, align = Paint.Align.RIGHT)
    private val muted = paint(9f, color = MUTED)
    private val mutedRight = paint(9f, color = MUTED, align = Paint.Align.RIGHT)
    private val rule = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }

    private fun paint(size: Float, bold: Boolean = false, color: Int = Color.BLACK, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            this.color = color
            textAlign = align
        }

    private companion object {
        const val REPORTS_DIR = "reports"

        /** Text baseline inside a [ROW]-high row. */
        const val BASELINE = 13f

        const val COL_DATE = MARGIN
        const val COL_MERCHANT = MARGIN + 75
        const val COL_CATEGORY = MARGIN + 315
        const val COL_AMOUNT_RIGHT = PAGE_WIDTH - MARGIN

        val BRAND_GREEN = Color.rgb(0x1B, 0x6B, 0x4A)
        val MUTED = Color.rgb(0x66, 0x66, 0x66)
    }
}
