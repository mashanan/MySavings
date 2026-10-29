package com.hanan.mysavings.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.usecase.FinancialSummary
import com.hanan.mysavings.ui.components.MySavingsTopBar
import com.hanan.mysavings.ui.settings.AppLanguage
import com.hanan.mysavings.ui.settings.SettingsViewModel
import com.hanan.mysavings.ui.settings.labelRes
import com.hanan.mysavings.ui.theme.MySavingsTheme
import com.hanan.mysavings.ui.util.formatRupiah

/**
 * Profile tab. The chosen language comes from SettingsViewModel, so that state is not kept
 * in two places; everything else from ProfileViewModel.
 */
@Composable
fun ProfileRoute(
    onEditProfile: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language by settingsViewModel.language.collectAsStateWithLifecycle()
    ProfileScreen(uiState = uiState, language = language, onEditProfile = onEditProfile, onSettingsClick = onSettingsClick)
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    language: AppLanguage,
    onEditProfile: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { MySavingsTopBar(title = stringResource(R.string.tab_profile)) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()),
        ) {
            ProfileHeader(uiState, onEditProfile)
            uiState.summary?.let { SummaryCard(it, Modifier.padding(horizontal = 16.dp)) }
            HorizontalDivider(Modifier.padding(top = 24.dp))
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

@Composable
private fun ProfileHeader(uiState: ProfileUiState, onEditProfile: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProfileAvatar(photo = uiState.photo, initials = uiState.initials)
        Text(
            text = uiState.name ?: stringResource(R.string.profile_add_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (uiState.name == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.profile_local_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onEditProfile) {
            Icon(painterResource(R.drawable.ic_edit), contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.profile_edit))
        }
    }
}

/** One headline number from each of the other tabs. */
@Composable
private fun SummaryCard(summary: FinancialSummary, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.profile_summary_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            SummaryRow(stringResource(R.string.profile_summary_spent), summary.spentThisMonth)
            SummaryRow(stringResource(R.string.profile_summary_saved), summary.totalSaved)
            SummaryRow(stringResource(R.string.profile_summary_debt), summary.debtRemaining)
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: Long) {
    // One TalkBack stop per row: "Total tabungan, Rp4.500.000".
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(formatRupiah(amount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    MySavingsTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                name = "Louise Hanan",
                initials = "LH",
                summary = FinancialSummary(spentThisMonth = 1_650_000, totalSaved = 4_500_000, debtRemaining = 8_500_000),
            ),
            language = AppLanguage.INDONESIAN,
            onEditProfile = {},
            onSettingsClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenEmptyPreview() {
    MySavingsTheme {
        ProfileScreen(uiState = ProfileUiState(), language = AppLanguage.ENGLISH, onEditProfile = {}, onSettingsClick = {})
    }
}
