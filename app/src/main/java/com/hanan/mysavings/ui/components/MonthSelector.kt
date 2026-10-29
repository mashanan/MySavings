package com.hanan.mysavings.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.hanan.mysavings.R
import com.hanan.mysavings.ui.util.currentLocale
import com.hanan.mysavings.ui.util.formatMonthYear
import java.time.YearMonth

/** "‹ October 2026 ›". Shared by Home and Stats; [canGoNext] is false on the current month. */
@Composable
fun MonthSelector(
    month: YearMonth,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.month_previous))
        }
        Text(
            text = formatMonthYear(month, currentLocale()),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                // Announce the new month when it changes via the arrows or a chart.
                .semantics { liveRegion = LiveRegionMode.Polite },
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.month_next))
        }
    }
}
