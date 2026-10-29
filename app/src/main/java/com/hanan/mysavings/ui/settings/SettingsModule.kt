package com.hanan.mysavings.ui.settings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    @Binds
    abstract fun bindLanguageController(impl: AppCompatLanguageController): LanguageController
}
