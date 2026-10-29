package com.hanan.strukku.ui.scan

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.hanan.strukku.R
import com.hanan.strukku.ui.components.PlaceholderContent
import com.hanan.strukku.ui.components.StrukKuTopBar
import com.hanan.strukku.ui.theme.StrukKuTheme

// Placeholder. Phase 4 adds the CameraX preview, camera permission flow and on-device OCR.
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onReceiptCaptured: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { StrukKuTopBar(title = stringResource(R.string.scan_title), onBack = onBack) },
    ) { innerPadding ->
        PlaceholderContent(
            message = stringResource(R.string.scan_placeholder),
            modifier = Modifier.padding(innerPadding),
        ) {
            Button(onClick = onReceiptCaptured) { Text(stringResource(R.string.action_capture)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanScreenPreview() {
    StrukKuTheme {
        ScanScreen(onBack = {}, onReceiptCaptured = {})
    }
}
