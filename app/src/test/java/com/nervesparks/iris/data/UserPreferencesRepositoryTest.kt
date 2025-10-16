package com.nervesparks.iris.data

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for UserPreferencesRepository.
 * Tests all preference get/set operations and enum conversions.
 */
class UserPreferencesRepositoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var repository: UserPreferencesRepository

    @Before
    fun setup() {
        mockContext = mock()
        mockSharedPreferences = mock()
        mockEditor = mock()
        
        // Mock the context to return our mock SharedPreferences
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        whenever(mockContext.getSharedPreferences("user_preferences", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        whenever(mockSharedPreferences.edit()).thenReturn(mockEditor)
        whenever(mockEditor.putString(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(mockEditor)
        whenever(mockEditor.putBoolean(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(mockEditor)
        
        repository = UserPreferencesRepository.getInstance(mockContext)
    }

    @Test
    fun getDefaultModelName_returnsEmptyString_whenNotSet() {
        whenever(mockSharedPreferences.getString("default_model_name", "")).thenReturn("")
        
        val result = repository.getDefaultModelName()
        
        assertEquals("", result)
    }

    @Test
    fun getDefaultModelName_returnsStoredValue() {
        val modelName = "llama-3.2-1b"
        whenever(mockSharedPreferences.getString("default_model_name", "")).thenReturn(modelName)
        
        val result = repository.getDefaultModelName()
        
        assertEquals(modelName, result)
    }

    @Test
    fun setDefaultModelName_storesValue() {
        val modelName = "gemma-2b"
        
        repository.setDefaultModelName(modelName)
        
        verify(mockEditor).putString("default_model_name", modelName)
        verify(mockEditor).apply()
    }

    @Test
    fun getPrivacyRedactionEnabled_returnsFalse_whenNotSet() {
        whenever(mockSharedPreferences.getBoolean("privacy_redaction_enabled", false)).thenReturn(false)
        
        val result = repository.getPrivacyRedactionEnabled()
        
        assertFalse(result)
    }

    @Test
    fun getPrivacyRedactionEnabled_returnsTrue_whenEnabled() {
        whenever(mockSharedPreferences.getBoolean("privacy_redaction_enabled", false)).thenReturn(true)
        
        val result = repository.getPrivacyRedactionEnabled()
        
        assertTrue(result)
    }

    @Test
    fun setPrivacyRedactionEnabled_storesValue() {
        repository.setPrivacyRedactionEnabled(true)
        
        verify(mockEditor).putBoolean("privacy_redaction_enabled", true)
        verify(mockEditor).apply()
    }

    @Test
    fun getThemePreference_returnsSystem_whenNotSet() {
        whenever(mockSharedPreferences.getString("theme_preference", ThemePreference.SYSTEM.name))
            .thenReturn(ThemePreference.SYSTEM.name)
        
        val result = repository.getThemePreference()
        
        assertEquals(ThemePreference.SYSTEM, result)
    }

    @Test
    fun getThemePreference_returnsLight_whenSet() {
        whenever(mockSharedPreferences.getString("theme_preference", ThemePreference.SYSTEM.name))
            .thenReturn(ThemePreference.LIGHT.name)
        
        val result = repository.getThemePreference()
        
        assertEquals(ThemePreference.LIGHT, result)
    }

    @Test
    fun getThemePreference_returnsDark_whenSet() {
        whenever(mockSharedPreferences.getString("theme_preference", ThemePreference.SYSTEM.name))
            .thenReturn(ThemePreference.DARK.name)
        
        val result = repository.getThemePreference()
        
        assertEquals(ThemePreference.DARK, result)
    }

    @Test
    fun getThemePreference_returnsSystem_whenInvalidValue() {
        whenever(mockSharedPreferences.getString("theme_preference", ThemePreference.SYSTEM.name))
            .thenReturn("INVALID")
        
        val result = repository.getThemePreference()
        
        assertEquals(ThemePreference.SYSTEM, result)
    }

    @Test
    fun setThemePreference_storesValue() {
        repository.setThemePreference(ThemePreference.DARK)
        
        verify(mockEditor).putString("theme_preference", ThemePreference.DARK.name)
        verify(mockEditor).apply()
    }

    @Test
    fun getLanguagePreference_returnsEnglish_whenNotSet() {
        whenever(mockSharedPreferences.getString("language_preference", LanguagePreference.ENGLISH.name))
            .thenReturn(LanguagePreference.ENGLISH.name)
        
        val result = repository.getLanguagePreference()
        
        assertEquals(LanguagePreference.ENGLISH, result)
    }

    @Test
    fun getLanguagePreference_returnsSpanish_whenSet() {
        whenever(mockSharedPreferences.getString("language_preference", LanguagePreference.ENGLISH.name))
            .thenReturn(LanguagePreference.SPANISH.name)
        
        val result = repository.getLanguagePreference()
        
        assertEquals(LanguagePreference.SPANISH, result)
    }

    @Test
    fun getLanguagePreference_returnsEnglish_whenInvalidValue() {
        whenever(mockSharedPreferences.getString("language_preference", LanguagePreference.ENGLISH.name))
            .thenReturn("INVALID")
        
        val result = repository.getLanguagePreference()
        
        assertEquals(LanguagePreference.ENGLISH, result)
    }

    @Test
    fun setLanguagePreference_storesValue() {
        repository.setLanguagePreference(LanguagePreference.SPANISH)
        
        verify(mockEditor).putString("language_preference", LanguagePreference.SPANISH.name)
        verify(mockEditor).apply()
    }

    @Test
    fun themePreference_enumValues() {
        val values = ThemePreference.values()
        
        assertEquals(3, values.size)
        assertTrue(values.contains(ThemePreference.LIGHT))
        assertTrue(values.contains(ThemePreference.DARK))
        assertTrue(values.contains(ThemePreference.SYSTEM))
    }

    @Test
    fun languagePreference_enumValues() {
        val values = LanguagePreference.values()
        
        assertEquals(2, values.size)
        assertTrue(values.contains(LanguagePreference.ENGLISH))
        assertTrue(values.contains(LanguagePreference.SPANISH))
    }

    @Test
    fun getInstance_returnsSameInstance() {
        val instance1 = UserPreferencesRepository.getInstance(mockContext)
        val instance2 = UserPreferencesRepository.getInstance(mockContext)
        
        assertSame(instance1, instance2)
    }

    @Test
    fun setDefaultModelName_withEmptyString_storesEmptyString() {
        repository.setDefaultModelName("")
        
        verify(mockEditor).putString("default_model_name", "")
        verify(mockEditor).apply()
    }

    @Test
    fun setDefaultModelName_withSpecialCharacters_storesValue() {
        val modelName = "model-v1.0_final (2024)"
        
        repository.setDefaultModelName(modelName)
        
        verify(mockEditor).putString("default_model_name", modelName)
        verify(mockEditor).apply()
    }
}
