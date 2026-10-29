package com.hanan.strukku.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptRowBuilderTest {

    private fun fragment(text: String, left: Int, top: Int, height: Int = 20, width: Int = 100) =
        TextFragment(text, left, top, left + width, top + height)

    @Test
    fun `label and amount returned as separate column blocks are joined into one row`() {
        // OCR order: left column first, then right column.
        val fragments = listOf(
            fragment("SUBTOTAL", left = 10, top = 100),
            fragment("TOTAL", left = 10, top = 130),
            fragment("TUNAI", left = 10, top = 160),
            fragment("86.000", left = 300, top = 101),
            fragment("99.330", left = 300, top = 131),
            fragment("100.000", left = 300, top = 159),
        )

        assertEquals(
            listOf("SUBTOTAL 86.000", "TOTAL 99.330", "TUNAI 100.000"),
            ReceiptRowBuilder.buildRows(fragments),
        )
    }

    @Test
    fun `fragments in a row are ordered left to right`() {
        val fragments = listOf(
            fragment("3.500", left = 300, top = 50),
            fragment("AQUA", left = 10, top = 50),
            fragment("2", left = 200, top = 52),
        )

        assertEquals(listOf("AQUA 2 3.500"), ReceiptRowBuilder.buildRows(fragments))
    }

    @Test
    fun `a slightly tilted photo still keeps rows apart`() {
        val fragments = listOf(
            fragment("TOTAL", left = 10, top = 100),
            fragment("50.000", left = 300, top = 106), // drifts down to the right
            fragment("TUNAI", left = 10, top = 125),
            fragment("100.000", left = 300, top = 131),
        )

        assertEquals(listOf("TOTAL 50.000", "TUNAI 100.000"), ReceiptRowBuilder.buildRows(fragments))
    }

    @Test
    fun `blank fragments are dropped and text is trimmed`() {
        val fragments = listOf(
            fragment("  INDOMARET ", left = 10, top = 10),
            fragment("   ", left = 200, top = 10),
        )

        assertEquals(listOf("INDOMARET"), ReceiptRowBuilder.buildRows(fragments))
    }

    @Test
    fun `no fragments gives no rows`() {
        assertEquals(emptyList<String>(), ReceiptRowBuilder.buildRows(emptyList()))
    }
}
