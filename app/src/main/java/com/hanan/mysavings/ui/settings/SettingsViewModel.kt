package com.hanan.mysavings.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val languageController: LanguageController,
) : ViewModel() {

    private val _language = MutableStateFlow(languageController.current())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun onSelectLanguage(language: AppLanguage) {
        // Re-selecting the current language would still recreate the activity; skip it.
        if (language == _language.value) return
        _language.value = language
        languageController.set(language)
    }
}
