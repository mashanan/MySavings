package com.hanan.mysavings.ui.profile

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.mysavings.domain.usecase.ChangeProfilePhotoUseCase
import com.hanan.mysavings.domain.usecase.ObserveProfileUseCase
import com.hanan.mysavings.domain.usecase.RemoveProfilePhotoUseCase
import com.hanan.mysavings.domain.usecase.UpdateProfileNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Edits the profile. The photo is applied as soon as it is picked or removed (like in most
 * apps, there is nothing to type); the name is saved with the Save button.
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val observeProfile: ObserveProfileUseCase,
    private val updateName: UpdateProfileNameUseCase,
    private val changePhoto: ChangeProfilePhotoUseCase,
    private val removePhoto: RemoveProfilePhotoUseCase,
    private val photoDecoder: ProfilePhotoDecoder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // The name field is filled once; later changes to the stored name must not overwrite typing.
            val profile = observeProfile().first()
            _uiState.update {
                it.copy(isLoading = false, name = profile.name.orEmpty(), photo = profile.photo?.let { p -> photoDecoder.decode(p.bytes) })
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }

    /** [sourceUri] is what the system photo picker returned. */
    fun onPhotoPicked(sourceUri: String) {
        _uiState.update { it.copy(isPhotoBusy = true, photoError = false) }
        viewModelScope.launch {
            val ok = changePhoto(sourceUri)
            val photo = if (ok) observeProfile().first().photo?.let { photoDecoder.decode(it.bytes) } else _uiState.value.photo
            _uiState.update { it.copy(isPhotoBusy = false, photoError = !ok, photo = photo) }
        }
    }

    fun onRemovePhoto() {
        viewModelScope.launch {
            removePhoto()
            _uiState.update { it.copy(photo = null, photoError = false) }
        }
    }

    fun onSave() {
        val current = _uiState.value
        if (current.isLoading || current.isSaving || current.isSaved) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            updateName(current.name)
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val photo: ImageBitmap? = null,
    /** True while a picked image is being cropped and stored. */
    val isPhotoBusy: Boolean = false,
    /** The last picked file could not be used as a photo. */
    val photoError: Boolean = false,
    val isSaving: Boolean = false,
    /** Becomes true once saved; the screen reacts by navigating back. */
    val isSaved: Boolean = false,
) {
    val initials: String? get() = initialsOf(name)
}
