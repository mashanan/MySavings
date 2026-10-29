package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.Category
import com.hanan.mysavings.domain.model.ReceiptField
import com.hanan.mysavings.domain.parser.TextFragment
import com.hanan.mysavings.domain.repository.CategoryRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ParseReceiptUseCaseTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-29T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private val groceries = Category(2, "Belanja Harian", "groceries", "#4DB6AC", listOf("indomaret"), true)
    private val other = Category(7, "Lainnya", Category.OTHER_ICON_KEY, "#90A4AE", emptyList(), true)

    private val categoryRepository = mockk<CategoryRepository> {
        every { observeAll() } returns flowOf(listOf(groceries, other))
    }
    private val parseReceipt = ParseReceiptUseCase(categoryRepository, clock)

    /** Lays rows out top to bottom, label on the left and amount on the right, like real OCR output. */
    private fun fragments(vararg rows: Pair<String, String?>): List<TextFragment> =
        rows.flatMapIndexed { index, (label, amount) ->
            val top = index * 40
            listOfNotNull(
                TextFragment(label, 10, top, 200, top + 20),
                amount?.let { TextFragment(it, 400, top + 1, 500, top + 21) },
            )
        }

    @Test
    fun `complete receipt fills every field and needs no checks`() = runTest {
        val draft = parseReceipt(
            fragments(
                "INDOMARET" to null,
                "12.10.2026 14:32" to null,
                "TOTAL" to "22.500",
                "TUNAI" to "50.000",
            ),
        )

        assertEquals("Indomaret", draft.merchant)
        assertEquals(22_500L, draft.total)
        assertEquals(LocalDate.of(2026, 10, 12), draft.date)
        assertEquals(groceries, draft.category)
        assertEquals(emptySet<ReceiptField>(), draft.missingFields)
    }

    @Test
    fun `unknown store falls back to Lainnya and flags the guessed category`() = runTest {
        val draft = parseReceipt(fragments("TOKO MAKMUR" to null, "TOTAL" to "10.000"))

        assertEquals(other, draft.category)
        assertEquals(setOf(ReceiptField.DATE, ReceiptField.CATEGORY), draft.missingFields)
    }

    @Test
    fun `no text at all gives an empty draft`() = runTest {
        val draft = parseReceipt(emptyList())

        assertTrue(draft.isEmpty)
        assertEquals(ReceiptField.entries.toSet(), draft.missingFields)
    }
}
