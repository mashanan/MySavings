package com.hanan.mysavings.ui.profile

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/**
 * Turns the stored JPEG into something Compose can draw. An interface so ViewModel tests
 * on the JVM, where Android's BitmapFactory does not run, can pass a fake.
 */
fun interface ProfilePhotoDecoder {
    fun decode(bytes: ByteArray): ImageBitmap?
}

class AndroidProfilePhotoDecoder @Inject constructor() : ProfilePhotoDecoder {
    // The stored photo is a 512 px JPEG: a few milliseconds to decode, no sampling needed.
    override fun decode(bytes: ByteArray): ImageBitmap? =
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {
    @Binds
    abstract fun bindProfilePhotoDecoder(impl: AndroidProfilePhotoDecoder): ProfilePhotoDecoder
}
