package com.nervesparks.iris.data

import android.content.Context

private const val USER_PREFERENCES_NAME = "user_preferences"
private const val KEY_DEFAULT_MODEL_NAME = "default_model_name"
private const val KEY_PRIVACY_REDACTION_ENABLED = "privacy_redaction_enabled"
private const val KEY_THEME_PREFERENCE = "theme_preference"
private const val KEY_LANGUAGE_PREFERENCE = "language_preference"
private const val KEY_TEMPERATURE = "model_temperature"
private const val KEY_TOP_P = "model_top_p"
private const val KEY_TOP_K = "model_top_k"
private const val KEY_CONTEXT_LENGTH = "model_context_length"

// Default parameter values
private const val DEFAULT_TEMPERATURE = 1.0f
private const val DEFAULT_TOP_P = 0.9f
private const val DEFAULT_TOP_K = 40
private const val DEFAULT_CONTEXT_LENGTH = 2048

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

    // Get temperature parameter, defaults to DEFAULT_TEMPERATURE
    fun getTemperature(): Float {
        return sharedPreferences.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE)
    }

    // Set temperature parameter
    fun setTemperature(temperature: Float) {
        sharedPreferences.edit().putFloat(KEY_TEMPERATURE, temperature).apply()
    }

    // Get top_p parameter, defaults to DEFAULT_TOP_P
    fun getTopP(): Float {
        return sharedPreferences.getFloat(KEY_TOP_P, DEFAULT_TOP_P)
    }

    // Set top_p parameter
    fun setTopP(topP: Float) {
        sharedPreferences.edit().putFloat(KEY_TOP_P, topP).apply()
    }

    // Get top_k parameter, defaults to DEFAULT_TOP_K
    fun getTopK(): Int {
        return sharedPreferences.getInt(KEY_TOP_K, DEFAULT_TOP_K)
    }

    // Set top_k parameter
    fun setTopK(topK: Int) {
        sharedPreferences.edit().putInt(KEY_TOP_K, topK).apply()
    }

    // Get context length parameter, defaults to DEFAULT_CONTEXT_LENGTH
    fun getContextLength(): Int {
        return sharedPreferences.getInt(KEY_CONTEXT_LENGTH, DEFAULT_CONTEXT_LENGTH)
    }

    // Set context length parameter
    fun setContextLength(contextLength: Int) {
        sharedPreferences.edit().putInt(KEY_CONTEXT_LENGTH, contextLength).apply()
    }

    // Reset all parameters to default values
    fun resetParametersToDefaults() {
        sharedPreferences.edit().apply {
            putFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE)
            putFloat(KEY_TOP_P, DEFAULT_TOP_P)
            putInt(KEY_TOP_K, DEFAULT_TOP_K)
            putInt(KEY_CONTEXT_LENGTH, DEFAULT_CONTEXT_LENGTH)
            apply()
        }
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
