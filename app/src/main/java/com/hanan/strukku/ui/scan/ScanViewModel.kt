package com.hanan.strukku.ui.scan

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.strukku.data.ocr.ReceiptTextRecognizer
import com.hanan.strukku.domain.model.ReceiptDraft
import com.hanan.strukku.domain.usecase.ParseReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the scan flow: camera -> processing -> result or failure. Camera permission and
 * binding stay in the UI layer because they are tied to the Activity and its lifecycle.
 */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val recognizer: ReceiptTextRecognizer,
    private val parseReceipt: ParseReceiptUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Camera)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun onCaptureStarted() {
        _uiState.value = ScanUiState.Processing
    }

    fun onImageCaptured(image: ImageProxy) {
        viewModelScope.launch {
            val draft = try {
                parseReceipt(recognizer.recognize(image))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // OCR failures are shown as "not readable"; the user can retry or type it in.
                null
            } finally {
                // The camera has a small buffer of frames; an unclosed image blocks the next capture.
                image.close()
            }
            _uiState.value = if (draft == null || draft.isEmpty) ScanUiState.Failed else ScanUiState.Result(draft)
        }
    }

    fun onCaptureFailed() {
        _uiState.value = ScanUiState.Failed
    }

    fun onRetry() {
        _uiState.value = ScanUiState.Camera
    }
}

sealed interface ScanUiState {
    data object Camera : ScanUiState
    data object Processing : ScanUiState
    data class Result(val draft: ReceiptDraft) : ScanUiState
    data object Failed : ScanUiState
}
