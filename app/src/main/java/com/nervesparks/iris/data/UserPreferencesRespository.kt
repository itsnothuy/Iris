package com.nervesparks.iris.data

import android.content.Context

private const val USER_PREFERENCES_NAME = "user_preferences"
private const val KEY_DEFAULT_MODEL_NAME = "default_model_name"
private const val KEY_PRIVACY_REDACTION_ENABLED = "privacy_redaction_enabled"
private const val KEY_THEME_PREFERENCE = "theme_preference"
private const val KEY_LANGUAGE_PREFERENCE = "language_preference"

enum class ThemePreference {
    LIGHT, DARK, SYSTEM
}

enum class LanguagePreference {
    ENGLISH, SPANISH
}

class UserPreferencesRepository private constructor(context: Context) {

    private val sharedPreferences =
        context.applicationContext.getSharedPreferences(USER_PREFERENCES_NAME, Context.MODE_PRIVATE)

    // Get the default model name, returns empty string if not set
    fun getDefaultModelName(): String {
        return sharedPreferences.getString(KEY_DEFAULT_MODEL_NAME, "") ?: ""
    }

    // Set the default model name
    fun setDefaultModelName(modelName: String) {
        sharedPreferences.edit().putString(KEY_DEFAULT_MODEL_NAME, modelName).apply()
    }

    // Get privacy redaction enabled status, defaults to false (opt-in)
    fun getPrivacyRedactionEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_PRIVACY_REDACTION_ENABLED, false)
    }

    // Set privacy redaction enabled status
    fun setPrivacyRedactionEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_PRIVACY_REDACTION_ENABLED, enabled).apply()
    }

    // Get theme preference, defaults to SYSTEM
    fun getThemePreference(): ThemePreference {
        val themeName = sharedPreferences.getString(KEY_THEME_PREFERENCE, ThemePreference.SYSTEM.name) ?: ThemePreference.SYSTEM.name
        return try {
            ThemePreference.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            ThemePreference.SYSTEM
        }
    }

    // Set theme preference
    fun setThemePreference(theme: ThemePreference) {
        sharedPreferences.edit().putString(KEY_THEME_PREFERENCE, theme.name).apply()
    }

    // Get language preference, defaults to ENGLISH
    fun getLanguagePreference(): LanguagePreference {
        val langName = sharedPreferences.getString(KEY_LANGUAGE_PREFERENCE, LanguagePreference.ENGLISH.name) ?: LanguagePreference.ENGLISH.name
        return try {
            LanguagePreference.valueOf(langName)
        } catch (e: IllegalArgumentException) {
            LanguagePreference.ENGLISH
        }
    }

    // Set language preference
    fun setLanguagePreference(language: LanguagePreference) {
        sharedPreferences.edit().putString(KEY_LANGUAGE_PREFERENCE, language.name).apply()
    }

    companion object {
        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesRepository(context).also { INSTANCE = it }
            }
        }
    }
}