package com.hanan.mysavings.ui.debts

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.hanan.mysavings.R
import com.hanan.mysavings.ui.components.ComingSoonContent
import com.hanan.mysavings.ui.components.MySavingsTopBar
import com.hanan.mysavings.ui.theme.MySavingsTheme

/**
 * Debts tab. Placeholder until the debts phase; it has no state yet, so there is no
 * ViewModel either.
 */
@Composable
fun DebtsScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { MySavingsTopBar(title = stringResource(R.string.tab_debts)) },
    ) { innerPadding ->
        ComingSoonContent(
            icon = R.drawable.ic_calendar,
            body = stringResource(R.string.debts_coming_soon_body),
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DebtsScreenPreview() {
    MySavingsTheme { DebtsScreen() }
}
