package com.hanan.mysavings.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
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
import com.hanan.mysavings.domain.model.SyncStatus
import com.hanan.mysavings.domain.usecase.FinancialSummary
import com.hanan.mysavings.ui.components.MySavingsTopBar
import com.hanan.mysavings.ui.settings.AppLanguage
import com.hanan.mysavings.ui.settings.SettingsViewModel
import com.hanan.mysavings.ui.settings.labelRes
import com.hanan.mysavings.ui.theme.MySavingsTheme
import com.hanan.mysavings.ui.util.currentLocale
import com.hanan.mysavings.ui.util.formatDateTime
import com.hanan.mysavings.ui.util.formatRupiah
import kotlinx.coroutines.launch
import java.time.ZoneId

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
    // The account picker needs the Activity, which LocalContext is inside setContent.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Leaving the account also forgets the account chosen in the picker, so the next sign-in asks again.
    val forgetPickedAccount = { scope.launch { clearGoogleCredential(context) } }
    ProfileScreen(
        uiState = uiState,
        language = language,
        onEditProfile = onEditProfile,
        onSettingsClick = onSettingsClick,
        account = AccountCallbacks(
            onSignIn = {
                viewModel.onSignInStarted()
                scope.launch { viewModel.onGoogleSignInOutcome(requestGoogleIdToken(context)) }
            },
            onSyncNow = viewModel::onSyncNow,
            onSignOutClick = viewModel::onSignOutClick,
            onKeepData = {
                forgetPickedAccount()
                viewModel.onSignOutKeepData()
            },
            onEraseData = {
                forgetPickedAccount()
                viewModel.onSignOutEraseData()
            },
            onConfirmErase = viewModel::onConfirmErase,
            onDeleteAccountClick = viewModel::onDeleteAccountClick,
            onDeleteConfirmed = {
                viewModel.onDeleteAccountConfirmed()
                // Firebase only deletes an account right after its owner signed in again.
                scope.launch {
                    viewModel.onDeleteAccountReauth(requestGoogleIdToken(context))
                    forgetPickedAccount()
                }
            },
            onDismissDialog = viewModel::onDismissDialog,
        ),
    )
}

/** Everything the account card and its dialogs can ask for, bundled to keep ProfileScreen readable. */
data class AccountCallbacks(
    val onSignIn: () -> Unit = {},
    val onSyncNow: () -> Unit = {},
    val onSignOutClick: () -> Unit = {},
    val onKeepData: () -> Unit = {},
    val onEraseData: () -> Unit = {},
    val onConfirmErase: () -> Unit = {},
    val onDeleteAccountClick: () -> Unit = {},
    val onDeleteConfirmed: () -> Unit = {},
    val onDismissDialog: () -> Unit = {},
)

/** "Syncing…", "Sync failed, will retry", "Last synced 30 Oct 21:30" or "Not synced yet". */
@Composable
private fun SyncStatusLine(status: SyncStatus) {
    val (text, color) = when {
        status.isSyncing -> stringResource(R.string.sync_running) to MaterialTheme.colorScheme.onSurfaceVariant
        status.lastAttemptFailed -> stringResource(R.string.sync_failed) to MaterialTheme.colorScheme.error
        status.lastSyncedAt != null -> stringResource(
            R.string.sync_last,
            formatDateTime(status.lastSyncedAt.atZone(ZoneId.systemDefault()), currentLocale()),
        ) to MaterialTheme.colorScheme.onSurfaceVariant
        else -> stringResource(R.string.sync_never) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = color)
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    language: AppLanguage,
    onEditProfile: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    account: AccountCallbacks = AccountCallbacks(),
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
            AccountCard(
                uiState = uiState,
                callbacks = account,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
            HorizontalDivider(Modifier.padding(top = 24.dp))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_title)) },
                supportingContent = {
                    Text(stringResource(R.string.profile_settings_language, stringResource(language.labelRes())))
                },
                leadingContent = { Icon(painterResource(R.drawable.ic_settings), contentDescription = null) },
                modifier = Modifier.clickable(onClick = onSettingsClick),
            )
        }
    }

    uiState.accountDialog?.let { AccountDialogs(it, account) }
}

/** Signed out: why and how to sign in. Signed in: which account, sync, and the ways out. */
@Composable
private fun AccountCard(
    uiState: ProfileUiState,
    callbacks: AccountCallbacks,
    modifier: Modifier = Modifier,
) {
    val account = uiState.account
    val syncStatus = uiState.syncStatus
    val error = uiState.signInError
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.profile_account),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            uiState.accountMessage?.let { message ->
                Text(
                    text = stringResource(message.textRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (message == AccountMessage.ACCOUNT_DELETED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
            if (uiState.isAccountBusy) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.account_working), style = MaterialTheme.typography.bodyMedium)
                }
                return@Column
            }
            if (account == null) {
                val onSignIn = callbacks.onSignIn
                val isSigningIn = uiState.isSigningIn
                Text(stringResource(R.string.account_signed_out_body), style = MaterialTheme.typography.bodyMedium)
                error?.let {
                    Text(
                        text = stringResource(
                            if (it == SignInError.NO_ACCOUNT) R.string.account_error_no_account else R.string.account_error_failed,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(onClick = onSignIn, enabled = !isSigningIn, modifier = Modifier.fillMaxWidth()) {
                    if (isSigningIn) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Text(stringResource(R.string.account_sign_in_google))
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.account_signed_in_as, account.email ?: account.displayName.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                syncStatus?.let { SyncStatusLine(it) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = callbacks.onSignOutClick) { Text(stringResource(R.string.account_sign_out)) }
                    TextButton(onClick = callbacks.onSyncNow, enabled = syncStatus?.isSyncing != true) {
                        Text(stringResource(R.string.sync_now))
                    }
                }
                // Out of the way and in the error color: it cannot be undone.
                TextButton(onClick = callbacks.onDeleteAccountClick, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.account_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun AccountMessage.textRes(): Int = when (this) {
    AccountMessage.ACCOUNT_DELETED -> R.string.account_deleted
    AccountMessage.DELETE_NOT_CONFIRMED -> R.string.account_delete_not_confirmed
    AccountMessage.DELETE_FAILED -> R.string.account_delete_failed
}

@Composable
private fun AccountDialogs(dialog: AccountDialog, callbacks: AccountCallbacks) {
    when (dialog) {
        AccountDialog.SignOutChoice -> AlertDialog(
            onDismissRequest = callbacks.onDismissDialog,
            title = { Text(stringResource(R.string.account_sign_out_title)) },
            text = { Text(stringResource(R.string.account_sign_out_body)) },
            // Keeping is the safe default, so it is the main button; erasing is the other one.
            confirmButton = { TextButton(onClick = callbacks.onKeepData) { Text(stringResource(R.string.account_keep_data)) } },
            dismissButton = {
                TextButton(onClick = callbacks.onEraseData) {
                    Text(stringResource(R.string.account_erase_data), color = MaterialTheme.colorScheme.error)
                }
            },
        )

        is AccountDialog.EraseWarning -> AlertDialog(
            onDismissRequest = callbacks.onDismissDialog,
            title = { Text(stringResource(R.string.account_erase_warning_title)) },
            text = { Text(pluralStringResource(R.plurals.account_erase_warning_body, dialog.unsynced, dialog.unsynced)) },
            confirmButton = {
                TextButton(onClick = callbacks.onConfirmErase) {
                    Text(stringResource(R.string.account_erase_anyway), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = callbacks.onDismissDialog) { Text(stringResource(R.string.action_cancel)) } },
        )

        AccountDialog.DeleteConfirm -> AlertDialog(
            onDismissRequest = callbacks.onDismissDialog,
            title = { Text(stringResource(R.string.account_delete_title)) },
            text = { Text(stringResource(R.string.account_delete_body)) },
            confirmButton = {
                TextButton(onClick = callbacks.onDeleteConfirmed) {
                    Text(stringResource(R.string.account_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = callbacks.onDismissDialog) { Text(stringResource(R.string.action_cancel)) } },
        )
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
