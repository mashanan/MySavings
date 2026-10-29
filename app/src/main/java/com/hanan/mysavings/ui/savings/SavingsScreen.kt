package com.hanan.mysavings.ui.savings

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
 * Savings tab. Placeholder until the savings goals phase; it has no state yet, so there is
 * no ViewModel either.
 */
@Composable
fun SavingsScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { MySavingsTopBar(title = stringResource(R.string.tab_savings)) },
    ) { innerPadding ->
        ComingSoonContent(
            icon = R.drawable.ic_tab_savings,
            body = stringResource(R.string.savings_coming_soon_body),
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SavingsScreenPreview() {
    MySavingsTheme { SavingsScreen() }
}
