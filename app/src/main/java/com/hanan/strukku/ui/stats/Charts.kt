package com.hanan.strukku.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hanan.strukku.domain.usecase.MonthTotal
import com.hanan.strukku.ui.util.formatMonthShort
import java.time.YearMonth

/**
 * Six-month spending bars drawn on a Canvas. Tapping a bar (or its month label) selects
 * that month. The Canvas itself is invisible to screen readers, so it carries a text
 * summary, and the labels below act as the accessible, focusable month buttons.
 */
@Composable
fun TrendBarChart(
    trend: List<MonthTotal>,
    selectedMonth: YearMonth,
    onSelectMonth: (YearMonth) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val otherColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val baselineColor = MaterialTheme.colorScheme.outlineVariant
    val fractions = ChartGeometry.barFractions(trend.map { it.total })

    Column(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .pointerInput(trend) {
                    detectTapGestures { offset ->
                        ChartGeometry.columnAt(offset.x, size.width.toFloat(), trend.size)
                            ?.let { onSelectMonth(trend[it].yearMonth) }
                    }
                }
                .semantics { contentDescription = description },
        ) {
            val columnWidth = size.width / trend.size
            val barWidth = columnWidth * 0.5f
            val radius = CornerRadius(6.dp.toPx())
            fractions.forEachIndexed { index, fraction ->
                val barHeight = fraction * size.height
                drawRoundRect(
                    color = if (trend[index].yearMonth == selectedMonth) selectedColor else otherColor,
                    topLeft = Offset(index * columnWidth + (columnWidth - barWidth) / 2, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = radius,
                )
            }
            drawLine(baselineColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
        }

        Row(Modifier.fillMaxWidth()) {
            trend.forEach { month ->
                val isSelected = month.yearMonth == selectedMonth
                Text(
                    text = formatMonthShort(month.yearMonth),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(role = Role.Tab) { onSelectMonth(month.yearMonth) }
                        .semantics { selected = isSelected }
                        .padding(vertical = 12.dp), // keeps the touch target at least 48dp tall
                )
            }
        }
    }
}

/** One coloured slice of the category donut. */
data class DonutSlice(val color: Color, val value: Long)

/** Category share as a donut, with [centerText] (e.g. the month total) in the hole. */
@Composable
fun CategoryDonutChart(
    slices: List<DonutSlice>,
    centerText: String,
    description: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 180.dp,
    thickness: Dp = 28.dp,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val geometry = ChartGeometry.donutSlices(slices.map { it.value })

    Box(modifier.size(diameter).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val stroke = thickness.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            // Empty ring when there is no data, so the layout doesn't jump.
            if (geometry.isEmpty()) {
                drawArc(trackColor, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            }
            geometry.forEachIndexed { index, slice ->
                drawArc(
                    color = slices[index].color,
                    startAngle = slice.startAngle,
                    sweepAngle = slice.sweepAngle,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(stroke),
                )
            }
        }
        Text(
            text = centerText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = thickness + 8.dp),
        )
    }
}
