package com.hanan.strukku.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.hanan.strukku.R
import com.hanan.strukku.ui.components.PlaceholderContent
import com.hanan.strukku.ui.components.StrukKuTopBar
import com.hanan.strukku.ui.theme.StrukKuTheme

// Placeholder. Phase 3 adds HomeViewModel, budget summary, category breakdown and recent transactions.
@Composable
fun HomeScreen(
    onScanClick: () -> Unit,
    onManualEntryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { StrukKuTopBar(title = stringResource(R.string.app_name)) },
        floatingActionButton = {
            // Phase 3 turns this into a menu offering scan or manual entry.
            FloatingActionButton(onClick = onScanClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.action_add_transaction),
                )
            }
        },
    ) { innerPadding ->
        PlaceholderContent(
            message = stringResource(R.string.home_placeholder),
            modifier = Modifier.padding(innerPadding),
        ) {
            Button(onClick = onScanClick) { Text(stringResource(R.string.action_scan_receipt)) }
            OutlinedButton(onClick = onManualEntryClick) { Text(stringResource(R.string.action_manual_entry)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    StrukKuTheme {
        HomeScreen(onScanClick = {}, onManualEntryClick = {})
    }
}
