package com.hanan.mysavings.ui.debts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.usecase.DebtError
import com.hanan.mysavings.domain.usecase.SaveDebtUseCase
import com.hanan.mysavings.ui.components.AmountTextField
import com.hanan.mysavings.ui.components.AppDatePickerDialog
import com.hanan.mysavings.ui.components.DateField
import com.hanan.mysavings.ui.components.MySavingsTopBar
import com.hanan.mysavings.ui.components.fieldSupportingText
import com.hanan.mysavings.ui.theme.MySavingsTheme
import com.hanan.mysavings.ui.util.formatRupiah
import java.time.LocalDate

@Composable
fun DebtFormRoute(
    debtId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: DebtFormViewModel = hiltViewModel<DebtFormViewModel, DebtFormViewModel.Factory> { it.create(debtId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Saving is state (isSaved), not a one-off event, so it cannot be missed while the screen is paused.
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    DebtFormScreen(
        uiState = uiState,
        onBack = onBack,
        callbacks = DebtFormCallbacks(
            onCreditorChange = viewModel::onCreditorChange,
            onAmountChange = viewModel::onAmountChange,
            onInstallmentPlanChange = viewModel::onInstallmentPlanChange,
            onInstallmentsChange = viewModel::onInstallmentsChange,
            onFirstDueDateChange = viewModel::onFirstDueDateChange,
            onNoteChange = viewModel::onNoteChange,
            onSave = viewModel::onSave,
        ),
    )
}

/** Bundled so the screen's signature stays readable with this many fields. */
data class DebtFormCallbacks(
    val onCreditorChange: (String) -> Unit,
    val onAmountChange: (String) -> Unit,
    val onInstallmentPlanChange: (Boolean) -> Unit,
    val onInstallmentsChange: (String) -> Unit,
    val onFirstDueDateChange: (LocalDate) -> Unit,
    val onNoteChange: (String) -> Unit,
    val onSave: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtFormScreen(
    uiState: DebtFormUiState,
    onBack: () -> Unit,
    callbacks: DebtFormCallbacks,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            MySavingsTopBar(
                title = stringResource(if (uiState.isEditing) R.string.debt_form_title_edit else R.string.debt_form_title_new),
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (DebtError.NOT_FOUND in uiState.errors) {
                Text(stringResource(R.string.debt_error_not_found), color = MaterialTheme.colorScheme.error)
            }
            val creditorError = DebtError.CREDITOR_BLANK in uiState.errors
            OutlinedTextField(
                value = uiState.creditor,
                onValueChange = callbacks.onCreditorChange,
                label = { Text(stringResource(R.string.debt_form_creditor)) },
                placeholder = { Text(stringResource(R.string.debt_form_creditor_placeholder)) },
                isError = creditorError,
                supportingText = fieldSupportingText(stringResource(R.string.debt_error_creditor).takeIf { creditorError }, null),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            val amountError = when {
                DebtError.AMOUNT_NOT_POSITIVE in uiState.errors -> stringResource(R.string.debt_error_amount)
                DebtError.AMOUNT_BELOW_PAID in uiState.errors -> stringResource(R.string.debt_error_below_paid)
                else -> null
            }
            AmountTextField(
                digits = uiState.amountDigits,
                onDigitsChange = callbacks.onAmountChange,
                label = stringResource(R.string.debt_form_amount),
                isError = amountError != null,
                errorText = amountError,
                modifier = Modifier.fillMaxWidth(),
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !uiState.isInstallmentPlan,
                    onClick = { callbacks.onInstallmentPlanChange(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.debt_form_single)) }
                SegmentedButton(
                    selected = uiState.isInstallmentPlan,
                    onClick = { callbacks.onInstallmentPlanChange(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.debt_form_installments)) }
            }
            DateField(
                date = uiState.firstDueDate,
                label = stringResource(if (uiState.isInstallmentPlan) R.string.debt_form_first_due else R.string.debt_form_due),
                onClick = { showDatePicker = true },
            )
            if (uiState.isInstallmentPlan) {
                val countError = DebtError.INSTALLMENTS_OUT_OF_RANGE in uiState.errors
                OutlinedTextField(
                    value = uiState.installmentDigits,
                    onValueChange = callbacks.onInstallmentsChange,
                    label = { Text(stringResource(R.string.debt_form_installment_count)) },
                    isError = countError,
                    supportingText = fieldSupportingText(
                        errorText = stringResource(R.string.debt_error_installments, SaveDebtUseCase.MAX_INSTALLMENTS).takeIf { countError },
                        hintText = null,
                    ) ?: uiState.perInstallment?.let { amount ->
                        { Text(stringResource(R.string.debt_form_per_installment, formatRupiah(amount))) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = uiState.note,
                onValueChange = callbacks.onNoteChange,
                label = { Text(stringResource(R.string.debt_form_note)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = callbacks.onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(2.dp))
                } else {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }

    if (showDatePicker) {
        // Any date: an installment plan that started months ago is common.
        AppDatePickerDialog(
            initialDate = uiState.firstDueDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                callbacks.onFirstDueDateChange(it)
                showDatePicker = false
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DebtFormScreenPreview() {
    MySavingsTheme {
        DebtFormScreen(
            uiState = DebtFormUiState(
                isEditing = false,
                creditor = "Leasing motor",
                amountDigits = "12000000",
                isInstallmentPlan = true,
                installmentDigits = "12",
                firstDueDate = LocalDate.of(2026, 11, 5),
            ),
            onBack = {},
            callbacks = DebtFormCallbacks({}, {}, {}, {}, {}, {}, {}),
        )
    }
}
