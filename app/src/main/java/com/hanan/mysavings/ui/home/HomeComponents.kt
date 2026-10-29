package com.hanan.mysavings.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.model.CategorySpending
import com.hanan.mysavings.domain.model.DailyAllowance
import com.hanan.mysavings.domain.model.MonthlySummary
import com.hanan.mysavings.domain.model.TransactionWithCategory
import com.hanan.mysavings.ui.components.CategoryIcon
import com.hanan.mysavings.ui.components.categoryColor
import com.hanan.mysavings.ui.util.formatRupiah
import com.hanan.mysavings.ui.components.displayName
import kotlin.math.roundToInt

/** Usage above this share of the budget is shown as a warning. */
private const val WARNING_FRACTION = 0.8f

@Composable
fun BudgetCard(
    summary: MonthlySummary,
    allowance: DailyAllowance?,
    onEditBudget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val limit = summary.budgetLimit
    val remaining = summary.remaining
    val usedFraction = summary.usedFraction

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (limit == null || remaining == null || usedFraction == null) {
                Text(stringResource(R.string.home_budget_empty_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.home_budget_empty_body, formatRupiah(summary.totalSpent)))
                Button(onClick = onEditBudget) { Text(stringResource(R.string.home_budget_set)) }
                return@Column
            }

            Text(
                text = stringResource(R.string.home_budget_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() },
            )
            val statusColor = budgetStatusColor(usedFraction)
            val percentUsed = (usedFraction * 100).roundToInt()
            val progressDescription = stringResource(R.string.home_budget_progress_desc, percentUsed)
            Text(
                text = formatRupiah(remaining),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (remaining < 0) statusColor else MaterialTheme.colorScheme.onPrimaryContainer,
            )
            LinearProgressIndicator(
                progress = { usedFraction.coerceIn(0f, 1f) },
                color = statusColor,
                // Read as "82% anggaran terpakai" instead of a bare percentage; over 100% is otherwise lost.
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .semantics { stateDescription = progressDescription },
            )
            Text(
                text = if (remaining < 0) {
                    stringResource(R.string.home_budget_over, formatRupiah(-remaining))
                } else {
                    stringResource(R.string.home_budget_used, formatRupiah(summary.totalSpent), formatRupiah(limit))
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (allowance != null) {
                Text(
                    text = if (allowance.daysLeft == 1) {
                        stringResource(R.string.home_daily_allowance_today, formatRupiah(allowance.amountPerDay))
                    } else {
                        pluralStringResource(
                            R.plurals.home_daily_allowance,
                            allowance.daysLeft,
                            formatRupiah(allowance.amountPerDay),
                            allowance.daysLeft,
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            TextButton(onClick = onEditBudget, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.home_budget_edit))
            }
        }
    }
}

@Composable
private fun budgetStatusColor(usedFraction: Float): Color = when {
    usedFraction > 1f -> MaterialTheme.colorScheme.error
    usedFraction >= WARNING_FRACTION -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun CategorySpendingRow(
    spending: CategorySpending,
    totalSpent: Long,
    modifier: Modifier = Modifier,
) {
    val share = if (totalSpent > 0) spending.amount.toFloat() / totalSpent else 0f
    Row(
        // One TalkBack stop per row: "Belanja Harian, Rp1.200.000, 73%".
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(spending.category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = spending.category.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(formatRupiah(spending.amount), style = MaterialTheme.typography.bodyLarge)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { share },
                    color = categoryColor(spending.category),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${(share * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun TransactionRow(
    item: TransactionWithCategory,
    modifier: Modifier = Modifier,
) {
    val transaction = item.transaction
    Row(
        // One TalkBack stop per row: "Indomaret, Belanja Harian, Rp87.500". The date is in the day header above.
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(item.category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = transaction.merchant ?: item.category.displayName(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Without a merchant the category is already the title; repeating it adds nothing.
            if (transaction.merchant != null) {
                Text(
                    text = item.category.displayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatRupiah(transaction.amount),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}
