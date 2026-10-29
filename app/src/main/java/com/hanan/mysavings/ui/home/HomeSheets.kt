package com.hanan.mysavings.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hanan.mysavings.R
import com.hanan.mysavings.ui.components.AmountTextField
import com.hanan.mysavings.ui.util.formatMonthYear
import com.hanan.mysavings.ui.util.parseAmount
import java.time.YearMonth

/** Lets the user pick between scanning a receipt and typing a transaction. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    onDismiss: () -> Unit,
    onScanClick: () -> Unit,
    onManualEntryClick: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.home_add_sheet_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        SheetOption(
            icon = R.drawable.ic_category_bills,
            title = stringResource(R.string.action_scan_receipt),
            description = stringResource(R.string.home_add_scan_desc),
            onClick = onScanClick,
        )
        SheetOption(
            icon = R.drawable.ic_add,
            title = stringResource(R.string.action_manual_entry),
            description = stringResource(R.string.home_add_manual_desc),
            onClick = onManualEntryClick,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SheetOption(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/**
 * Sets the overall budget for [yearMonth]. The text being typed is local UI state
 * (rememberSaveable survives rotation); only the final value goes to the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSheet(
    yearMonth: YearMonth,
    currentLimit: Long?,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var digits by rememberSaveable { mutableStateOf(currentLimit?.toString().orEmpty()) }
    var showError by rememberSaveable { mutableStateOf(false) }

    fun save() {
        val amount = parseAmount(digits)
        if (amount == null || amount <= 0) {
            showError = true
        } else {
            onSave(amount)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.budget_sheet_title, formatMonthYear(yearMonth)),
                style = MaterialTheme.typography.titleMedium,
            )
            AmountTextField(
                digits = digits,
                onDigitsChange = {
                    digits = it
                    showError = false
                },
                label = stringResource(R.string.budget_sheet_amount),
                isError = showError,
                errorText = stringResource(R.string.budget_sheet_error),
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { save() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Button(onClick = ::save) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}
