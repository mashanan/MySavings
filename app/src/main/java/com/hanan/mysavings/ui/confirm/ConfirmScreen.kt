package com.hanan.mysavings.ui.confirm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.model.Category
import com.hanan.mysavings.domain.model.ReceiptField
import com.hanan.mysavings.ui.components.AmountTextField
import com.hanan.mysavings.ui.components.AppDatePickerDialog
import com.hanan.mysavings.ui.components.CategoryIcon
import com.hanan.mysavings.ui.components.DateField
import com.hanan.mysavings.ui.components.MySavingsTopBar
import com.hanan.mysavings.ui.components.fieldSupportingText
import com.hanan.mysavings.ui.theme.MySavingsTheme
import com.hanan.mysavings.ui.components.displayName
import java.time.LocalDate

@Composable
fun ConfirmRoute(
    prefill: ConfirmPrefill,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ConfirmViewModel = hiltViewModel<ConfirmViewModel, ConfirmViewModel.Factory> { it.create(prefill) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Saving is modelled as state (isSaved) rather than a one-off event, so it cannot be
    // lost if the screen is not collecting at the exact moment the save completes.
    LaunchedEffect(uiState.form.isSaved) {
        if (uiState.form.isSaved) onSaved()
    }

    ConfirmScreen(
        uiState = uiState,
        onBack = onBack,
        onMerchantChange = viewModel::onMerchantChange,
        onAmountChange = viewModel::onAmountChange,
        onDateChange = viewModel::onDateChange,
        onCategoryChange = viewModel::onCategoryChange,
        onNoteChange = viewModel::onNoteChange,
        onSave = viewModel::onSave,
    )
}

@Composable
fun ConfirmScreen(
    uiState: ConfirmUiState,
    onBack: () -> Unit,
    onMerchantChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onCategoryChange: (Long) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = uiState.form
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val checkHint = stringResource(R.string.scan_needs_check)
    fun hintFor(field: ReceiptField) = checkHint.takeIf { field in form.needsCheck }

    // Every way out of the form (system back, top bar arrow, Batal) asks first if it would lose input.
    val requestLeave = { if (uiState.hasChanges) showDiscardDialog = true else onBack() }
    BackHandler(enabled = uiState.hasChanges && !form.isSaved) { showDiscardDialog = true }

    Scaffold(
        modifier = modifier,
        topBar = {
            MySavingsTopBar(
                title = stringResource(if (uiState.isFromScan) R.string.confirm_title_scan else R.string.confirm_title_manual),
                onBack = requestLeave,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = form.merchant,
                onValueChange = onMerchantChange,
                label = { Text(stringResource(R.string.confirm_merchant)) },
                supportingText = fieldSupportingText(errorText = null, hintText = hintFor(ReceiptField.MERCHANT)),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            AmountTextField(
                digits = form.amountDigits,
                onDigitsChange = onAmountChange,
                label = stringResource(R.string.confirm_amount),
                isError = form.amountError,
                errorText = stringResource(R.string.confirm_error_amount),
                hintText = hintFor(ReceiptField.TOTAL),
                modifier = Modifier.fillMaxWidth(),
            )
            DateField(
                date = form.date,
                label = stringResource(R.string.confirm_date),
                isError = form.dateError,
                errorText = stringResource(R.string.confirm_error_date),
                hintText = hintFor(ReceiptField.DATE),
                onClick = { showDatePicker = true },
            )
            CategoryDropdown(
                categories = uiState.categories,
                selected = uiState.selectedCategory,
                isError = form.categoryError,
                hintText = hintFor(ReceiptField.CATEGORY),
                onSelect = onCategoryChange,
            )
            OutlinedTextField(
                value = form.note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.confirm_note)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                OutlinedButton(onClick = requestLeave, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(Modifier.width(12.dp))
                Button(onClick = onSave, enabled = !form.isSaving, modifier = Modifier.weight(1f)) {
                    if (form.isSaving) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(2.dp))
                    } else {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = form.date,
            maxDate = uiState.maxDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onDateChange(it)
                showDatePicker = false
            },
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.confirm_discard_title)) },
            text = { Text(stringResource(R.string.confirm_discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) { Text(stringResource(R.string.confirm_discard_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text(stringResource(R.string.confirm_keep_editing)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selected: Category?,
    isError: Boolean,
    hintText: String?,
    onSelect: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.displayName().orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.confirm_category)) },
            placeholder = { Text(stringResource(R.string.confirm_category_hint)) },
            leadingIcon = selected?.let { { CategoryIcon(it, size = 28.dp) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = isError,
            supportingText = fieldSupportingText(
                errorText = stringResource(R.string.confirm_error_category).takeIf { isError },
                hintText = hintText,
            ),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName()) },
                    leadingIcon = { CategoryIcon(category, size = 28.dp) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmScreenScannedPreview() {
    val today = LocalDate.of(2026, 10, 29)
    MySavingsTheme {
        ConfirmScreen(
            uiState = ConfirmUiState(
                form = ConfirmForm(
                    merchant = "Indomaret",
                    amountDigits = "87500",
                    date = today,
                    needsCheck = setOf(ReceiptField.DATE, ReceiptField.CATEGORY),
                ),
                categories = emptyList(),
                maxDate = today,
                isFromScan = true,
            ),
            onBack = {},
            onMerchantChange = {},
            onAmountChange = {},
            onDateChange = {},
            onCategoryChange = {},
            onNoteChange = {},
            onSave = {},
        )
    }
}
