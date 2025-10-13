package com.nervesparks.iris

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ChatScreen enum.
 * Tests enum values and their properties.
 */
class ChatScreenTest {

    @Test
    fun chatScreen_enumValues() {
        val screens = ChatScreen.values()
        
        assertEquals(7, screens.size)
        assertTrue(screens.contains(ChatScreen.Start))
        assertTrue(screens.contains(ChatScreen.Settings))
        assertTrue(screens.contains(ChatScreen.SearchResults))
        assertTrue(screens.contains(ChatScreen.ModelsScreen))
        assertTrue(screens.contains(ChatScreen.ParamsScreen))
        assertTrue(screens.contains(ChatScreen.AboutScreen))
        assertTrue(screens.contains(ChatScreen.BenchMarkScreen))
    }

    @Test
    fun chatScreen_startHasCorrectTitle() {
        assertEquals(R.string.app_name, ChatScreen.Start.title)
    }

    @Test
    fun chatScreen_settingsHasCorrectTitle() {
        assertEquals(R.string.settings_screen_title, ChatScreen.Settings.title)
    }

    @Test
    fun chatScreen_searchResultsHasCorrectTitle() {
        assertEquals(R.string.search_results_screen_title, ChatScreen.SearchResults.title)
    }

    @Test
    fun chatScreen_modelsScreenHasCorrectTitle() {
        assertEquals(R.string.models_screen_title, ChatScreen.ModelsScreen.title)
    }

    @Test
    fun chatScreen_paramsScreenHasCorrectTitle() {
        assertEquals(R.string.parameters_screen_title, ChatScreen.ParamsScreen.title)
    }

    @Test
    fun chatScreen_aboutScreenHasCorrectTitle() {
        assertEquals(R.string.about_screen_title, ChatScreen.AboutScreen.title)
    }

    @Test
    fun chatScreen_benchMarkScreenHasCorrectTitle() {
        assertEquals(R.string.benchmark_screen_title, ChatScreen.BenchMarkScreen.title)
    }

    @Test
    fun chatScreen_valueOf_returnsCorrectScreen() {
        assertEquals(ChatScreen.Start, ChatScreen.valueOf("Start"))
        assertEquals(ChatScreen.Settings, ChatScreen.valueOf("Settings"))
        assertEquals(ChatScreen.SearchResults, ChatScreen.valueOf("SearchResults"))
        assertEquals(ChatScreen.ModelsScreen, ChatScreen.valueOf("ModelsScreen"))
        assertEquals(ChatScreen.ParamsScreen, ChatScreen.valueOf("ParamsScreen"))
        assertEquals(ChatScreen.AboutScreen, ChatScreen.valueOf("AboutScreen"))
        assertEquals(ChatScreen.BenchMarkScreen, ChatScreen.valueOf("BenchMarkScreen"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun chatScreen_valueOf_throwsExceptionForInvalidValue() {
        ChatScreen.valueOf("InvalidScreen")
    }

    @Test
    fun chatScreen_allScreensHaveUniqueTitles() {
        val screens = ChatScreen.values()
        val titles = screens.map { it.title }.toSet()
        
        // All screens should have unique title resource IDs
        assertEquals(screens.size, titles.size)
    }

    @Test
    fun chatScreen_enumOrder() {
        val screens = ChatScreen.values()
        
        assertEquals(ChatScreen.Start, screens[0])
        assertEquals(ChatScreen.Settings, screens[1])
        assertEquals(ChatScreen.SearchResults, screens[2])
        assertEquals(ChatScreen.ModelsScreen, screens[3])
        assertEquals(ChatScreen.ParamsScreen, screens[4])
        assertEquals(ChatScreen.AboutScreen, screens[5])
        assertEquals(ChatScreen.BenchMarkScreen, screens[6])
    }

    @Test
    fun chatScreen_equality() {
        val screen1 = ChatScreen.Start
        val screen2 = ChatScreen.Start
        
        assertSame(screen1, screen2)
        assertEquals(screen1, screen2)
    }

    @Test
    fun chatScreen_toString_returnsName() {
        assertEquals("Start", ChatScreen.Start.toString())
        assertEquals("Settings", ChatScreen.Settings.toString())
        assertEquals("SearchResults", ChatScreen.SearchResults.toString())
        assertEquals("ModelsScreen", ChatScreen.ModelsScreen.toString())
        assertEquals("ParamsScreen", ChatScreen.ParamsScreen.toString())
        assertEquals("AboutScreen", ChatScreen.AboutScreen.toString())
        assertEquals("BenchMarkScreen", ChatScreen.BenchMarkScreen.toString())
    }
}
