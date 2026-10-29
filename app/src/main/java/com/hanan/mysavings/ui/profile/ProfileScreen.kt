package com.hanan.mysavings.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanan.mysavings.R
import com.hanan.mysavings.ui.components.MySavingsTopBar
import com.hanan.mysavings.ui.settings.AppLanguage
import com.hanan.mysavings.ui.settings.SettingsViewModel
import com.hanan.mysavings.ui.settings.labelRes
import com.hanan.mysavings.ui.theme.MySavingsTheme

/**
 * Profile tab. For now it only leads to Settings; account details and online sync come in
 * later phases. Reuses SettingsViewModel to show the chosen language, so the same state is
 * not kept in two places.
 */
@Composable
fun ProfileRoute(
    onSettingsClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    ProfileScreen(language = language, onSettingsClick = onSettingsClick)
}

@Composable
fun ProfileScreen(
    language: AppLanguage,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { MySavingsTopBar(title = stringResource(R.string.tab_profile)) },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_title)) },
                supportingContent = {
                    Text(stringResource(R.string.profile_settings_language, stringResource(language.labelRes())))
                },
                leadingContent = { Icon(painterResource(R.drawable.ic_settings), contentDescription = null) },
                modifier = Modifier.clickable(onClick = onSettingsClick),
            )
            // Not clickable yet; dimmed so it reads as "planned", not as a broken button.
            ListItem(
                headlineContent = { Text(stringResource(R.string.profile_account)) },
                supportingContent = { Text(stringResource(R.string.profile_account_soon)) },
                leadingContent = { Icon(painterResource(R.drawable.ic_tab_profile), contentDescription = null) },
                modifier = Modifier.alpha(0.6f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    MySavingsTheme {
        ProfileScreen(language = AppLanguage.INDONESIAN, onSettingsClick = {})
    }
}
