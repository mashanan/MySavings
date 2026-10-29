package com.hanan.mysavings.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanan.mysavings.R
import com.hanan.mysavings.ui.components.MySavingsTopBar
import com.hanan.mysavings.ui.theme.MySavingsTheme

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    SettingsScreen(language = language, onBack = onBack, onSelectLanguage = viewModel::onSelectLanguage)
}

@Composable
fun SettingsScreen(
    language: AppLanguage,
    onBack: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { MySavingsTopBar(title = stringResource(R.string.settings_title), onBack = onBack) },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() },
            )
            // selectableGroup + Role.RadioButton: TalkBack announces "1 of 3, selected".
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { option ->
                    LanguageRow(
                        label = stringResource(option.labelRes()),
                        selected = option == language,
                        onClick = { onSelectLanguage(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The row handles the click; the radio button is only the visual state.
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
    }
}

private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.ENGLISH -> R.string.language_english
    AppLanguage.INDONESIAN -> R.string.language_indonesian
    AppLanguage.SYSTEM -> R.string.language_system
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MySavingsTheme {
        SettingsScreen(language = AppLanguage.ENGLISH, onBack = {}, onSelectLanguage = {})
    }
}
