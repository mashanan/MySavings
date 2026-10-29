package com.hanan.mysavings.ui.debts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.usecase.PaymentError
import com.hanan.mysavings.ui.components.AmountTextField
import com.hanan.mysavings.ui.components.AppDatePickerDialog
import com.hanan.mysavings.ui.components.DateField
import com.hanan.mysavings.ui.util.formatRupiah
import java.time.LocalDate

/** Everything the sheet can ask the ViewModel to do, bundled so DebtDetailScreen stays readable. */
data class PaymentSheetCallbacks(
    val onDismiss: () -> Unit,
    val onAmountChange: (String) -> Unit,
    val onDateChange: (LocalDate) -> Unit,
    val onNoteChange: (String) -> Unit,
    val onSave: () -> Unit,
)

/** Record a payment towards one debt. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSheet(
    state: PaymentSheetState,
    creditor: String,
    /** Still owed, shown as the limit when the amount is too high. */
    remaining: Long,
    callbacks: PaymentSheetCallbacks,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = callbacks.onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(if (state.isPayOff) R.string.payment_sheet_title_pay_off else R.string.payment_sheet_title, creditor),
                style = MaterialTheme.typography.titleMedium,
            )
            val amountErrorText = when (state.error) {
                PaymentError.AMOUNT_NOT_POSITIVE -> stringResource(R.string.payment_error_amount)
                PaymentError.EXCEEDS_REMAINING -> stringResource(R.string.payment_error_exceeds, formatRupiah(remaining))
                PaymentError.NOT_FOUND -> stringResource(R.string.debt_error_not_found)
                else -> null
            }
            AmountTextField(
                digits = state.amountDigits,
                onDigitsChange = callbacks.onAmountChange,
                label = stringResource(R.string.payment_amount),
                isError = amountErrorText != null,
                errorText = amountErrorText,
                modifier = Modifier.fillMaxWidth(),
            )
            DateField(
                date = state.date,
                label = stringResource(R.string.payment_date),
                isError = state.error == PaymentError.DATE_IN_FUTURE,
                errorText = stringResource(R.string.payment_error_date),
                onClick = { showDatePicker = true },
            )
            OutlinedTextField(
                value = state.note,
                onValueChange = callbacks.onNoteChange,
                label = { Text(stringResource(R.string.payment_note)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(onClick = callbacks.onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Button(onClick = callbacks.onSave, enabled = !state.isSaving) { Text(stringResource(R.string.action_save)) }
            }
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = state.date,
            maxDate = state.maxDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                callbacks.onDateChange(it)
                showDatePicker = false
            },
        )
    }
}
