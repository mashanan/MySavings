package com.hanan.strukku.ui.scan

import androidx.camera.core.ImageProxy
import com.hanan.strukku.data.ocr.ReceiptTextRecognizer
import com.hanan.strukku.domain.model.ReceiptDraft
import com.hanan.strukku.domain.model.ReceiptField
import com.hanan.strukku.domain.parser.TextFragment
import com.hanan.strukku.domain.usecase.ParseReceiptUseCase
import com.hanan.strukku.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ScanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fragments = listOf(TextFragment("TOTAL 10.000", 0, 0, 100, 20))
    private val draft = ReceiptDraft("Toko", 10_000, null, null, setOf(ReceiptField.DATE, ReceiptField.CATEGORY))
    private val emptyDraft = ReceiptDraft(null, null, null, null, ReceiptField.entries.toSet())

    private val recognizer = mockk<ReceiptTextRecognizer>()
    private val parseReceipt = mockk<ParseReceiptUseCase>()
    private val image = mockk<ImageProxy>(relaxed = true)

    private val viewModel = ScanViewModel(recognizer, parseReceipt)

    @Test
    fun `starts on the camera and shows processing once a photo is taken`() {
        assertEquals(ScanUiState.Camera, viewModel.uiState.value)

        viewModel.onCaptureStarted()

        assertEquals(ScanUiState.Processing, viewModel.uiState.value)
    }

    @Test
    fun `readable receipt shows the result and releases the camera image`() {
        coEvery { recognizer.recognize(image) } returns fragments
        coEvery { parseReceipt(fragments) } returns draft

        viewModel.onImageCaptured(image)

        assertEquals(ScanUiState.Result(draft), viewModel.uiState.value)
        verify { image.close() }
    }

    @Test
    fun `photo without receipt text shows the failure state`() {
        coEvery { recognizer.recognize(image) } returns emptyList()
        coEvery { parseReceipt(emptyList()) } returns emptyDraft

        viewModel.onImageCaptured(image)

        assertEquals(ScanUiState.Failed, viewModel.uiState.value)
    }

    @Test
    fun `OCR error shows the failure state and still releases the image`() {
        coEvery { recognizer.recognize(image) } throws IllegalStateException("model not ready")

        viewModel.onImageCaptured(image)

        assertEquals(ScanUiState.Failed, viewModel.uiState.value)
        verify { image.close() }
    }

    @Test
    fun `capture error shows failure and retry returns to the camera`() {
        viewModel.onCaptureFailed()
        assertEquals(ScanUiState.Failed, viewModel.uiState.value)

        viewModel.onRetry()
        assertEquals(ScanUiState.Camera, viewModel.uiState.value)
    }
}
