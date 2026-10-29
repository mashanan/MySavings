package com.hanan.strukku.ui.confirm

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanan.strukku.R
import com.hanan.strukku.domain.model.Category
import com.hanan.strukku.ui.components.AmountTextField
import com.hanan.strukku.ui.components.CategoryIcon
import com.hanan.strukku.ui.components.StrukKuTopBar
import com.hanan.strukku.ui.theme.StrukKuTheme
import com.hanan.strukku.ui.util.formatShortDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun ConfirmRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ConfirmViewModel = hiltViewModel(),
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

    Scaffold(
        modifier = modifier,
        topBar = { StrukKuTopBar(title = stringResource(R.string.confirm_title_manual), onBack = onBack) },
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
                modifier = Modifier.fillMaxWidth(),
            )
            DateField(
                date = form.date,
                isError = form.dateError,
                onClick = { showDatePicker = true },
            )
            CategoryDropdown(
                categories = uiState.categories,
                selected = uiState.selectedCategory,
                isError = form.categoryError,
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
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
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
        TransactionDatePicker(
            initialDate = form.date,
            maxDate = uiState.maxDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onDateChange(it)
                showDatePicker = false
            },
        )
    }
}

/** Read-only field that opens the date picker when tapped anywhere. */
@Composable
private fun DateField(date: LocalDate, isError: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    // A read-only text field consumes taps itself, so listen to its press interactions instead.
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { if (it is PressInteraction.Release) onClick() }
    }
    OutlinedTextField(
        value = formatShortDate(date),
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.confirm_date)) },
        trailingIcon = {
            IconButton(onClick = onClick) {
                Icon(painterResource(R.drawable.ic_calendar), stringResource(R.string.confirm_pick_date))
            }
        },
        isError = isError,
        supportingText = if (isError) ({ Text(stringResource(R.string.confirm_error_date)) }) else null,
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selected: Category?,
    isError: Boolean,
    onSelect: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.confirm_category)) },
            placeholder = { Text(stringResource(R.string.confirm_category_hint)) },
            leadingIcon = selected?.let { { CategoryIcon(it, size = 28.dp) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = isError,
            supportingText = if (isError) ({ Text(stringResource(R.string.confirm_error_category)) }) else null,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDatePicker(
    initialDate: LocalDate,
    maxDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    // The Material date picker works in UTC milliseconds at midnight.
    val maxMillis = maxDate.toUtcMillis()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toUtcMillis(),
        yearRange = 2000..maxDate.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= maxMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(it.toUtcLocalDate()) } ?: onDismiss() },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Preview(showBackground = true)
@Composable
private fun ConfirmScreenPreview() {
    val today = LocalDate.of(2026, 10, 29)
    StrukKuTheme {
        ConfirmScreen(
            uiState = ConfirmUiState(
                form = ConfirmForm(merchant = "Indomaret", amountDigits = "87500", date = today, amountError = false),
                categories = emptyList(),
                maxDate = today,
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
