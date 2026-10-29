package com.hanan.mysavings.data.di

import com.hanan.mysavings.data.ocr.MlKitReceiptRecognizer
import com.hanan.mysavings.data.ocr.ReceiptTextRecognizer
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
