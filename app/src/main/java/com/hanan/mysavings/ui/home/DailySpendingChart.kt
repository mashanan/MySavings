package com.hanan.mysavings.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.usecase.DayTotal
import com.hanan.mysavings.ui.components.ChartGeometry
import com.hanan.mysavings.ui.util.currentLocale
import com.hanan.mysavings.ui.util.formatDayOfWeekShort
import com.hanan.mysavings.ui.util.formatRupiah
import com.hanan.mysavings.ui.util.formatRupiahShort
import com.hanan.mysavings.ui.util.formatShortDate
import java.time.LocalDate

/**
 * Seven days of spending as bars. Tapping a bar (or its weekday label) shows that day's
 * total above the chart; the last day is shown until then. As in the Stats chart, the Canvas
 * carries a text summary for screen readers and the labels are the focusable day buttons.
 */
@Composable
fun DailySpendingChart(
    days: List<DayTotal>,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    // Keyed on the last day, so moving to another month or a new day resets the selection.
    var selectedDate by rememberSaveable(days.lastOrNull()?.date) { mutableStateOf(days.lastOrNull()?.date) }
    val selected = days.firstOrNull { it.date == selectedDate } ?: days.lastOrNull() ?: return

    val selectedColor = MaterialTheme.colorScheme.primary
    val otherColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val baselineColor = MaterialTheme.colorScheme.outlineVariant
    val fractions = ChartGeometry.barFractions(days.map { it.total })
    val locale = currentLocale()
    val description = stringResource(
        R.string.home_chart_desc,
        days.joinToString { "${formatDayOfWeekShort(it.date, locale)} ${formatRupiahShort(it.total, locale)}" },
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                // Announced when a bar is tapped, so the total is heard without moving focus.
                modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${formatDayOfWeekShort(selected.date, locale)}, ${formatShortDate(selected.date, locale)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatRupiah(selected.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .pointerInput(days) {
                        detectTapGestures { offset ->
                            ChartGeometry.columnAt(offset.x, size.width.toFloat(), days.size)
                                ?.let { selectedDate = days[it].date }
                        }
                    }
                    .semantics { contentDescription = description },
            ) {
                val columnWidth = size.width / days.size
                val barWidth = columnWidth * 0.5f
                val radius = CornerRadius(6.dp.toPx())
                fractions.forEachIndexed { index, fraction ->
                    val barHeight = fraction * size.height
                    drawRoundRect(
                        color = if (days[index].date == selected.date) selectedColor else otherColor,
                        topLeft = Offset(index * columnWidth + (columnWidth - barWidth) / 2, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = radius,
                    )
                }
                drawLine(baselineColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            }

            Row(Modifier.fillMaxWidth()) {
                days.forEach { day ->
                    val isSelected = day.date == selected.date
                    Text(
                        text = formatDayOfWeekShort(day.date, locale),
                        style = MaterialTheme.typography.labelMedium,
                        // Today stands out even when another day is selected.
                        fontWeight = if (isSelected || day.date == today) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(role = Role.Tab) { selectedDate = day.date }
                            .semantics { this.selected = isSelected }
                            .padding(vertical = 12.dp), // keeps the touch target at least 48dp tall
                    )
                }
            }
        }
    }
}
