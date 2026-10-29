package com.hanan.strukku.data.ocr

import androidx.camera.core.ImageProxy
import com.hanan.strukku.domain.parser.TextFragment

/**
 * Reads text from a camera frame. An interface so the ScanViewModel can be unit tested
 * with a fake, without a camera or ML Kit.
 */
interface ReceiptTextRecognizer {
    /** Returns every recognized text line with its position. Does not close [image]. */
    suspend fun recognize(image: ImageProxy): List<TextFragment>
}
