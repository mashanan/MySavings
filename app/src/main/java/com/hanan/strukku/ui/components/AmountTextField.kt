package com.hanan.strukku.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.hanan.strukku.ui.util.groupThousands
import com.hanan.strukku.ui.util.sanitizeAmountInput

/**
 * Rupiah input. The state holds digits only ("25000"); the dots ("25.000") are added
 * visually, so the stored value never has to be re-parsed from formatted text.
 */
@Composable
fun AmountTextField(
    digits: String,
    onDigitsChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
    hintText: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = digits,
        onValueChange = { onDigitsChange(sanitizeAmountInput(it)) },
        label = { Text(label) },
        prefix = { Text("Rp") },
        singleLine = true,
        isError = isError,
        supportingText = fieldSupportingText(errorText?.takeIf { isError }, hintText),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        keyboardActions = keyboardActions,
        visualTransformation = ThousandsSeparatorTransformation,
        modifier = modifier,
    )
}

/** Inserts "." every three digits and maps the cursor between the raw and the dotted text. */
private object ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val grouped = groupThousands(raw)

        // originalToTransformed[i] = position in `grouped` of raw offset i.
        val originalToTransformed = IntArray(raw.length + 1)
        var transformedIndex = 0
        for (i in raw.indices) {
            originalToTransformed[i] = transformedIndex
            transformedIndex++
            val digitsLeft = raw.length - i - 1
            if (digitsLeft > 0 && digitsLeft % 3 == 0) transformedIndex++ // skip the dot
        }
        originalToTransformed[raw.length] = grouped.length

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = originalToTransformed[offset]

            override fun transformedToOriginal(offset: Int): Int =
                originalToTransformed.indexOfFirst { it >= offset }.takeIf { it >= 0 } ?: raw.length
        }
        return TransformedText(AnnotatedString(grouped), mapping)
    }
}
