package com.hanan.strukku.ui.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartGeometryTest {

    private val delta = 0.001f

    @Test
    fun `bars are scaled relative to the tallest`() {
        assertEquals(listOf(0.5f, 1f, 0.25f), ChartGeometry.barFractions(listOf(200, 400, 100)))
    }

    @Test
    fun `no spending gives flat bars instead of dividing by zero`() {
        assertEquals(listOf(0f, 0f, 0f), ChartGeometry.barFractions(listOf(0, 0, 0)))
        assertEquals(emptyList<Float>(), ChartGeometry.barFractions(emptyList()))
    }

    @Test
    fun `tiny non-zero values stay visible, zero stays zero`() {
        val fractions = ChartGeometry.barFractions(listOf(1, 1_000_000, 0))

        assertEquals(ChartGeometry.MIN_VISIBLE_BAR_FRACTION, fractions[0], delta)
        assertEquals(0f, fractions[2], delta)
    }

    @Test
    fun `donut slices are proportional, start at 12 o'clock and leave gaps`() {
        val slices = ChartGeometry.donutSlices(listOf(300, 100), gapDegrees = 2f)

        // 75% and 25% of the circle, each minus the 2° gap, centred in its share.
        assertEquals(-89f, slices[0].startAngle, delta)
        assertEquals(268f, slices[0].sweepAngle, delta)
        assertEquals(181f, slices[1].startAngle, delta)
        assertEquals(88f, slices[1].sweepAngle, delta)
    }

    @Test
    fun `slices plus gaps add up to a full circle`() {
        val slices = ChartGeometry.donutSlices(listOf(5, 3, 2, 7), gapDegrees = 2f)

        assertEquals(360f, slices.sumOf { it.sweepAngle.toDouble() }.toFloat() + 4 * 2f, delta)
    }

    @Test
    fun `a single category is a full ring without a gap`() {
        val slice = ChartGeometry.donutSlices(listOf(500)).single()

        assertEquals(-90f, slice.startAngle, delta)
        assertEquals(360f, slice.sweepAngle, delta)
    }

    @Test
    fun `no data gives no slices`() {
        assertTrue(ChartGeometry.donutSlices(listOf(0, 0)).isEmpty())
    }

    @Test
    fun `tap position maps to the right column`() {
        assertEquals(0, ChartGeometry.columnAt(x = 10f, width = 600f, count = 6))
        assertEquals(2, ChartGeometry.columnAt(x = 250f, width = 600f, count = 6))
        assertEquals(5, ChartGeometry.columnAt(x = 600f, width = 600f, count = 6)) // right edge
        assertNull(ChartGeometry.columnAt(x = -1f, width = 600f, count = 6))
        assertNull(ChartGeometry.columnAt(x = 10f, width = 600f, count = 0))
    }
}
