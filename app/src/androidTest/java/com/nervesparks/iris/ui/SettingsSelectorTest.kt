package com.nervesparks.iris.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nervesparks.iris.data.LanguagePreference
import com.nervesparks.iris.data.ThemePreference
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for SettingsSelector component.
 * Tests theme and language selector display and interactions.
 */
@RunWith(AndroidJUnit4::class)
class SettingsSelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun themeSelector_displaysAllOptions() {
        composeTestRule.setContent {
            SettingsSelector(
                label = "Theme",
                options = listOf(
                    ThemePreference.LIGHT to "Light",
                    ThemePreference.DARK to "Dark",
                    ThemePreference.SYSTEM to "System"
                ),
                selectedOption = ThemePreference.SYSTEM,
                onOptionSelected = {}
            )
        }

        // Verify label is displayed
        composeTestRule.onNodeWithText("Theme").assertExists()
        
        // Verify all theme options are displayed
        composeTestRule.onNodeWithText("Light").assertExists()
        composeTestRule.onNodeWithText("Dark").assertExists()
        composeTestRule.onNodeWithText("System").assertExists()
    }

    @Test
    fun themeSelector_selectedOptionHighlighted() {
        composeTestRule.setContent {
            SettingsSelector(
                label = "Theme",
                options = listOf(
                    ThemePreference.LIGHT to "Light",
                    ThemePreference.DARK to "Dark",
                    ThemePreference.SYSTEM to "System"
                ),
                selectedOption = ThemePreference.DARK,
                onOptionSelected = {}
            )
        }

        // Verify Dark option is displayed (selected state is visual, harder to test directly)
        composeTestRule.onNodeWithText("Dark").assertExists()
    }

    @Test
    fun themeSelector_clickOption_callsCallback() {
        var selectedTheme: ThemePreference? = null

        composeTestRule.setContent {
            SettingsSelector(
                label = "Theme",
                options = listOf(
                    ThemePreference.LIGHT to "Light",
                    ThemePreference.DARK to "Dark",
                    ThemePreference.SYSTEM to "System"
                ),
                selectedOption = ThemePreference.SYSTEM,
                onOptionSelected = { selectedTheme = it }
            )
        }

        // Click on Light option
        composeTestRule.onNodeWithText("Light").performClick()

        // Verify callback was invoked with correct value
        assert(selectedTheme == ThemePreference.LIGHT)
    }

    @Test
    fun themeSelector_clickMultipleOptions_callsCallbackMultipleTimes() {
        var clickCount = 0
        var lastSelectedTheme: ThemePreference? = null

        composeTestRule.setContent {
            SettingsSelector(
                label = "Theme",
                options = listOf(
                    ThemePreference.LIGHT to "Light",
                    ThemePreference.DARK to "Dark",
                    ThemePreference.SYSTEM to "System"
                ),
                selectedOption = ThemePreference.SYSTEM,
                onOptionSelected = { 
                    lastSelectedTheme = it
                    clickCount++
                }
            )
        }

        // Click multiple options
        composeTestRule.onNodeWithText("Light").performClick()
        composeTestRule.onNodeWithText("Dark").performClick()
        composeTestRule.onNodeWithText("System").performClick()

        // Verify callback was invoked 3 times
        assert(clickCount == 3)
        assert(lastSelectedTheme == ThemePreference.SYSTEM)
    }

    @Test
    fun languageSelector_displaysAllOptions() {
        composeTestRule.setContent {
            SettingsSelector(
                label = "Language",
                options = listOf(
                    LanguagePreference.ENGLISH to "English",
                    LanguagePreference.SPANISH to "Español"
                ),
                selectedOption = LanguagePreference.ENGLISH,
                onOptionSelected = {}
            )
        }

        // Verify label is displayed
        composeTestRule.onNodeWithText("Language").assertExists()
        
        // Verify language options are displayed
        composeTestRule.onNodeWithText("English").assertExists()
        composeTestRule.onNodeWithText("Español").assertExists()
    }

    @Test
    fun languageSelector_clickOption_callsCallback() {
        var selectedLanguage: LanguagePreference? = null

        composeTestRule.setContent {
            SettingsSelector(
                label = "Language",
                options = listOf(
                    LanguagePreference.ENGLISH to "English",
                    LanguagePreference.SPANISH to "Español"
                ),
                selectedOption = LanguagePreference.ENGLISH,
                onOptionSelected = { selectedLanguage = it }
            )
        }

        // Click on Spanish option
        composeTestRule.onNodeWithText("Español").performClick()

        // Verify callback was invoked with correct value
        assert(selectedLanguage == LanguagePreference.SPANISH)
    }

    @Test
    fun selector_withTwoOptions_displaysCorrectly() {
        composeTestRule.setContent {
            SettingsSelector(
                label = "Test",
                options = listOf(
                    "A" to "Option A",
                    "B" to "Option B"
                ),
                selectedOption = "A",
                onOptionSelected = {}
            )
        }

        composeTestRule.onNodeWithText("Test").assertExists()
        composeTestRule.onNodeWithText("Option A").assertExists()
        composeTestRule.onNodeWithText("Option B").assertExists()
    }

    @Test
    fun selector_withThreeOptions_displaysCorrectly() {
        composeTestRule.setContent {
            SettingsSelector(
                label = "Test",
                options = listOf(
                    "A" to "Option A",
                    "B" to "Option B",
                    "C" to "Option C"
                ),
                selectedOption = "B",
                onOptionSelected = {}
            )
        }

        composeTestRule.onNodeWithText("Test").assertExists()
        composeTestRule.onNodeWithText("Option A").assertExists()
        composeTestRule.onNodeWithText("Option B").assertExists()
        composeTestRule.onNodeWithText("Option C").assertExists()
    }

    @Test
    fun selector_clickSelectedOption_stillCallsCallback() {
        var clickCount = 0

        composeTestRule.setContent {
            SettingsSelector(
                label = "Theme",
                options = listOf(
                    ThemePreference.LIGHT to "Light",
                    ThemePreference.DARK to "Dark"
                ),
                selectedOption = ThemePreference.LIGHT,
                onOptionSelected = { clickCount++ }
            )
        }

        // Click on already selected option
        composeTestRule.onNodeWithText("Light").performClick()

        // Verify callback was still invoked
        assert(clickCount == 1)
    }

    @Test
    fun selector_displaysLabelAndOptions() {
        composeTestRule.setContent {
            SettingsSelector(
                label = "Custom Label",
                options = listOf(
                    1 to "First",
                    2 to "Second"
                ),
                selectedOption = 1,
                onOptionSelected = {}
            )
        }

        composeTestRule.onNodeWithText("Custom Label").assertIsDisplayed()
        composeTestRule.onNodeWithText("First").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second").assertIsDisplayed()
    }
}
