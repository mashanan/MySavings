package com.hanan.mysavings.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.usecase.UpdateProfileNameUseCase
import com.hanan.mysavings.ui.components.MySavingsTopBar
import com.hanan.mysavings.ui.theme.MySavingsTheme

@Composable
fun EditProfileRoute(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Saving is state (isSaved), not a one-off event, so it cannot be missed while the screen is paused.
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    // The system photo picker: no storage permission needed, the user only shares the one image.
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onPhotoPicked(it.toString()) }
    }

    EditProfileScreen(
        uiState = uiState,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onChangePhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onRemovePhoto = viewModel::onRemovePhoto,
        onSave = viewModel::onSave,
    )
}

@Composable
fun EditProfileScreen(
    uiState: EditProfileUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { MySavingsTopBar(title = stringResource(R.string.profile_edit), onBack = onBack) },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                ProfileAvatar(photo = uiState.photo, initials = uiState.initials, size = 120.dp)
                if (uiState.isPhotoBusy) CircularProgressIndicator(Modifier.size(48.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onChangePhoto, enabled = !uiState.isPhotoBusy) {
                    Text(stringResource(R.string.edit_profile_change_photo))
                }
                if (uiState.photo != null) {
                    TextButton(onClick = onRemovePhoto, enabled = !uiState.isPhotoBusy) {
                        Text(stringResource(R.string.edit_profile_remove_photo))
                    }
                }
            }
            if (uiState.photoError) {
                Text(
                    text = stringResource(R.string.edit_profile_photo_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedTextField(
                value = uiState.name,
                // The limit is applied while typing, so the user sees exactly what will be saved.
                onValueChange = { onNameChange(it.take(UpdateProfileNameUseCase.MAX_LENGTH)) },
                label = { Text(stringResource(R.string.edit_profile_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving && !uiState.isPhotoBusy,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditProfileScreenPreview() {
    MySavingsTheme {
        EditProfileScreen(
            uiState = EditProfileUiState(isLoading = false, name = "Louise Hanan"),
            onBack = {},
            onNameChange = {},
            onChangePhoto = {},
            onRemovePhoto = {},
            onSave = {},
        )
    }
}
