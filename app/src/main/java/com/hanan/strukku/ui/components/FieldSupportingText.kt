package com.hanan.strukku.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Supporting text slot for form fields: an error wins over a hint, and a hint is drawn in
 * the warning color so "please check this scanned value" stands out without looking broken.
 */
fun fieldSupportingText(errorText: String?, hintText: String?): (@Composable () -> Unit)? = when {
    errorText != null -> { { Text(errorText) } }
    hintText != null -> { { Text(hintText, color = MaterialTheme.colorScheme.tertiary) } }
    else -> null
}
