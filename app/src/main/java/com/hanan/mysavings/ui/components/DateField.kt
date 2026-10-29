package com.hanan.mysavings.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hanan.mysavings.R
import com.hanan.mysavings.ui.util.currentLocale
import com.hanan.mysavings.ui.util.formatShortDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Read-only field that opens a date picker when tapped anywhere. With [onClear] the date is
 * optional: an empty field shows [placeholder], and a filled one gets a clear button.
 */
@Composable
fun DateField(
    date: LocalDate?,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    errorText: String? = null,
    hintText: String? = null,
    onClear: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    // A read-only text field consumes taps itself, so listen to its press interactions instead.
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { if (it is PressInteraction.Release) onClick() }
    }
    OutlinedTextField(
        value = date?.let { formatShortDate(it, currentLocale()) }.orEmpty(),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        trailingIcon = {
            if (onClear != null && date != null) {
                IconButton(onClick = onClear) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_clear_date))
                }
            } else {
                IconButton(onClick = onClick) {
                    Icon(painterResource(R.drawable.ic_calendar), stringResource(R.string.action_pick_date))
                }
            }
        },
        isError = isError,
        supportingText = fieldSupportingText(errorText = errorText?.takeIf { isError }, hintText = hintText),
        interactionSource = interactionSource,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Material date picker limited to [minDate]..[maxDate] (either end may be open). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
) {
    // The Material date picker works in UTC milliseconds at midnight.
    val minMillis = minDate?.toUtcMillis() ?: Long.MIN_VALUE
    val maxMillis = maxDate?.toUtcMillis() ?: Long.MAX_VALUE
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toUtcMillis(),
        // Open ends reach far enough for receipts in the past and goals years ahead.
        yearRange = (minDate?.year ?: 2000)..(maxDate?.year ?: (initialDate.year + 30)),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in minMillis..maxMillis
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
