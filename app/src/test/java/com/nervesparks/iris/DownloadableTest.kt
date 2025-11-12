package com.nervesparks.iris

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Unit tests for Downloadable data class and its sealed State interface.
 */
class DownloadableTest {

    @Test
    fun downloadable_creation_withRequiredFields() {
        val name = "llama-3.2-1b"
        val source = Uri.parse("https://example.com/model.gguf")
        val destination = File("/path/to/model.gguf")

        val downloadable = Downloadable(name, source, destination)

        assertEquals(name, downloadable.name)
        assertEquals(source, downloadable.source)
        assertEquals(destination, downloadable.destination)
    }

    @Test
    fun downloadable_equality_sameValues() {
        val name = "model"
        val source = Uri.parse("https://example.com/model.gguf")
        val destination = File("/path/to/model.gguf")

        val downloadable1 = Downloadable(name, source, destination)
        val downloadable2 = Downloadable(name, source, destination)

        assertEquals(downloadable1, downloadable2)
        assertEquals(downloadable1.hashCode(), downloadable2.hashCode())
    }

    @Test
    fun downloadable_equality_differentNames() {
        val source = Uri.parse("https://example.com/model.gguf")
        val destination = File("/path/to/model.gguf")

        val downloadable1 = Downloadable("model1", source, destination)
        val downloadable2 = Downloadable("model2", source, destination)

        assertNotEquals(downloadable1, downloadable2)
    }

    @Test
    fun downloadable_equality_differentSources() {
        val name = "model"
        val destination = File("/path/to/model.gguf")

        val downloadable1 = Downloadable(name, Uri.parse("https://example1.com/model.gguf"), destination)
        val downloadable2 = Downloadable(name, Uri.parse("https://example2.com/model.gguf"), destination)

        assertNotEquals(downloadable1, downloadable2)
    }

    @Test
    fun downloadable_equality_differentDestinations() {
        val name = "model"
        val source = Uri.parse("https://example.com/model.gguf")

        val downloadable1 = Downloadable(name, source, File("/path1/model.gguf"))
        val downloadable2 = Downloadable(name, source, File("/path2/model.gguf"))

        assertNotEquals(downloadable1, downloadable2)
    }

    @Test
    fun downloadable_copy_modifiesOnlySpecifiedFields() {
        val original = Downloadable(
            "original",
            Uri.parse("https://example.com/original.gguf"),
            File("/original/path"),
        )

        val modified = original.copy(name = "modified")

        assertEquals("modified", modified.name)
        assertEquals(original.source, modified.source)
        assertEquals(original.destination, modified.destination)
    }

    @Test
    fun downloadableState_readyState_isCorrectType() {
        val state: Downloadable.State = Downloadable.Ready

        assertTrue(state is Downloadable.Ready)
    }

    @Test
    fun downloadableState_downloadingState_containsIdAndSize() {
        val downloadId = 12345L
        val totalSize = 1024L * 1024L * 100L // 100 MB

        val state: Downloadable.State = Downloadable.Downloading(downloadId, totalSize)

        assertTrue(state is Downloadable.Downloading)
        assertEquals(downloadId, (state as Downloadable.Downloading).id)
        assertEquals(totalSize, state.totalSize)
    }

    @Test
    fun downloadableState_downloadedState_containsDownloadable() {
        val downloadable = Downloadable(
            "test-model",
            Uri.parse("https://example.com/test.gguf"),
            File("/test/path"),
        )

        val state: Downloadable.State = Downloadable.Downloaded(downloadable)

        assertTrue(state is Downloadable.Downloaded)
        assertEquals(downloadable, (state as Downloadable.Downloaded).downloadable)
    }

    @Test
    fun downloadableState_errorState_containsMessage() {
        val errorMessage = "Download failed: Network error"

        val state: Downloadable.State = Downloadable.Error(errorMessage)

        assertTrue(state is Downloadable.Error)
        assertEquals(errorMessage, (state as Downloadable.Error).message)
    }

    @Test
    fun downloadableState_stoppedState_isCorrectType() {
        val state: Downloadable.State = Downloadable.Stopped

        assertTrue(state is Downloadable.Stopped)
    }

    @Test
    fun downloadable_withLongName_handlesCorrectly() {
        val longName = "a".repeat(256)
        val source = Uri.parse("https://example.com/model.gguf")
        val destination = File("/path/to/model.gguf")

        val downloadable = Downloadable(longName, source, destination)

        assertEquals(longName, downloadable.name)
        assertEquals(256, downloadable.name.length)
    }

    @Test
    fun downloadable_withSpecialCharactersInName_handlesCorrectly() {
        val name = "model-v1.0_final (2024)"
        val source = Uri.parse("https://example.com/model.gguf")
        val destination = File("/path/to/model.gguf")

        val downloadable = Downloadable(name, source, destination)

        assertEquals(name, downloadable.name)
    }

    @Test
    fun downloadableState_downloadingWithNegativeSize_allowsUnknownSize() {
        val downloadId = 123L
        val unknownSize = -1L

        val state: Downloadable.State = Downloadable.Downloading(downloadId, unknownSize)

        assertTrue(state is Downloadable.Downloading)
        assertEquals(unknownSize, (state as Downloadable.Downloading).totalSize)
    }

    @Test
    fun downloadable_toString_containsAllFields() {
        val downloadable = Downloadable(
            "test-model",
            Uri.parse("https://example.com/test.gguf"),
            File("/test/path"),
        )

        val result = downloadable.toString()

        assertTrue(result.contains("test-model"))
        assertTrue(result.contains("https://example.com/test.gguf"))
        assertTrue(result.contains("/test/path"))
    }

    @Test
    fun downloadableState_equality_readyStates() {
        val state1: Downloadable.State = Downloadable.Ready
        val state2: Downloadable.State = Downloadable.Ready

        assertEquals(state1, state2)
    }

    @Test
    fun downloadableState_equality_downloadingStates() {
        val state1 = Downloadable.Downloading(123L, 1024L)
        val state2 = Downloadable.Downloading(123L, 1024L)

        assertEquals(state1, state2)
    }

    @Test
    fun downloadableState_equality_errorStates() {
        val state1 = Downloadable.Error("Error message")
        val state2 = Downloadable.Error("Error message")

        assertEquals(state1, state2)
    }

    @Test
    fun downloadableState_equality_stoppedStates() {
        val state1: Downloadable.State = Downloadable.Stopped
        val state2: Downloadable.State = Downloadable.Stopped

        assertEquals(state1, state2)
    }

    @Test
    fun downloadable_withEmptyName_createsSuccessfully() {
        val downloadable = Downloadable(
            "",
            Uri.parse("https://example.com/model.gguf"),
            File("/path/to/model.gguf"),
        )

        assertEquals("", downloadable.name)
    }
}
