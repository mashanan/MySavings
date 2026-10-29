package com.hanan.mysavings.domain.parser

import kotlin.math.abs
import kotlin.math.min

/** A piece of recognized text and its position in the (upright) photo, in pixels. */
data class TextFragment(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val centerY: Int get() = (top + bottom) / 2
    val height: Int get() = bottom - top
}

/**
 * Rebuilds the printed rows of a receipt from OCR fragments.
 *
 * Receipts are laid out in columns ("TOTAL" on the left, "87.500" on the right), and OCR
 * engines often return each column as a separate block. Reading blocks in order would put
 * every label first and every amount after, so the parser could never tell which amount
 * belongs to "TOTAL". Grouping fragments whose vertical centers line up restores the rows.
 */
object ReceiptRowBuilder {

    /** Two fragments share a row when their centers are within this share of the smaller height. */
    private const val SAME_ROW_TOLERANCE = 0.5

    fun buildRows(fragments: List<TextFragment>): List<String> {
        val rows = mutableListOf<MutableList<TextFragment>>()
        for (fragment in fragments.filter { it.text.isNotBlank() }.sortedBy { it.centerY }) {
            val row = rows.lastOrNull()
            if (row != null && row.belongsTogetherWith(fragment)) row += fragment else rows += mutableListOf(fragment)
        }
        return rows.map { row -> row.sortedBy { it.left }.joinToString(" ") { it.text.trim() } }
    }

    private fun List<TextFragment>.belongsTogetherWith(fragment: TextFragment): Boolean {
        val rowCenter = map { it.centerY }.average()
        val tolerance = min(fragment.height, minOf { it.height }) * SAME_ROW_TOLERANCE
        return abs(fragment.centerY - rowCenter) <= tolerance
    }
}
