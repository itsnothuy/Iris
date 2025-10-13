package com.nervesparks.iris.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Instrumented tests for UserPreferencesRepository.
 * Tests theme and language preference persistence.
 */
@RunWith(AndroidJUnit4::class)
class UserPreferencesRepositoryTest {
    
    private lateinit var context: Context
    private lateinit var repository: UserPreferencesRepository
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear any existing preferences for a clean test environment
        context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        repository = UserPreferencesRepository.getInstance(context)
    }
    
    @After
    fun teardown() {
        // Clean up preferences after tests
        context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
    
    @Test
    fun themePreference_defaultsToSystem() {
        val theme = repository.getThemePreference()
        assertEquals(ThemePreference.SYSTEM, theme)
    }
    
    @Test
    fun themePreference_savesLight() {
        repository.setThemePreference(ThemePreference.LIGHT)
        val theme = repository.getThemePreference()
        assertEquals(ThemePreference.LIGHT, theme)
    }
    
    @Test
    fun themePreference_savesDark() {
        repository.setThemePreference(ThemePreference.DARK)
        val theme = repository.getThemePreference()
        assertEquals(ThemePreference.DARK, theme)
    }
    
    @Test
    fun themePreference_savesSystem() {
        repository.setThemePreference(ThemePreference.SYSTEM)
        val theme = repository.getThemePreference()
        assertEquals(ThemePreference.SYSTEM, theme)
    }
    
    @Test
    fun themePreference_persistsAcrossInstances() {
        repository.setThemePreference(ThemePreference.DARK)
        
        // Create a new repository instance
        val newRepository = UserPreferencesRepository.getInstance(context)
        val theme = newRepository.getThemePreference()
        
        assertEquals(ThemePreference.DARK, theme)
    }
    
    @Test
    fun languagePreference_defaultsToEnglish() {
        val language = repository.getLanguagePreference()
        assertEquals(LanguagePreference.ENGLISH, language)
    }
    
    @Test
    fun languagePreference_savesEnglish() {
        repository.setLanguagePreference(LanguagePreference.ENGLISH)
        val language = repository.getLanguagePreference()
        assertEquals(LanguagePreference.ENGLISH, language)
    }
    
    @Test
    fun languagePreference_savesSpanish() {
        repository.setLanguagePreference(LanguagePreference.SPANISH)
        val language = repository.getLanguagePreference()
        assertEquals(LanguagePreference.SPANISH, language)
    }
    
    @Test
    fun languagePreference_persistsAcrossInstances() {
        repository.setLanguagePreference(LanguagePreference.SPANISH)
        
        // Create a new repository instance
        val newRepository = UserPreferencesRepository.getInstance(context)
        val language = newRepository.getLanguagePreference()
        
        assertEquals(LanguagePreference.SPANISH, language)
    }
    
    @Test
    fun themeAndLanguage_canBothBeSaved() {
        repository.setThemePreference(ThemePreference.DARK)
        repository.setLanguagePreference(LanguagePreference.SPANISH)
        
        assertEquals(ThemePreference.DARK, repository.getThemePreference())
        assertEquals(LanguagePreference.SPANISH, repository.getLanguagePreference())
    }
    
    @Test
    fun preferences_independentOfEachOther() {
        // Set theme
        repository.setThemePreference(ThemePreference.LIGHT)
        assertEquals(ThemePreference.LIGHT, repository.getThemePreference())
        
        // Set language
        repository.setLanguagePreference(LanguagePreference.SPANISH)
        
        // Theme should still be LIGHT
        assertEquals(ThemePreference.LIGHT, repository.getThemePreference())
        assertEquals(LanguagePreference.SPANISH, repository.getLanguagePreference())
    }
    
    @Test
    fun privacyRedaction_defaultsToFalse() {
        val enabled = repository.getPrivacyRedactionEnabled()
        assertFalse(enabled)
    }
    
    @Test
    fun allPreferences_canBeSetAndRetrieved() {
        repository.setThemePreference(ThemePreference.DARK)
        repository.setLanguagePreference(LanguagePreference.SPANISH)
        repository.setPrivacyRedactionEnabled(true)
        repository.setDefaultModelName("test-model")
        
        assertEquals(ThemePreference.DARK, repository.getThemePreference())
        assertEquals(LanguagePreference.SPANISH, repository.getLanguagePreference())
        assertTrue(repository.getPrivacyRedactionEnabled())
        assertEquals("test-model", repository.getDefaultModelName())
    }
}
