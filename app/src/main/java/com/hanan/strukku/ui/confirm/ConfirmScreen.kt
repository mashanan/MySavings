package com.hanan.strukku.ui.confirm

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

// Placeholder. Phase 3 adds the manual-entry form; phase 5 pre-fills it from a scanned receipt.
@Composable
fun ConfirmScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { StrukKuTopBar(title = stringResource(R.string.confirm_title), onBack = onBack) },
    ) { innerPadding ->
        PlaceholderContent(
            message = stringResource(R.string.confirm_placeholder),
            modifier = Modifier.padding(innerPadding),
        ) {
            Button(onClick = onSaved) { Text(stringResource(R.string.action_save)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmScreenPreview() {
    StrukKuTheme {
        ConfirmScreen(onBack = {}, onSaved = {})
    }
}
