package com.hanan.mysavings.ui.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hanan.mysavings.R
import com.hanan.mysavings.ui.util.currentLocale
import com.hanan.mysavings.ui.util.formatShortDate
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/**
 * A month grid with a dot under each day that has a payment due. The week starts on the
 * locale's first day (Monday in Indonesia, Sunday in the US). Tapping a day selects it.
 */
@Composable
fun DebtCalendar(
    month: YearMonth,
    today: LocalDate,
    dots: Map<LocalDate, DueDot>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val weeks = CalendarLogic.monthCells(month, firstDayOfWeek).chunked(7)

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            CalendarLogic.weekdays(firstDayOfWeek).forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    // The day buttons below already say the full date.
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp).clearAndSetSemantics {},
                )
            }
        }
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                dot = dots[date],
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                onClick = { onSelectDate(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    dot: DueDot?,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val dateText = formatShortDate(date, currentLocale())
    val description = if (dot == null) dateText else stringResource(R.string.debt_calendar_day_has_payment, dateText)

    Column(
        modifier = Modifier
            .padding(2.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(if (isSelected) colors.primaryContainer else Color.Transparent, CircleShape)
            .then(if (isToday) Modifier.border(1.dp, colors.primary, CircleShape) else Modifier)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
                selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday || dot != null) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
            modifier = Modifier.clearAndSetSemantics {},
        )
        // Always reserve the dot's space, so days with and without one line up.
        Box(
            Modifier
                .padding(top = 2.dp)
                .size(6.dp)
                .background(dot?.let { dotColor(it) } ?: Color.Transparent, CircleShape),
        )
    }
}

@Composable
fun dotColor(dot: DueDot): Color = when (dot) {
    DueDot.OVERDUE -> MaterialTheme.colorScheme.error
    DueDot.SOON -> MaterialTheme.colorScheme.tertiary
    DueDot.UPCOMING -> MaterialTheme.colorScheme.primary
    DueDot.PAID -> MaterialTheme.colorScheme.outline
}
