package com.hanan.mysavings.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.model.Category
import com.hanan.mysavings.domain.model.CategorySpending
import com.hanan.mysavings.domain.model.DailyAllowance
import com.hanan.mysavings.domain.model.MonthInsights
import com.hanan.mysavings.domain.model.MonthlySummary
import com.hanan.mysavings.domain.model.PeriodComparison
import com.hanan.mysavings.domain.model.Transaction
import com.hanan.mysavings.domain.model.TransactionSource
import com.hanan.mysavings.domain.usecase.ObserveTransactionsByDayUseCase
import com.hanan.mysavings.domain.usecase.DayTotal
import com.hanan.mysavings.ui.components.MonthSelector
import com.hanan.mysavings.ui.components.MySavingsTopBar
import com.hanan.mysavings.ui.theme.MySavingsTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** Connects [HomeScreen] to its ViewModel. Kept separate so the screen itself stays previewable. */
@Composable
fun HomeRoute(
    onScanClick: () -> Unit,
    onManualEntryClick: () -> Unit,
    onStatsClick: () -> Unit,
    showSavedMessage: Boolean,
    onSavedMessageShown: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshDate()
        onPauseOrDispose {}
    }

    // Budget alerts need notification permission on Android 13+. Asking right after the user
    // sets a budget makes the reason obvious. If they decline twice, Android stops showing
    // the dialog by itself, so asking again later is harmless.
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    HomeScreen(
        uiState = uiState,
        onScanClick = onScanClick,
        onManualEntryClick = onManualEntryClick,
        onStatsClick = onStatsClick,
        onSaveBudget = { limit ->
            viewModel.onSaveBudget(limit)
            requestNotificationPermission()
        },
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        showSavedMessage = showSavedMessage,
        onSavedMessageShown = onSavedMessageShown,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onScanClick: () -> Unit,
    onManualEntryClick: () -> Unit,
    onStatsClick: () -> Unit,
    onSaveBudget: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    showSavedMessage: Boolean = false,
    onSavedMessageShown: () -> Unit = {},
) {
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showBudgetSheet by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val savedMessage = stringResource(R.string.home_transaction_saved)
    val scope = rememberCoroutineScope()
    LaunchedEffect(showSavedMessage) {
        if (showSavedMessage) {
            // Launched in a separate scope: clearing the flag restarts this effect, which
            // would otherwise cancel the snackbar while it is still on screen.
            scope.launch { snackbarHostState.showSnackbar(savedMessage) }
            onSavedMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            MySavingsTopBar(title = stringResource(R.string.tab_expenses)) {
                IconButton(onClick = onStatsClick) {
                    Icon(painterResource(R.drawable.ic_chart), stringResource(R.string.stats_open))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.action_add_transaction),
                )
            }
        },
    ) { innerPadding ->
        when (uiState) {
            HomeUiState.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }

            is HomeUiState.Content -> HomeContent(
                state = uiState,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onEditBudget = { showBudgetSheet = true },
                onScanClick = onScanClick,
                onManualEntryClick = onManualEntryClick,
                contentPadding = innerPadding,
            )
        }
    }

    if (showAddSheet) {
        AddTransactionSheet(
            onDismiss = { showAddSheet = false },
            onScanClick = {
                showAddSheet = false
                onScanClick()
            },
            onManualEntryClick = {
                showAddSheet = false
                onManualEntryClick()
            },
        )
    }

    if (showBudgetSheet && uiState is HomeUiState.Content) {
        BudgetSheet(
            yearMonth = uiState.summary.yearMonth,
            currentLimit = uiState.summary.budgetLimit,
            onDismiss = { showBudgetSheet = false },
            onSave = {
                onSaveBudget(it)
                showBudgetSheet = false
            },
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Content,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEditBudget: () -> Unit,
    onScanClick: () -> Unit,
    onManualEntryClick: () -> Unit,
    contentPadding: PaddingValues,
) {
    // One LazyColumn for the whole page so only visible rows are composed.
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        // Bottom padding keeps the last row clear of the FAB.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
    ) {
        val summary = state.summary
        val insights = state.insights

        item(key = "month") {
            MonthSelector(
                month = summary.yearMonth,
                canGoNext = state.canGoNext,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        item(key = "budget") {
            BudgetCard(summary = summary, allowance = insights.dailyAllowance, onEditBudget = onEditBudget)
        }
        item(key = "quick-actions") {
            QuickActionsRow(
                onScanClick = onScanClick,
                onManualEntryClick = onManualEntryClick,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        item(key = "stats") {
            MonthStatsRow(insights = insights, modifier = Modifier.padding(top = 12.dp))
        }
        insights.comparison?.let { comparison ->
            item(key = "comparison") {
                ComparisonCard(comparison = comparison, modifier = Modifier.padding(top = 8.dp))
            }
        }

        sectionTitle(R.string.home_chart_title)
        item(key = "chart") {
            DailySpendingChart(days = state.dailyTotals, today = state.today)
        }

        // One friendly card instead of two empty sections with a line of grey text each.
        if (state.transactionDays.isEmpty()) {
            item(key = "empty") {
                EmptyMonthCard(
                    month = summary.yearMonth,
                    isCurrentMonth = summary.yearMonth == YearMonth.from(state.today),
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
            return@LazyColumn
        }

        sectionTitle(R.string.home_by_category_title)
        items(summary.spendingByCategory, key = { "category-${it.category.id}" }) {
            CategorySpendingRow(spending = it, totalSpent = summary.totalSpent)
        }

        sectionTitle(R.string.home_transactions_title)
        state.transactionDays.forEach { day ->
            item(key = "day-${day.date}") {
                DayHeader(day = day, today = state.today)
            }
            items(day.transactions, key = { "transaction-${it.transaction.id}" }) {
                TransactionRow(item = it)
            }
        }
    }
}

private fun LazyListScope.sectionTitle(titleRes: Int) {
    item(key = "title-$titleRes") {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            // Marked as a heading so screen-reader users can jump between sections.
            modifier = Modifier.padding(top = 24.dp, bottom = 4.dp).semantics { heading() },
        )
    }
}

private val previewFood = Category(1, "Makan & Minum", "food", "#E57373", emptyList(), true)
private val previewGroceries = Category(2, "Belanja Harian", "groceries", "#4DB6AC", emptyList(), true)
private val previewToday = LocalDate.of(2026, 10, 29)

private fun previewWeek(totals: List<Long>) =
    totals.mapIndexed { index, total -> DayTotal(previewToday.minusDays(6L - index), total) }

@Preview(showBackground = true)
@Preview(showBackground = true, name = "Large font", fontScale = 1.5f)
@Composable
private fun HomeScreenPreview() {
    val summary = MonthlySummary(
        yearMonth = YearMonth.of(2026, 10),
        totalSpent = 1_650_000,
        budgetLimit = 2_000_000,
        spendingByCategory = listOf(
            CategorySpending(previewGroceries, 1_200_000, null),
            CategorySpending(previewFood, 450_000, null),
        ),
    )
    val transactionDays = ObserveTransactionsByDayUseCase.groupByDay(
        listOf(
            Transaction(3, 35_000, previewToday, null, null, 1, TransactionSource.MANUAL, Instant.EPOCH),
            Transaction(2, 87_500, previewToday.minusDays(1), "Indomaret", null, 2, TransactionSource.SCAN, Instant.EPOCH),
            Transaction(1, 120_000, previewToday.minusDays(3), "Superindo", null, 2, TransactionSource.SCAN, Instant.EPOCH),
        ),
        listOf(previewFood, previewGroceries),
    )
    val insights = MonthInsights(
        transactionCount = 2,
        averagePerDay = 56_896,
        topCategory = summary.spendingByCategory.first(),
        dailyAllowance = DailyAllowance(amountPerDay = 116_666, daysLeft = 3),
        comparison = PeriodComparison(previousTotal = 2_010_000, changePercent = -18),
    )
    MySavingsTheme {
        HomeScreen(uiState = HomeUiState.Content(summary, insights, transactionDays, previewWeek(listOf(45_000, 0, 120_000, 87_500, 30_000, 0, 35_000)), previewToday, canGoNext = false), onScanClick = {}, onManualEntryClick = {}, onStatsClick = {}, onSaveBudget = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    val summary = MonthlySummary(YearMonth.of(2026, 10), 0, null, emptyList())
    val insights = MonthInsights(0, 0, null, null, null)
    MySavingsTheme {
        HomeScreen(uiState = HomeUiState.Content(summary, insights, emptyList(), previewWeek(List(7) { 0L }), previewToday, canGoNext = false), onScanClick = {}, onManualEntryClick = {}, onStatsClick = {}, onSaveBudget = {})
    }
}
