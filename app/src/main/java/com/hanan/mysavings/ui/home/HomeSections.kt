package com.hanan.mysavings.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.usecase.TransactionDay
import com.hanan.mysavings.ui.util.currentLocale
import com.hanan.mysavings.ui.util.formatDayHeader
import com.hanan.mysavings.ui.util.formatMonthYear
import com.hanan.mysavings.ui.util.formatRupiah
import java.time.LocalDate
import java.time.YearMonth

/**
 * The two ways to add a transaction, one tap away. The FAB does the same but stays on screen
 * while scrolling; these cards make the options visible without opening a sheet first.
 */
@Composable
fun QuickActionsRow(
    onScanClick: () -> Unit,
    onManualEntryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // IntrinsicSize.Min keeps both cards equally tall when one label wraps.
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickActionCard(
            icon = R.drawable.ic_category_bills,
            label = stringResource(R.string.action_scan_receipt),
            onClick = onScanClick,
            modifier = Modifier.weight(1f),
        )
        QuickActionCard(
            icon = R.drawable.ic_add,
            label = stringResource(R.string.action_manual_entry),
            onClick = onManualEntryClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickActionCard(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The Card's onClick overload gives the button role, ripple and a 48dp touch target.
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Decorative: the label already says what the button does.
            Icon(painterResource(icon), contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** "Today", "Yesterday" or "Tue, 27 Oct", with the day's total on the right. */
@Composable
fun DayHeader(
    day: TransactionDay,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val label = when (day.date) {
        today -> stringResource(R.string.home_day_today)
        today.minusDays(1) -> stringResource(R.string.home_day_yesterday)
        else -> formatDayHeader(day.date, currentLocale())
    }
    Row(
        // A heading per day lets screen-reader users jump from day to day.
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatRupiah(day.total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Shown instead of the category and transaction sections when the month has no transactions.
 * The current month invites the first entry; a past month just says it was empty.
 */
@Composable
fun EmptyMonthCard(
    month: YearMonth,
    isCurrentMonth: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_category_bills),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp),
                )
            }
            Text(
                text = if (isCurrentMonth) {
                    stringResource(R.string.home_empty_current_title)
                } else {
                    stringResource(R.string.home_empty_past_title, formatMonthYear(month, currentLocale()))
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (isCurrentMonth) {
                Text(
                    text = stringResource(R.string.home_empty_current_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
