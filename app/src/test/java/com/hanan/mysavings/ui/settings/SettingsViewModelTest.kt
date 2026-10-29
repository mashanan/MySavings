package com.hanan.mysavings.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {

    /** Records what the screen asked for, like the real controller minus Android. */
    private class FakeLanguageController(var language: AppLanguage) : LanguageController {
        val changes = mutableListOf<AppLanguage>()
        override fun current() = language
        override fun set(language: AppLanguage) {
            this.language = language
            changes += language
        }
    }

    @Test
    fun `shows the language currently in use`() {
        val viewModel = SettingsViewModel(FakeLanguageController(AppLanguage.INDONESIAN))

        assertEquals(AppLanguage.INDONESIAN, viewModel.language.value)
    }

    @Test
    fun `selecting a language applies it`() {
        val controller = FakeLanguageController(AppLanguage.ENGLISH)
        val viewModel = SettingsViewModel(controller)

        viewModel.onSelectLanguage(AppLanguage.INDONESIAN)
        viewModel.onSelectLanguage(AppLanguage.SYSTEM)

        assertEquals(listOf(AppLanguage.INDONESIAN, AppLanguage.SYSTEM), controller.changes)
        assertEquals(AppLanguage.SYSTEM, viewModel.language.value)
    }

    @Test
    fun `re-selecting the current language does nothing, so the screen is not recreated`() {
        val controller = FakeLanguageController(AppLanguage.ENGLISH)

        SettingsViewModel(controller).onSelectLanguage(AppLanguage.ENGLISH)

        assertEquals(emptyList<AppLanguage>(), controller.changes)
    }

    @Test
    fun `language tags map to app languages, including the legacy Indonesian code`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-GB"))
        assertEquals(AppLanguage.INDONESIAN, AppLanguage.fromTag("id"))
        assertEquals(AppLanguage.INDONESIAN, AppLanguage.fromTag("in"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(null))
        // A language the app has no translation for falls back to following the system.
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("fr"))
    }

    @Test
    fun `the default for new users is English`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.DEFAULT)
    }
}
