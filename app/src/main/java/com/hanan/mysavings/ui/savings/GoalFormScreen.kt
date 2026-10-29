package com.hanan.mysavings.ui.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.model.GoalCategory
import com.hanan.mysavings.domain.usecase.GoalError
import com.hanan.mysavings.ui.components.AmountTextField
import com.hanan.mysavings.ui.components.AppDatePickerDialog
import com.hanan.mysavings.ui.components.DateField
import com.hanan.mysavings.ui.components.MySavingsTopBar
import com.hanan.mysavings.ui.components.fieldSupportingText
import com.hanan.mysavings.ui.theme.MySavingsTheme
import java.time.LocalDate

@Composable
fun GoalFormRoute(
    goalId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: GoalFormViewModel = hiltViewModel<GoalFormViewModel, GoalFormViewModel.Factory> { it.create(goalId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Saving is state (isSaved), not a one-off event, so it cannot be missed while the screen is paused.
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    GoalFormScreen(
        uiState = uiState,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onTargetChange = viewModel::onTargetChange,
        onCategoryChange = viewModel::onCategoryChange,
        onTargetDateChange = viewModel::onTargetDateChange,
        onSave = viewModel::onSave,
    )
}

@Composable
fun GoalFormScreen(
    uiState: GoalFormUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onCategoryChange: (GoalCategory) -> Unit,
    onTargetDateChange: (LocalDate?) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            MySavingsTopBar(
                title = stringResource(if (uiState.isEditing) R.string.goal_form_title_edit else R.string.goal_form_title_new),
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
            if (GoalError.NOT_FOUND in uiState.errors) {
                Text(stringResource(R.string.goal_error_not_found), color = MaterialTheme.colorScheme.error)
            }
            val nameError = GoalError.NAME_BLANK in uiState.errors
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.goal_form_name)) },
                placeholder = { Text(stringResource(R.string.goal_form_name_placeholder)) },
                isError = nameError,
                supportingText = fieldSupportingText(stringResource(R.string.goal_error_name).takeIf { nameError }, null),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            AmountTextField(
                digits = uiState.targetDigits,
                onDigitsChange = onTargetChange,
                label = stringResource(R.string.goal_form_target),
                isError = GoalError.TARGET_NOT_POSITIVE in uiState.errors,
                errorText = stringResource(R.string.goal_error_target),
                imeAction = ImeAction.Done,
                modifier = Modifier.fillMaxWidth(),
            )
            DateField(
                date = uiState.targetDate,
                label = stringResource(R.string.goal_form_deadline),
                placeholder = stringResource(R.string.goal_form_no_deadline),
                isError = GoalError.TARGET_DATE_IN_PAST in uiState.errors,
                errorText = stringResource(R.string.goal_error_deadline),
                onClick = { showDatePicker = true },
                onClear = { onTargetDateChange(null) },
            )
            Text(
                text = stringResource(R.string.goal_form_category),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp).semantics { heading() },
            )
            CategoryChips(selected = uiState.category, onSelect = onCategoryChange)
            Button(
                onClick = onSave,
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
        AppDatePickerDialog(
            // Without a deadline yet, start the picker a month ahead rather than on today.
            initialDate = uiState.targetDate ?: uiState.minDate.plusMonths(1),
            minDate = uiState.minDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onTargetDateChange(it)
                showDatePicker = false
            },
        )
    }
}

/** All categories as chips that wrap onto several lines; easier to scan than a dropdown. */
@Composable
private fun CategoryChips(selected: GoalCategory, onSelect: (GoalCategory) -> Unit) {
    FlowRow(
        // selectableGroup: TalkBack announces the chips as one group with one selected item.
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GoalCategory.entries.forEach { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(stringResource(category.labelRes())) },
                leadingIcon = { Icon(painterResource(category.iconRes()), contentDescription = null) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GoalFormScreenPreview() {
    MySavingsTheme {
        GoalFormScreen(
            uiState = GoalFormUiState(
                isEditing = false,
                name = "Laptop kerja",
                targetDigits = "12000000",
                category = GoalCategory.GADGET,
                minDate = LocalDate.of(2026, 10, 29),
                errors = setOf(GoalError.TARGET_DATE_IN_PAST),
            ),
            onBack = {},
            onNameChange = {},
            onTargetChange = {},
            onCategoryChange = {},
            onTargetDateChange = {},
            onSave = {},
        )
    }
}
