package com.hanan.mysavings.ui.components

/**
 * The numbers behind the charts, kept apart from the Canvas drawing code so they can be
 * unit tested without rendering anything.
 */
object ChartGeometry {

    /** Bars for non-zero values never shrink below this share of the height, so they stay visible and tappable. */
    const val MIN_VISIBLE_BAR_FRACTION = 0.02f

    /** Gap between donut slices, in degrees. */
    const val DONUT_GAP_DEGREES = 2f

    /** Height of each bar as a fraction (0..1) of the tallest bar. All zero when there is no data. */
    fun barFractions(values: List<Long>): List<Float> {
        val max = values.maxOrNull()?.takeIf { it > 0 } ?: return values.map { 0f }
        return values.map { value ->
            if (value <= 0) 0f else (value.toFloat() / max).coerceAtLeast(MIN_VISIBLE_BAR_FRACTION)
        }
    }

    data class Slice(val startAngle: Float, val sweepAngle: Float)

    /**
     * Donut slices proportional to [values], starting at 12 o'clock (-90°) and going clockwise.
     * With more than one slice a small gap separates neighbours; a single slice is a full ring.
     */
    fun donutSlices(values: List<Long>, gapDegrees: Float = DONUT_GAP_DEGREES): List<Slice> {
        val total = values.filter { it > 0 }.sum()
        if (total <= 0) return emptyList()

        val visible = values.count { it > 0 }
        val gap = if (visible > 1) gapDegrees else 0f
        var angle = -90f
        return values.map { value ->
            val share = 360f * value.coerceAtLeast(0) / total
            val sweep = (share - gap).coerceAtLeast(0f)
            Slice(startAngle = angle + gap / 2, sweepAngle = sweep).also { angle += share }
        }
    }

    /** Which of [count] equal-width columns a tap at [x] (0..[width]) falls in. */
    fun columnAt(x: Float, width: Float, count: Int): Int? {
        if (count <= 0 || width <= 0f || x < 0f || x > width) return null
        return (x / (width / count)).toInt().coerceAtMost(count - 1)
    }
}
