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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Stores the profile photo as a small square JPEG in the app's private files.
 * The source is an image from the photo picker or, after signing in, the Google account photo.
 *
 * Keeping our own copy (instead of the picked image's address) means the photo survives
 * the original being deleted from the gallery, and needs no storage permission: the
 * system photo picker grants one-time access, which is all the import needs.
 *
 * The file is named after its version ("avatar-1793375485883.jpg"), so the store always
 * knows which photo it holds without keeping that anywhere else.
 */
@Singleton
class AndroidProfilePhotoStore(
    private val context: Context,
    private val directory: File,
    private val ioDispatcher: CoroutineDispatcher,
) : ProfilePhotoStore {

    // Tests give each fake phone its own directory.
    @Inject
    constructor(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) : this(context, File(context.filesDir, "profile"), ioDispatcher)

    /** Millis of the photo on disk; `null` = none, or not looked yet. */
    private val storedVersion = MutableStateFlow<Long?>(null)

    override suspend fun import(sourceUri: String, version: Instant): Boolean = withContext(ioDispatcher) {
        try {
            val square = decodeSquare(sourceUri.toUri())
            writeFile(version.toEpochMilli()) { square.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Not an image, unreadable, or access was revoked: keep the old photo.
            false
        }
    }

    override suspend fun load(version: Instant): ByteArray? = withContext(ioDispatcher) {
        adoptLegacyFile(version.toEpochMilli())
        fileFor(version.toEpochMilli()).takeIf { it.exists() }?.readBytes()
    }

    override suspend fun delete() {
        withContext(ioDispatcher) {
            directory.listFiles()?.forEach { it.delete() }
            storedVersion.value = null
        }
    }

    override fun observeStoredVersion(): Flow<Instant?> = storedVersion
        .onStart { withContext(ioDispatcher) { storedVersion.value = scanVersion() } }
        .map { millis -> millis?.let(Instant::ofEpochMilli) }

    /**
     * For sync: the version on disk. A photo from before versioned names (phase 10) has no
     * version in its name; it is the photo the profile row describes, so it takes that version.
     */
    suspend fun storedVersion(rowVersion: Long?): Long? = withContext(ioDispatcher) {
        rowVersion?.let(::adoptLegacyFile)
        scanVersion().also { storedVersion.value = it }
    }

    /** For sync: stores a downloaded photo as it is (it was already cropped on the other phone). */
    suspend fun write(bytes: ByteArray, version: Long) = withContext(ioDispatcher) {
        writeFile(version) { it.write(bytes) }
    }

    /**
     * Writes next to the old photo, then swaps and removes the rest, so a failed write never
     * leaves half a file and there is never more than one photo.
     */
    private fun writeFile(version: Long, write: (OutputStream) -> Unit) {
        directory.mkdirs()
        val temp = File(directory, "avatar.tmp")
        temp.outputStream().use(write)
        val target = fileFor(version)
        temp.renameTo(target)
        directory.listFiles()?.filter { it != target }?.forEach { it.delete() }
        storedVersion.value = version
    }

    private fun fileFor(version: Long) = File(directory, "avatar-$version.jpg")

    private fun scanVersion(): Long? = directory.listFiles()
        ?.firstNotNullOfOrNull { VERSIONED_NAME.matchEntire(it.name)?.groupValues?.get(1)?.toLong() }

    private fun adoptLegacyFile(version: Long) {
        val legacy = File(directory, LEGACY_NAME)
        if (legacy.exists() && !fileFor(version).exists()) legacy.renameTo(fileFor(version))
    }

    /**
     * Decodes straight to roughly [SIZE] px on the short side (a 12 MP photo never has to fit
     * in memory at full size), then cuts the centered square. ImageDecoder also applies the
     * EXIF rotation, so photos taken in portrait are not shown sideways.
     */
    private fun decodeSquare(uri: Uri): Bitmap {
        val source = when (uri.scheme) {
            // The Google account photo after signing in.
            "https" -> ImageDecoder.createSource(ByteBuffer.wrap(download(uri)))
            // An image from the photo picker.
            else -> ImageDecoder.createSource(context.contentResolver, uri)
        }
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

    /** Small file, called on the IO dispatcher: a plain connection is enough, no HTTP library needed. */
    private fun download(uri: Uri): ByteArray {
        val connection = URL(uri.toString()).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        try {
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
        const val LEGACY_NAME = "avatar.jpg"
        val VERSIONED_NAME = Regex("avatar-(\\d+)\\.jpg")

        /** Plenty for a profile picture, and only tens of kilobytes as JPEG. */
        const val SIZE = 512
        const val JPEG_QUALITY = 85
    }
}
