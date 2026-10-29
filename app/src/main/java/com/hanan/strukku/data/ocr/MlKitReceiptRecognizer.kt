package com.hanan.strukku.data.ocr

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hanan.strukku.data.di.DefaultDispatcher
import com.hanan.strukku.domain.parser.TextFragment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device OCR with the bundled ML Kit Latin model. Nothing leaves the phone and the
 * photo is only held in memory; it is never written to storage.
 */
@Singleton
class MlKitReceiptRecognizer @Inject constructor(
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ReceiptTextRecognizer {

    // Creating the client loads the model, so keep a single instance for the app's lifetime.
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(image: ImageProxy): List<TextFragment> {
        // toBitmap() decodes a full-resolution photo: too slow for the main thread.
        val input = withContext(defaultDispatcher) {
            // ML Kit applies the rotation, so the returned boxes are in upright coordinates.
            InputImage.fromBitmap(image.toBitmap(), image.imageInfo.rotationDegrees)
        }
        val result = recognizer.process(input).await()

        return result.textBlocks
            .flatMap { it.lines }
            .mapNotNull { line ->
                line.boundingBox?.let { box -> TextFragment(line.text, box.left, box.top, box.right, box.bottom) }
            }
    }
}
