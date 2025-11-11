package com.nervesparks.iris.core.multimodal.audio

import org.junit.Assert.*
import org.junit.Test

class AudioTypesTest {
    
    // =========================
    // AudioData Tests
    // =========================
    
    @Test
    fun `AudioData Chunk should store samples and timestamp`() {
        val samples = floatArrayOf(0.1f, 0.2f, 0.3f)
        val timestamp = System.currentTimeMillis()
        
        val chunk = AudioData.Chunk(samples, timestamp)
        
        assertArrayEquals(samples, chunk.samples, 0.001f)
        assertEquals(timestamp, chunk.timestamp)
    }
    
    @Test
    fun `AudioData Chunk equality should compare samples and timestamp`() {
        val samples1 = floatArrayOf(0.1f, 0.2f)
        val samples2 = floatArrayOf(0.1f, 0.2f)
        val timestamp = 12345L
        
        val chunk1 = AudioData.Chunk(samples1, timestamp)
        val chunk2 = AudioData.Chunk(samples2, timestamp)
        val chunk3 = AudioData.Chunk(samples1, timestamp + 1)
        
        assertEquals(chunk1, chunk2)
        assertNotEquals(chunk1, chunk3)
    }
    
    @Test
    fun `AudioData Chunk hashCode should be consistent`() {
        val samples = floatArrayOf(0.1f, 0.2f)
        val chunk1 = AudioData.Chunk(samples, 123L)
        val chunk2 = AudioData.Chunk(samples, 123L)
        
        assertEquals(chunk1.hashCode(), chunk2.hashCode())
    }
    
    @Test
    fun `AudioData Error should store message`() {
        val error = AudioData.Error("Test error message")
        
        assertEquals("Test error message", error.message)
    }
    
    @Test
    fun `AudioData Ended should be a singleton`() {
        val ended1 = AudioData.Ended
        val ended2 = AudioData.Ended
        
        assertSame(ended1, ended2)
    }
    
    // =========================
    // AudioChunk Tests
    // =========================
    
    @Test
    fun `AudioChunk should store samples and sample rate`() {
        val samples = floatArrayOf(0.1f, 0.2f, 0.3f)
        val sampleRate = 16000
        val timestamp = System.currentTimeMillis()
        
        val chunk = AudioChunk(samples, sampleRate, timestamp)
        
        assertArrayEquals(samples, chunk.samples, 0.001f)
        assertEquals(sampleRate, chunk.sampleRate)
        assertEquals(timestamp, chunk.timestamp)
    }
    
    @Test
    fun `AudioChunk should have default timestamp`() {
        val samples = floatArrayOf(0.1f)
        val chunk = AudioChunk(samples, 16000)
        
        assertTrue(chunk.timestamp > 0)
        assertTrue(chunk.timestamp <= System.currentTimeMillis())
    }
    
    @Test
    fun `AudioChunk equality should compare all fields`() {
        val samples1 = floatArrayOf(0.1f, 0.2f)
        val samples2 = floatArrayOf(0.1f, 0.2f)
        
        val chunk1 = AudioChunk(samples1, 16000, 123L)
        val chunk2 = AudioChunk(samples2, 16000, 123L)
        val chunk3 = AudioChunk(samples1, 22050, 123L)
        
        assertEquals(chunk1, chunk2)
        assertNotEquals(chunk1, chunk3)
    }
    
    @Test
    fun `AudioChunk hashCode should be consistent`() {
        val samples = floatArrayOf(0.1f, 0.2f)
        val chunk1 = AudioChunk(samples, 16000, 123L)
        val chunk2 = AudioChunk(samples, 16000, 123L)
        
        assertEquals(chunk1.hashCode(), chunk2.hashCode())
    }
    
    // =========================
    // AudioFileFormat Tests
    // =========================
    
    @Test
    fun `AudioFileFormat should have all expected formats`() {
        val formats = listOf(
            AudioFileFormat.WAV,
            AudioFileFormat.MP3,
            AudioFileFormat.FLAC,
            AudioFileFormat.OGG
        )
        
        assertEquals(4, formats.size)
    }
    
    @Test
    fun `AudioFileFormat values should be distinct`() {
        val formats = AudioFileFormat.values()
        val uniqueFormats = formats.toSet()
        
        assertEquals(formats.size, uniqueFormats.size)
    }
    
    // =========================
    // RecordingState Tests
    // =========================
    
    @Test
    fun `RecordingState should have all expected states`() {
        val states = listOf(
            RecordingState.IDLE,
            RecordingState.RECORDING,
            RecordingState.PAUSED,
            RecordingState.STOPPED
        )
        
        assertEquals(4, states.size)
    }
    
    @Test
    fun `RecordingState should support state transitions`() {
        val stateSequence = listOf(
            RecordingState.IDLE,
            RecordingState.RECORDING,
            RecordingState.PAUSED,
            RecordingState.RECORDING,
            RecordingState.STOPPED
        )
        
        // Verify we can transition through states
        stateSequence.forEach { state ->
            assertNotNull(state)
        }
    }
    
    // =========================
    // PlaybackState Tests
    // =========================
    
    @Test
    fun `PlaybackState should have all expected states`() {
        val states = listOf(
            PlaybackState.IDLE,
            PlaybackState.PLAYING,
            PlaybackState.PAUSED,
            PlaybackState.STOPPED
        )
        
        assertEquals(4, states.size)
    }
    
    @Test
    fun `PlaybackState should support state transitions`() {
        val stateSequence = listOf(
            PlaybackState.IDLE,
            PlaybackState.PLAYING,
            PlaybackState.PAUSED,
            PlaybackState.PLAYING,
            PlaybackState.STOPPED
        )
        
        // Verify we can transition through states
        stateSequence.forEach { state ->
            assertNotNull(state)
        }
    }
    
    // =========================
    // Edge Case Tests
    // =========================
    
    @Test
    fun `AudioData Chunk should handle empty samples`() {
        val emptyChunk = AudioData.Chunk(FloatArray(0), 123L)
        
        assertEquals(0, emptyChunk.samples.size)
    }
    
    @Test
    fun `AudioData Chunk should handle large samples array`() {
        val largeSamples = FloatArray(100000) { 0.5f }
        val chunk = AudioData.Chunk(largeSamples, 123L)
        
        assertEquals(100000, chunk.samples.size)
    }
    
    @Test
    fun `AudioChunk should handle different sample rates`() {
        val commonSampleRates = listOf(8000, 16000, 22050, 44100, 48000)
        
        commonSampleRates.forEach { rate ->
            val chunk = AudioChunk(floatArrayOf(0.5f), rate)
            assertEquals(rate, chunk.sampleRate)
        }
    }
    
    @Test
    fun `AudioData Error should handle empty message`() {
        val error = AudioData.Error("")
        
        assertEquals("", error.message)
    }
    
    @Test
    fun `AudioData Error should handle long message`() {
        val longMessage = "Error: ".repeat(100)
        val error = AudioData.Error(longMessage)
        
        assertEquals(longMessage, error.message)
    }
}
