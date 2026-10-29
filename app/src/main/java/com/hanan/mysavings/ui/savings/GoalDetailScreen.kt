package com.hanan.mysavings.ui.savings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.model.GoalCategory
import com.hanan.mysavings.domain.model.GoalProgress
import com.hanan.mysavings.domain.model.SavingsDeposit
import com.hanan.mysavings.domain.model.SavingsGoal
import com.hanan.mysavings.domain.usecase.GoalDetail
import com.hanan.mysavings.ui.components.MySavingsTopBar
import com.hanan.mysavings.ui.theme.MySavingsTheme
import com.hanan.mysavings.ui.util.currentLocale
import com.hanan.mysavings.ui.util.formatMonthYear
import com.hanan.mysavings.ui.util.formatRupiah
import com.hanan.mysavings.ui.util.formatShortDate
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

@Composable
fun GoalDetailRoute(
    goalId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: GoalDetailViewModel = hiltViewModel<GoalDetailViewModel, GoalDetailViewModel.Factory> { it.create(goalId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Deleted (here or elsewhere): nothing left to show, so close the screen.
    LaunchedEffect(uiState) {
        if (uiState == GoalDetailUiState.Deleted) onBack()
    }
    // LocalResources (not LocalContext) so a language change while the screen is open is picked up.
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            snackbarHostState.showSnackbar(resources.getString(event.messageRes()))
        }
    }

    GoalDetailScreen(
        uiState = uiState,
        sheet = sheet,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEdit = onEdit,
        onDeleteGoal = viewModel::onDeleteGoal,
        onOpenDeposit = viewModel::onOpenDeposit,
        onOpenWithdraw = viewModel::onOpenWithdraw,
        onDeleteDeposit = viewModel::onDeleteDeposit,
        sheetCallbacks = DepositSheetCallbacks(
            onDismiss = viewModel::onSheetDismiss,
            onAmountChange = viewModel::onSheetAmountChange,
            onDateChange = viewModel::onSheetDateChange,
            onNoteChange = viewModel::onSheetNoteChange,
            onSave = viewModel::onSheetSave,
        ),
    )
}

private fun GoalDetailEvent.messageRes(): Int = when (this) {
    GoalDetailEvent.DepositSaved -> R.string.deposit_saved
    GoalDetailEvent.WithdrawalSaved -> R.string.withdrawal_saved
    GoalDetailEvent.DepositDeleted -> R.string.deposit_deleted
    GoalDetailEvent.DepositDeleteBlocked -> R.string.deposit_delete_blocked
}

@Composable
fun GoalDetailScreen(
    uiState: GoalDetailUiState,
    sheet: DepositSheetState?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteGoal: () -> Unit,
    onOpenDeposit: () -> Unit,
    onOpenWithdraw: () -> Unit,
    onDeleteDeposit: (Long) -> Unit,
    sheetCallbacks: DepositSheetCallbacks,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val detail = (uiState as? GoalDetailUiState.Content)?.detail
    var showDeleteGoalDialog by rememberSaveable { mutableStateOf(false) }
    var depositToDelete by remember { mutableStateOf<SavingsDeposit?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            MySavingsTopBar(title = detail?.goal?.name.orEmpty(), onBack = onBack) {
                if (detail != null) {
                    IconButton(onClick = onEdit) {
                        Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.goal_edit))
                    }
                    IconButton(onClick = { showDeleteGoalDialog = true }) {
                        Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.goal_delete))
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (detail == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) { CircularProgressIndicator() }
        } else {
            GoalDetailContent(
                detail = detail,
                onOpenDeposit = onOpenDeposit,
                onOpenWithdraw = onOpenWithdraw,
                onDepositClick = { depositToDelete = it },
                contentPadding = innerPadding,
            )
        }
    }

    if (sheet != null && detail != null) {
        DepositSheet(state = sheet, goalName = detail.goal.name, balance = detail.progress.saved, callbacks = sheetCallbacks)
    }

    if (showDeleteGoalDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteGoalDialog = false },
            title = { Text(stringResource(R.string.goal_delete_title)) },
            text = { Text(stringResource(R.string.goal_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteGoalDialog = false
                    onDeleteGoal()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGoalDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    depositToDelete?.let { deposit ->
        AlertDialog(
            onDismissRequest = { depositToDelete = null },
            title = { Text(stringResource(R.string.deposit_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.deposit_delete_body,
                        formatSignedRupiah(deposit.amount),
                        formatShortDate(deposit.date, currentLocale()),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    depositToDelete = null
                    onDeleteDeposit(deposit.id)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { depositToDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun GoalDetailContent(
    detail: GoalDetail,
    onOpenDeposit: () -> Unit,
    onOpenWithdraw: () -> Unit,
    onDepositClick: (SavingsDeposit) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
    ) {
        item(key = "progress") {
            ProgressCard(goal = detail.goal, progress = detail.progress)
        }
        item(key = "actions") {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Button(onClick = onOpenDeposit, modifier = Modifier.weight(1f)) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.goal_deposit))
                }
                Spacer(Modifier.width(12.dp))
                // Nothing to take out of an empty goal.
                OutlinedButton(onClick = onOpenWithdraw, enabled = detail.progress.saved > 0, modifier = Modifier.weight(1f)) {
                    Icon(painterResource(R.drawable.ic_remove), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.goal_withdraw))
                }
            }
        }
        item(key = "history-title") {
            Text(
                text = stringResource(R.string.goal_history),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp).semantics { heading() },
            )
        }
        if (detail.deposits.isEmpty()) {
            item(key = "history-empty") {
                Text(
                    text = stringResource(R.string.goal_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        } else {
            items(detail.deposits, key = { it.id }) { deposit ->
                DepositRow(deposit = deposit, onClick = { onDepositClick(deposit) })
            }
        }
    }
}

@Composable
private fun ProgressCard(goal: SavingsGoal, progress: GoalProgress) {
    val percent = (progress.fraction * 100).roundToInt()
    val progressDescription = stringResource(R.string.goal_progress_desc, percent)
    val locale = currentLocale()
    Card(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GoalCategoryIcon(goal.category)
                Text(
                    text = stringResource(goal.category.labelRes()),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text("$percent%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Text(
                text = formatRupiah(progress.saved),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            LinearProgressIndicator(
                progress = { progress.fraction.coerceIn(0f, 1f) },
                color = goal.category.color(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .semantics { stateDescription = progressDescription },
            )
            Text(stringResource(R.string.goal_progress_amount, formatRupiah(progress.saved), formatRupiah(progress.target)))
            when {
                progress.isReached -> Text(
                    text = stringResource(R.string.goal_reached),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                else -> Text(stringResource(R.string.goal_remaining, formatRupiah(progress.remaining)))
            }
            if (progress.isOverdue) {
                Text(stringResource(R.string.goal_overdue), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }
            val deadline = goal.targetDate
            if (progress.monthlySuggestion != null && deadline != null) {
                Text(
                    text = stringResource(
                        R.string.goal_monthly_suggestion,
                        formatRupiah(progress.monthlySuggestion),
                        formatMonthYear(YearMonth.from(deadline), locale),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            } else if (deadline != null && !progress.isOverdue) {
                Text(stringResource(R.string.goal_deadline, formatShortDate(deadline, locale)))
            }
        }
    }
}

@Composable
private fun DepositRow(deposit: SavingsDeposit, onClick: () -> Unit) {
    Row(
        // One TalkBack stop per row; a tap offers to delete the entry.
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.action_delete), onClick = onClick)
            .padding(vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (deposit.isWithdrawal) R.drawable.ic_remove else R.drawable.ic_add),
            contentDescription = null,
            tint = if (deposit.isWithdrawal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(if (deposit.isWithdrawal) R.string.withdrawal_label else R.string.deposit_label),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = listOfNotNull(formatShortDate(deposit.date, currentLocale()), deposit.note).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatSignedRupiah(deposit.amount),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (deposit.isWithdrawal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}

/** "+Rp500.000" for a deposit, "-Rp200.000" for a withdrawal. */
private fun formatSignedRupiah(amount: Long): String = if (amount < 0) formatRupiah(amount) else "+${formatRupiah(amount)}"

@Preview(showBackground = true)
@Composable
private fun GoalDetailScreenPreview() {
    val today = LocalDate.of(2026, 10, 29)
    val goal = SavingsGoal(1, "Laptop kerja", 12_000_000, GoalCategory.GADGET, LocalDate.of(2027, 3, 1), Instant.EPOCH)
    val deposits = listOf(
        SavingsDeposit(3, 1, -500_000, LocalDate.of(2026, 10, 20), "Servis motor", Instant.EPOCH),
        SavingsDeposit(2, 1, 2_000_000, LocalDate.of(2026, 10, 1), "Gaji Oktober", Instant.EPOCH),
        SavingsDeposit(1, 1, 3_000_000, LocalDate.of(2026, 9, 1), null, Instant.EPOCH),
    )
    MySavingsTheme {
        GoalDetailScreen(
            uiState = GoalDetailUiState.Content(GoalDetail(goal, GoalProgress.of(goal, 4_500_000, today), deposits)),
            sheet = null,
            onBack = {},
            onEdit = {},
            onDeleteGoal = {},
            onOpenDeposit = {},
            onOpenWithdraw = {},
            onDeleteDeposit = {},
            sheetCallbacks = DepositSheetCallbacks({}, {}, {}, {}, {}),
        )
    }
}
