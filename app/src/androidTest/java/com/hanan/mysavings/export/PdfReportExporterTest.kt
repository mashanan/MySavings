package com.hanan.mysavings.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hanan.mysavings.domain.model.Category
import com.hanan.mysavings.domain.model.CategorySpending
import com.hanan.mysavings.domain.model.MonthlyReport
import com.hanan.mysavings.domain.model.Transaction
import com.hanan.mysavings.domain.model.TransactionSource
import com.hanan.mysavings.domain.model.TransactionWithCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Writes real PDFs and reads them back with Android's own PdfRenderer: the file must be a
 * valid PDF, have the page count the layout predicts, and every page must render.
 */
@RunWith(AndroidJUnit4::class)
class PdfReportExporterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = Clock.fixed(Instant.parse("2026-10-29T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private val exporter = PdfReportExporter(context, Dispatchers.IO, clock)

    private val categories = listOf(
        Category(2, "Belanja Harian", "groceries", "#4DB6AC", emptyList(), true),
        Category(1, "Makan & Minum", "food", "#E57373", emptyList(), true),
        Category(3, "Transportasi", "transport", "#64B5F6", emptyList(), true),
    )

    @Test
    fun longMonthSpansSeveralPagesThatAllRender() = runTest {
        val october = YearMonth.of(2026, 10)
        val transactions = (1..60).map { i ->
            val category = categories[i % categories.size]
            TransactionWithCategory(
                Transaction(
                    id = i.toLong(),
                    amount = 10_000L + i * 1_250L,
                    date = october.atDay(1 + (i - 1) / 2),
                    // Every 7th name is far too long for its column, to exercise the "…" truncation.
                    merchant = if (i % 7 == 0) "Toko Serba Ada Sumber Rejeki Makmur Jaya Abadi Sentosa Cabang $i" else "Toko $i",
                    note = null,
                    categoryId = category.id,
                    source = TransactionSource.MANUAL,
                    createdAt = Instant.EPOCH,
                ),
                category,
            )
        }
        val report = MonthlyReport(
            yearMonth = october,
            totalSpent = transactions.sumOf { it.transaction.amount },
            budgetLimit = 2_000_000,
            spendingByCategory = categories.map { c ->
                CategorySpending(c, transactions.filter { it.category == c }.sumOf { it.transaction.amount }, null)
            },
            transactions = transactions,
        )

        val file = exporter.export(report)

        assertEquals("MySavings-Report-2026-10.pdf", file.name)
        val expectedPages = ReportLayout.pages(categoryCount = 3, transactionCount = 60).size
        assertTrue("60 transactions should need more than one page", expectedPages > 1)
        assertEquals(expectedPages, renderAllPages(file))
    }

    @Test
    fun emptyMonthIsASinglePage() = runTest {
        val report = MonthlyReport(YearMonth.of(2026, 9), 0, null, emptyList(), emptyList())

        assertEquals(1, renderAllPages(exporter.export(report)))
    }

    /** Opens the PDF the way a viewer would and draws every page; returns the page count. */
    private fun renderAllPages(file: File): Int =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                repeat(renderer.pageCount) { index ->
                    renderer.openPage(index).use { page ->
                        assertEquals(ReportLayout.PAGE_WIDTH, page.width)
                        assertEquals(ReportLayout.PAGE_HEIGHT, page.height)
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap.recycle()
                    }
                }
                renderer.pageCount
            }
        }
}
