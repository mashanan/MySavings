package com.hanan.mysavings.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.model.MonthInsights
import com.hanan.mysavings.domain.model.PeriodComparison
import com.hanan.mysavings.ui.components.displayName
import com.hanan.mysavings.ui.util.formatRupiah
import kotlin.math.absoluteValue

/** Placeholder for a stat that has no value yet; a symbol, so it needs no translation. */
private const val NO_VALUE = "–"

/** Three small tiles: how many transactions, the daily average and the biggest category. */
@Composable
fun MonthStatsRow(
    insights: MonthInsights,
    modifier: Modifier = Modifier,
) {
    // IntrinsicSize.Min gives all tiles the height of the tallest one, e.g. with a large font.
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatTile(
            label = stringResource(R.string.home_stat_transactions),
            value = insights.transactionCount.toString(),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = stringResource(R.string.home_stat_average),
            value = insights.averagePerDay?.let { formatRupiah(it) } ?: NO_VALUE,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = stringResource(R.string.home_stat_top_category),
            value = insights.topCategory?.category?.displayName() ?: NO_VALUE,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        // One TalkBack stop per tile: "Transaksi, 12".
        modifier = modifier.fillMaxHeight().semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Spending so far against the same days of last month. */
@Composable
fun ComparisonCard(
    comparison: PeriodComparison,
    modifier: Modifier = Modifier,
) {
    val percent = comparison.changePercent
    val (message, color) = when {
        percent < 0 -> stringResource(R.string.home_compare_less, percent.absoluteValue) to MaterialTheme.colorScheme.primary
        percent > 0 -> stringResource(R.string.home_compare_more, percent) to MaterialTheme.colorScheme.tertiary
        else -> stringResource(R.string.home_compare_same) to Color.Unspecified
    }

    Card(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.home_compare_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = message, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            Text(
                text = stringResource(R.string.home_compare_previous, formatRupiah(comparison.previousTotal)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
