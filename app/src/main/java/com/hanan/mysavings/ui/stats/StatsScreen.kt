package com.hanan.mysavings.ui.stats

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.model.Category
import com.hanan.mysavings.domain.model.CategorySpending
import com.hanan.mysavings.domain.model.MonthlySummary
import com.hanan.mysavings.domain.usecase.MonthTotal
import com.hanan.mysavings.ui.components.MonthSelector
import com.hanan.mysavings.ui.components.MySavingsTopBar
import com.hanan.mysavings.ui.components.categoryColor
import com.hanan.mysavings.ui.home.CategorySpendingRow
import com.hanan.mysavings.ui.theme.MySavingsTheme
import com.hanan.mysavings.ui.util.currentLocale
import com.hanan.mysavings.ui.util.formatMonthShort
import com.hanan.mysavings.ui.util.formatRupiah
import com.hanan.mysavings.ui.util.formatRupiahShort
import kotlinx.coroutines.launch
import java.io.File
import java.time.YearMonth

@Composable
fun StatsRoute(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.stats_export_share)

    LaunchedEffect(exportState) {
        val ready = exportState as? ExportState.Ready ?: return@LaunchedEffect
        context.sharePdf(ready.file, shareTitle)
        viewModel.onExportHandled()
    }

    StatsScreen(
        uiState = uiState,
        onBack = onBack,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onSelectMonth = viewModel::onSelectMonth,
        isExporting = exportState == ExportState.Exporting,
        showExportError = exportState == ExportState.Failed,
        onExport = viewModel::onExport,
        onExportErrorShown = viewModel::onExportHandled,
    )
}

/**
 * Opens the system share sheet for a report. The FileProvider turns the private cache file
 * into a content:// URI, and the grant flag lets only the chosen app read it, temporarily.
 */
private fun Context.sharePdf(file: File, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("application/pdf")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    startActivity(Intent.createChooser(send, chooserTitle))
}

@Composable
fun StatsScreen(
    uiState: StatsUiState,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    isExporting: Boolean = false,
    showExportError: Boolean = false,
    onExport: () -> Unit = {},
    onExportErrorShown: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = stringResource(R.string.stats_export_failed)
    val scope = rememberCoroutineScope()
    LaunchedEffect(showExportError) {
        if (showExportError) {
            // Separate scope: clearing the flag restarts this effect and would cancel the snackbar.
            scope.launch { snackbarHostState.showSnackbar(errorMessage) }
            onExportErrorShown()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MySavingsTopBar(title = stringResource(R.string.stats_title), onBack = onBack) {
                if (isExporting) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(12.dp).size(24.dp))
                } else {
                    IconButton(onClick = onExport, enabled = uiState is StatsUiState.Content) {
                        Icon(painterResource(R.drawable.ic_share), stringResource(R.string.stats_export))
                    }
                }
            }
        },
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
    val locale = currentLocale()
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
                            state.trend.joinToString { "${formatMonthShort(it.yearMonth, locale)} ${formatRupiahShort(it.total, locale)}" },
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
                        centerText = formatRupiahShort(summary.totalSpent, locale),
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

@Preview(showBackground = true)
@Composable
private fun StatsScreenPreview() {
    val october = YearMonth.of(2026, 10)
    val food = Category(1, "Makan & Minum", "food", "#E57373", emptyList(), true)
    val groceries = Category(2, "Belanja Harian", "groceries", "#4DB6AC", emptyList(), true)
    val transport = Category(3, "Transportasi", "transport", "#64B5F6", emptyList(), true)
    val totals = listOf(1_450_000L, 1_820_000L, 990_000L, 2_100_000L, 1_700_000L, 1_650_000L)
    MySavingsTheme {
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
