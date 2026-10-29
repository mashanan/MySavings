package com.hanan.strukku.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanan.strukku.R
import com.hanan.strukku.domain.model.Category
import com.hanan.strukku.domain.model.CategorySpending
import com.hanan.strukku.domain.model.MonthlySummary
import com.hanan.strukku.domain.model.Transaction
import com.hanan.strukku.domain.model.TransactionSource
import com.hanan.strukku.domain.model.TransactionWithCategory
import com.hanan.strukku.ui.components.StrukKuTopBar
import com.hanan.strukku.ui.theme.StrukKuTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** Connects [HomeScreen] to its ViewModel. Kept separate so the screen itself stays previewable. */
@Composable
fun HomeRoute(
    onScanClick: () -> Unit,
    onManualEntryClick: () -> Unit,
    showSavedMessage: Boolean,
    onSavedMessageShown: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onScanClick = onScanClick,
        onManualEntryClick = onManualEntryClick,
        onSaveBudget = viewModel::onSaveBudget,
        showSavedMessage = showSavedMessage,
        onSavedMessageShown = onSavedMessageShown,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onScanClick: () -> Unit,
    onManualEntryClick: () -> Unit,
    onSaveBudget: (Long) -> Unit,
    modifier: Modifier = Modifier,
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
        topBar = { StrukKuTopBar(title = stringResource(R.string.app_name)) },
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
                summary = uiState.summary,
                onEditBudget = { showBudgetSheet = true },
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
    summary: MonthlySummary,
    onEditBudget: () -> Unit,
    contentPadding: PaddingValues,
) {
    // One LazyColumn for the whole page so only visible rows are composed.
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        // Bottom padding keeps the last row clear of the FAB.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
    ) {
        item(key = "budget") {
            BudgetCard(summary = summary, onEditBudget = onEditBudget)
        }

        sectionTitle(R.string.home_by_category_title)
        if (summary.spendingByCategory.isEmpty()) {
            emptyText(R.string.home_empty_month)
        } else {
            items(summary.spendingByCategory, key = { "category-${it.category.id}" }) {
                CategorySpendingRow(spending = it, totalSpent = summary.totalSpent)
            }
        }

        sectionTitle(R.string.home_recent_title)
        if (summary.recentTransactions.isEmpty()) {
            emptyText(R.string.home_empty_recent)
        } else {
            items(summary.recentTransactions, key = { "transaction-${it.transaction.id}" }) {
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
            modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
        )
    }
}

private fun LazyListScope.emptyText(textRes: Int) {
    item(key = "empty-$textRes") {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        )
    }
}

private val previewFood = Category(1, "Makan & Minum", "food", "#E57373", emptyList(), true)
private val previewGroceries = Category(2, "Belanja Harian", "groceries", "#4DB6AC", emptyList(), true)

@Preview(showBackground = true)
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
        recentTransactions = listOf(
            TransactionWithCategory(
                Transaction(1, 87_500, LocalDate.of(2026, 10, 28), "Indomaret", null, 2, TransactionSource.SCAN, Instant.EPOCH),
                previewGroceries,
            ),
            TransactionWithCategory(
                Transaction(2, 35_000, LocalDate.of(2026, 10, 27), null, null, 1, TransactionSource.MANUAL, Instant.EPOCH),
                previewFood,
            ),
        ),
    )
    StrukKuTheme {
        HomeScreen(uiState = HomeUiState.Content(summary), onScanClick = {}, onManualEntryClick = {}, onSaveBudget = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    val summary = MonthlySummary(YearMonth.of(2026, 10), 0, null, emptyList(), emptyList())
    StrukKuTheme {
        HomeScreen(uiState = HomeUiState.Content(summary), onScanClick = {}, onManualEntryClick = {}, onSaveBudget = {})
    }
}
