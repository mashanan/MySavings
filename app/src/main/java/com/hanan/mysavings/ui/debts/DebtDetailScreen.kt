package com.hanan.mysavings.ui.debts

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
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.model.Debt
import com.hanan.mysavings.domain.model.DebtPayment
import com.hanan.mysavings.domain.model.DebtStatus
import com.hanan.mysavings.domain.model.InstallmentStatus
import com.hanan.mysavings.domain.usecase.DebtDetail
import com.hanan.mysavings.ui.components.MySavingsTopBar
import com.hanan.mysavings.ui.theme.MySavingsTheme
import com.hanan.mysavings.ui.util.currentLocale
import com.hanan.mysavings.ui.util.formatRupiah
import com.hanan.mysavings.ui.util.formatShortDate
import java.time.Instant
import java.time.LocalDate

@Composable
fun DebtDetailRoute(
    debtId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: DebtDetailViewModel = hiltViewModel<DebtDetailViewModel, DebtDetailViewModel.Factory> { it.create(debtId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Deleted (here or elsewhere): nothing left to show, so close the screen.
    LaunchedEffect(uiState) {
        if (uiState == DebtDetailUiState.Deleted) onBack()
    }
    // LocalResources (not LocalContext) so a language change while the screen is open is picked up.
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event -> snackbarHostState.showSnackbar(resources.getString(event.messageRes())) }
    }

    DebtDetailScreen(
        uiState = uiState,
        sheet = sheet,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEdit = onEdit,
        onDeleteDebt = viewModel::onDeleteDebt,
        onOpenPay = viewModel::onOpenPay,
        onOpenPayOff = viewModel::onOpenPayOff,
        onDeletePayment = viewModel::onDeletePayment,
        sheetCallbacks = PaymentSheetCallbacks(
            onDismiss = viewModel::onSheetDismiss,
            onAmountChange = viewModel::onSheetAmountChange,
            onDateChange = viewModel::onSheetDateChange,
            onNoteChange = viewModel::onSheetNoteChange,
            onSave = viewModel::onSheetSave,
        ),
    )
}

private fun DebtDetailEvent.messageRes(): Int = when (this) {
    DebtDetailEvent.PaymentSaved -> R.string.payment_saved
    DebtDetailEvent.PaidOff -> R.string.payment_paid_off
    DebtDetailEvent.PaymentDeleted -> R.string.payment_deleted
}

@Composable
fun DebtDetailScreen(
    uiState: DebtDetailUiState,
    sheet: PaymentSheetState?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteDebt: () -> Unit,
    onOpenPay: () -> Unit,
    onOpenPayOff: () -> Unit,
    onDeletePayment: (Long) -> Unit,
    sheetCallbacks: PaymentSheetCallbacks,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val detail = (uiState as? DebtDetailUiState.Content)?.detail
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<DebtPayment?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            MySavingsTopBar(title = detail?.debt?.creditor.orEmpty(), onBack = onBack) {
                if (detail != null) {
                    IconButton(onClick = onEdit) {
                        Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.debt_edit))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.debt_delete))
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (detail == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) { CircularProgressIndicator() }
        } else {
            DebtDetailContent(
                detail = detail,
                onOpenPay = onOpenPay,
                onOpenPayOff = onOpenPayOff,
                onPaymentClick = { paymentToDelete = it },
                contentPadding = innerPadding,
            )
        }
    }

    if (sheet != null && detail != null) {
        PaymentSheet(state = sheet, creditor = detail.debt.creditor, remaining = detail.status.remaining, callbacks = sheetCallbacks)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.debt_delete_title)) },
            text = { Text(stringResource(R.string.debt_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteDebt()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    paymentToDelete?.let { payment ->
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = { Text(stringResource(R.string.payment_delete_title)) },
            text = {
                Text(stringResource(R.string.payment_delete_body, formatRupiah(payment.amount), formatShortDate(payment.date, currentLocale())))
            },
            confirmButton = {
                TextButton(onClick = {
                    paymentToDelete = null
                    onDeletePayment(payment.id)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { paymentToDelete = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun DebtDetailContent(
    detail: DebtDetail,
    onOpenPay: () -> Unit,
    onOpenPayOff: () -> Unit,
    onPaymentClick: (DebtPayment) -> Unit,
    contentPadding: PaddingValues,
) {
    val debt = detail.debt
    val status = detail.status
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
    ) {
        item(key = "status") { StatusCard(debt, status) }
        if (!status.isPaidOff) {
            item(key = "actions") {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Button(onClick = onOpenPay, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.debt_pay)) }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = onOpenPayOff, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.debt_pay_off)) }
                }
            }
        }
        debt.note?.let { note ->
            item(key = "note") {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
        // A single payment has a one-line "schedule"; the status card already says it all.
        if (debt.isInstallmentPlan) {
            sectionTitle("schedule-title", R.string.debt_schedule)
            items(status.installments, key = { "installment-${it.installment.number}" }) {
                InstallmentRow(it, debt.installmentCount)
            }
        }
        sectionTitle("payments-title", R.string.debt_payments)
        if (detail.payments.isEmpty()) {
            item(key = "payments-empty") {
                Text(
                    text = stringResource(R.string.debt_payments_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        } else {
            items(detail.payments, key = { "payment-${it.id}" }) { payment ->
                PaymentRow(payment = payment, onClick = { onPaymentClick(payment) })
            }
        }
    }
}

private fun LazyListScope.sectionTitle(key: String, titleRes: Int) {
    item(key = key) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 4.dp).semantics { heading() },
        )
    }
}

@Composable
private fun StatusCard(debt: Debt, status: DebtStatus) {
    val locale = currentLocale()
    val next = status.next
    Card(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.debt_remaining_title), style = MaterialTheme.typography.titleSmall)
            Text(formatRupiah(status.remaining), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { (status.paid.toFloat() / debt.totalAmount).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp),
            )
            Text(stringResource(R.string.debt_paid_of_total, formatRupiah(status.paid), formatRupiah(debt.totalAmount)))
            when {
                next == null -> Text(stringResource(R.string.debt_paid_off), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                else -> Text(
                    text = stringResource(
                        R.string.debt_next_payment,
                        formatRupiah(next.remaining),
                        formatShortDate(next.installment.dueDate, locale),
                    ),
                    fontWeight = FontWeight.Medium,
                )
            }
            if (status.overdueCount > 0) {
                Text(
                    text = pluralStringResource(R.plurals.debts_overdue_count, status.overdueCount, status.overdueCount),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun InstallmentRow(status: InstallmentStatus, count: Int) {
    val installment = status.installment
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(installmentLabel(installment.number, count), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = formatShortDate(installment.dueDate, currentLocale()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatRupiah(installment.amount), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = when {
                    status.isPaid -> stringResource(R.string.debt_installment_paid)
                    status.isOverdue -> stringResource(R.string.debt_installment_overdue)
                    status.paidAmount > 0 -> stringResource(R.string.debt_installment_partly, formatRupiah(status.remaining))
                    else -> stringResource(R.string.debt_installment_open)
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = when {
                    status.isPaid -> MaterialTheme.colorScheme.primary
                    status.isOverdue -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun PaymentRow(payment: DebtPayment, onClick: () -> Unit) {
    Row(
        // One TalkBack stop per row; a tap offers to delete the entry.
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.action_delete), onClick = onClick)
            .padding(vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(formatShortDate(payment.date, currentLocale()), style = MaterialTheme.typography.bodyLarge)
            payment.note?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(formatRupiah(payment.amount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true)
@Composable
private fun DebtDetailScreenPreview() {
    val today = LocalDate.of(2026, 10, 29)
    val debt = Debt(1, "Leasing motor", 12_000_000, LocalDate.of(2026, 6, 5), 12, "Honda Beat, DP sudah", Instant.EPOCH)
    val payments = listOf(
        DebtPayment(2, 1, 2_000_000, LocalDate.of(2026, 8, 5), null, Instant.EPOCH),
        DebtPayment(1, 1, 2_000_000, LocalDate.of(2026, 7, 5), "Bayar dua bulan", Instant.EPOCH),
    )
    MySavingsTheme {
        DebtDetailScreen(
            uiState = DebtDetailUiState.Content(DebtDetail(debt, DebtStatus.of(debt, 4_000_000, today), payments)),
            sheet = null,
            onBack = {},
            onEdit = {},
            onDeleteDebt = {},
            onOpenPay = {},
            onOpenPayOff = {},
            onDeletePayment = {},
            sheetCallbacks = PaymentSheetCallbacks({}, {}, {}, {}, {}),
        )
    }
}
