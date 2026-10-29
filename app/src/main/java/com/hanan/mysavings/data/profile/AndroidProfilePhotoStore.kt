package com.hanan.mysavings.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.net.toUri
import com.hanan.mysavings.data.di.IoDispatcher
import com.hanan.mysavings.domain.repository.ProfilePhotoStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Stores the profile photo as a small square JPEG in the app's private files.
 *
 * Keeping our own copy (instead of the picked image's address) means the photo survives
 * the original being deleted from the gallery, and needs no storage permission: the
 * system photo picker grants one-time access, which is all the import needs.
 */
@Singleton
class AndroidProfilePhotoStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ProfilePhotoStore {

    private val file get() = File(context.filesDir, "profile/avatar.jpg")

    override suspend fun import(sourceUri: String): Boolean = withContext(ioDispatcher) {
        try {
            val square = decodeSquare(sourceUri.toUri())
            val target = file
            target.parentFile?.mkdirs()
            // Write next to the old photo and swap, so a failed write never leaves half a file.
            val temp = File(target.parentFile, "avatar.tmp")
            temp.outputStream().use { square.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            temp.renameTo(target)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Not an image, unreadable, or access was revoked: keep the old photo.
            false
        }
    }

    override suspend fun load(): ByteArray? = withContext(ioDispatcher) {
        file.takeIf { it.exists() }?.readBytes()
    }

    override suspend fun delete() {
        withContext(ioDispatcher) { file.delete() }
    }

    /**
     * Decodes straight to roughly [SIZE] px on the short side (a 12 MP photo never has to fit
     * in memory at full size), then cuts the centered square. ImageDecoder also applies the
     * EXIF rotation, so photos taken in portrait are not shown sideways.
     */
    private fun decodeSquare(uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val scale = SIZE.toFloat() / min(info.size.width, info.size.height)
            if (scale < 1f) {
                decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
            }
            // Hardware bitmaps cannot be cropped or compressed.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val side = min(bitmap.width, bitmap.height)
        return Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
    }

    private companion object {
        /** Plenty for a profile picture, and only tens of kilobytes as JPEG. */
        const val SIZE = 512
        const val JPEG_QUALITY = 85
    }
}
