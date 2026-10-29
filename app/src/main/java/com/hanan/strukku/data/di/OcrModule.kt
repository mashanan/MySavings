package com.hanan.strukku.data.di

import com.hanan.strukku.data.ocr.MlKitReceiptRecognizer
import com.hanan.strukku.data.ocr.ReceiptTextRecognizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class OcrModule {
    @Binds
    abstract fun bindReceiptTextRecognizer(impl: MlKitReceiptRecognizer): ReceiptTextRecognizer
}
