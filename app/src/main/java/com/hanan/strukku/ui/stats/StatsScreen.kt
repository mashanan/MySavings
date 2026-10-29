package com.hanan.strukku.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanan.strukku.R
import com.hanan.strukku.domain.model.Category
import com.hanan.strukku.domain.model.CategorySpending
import com.hanan.strukku.domain.model.MonthlySummary
import com.hanan.strukku.domain.usecase.MonthTotal
import com.hanan.strukku.ui.components.StrukKuTopBar
import com.hanan.strukku.ui.components.categoryColor
import com.hanan.strukku.ui.home.CategorySpendingRow
import com.hanan.strukku.ui.theme.StrukKuTheme
import com.hanan.strukku.ui.util.formatMonthShort
import com.hanan.strukku.ui.util.formatMonthYear
import com.hanan.strukku.ui.util.formatRupiah
import com.hanan.strukku.ui.util.formatRupiahShort
import java.time.YearMonth

@Composable
fun StatsRoute(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(
        uiState = uiState,
        onBack = onBack,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onSelectMonth = viewModel::onSelectMonth,
    )
}

@Composable
fun StatsScreen(
    uiState: StatsUiState,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { StrukKuTopBar(title = stringResource(R.string.stats_title), onBack = onBack) },
    ) { innerPadding ->
        when (uiState) {
            StatsUiState.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }

            is StatsUiState.Content -> StatsContent(
                state = uiState,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onSelectMonth = onSelectMonth,
                contentPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun StatsContent(
    state: StatsUiState.Content,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    contentPadding: PaddingValues,
) {
    val summary = state.summary
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "month") {
            MonthSelector(state.selectedMonth, state.canGoNext, onPreviousMonth, onNextMonth)
        }

        item(key = "trend") {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.stats_month_total), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = formatRupiah(summary.totalSpent),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.stats_trend_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                    )
                    TrendBarChart(
                        trend = state.trend,
                        selectedMonth = state.selectedMonth,
                        onSelectMonth = onSelectMonth,
                        description = stringResource(
                            R.string.stats_trend_desc,
                            state.trend.joinToString { "${formatMonthShort(it.yearMonth)} ${formatRupiahShort(it.total)}" },
                        ),
                    )
                }
            }
        }

        item(key = "by-category-title") {
            Text(
                text = stringResource(R.string.stats_by_category),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
        }

        if (summary.spendingByCategory.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.stats_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            item(key = "donut") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CategoryDonutChart(
                        slices = summary.spendingByCategory.map { DonutSlice(categoryColor(it.category), it.amount) },
                        centerText = formatRupiahShort(summary.totalSpent),
                        // The legend rows below already read out each category, so keep this short.
                        description = pluralStringResource(
                            R.plurals.stats_donut_desc,
                            summary.spendingByCategory.size,
                            summary.spendingByCategory.size,
                        ),
                    )
                }
            }
            items(summary.spendingByCategory, key = { "category-${it.category.id}" }) {
                CategorySpendingRow(spending = it, totalSpent = summary.totalSpent)
            }
        }
    }
}

@Composable
private fun MonthSelector(
    month: YearMonth,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.stats_previous_month))
        }
        Text(
            text = formatMonthYear(month),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                // Announce the new month when it changes via the arrows or the chart.
                .semantics { liveRegion = LiveRegionMode.Polite },
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.stats_next_month))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsScreenPreview() {
    val october = YearMonth.of(2026, 10)
    val food = Category(1, "Makan & Minum", "food", "#E57373", emptyList(), true)
    val groceries = Category(2, "Belanja Harian", "groceries", "#4DB6AC", emptyList(), true)
    val transport = Category(3, "Transportasi", "transport", "#64B5F6", emptyList(), true)
    val totals = listOf(1_450_000L, 1_820_000L, 990_000L, 2_100_000L, 1_700_000L, 1_650_000L)
    StrukKuTheme {
        StatsScreen(
            uiState = StatsUiState.Content(
                selectedMonth = october,
                canGoNext = false,
                trend = totals.mapIndexed { i, total -> MonthTotal(october.minusMonths(5L - i), total) },
                summary = MonthlySummary(
                    yearMonth = october,
                    totalSpent = 1_650_000,
                    budgetLimit = null,
                    spendingByCategory = listOf(
                        CategorySpending(groceries, 900_000, null),
                        CategorySpending(food, 450_000, null),
                        CategorySpending(transport, 300_000, null),
                    ),
                    recentTransactions = emptyList(),
                ),
            ),
            onBack = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onSelectMonth = {},
        )
    }
}
