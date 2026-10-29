package com.hanan.mysavings.ui.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.model.Debt
import com.hanan.mysavings.domain.model.DebtStatus
import com.hanan.mysavings.domain.usecase.DebtWithStatus
import com.hanan.mysavings.domain.usecase.DueItem
import com.hanan.mysavings.ui.components.IconMessage
import com.hanan.mysavings.ui.components.MonthSelector
import com.hanan.mysavings.ui.components.MySavingsTopBar
import com.hanan.mysavings.ui.theme.MySavingsTheme
import com.hanan.mysavings.ui.util.currentLocale
import com.hanan.mysavings.ui.util.formatRupiah
import com.hanan.mysavings.ui.util.formatShortDate
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun DebtsRoute(
    onNewDebt: () -> Unit,
    onDebtClick: (Long) -> Unit,
    viewModel: DebtsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DebtsScreen(
        uiState = uiState,
        onNewDebt = onNewDebt,
        onDebtClick = onDebtClick,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onSelectDate = viewModel::onSelectDate,
    )
}

@Composable
fun DebtsScreen(
    uiState: DebtsUiState,
    onNewDebt: () -> Unit,
    onDebtClick: (Long) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { MySavingsTopBar(title = stringResource(R.string.tab_debts)) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewDebt,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.debts_new)) },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            DebtsUiState.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }

            is DebtsUiState.Content -> if (!uiState.hasDebts) {
                IconMessage(
                    icon = R.drawable.ic_calendar,
                    title = stringResource(R.string.debts_empty_title),
                    body = stringResource(R.string.debts_empty_body),
                    modifier = Modifier.padding(innerPadding),
                )
            } else {
                DebtsContent(uiState, onDebtClick, onPreviousMonth, onNextMonth, onSelectDate, innerPadding)
            }
        }
    }
}

@Composable
private fun DebtsContent(
    state: DebtsUiState.Content,
    onDebtClick: (Long) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    contentPadding: PaddingValues,
) {
    var showPaidOff by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        // Bottom padding keeps the last card clear of the FAB.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "total") {
            TotalDebtCard(totalRemaining = state.totalRemaining, overdueCount = state.overdueCount)
        }
        item(key = "calendar") {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    MonthSelector(
                        month = state.month,
                        canGoNext = true,
                        onPrevious = onPreviousMonth,
                        onNext = onNextMonth,
                    )
                    DebtCalendar(
                        month = state.month,
                        today = state.today,
                        dots = state.dots,
                        selectedDate = state.selectedDate,
                        onSelectDate = onSelectDate,
                    )
                    CalendarLegend(Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
                }
            }
        }
        state.selectedDate?.let { date ->
            sectionTitle("selected-title", R.string.debts_due_on, formatArg = date)
            if (state.dueOnSelectedDate.isEmpty()) {
                item(key = "selected-empty") {
                    Text(
                        text = stringResource(R.string.debts_nothing_due),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.dueOnSelectedDate, key = { "due-${it.debtId}-${it.installment.installment.number}" }) {
                    DueRow(item = it, onClick = { onDebtClick(it.debtId) })
                }
            }
        }

        sectionTitle("upcoming-title", R.string.debts_upcoming)
        if (state.active.isEmpty()) {
            item(key = "all-paid") {
                Text(stringResource(R.string.debts_all_paid), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(state.active, key = { "active-${it.debt.id}" }) {
                DebtCard(item = it, onClick = { onDebtClick(it.debt.id) })
            }
        }

        if (state.paidOff.isNotEmpty()) {
            item(key = "paid-title") {
                Text(
                    text = stringResource(
                        if (showPaidOff) R.string.debts_paid_off_hide else R.string.debts_paid_off_show,
                        state.paidOff.size,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPaidOff = !showPaidOff }
                        .padding(vertical = 12.dp), // keeps the touch target at least 48dp tall
                )
            }
            if (showPaidOff) {
                items(state.paidOff, key = { "paid-${it.debt.id}" }) {
                    DebtCard(item = it, onClick = { onDebtClick(it.debt.id) })
                }
            }
        }
    }
}

private fun LazyListScope.sectionTitle(key: String, titleRes: Int, formatArg: LocalDate? = null) {
    item(key = key) {
        Text(
            text = if (formatArg == null) stringResource(titleRes) else stringResource(titleRes, formatShortDate(formatArg, currentLocale())),
            style = MaterialTheme.typography.titleMedium,
            // Marked as a heading so screen-reader users can jump between sections.
            modifier = Modifier.padding(top = 8.dp).semantics { heading() },
        )
    }
}

@Composable
private fun TotalDebtCard(totalRemaining: Long, overdueCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.debts_total_title), style = MaterialTheme.typography.titleSmall)
            Text(formatRupiah(totalRemaining), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (overdueCount > 0) {
                Text(
                    text = pluralStringResource(R.plurals.debts_overdue_count, overdueCount, overdueCount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Text(stringResource(R.string.debts_none_overdue), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CalendarLegend(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(
            DueDot.OVERDUE to R.string.debts_legend_overdue,
            DueDot.SOON to R.string.debts_legend_soon,
            DueDot.UPCOMING to R.string.debts_legend_upcoming,
            DueDot.PAID to R.string.debts_legend_paid,
        ).forEach { (dot, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor(dot)))
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

/** One installment due on the selected day. */
@Composable
private fun DueRow(item: DueItem, onClick: () -> Unit) {
    val status = item.installment
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.creditor, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = installmentLabel(status.installment.number, item.installmentCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (status.isPaid) stringResource(R.string.debt_installment_paid) else formatRupiah(status.remaining),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = when {
                status.isPaid -> MaterialTheme.colorScheme.outline
                status.isOverdue -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun DebtCard(item: DebtWithStatus, onClick: () -> Unit) {
    val debt = item.debt
    val status = item.status
    val next = status.next
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = debt.creditor,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(formatRupiah(status.remaining), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                text = when {
                    next == null -> stringResource(R.string.debt_paid_off)
                    debt.isInstallmentPlan -> stringResource(
                        R.string.debt_next_installment,
                        formatShortDate(next.installment.dueDate, currentLocale()),
                        installmentLabel(next.installment.number, debt.installmentCount),
                    )
                    else -> stringResource(R.string.debt_due, formatShortDate(next.installment.dueDate, currentLocale()))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (status.overdueCount > 0) {
                Text(
                    text = pluralStringResource(R.plurals.debts_overdue_count, status.overdueCount, status.overdueCount),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            LinearProgressIndicator(
                progress = { (status.paid.toFloat() / debt.totalAmount).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
        }
    }
}

/** "Installment 2 of 12", or "Single payment" for a debt paid in one go. */
@Composable
internal fun installmentLabel(number: Int, count: Int): String =
    if (count == 1) stringResource(R.string.debt_single_payment) else stringResource(R.string.debt_installment_of, number, count)

@Preview(showBackground = true)
@Composable
private fun DebtsScreenPreview() {
    val today = LocalDate.of(2026, 10, 29)
    val motor = Debt(1, "Leasing motor", 12_000_000, LocalDate.of(2026, 6, 5), 12, null, Instant.EPOCH)
    val friend = Debt(2, "Budi", 500_000, LocalDate.of(2026, 10, 31), 1, null, Instant.EPOCH)
    val active = listOf(
        DebtWithStatus(friend, DebtStatus.of(friend, 0, today)),
        DebtWithStatus(motor, DebtStatus.of(motor, 4_000_000, today)),
    )
    MySavingsTheme {
        DebtsScreen(
            uiState = DebtsUiState.Content(
                totalRemaining = 8_500_000,
                overdueCount = 1,
                hasDebts = true,
                month = YearMonth.of(2026, 10),
                today = today,
                dots = mapOf(LocalDate.of(2026, 10, 5) to DueDot.OVERDUE, LocalDate.of(2026, 10, 31) to DueDot.SOON),
                selectedDate = today,
                dueOnSelectedDate = emptyList(),
                active = active,
                paidOff = emptyList(),
            ),
            onNewDebt = {},
            onDebtClick = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onSelectDate = {},
        )
    }
}
