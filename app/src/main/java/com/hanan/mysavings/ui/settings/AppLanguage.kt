package com.hanan.mysavings.ui.settings

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Languages the app is translated into. To add one: a values-xx folder plus an entry here. */
enum class AppLanguage(val tag: String?) {
    ENGLISH("en"),
    INDONESIAN("id"),

    /** Use the phone's language when translated, English otherwise. */
    SYSTEM(null),
    ;

    companion object {
        /** First launch shows English for everyone; people switch in Settings. */
        val DEFAULT = ENGLISH

        fun fromTag(tag: String?): AppLanguage {
            val language = tag?.let { Locale.forLanguageTag(it).language } ?: return SYSTEM
            return when (language) {
                "en" -> ENGLISH
                "id", "in" -> INDONESIAN // Java still reports Indonesian as the legacy "in"
                else -> SYSTEM
            }
        }
    }
}

/** Reads and changes the app's language. An interface so the Settings ViewModel can be unit tested. */
interface LanguageController {
    fun current(): AppLanguage

    fun set(language: AppLanguage)
}

/**
 * Uses Android's per-app language API through AppCompat (native on Android 13+, back-ported
 * on 10-12). Changing it recreates the activity, so every screen redraws in the new language.
 */
@Singleton
class AppCompatLanguageController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LanguageController {

    override fun current(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) AppLanguage.SYSTEM else AppLanguage.fromTag(locales.toLanguageTags())
    }

    override fun set(language: AppLanguage) {
        prefs(context).edit { putString(KEY_LANGUAGE, language.tag) }
        AppCompatDelegate.setApplicationLocales(
            language.tag?.let(LocaleListCompat::forLanguageTags) ?: LocaleListCompat.getEmptyLocaleList(),
        )
    }

    /**
     * Applies [AppLanguage.DEFAULT] the very first time the app opens. A flag (rather than
     * "no language set") is needed because "follow system" is also stored as "no language".
     * Must run in an Activity: on Android 13+ AppCompat needs one to reach the system service.
     */
    fun applyDefaultOnFirstLaunch() {
        if (prefs(context).getBoolean(KEY_INITIALIZED, false)) return
        prefs(context).edit { putBoolean(KEY_INITIALIZED, true) }
        if (AppCompatDelegate.getApplicationLocales().isEmpty) set(AppLanguage.DEFAULT)
    }

    companion object {
        private const val PREFS = "language"
        private const val KEY_INITIALIZED = "initialized"
        private const val KEY_LANGUAGE = "language_tag"

        private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        /**
         * A context in the app's language, for code without an Activity (workers, PDF export).
         * Android 13+ applies the per-app language to the whole process already; on 10-12 only
         * Activities get it, so the saved choice is applied here by hand.
         */
        fun localizedContext(context: Context): Context {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return context
            val tag = prefs(context).getString(KEY_LANGUAGE, null) ?: return context
            val config = Configuration(context.resources.configuration).apply { setLocales(LocaleList.forLanguageTags(tag)) }
            return context.createConfigurationContext(config)
        }
    }
}
