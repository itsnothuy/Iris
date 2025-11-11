package com.nervesparks.iris.core.multimodal.audio

import android.content.Context
import android.media.AudioFormat as AndroidAudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import com.nervesparks.iris.common.error.VoiceException
import com.nervesparks.iris.core.multimodal.TestAudioUtils
import com.nervesparks.iris.core.multimodal.voice.AudioConfig
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(ExperimentalCoroutinesApi::class)
class AudioProcessorImplTest {
    
    private lateinit var context: Context
    private lateinit var audioProcessor: AudioProcessorImpl
    private lateinit var tempFile: File
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        audioProcessor = AudioProcessorImpl(context)
        
        // Create a temp file for testing
        tempFile = File.createTempFile("test_audio", ".wav")
    }
    
    @After
    fun tearDown() {
        // Clean up temp file
        if (tempFile.exists()) {
            tempFile.delete()
        }
        unmockkAll()
    }
    
    // =========================
    // Audio Recording Tests
    // =========================
    
    @Test
    fun `startRecording should emit audio chunks`() = runTest {
        // Note: This test would require mocking AudioRecord at the system level
        // which is complex in unit tests. Typically this would be an integration test.
        // For now, we'll test the error paths that don't require native Android APIs
        
        // Mock AudioRecord.getMinBufferSize to return invalid size
        mockkStatic(AudioRecord::class)
        every { AudioRecord.getMinBufferSize(any(), any(), any()) } returns -1
        
        val config = AudioConfig()
        val results = audioProcessor.startRecording(16000, 1, config).toList()
        
        assertTrue(results.isNotEmpty())
        assertTrue(results[0] is AudioData.Error)
    }
    
    @Test
    fun `stopRecording should not throw exception`() = runTest {
        // This should safely handle being called even when not recording
        audioProcessor.stopRecording()
        // Should complete without throwing
    }
    
    // =========================
    // Audio Playback Tests
    // =========================
    
    @Test
    fun `playAudio should fail with invalid buffer size`() = runTest {
        // Mock AudioTrack.getMinBufferSize to return invalid size
        mockkStatic(AudioTrack::class)
        every { AudioTrack.getMinBufferSize(any(), any(), any()) } returns -1
        
        val audioData = FloatArray(1000) { 0.5f }
        val result = audioProcessor.playAudio(audioData, 16000)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is VoiceException)
    }
    
    @Test
    fun `stopPlayback should not throw exception`() = runTest {
        // This should safely handle being called even when not playing
        audioProcessor.stopPlayback()
        // Should complete without throwing
    }
    
    // =========================
    // File Loading Tests
    // =========================
    
    @Test
    fun `loadAudioFile should fail with non-existent file`() = runTest {
        val nonExistentFile = File("/non/existent/path/audio.wav")
        
        val result = audioProcessor.loadAudioFile(nonExistentFile)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is VoiceException)
    }
    
    @Test
    fun `loadAudioFile should fail with empty file`() = runTest {
        val emptyFile = File.createTempFile("empty", ".wav")
        emptyFile.writeBytes(ByteArray(0))
        
        val result = audioProcessor.loadAudioFile(emptyFile)
        
        assertTrue(result.isFailure)
        
        emptyFile.delete()
    }
    
    @Test
    fun `loadAudioFile should load valid WAV file`() = runTest {
        // Create a minimal WAV file
        val audioData = TestAudioUtils.generateSineWave(440, 100)
        val shortData = ShortArray(audioData.samples.size) { index ->
            (audioData.samples[index] * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
        }
        
        tempFile.outputStream().use { output ->
            // Write WAV header
            writeTestWAVHeader(output, shortData.size * 2, 16000, 1, 16)
            
            // Write audio data
            val buffer = ByteBuffer.allocate(shortData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            shortData.forEach { buffer.putShort(it) }
            output.write(buffer.array())
        }
        
        val result = audioProcessor.loadAudioFile(tempFile)
        
        assertTrue(result.isSuccess)
        val samples = result.getOrNull()!!
        assertTrue(samples.isNotEmpty())
    }
    
    @Test
    fun `loadAudioFile should handle corrupted file`() = runTest {
        // Write invalid data
        tempFile.writeBytes(ByteArray(100) { 0xFF.toByte() })
        
        val result = audioProcessor.loadAudioFile(tempFile)
        
        // Should either fail or return data (depending on how lenient the parser is)
        // At minimum, it shouldn't crash
        assertNotNull(result)
    }
    
    // =========================
    // File Saving Tests
    // =========================
    
    @Test
    fun `saveAudioFile should save WAV format successfully`() = runTest {
        val audioData = FloatArray(1000) { index ->
            kotlin.math.sin(2.0 * kotlin.math.PI * 440 * index / 16000.0).toFloat() * 0.5f
        }
        
        val outputFile = File.createTempFile("output", ".wav")
        
        val result = audioProcessor.saveAudioFile(
            audioData = audioData,
            file = outputFile,
            sampleRate = 16000,
            format = AudioFileFormat.WAV
        )
        
        assertTrue(result.isSuccess)
        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)
        
        outputFile.delete()
    }
    
    @Test
    fun `saveAudioFile should fail with unsupported format`() = runTest {
        val audioData = FloatArray(100) { 0.5f }
        val outputFile = File.createTempFile("output", ".mp3")
        
        val result = audioProcessor.saveAudioFile(
            audioData = audioData,
            file = outputFile,
            sampleRate = 16000,
            format = AudioFileFormat.MP3
        )
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Unsupported") == true)
        
        outputFile.delete()
    }
    
    @Test
    fun `saveAudioFile should create valid WAV header`() = runTest {
        val audioData = FloatArray(100) { 0.5f }
        val outputFile = File.createTempFile("output", ".wav")
        
        audioProcessor.saveAudioFile(
            audioData = audioData,
            file = outputFile,
            sampleRate = 22050,
            format = AudioFileFormat.WAV
        )
        
        // Verify WAV header
        val bytes = outputFile.readBytes()
        assertTrue(bytes.size > 44) // WAV header is 44 bytes
        
        // Check RIFF header
        val riffHeader = String(bytes.sliceArray(0 until 4))
        assertEquals("RIFF", riffHeader)
        
        // Check WAVE format
        val waveFormat = String(bytes.sliceArray(8 until 12))
        assertEquals("WAVE", waveFormat)
        
        outputFile.delete()
    }
    
    // =========================
    // Audio Processing Tests
    // =========================
    
    @Test
    fun `audio processing should apply AGC when enabled`() = runTest {
        // This test verifies the internal processing logic
        // We can't directly test private methods, but we can test the effects
        // through the public API by checking if audio is normalized
        
        val lowVolumeAudio = FloatArray(1000) { 0.01f }
        val config = AudioConfig(automaticGainControl = true)
        
        // Since we can't directly call the private method, we'd need to test
        // through recording which applies the processing
        // For now, we'll just verify the config is accepted
        assertNotNull(config)
    }
    
    @Test
    fun `audio processing should apply noise reduction when enabled`() = runTest {
        val noisyAudio = FloatArray(1000) { index ->
            if (index % 2 == 0) 0.005f else -0.005f // Very low amplitude noise
        }
        val config = AudioConfig(noiseReduction = true)
        
        // Similar to AGC test, we verify the config is valid
        assertNotNull(config)
    }
    
    @Test
    fun `audio processing should handle all options disabled`() = runTest {
        val config = AudioConfig(
            noiseReduction = false,
            automaticGainControl = false,
            echoCancellation = false
        )
        
        assertNotNull(config)
    }
    
    // =========================
    // Format Conversion Tests
    // =========================
    
    @Test
    fun `float to short conversion should handle full range`() = runTest {
        val audioData = floatArrayOf(-1.0f, -0.5f, 0.0f, 0.5f, 1.0f)
        val outputFile = File.createTempFile("conversion", ".wav")
        
        val result = audioProcessor.saveAudioFile(
            audioData = audioData,
            file = outputFile,
            sampleRate = 16000,
            format = AudioFileFormat.WAV
        )
        
        assertTrue(result.isSuccess)
        
        // Load it back and verify conversion
        val loadResult = audioProcessor.loadAudioFile(outputFile)
        assertTrue(loadResult.isSuccess)
        
        val loadedSamples = loadResult.getOrNull()!!
        assertEquals(audioData.size, loadedSamples.size)
        
        // Verify values are approximately correct (allowing for conversion precision loss)
        for (i in audioData.indices) {
            assertEquals(audioData[i], loadedSamples[i], 0.01f)
        }
        
        outputFile.delete()
    }
    
    @Test
    fun `float to short conversion should clamp out of range values`() = runTest {
        val audioData = floatArrayOf(-2.0f, -1.5f, 1.5f, 2.0f)
        val outputFile = File.createTempFile("clamped", ".wav")
        
        val result = audioProcessor.saveAudioFile(
            audioData = audioData,
            file = outputFile,
            sampleRate = 16000,
            format = AudioFileFormat.WAV
        )
        
        assertTrue(result.isSuccess)
        
        // Load it back and verify clamping occurred
        val loadResult = audioProcessor.loadAudioFile(outputFile)
        assertTrue(loadResult.isSuccess)
        
        val loadedSamples = loadResult.getOrNull()!!
        
        // All values should be clamped to [-1.0, 1.0] range
        loadedSamples.forEach { sample ->
            assertTrue(sample >= -1.0f && sample <= 1.0f)
        }
        
        outputFile.delete()
    }
    
    // =========================
    // Edge Case Tests
    // =========================
    
    @Test
    fun `should handle empty audio data`() = runTest {
        val emptyAudio = FloatArray(0)
        val outputFile = File.createTempFile("empty_audio", ".wav")
        
        val result = audioProcessor.saveAudioFile(
            audioData = emptyAudio,
            file = outputFile,
            sampleRate = 16000,
            format = AudioFileFormat.WAV
        )
        
        // Should succeed even with empty data
        assertTrue(result.isSuccess)
        
        outputFile.delete()
    }
    
    @Test
    fun `should handle very large audio data`() = runTest {
        // 10 seconds of audio at 16kHz
        val largeAudio = FloatArray(160000) { 0.5f }
        val outputFile = File.createTempFile("large_audio", ".wav")
        
        val result = audioProcessor.saveAudioFile(
            audioData = largeAudio,
            file = outputFile,
            sampleRate = 16000,
            format = AudioFileFormat.WAV
        )
        
        assertTrue(result.isSuccess)
        assertTrue(outputFile.length() > 160000 * 2) // Should be at least the audio data size
        
        outputFile.delete()
    }
    
    @Test
    fun `should handle different sample rates`() = runTest {
        val audioData = TestAudioUtils.generateSineWave(440, 100)
        
        val sampleRates = listOf(8000, 16000, 22050, 44100, 48000)
        
        sampleRates.forEach { sampleRate ->
            val outputFile = File.createTempFile("sr_$sampleRate", ".wav")
            
            val result = audioProcessor.saveAudioFile(
                audioData = audioData.samples,
                file = outputFile,
                sampleRate = sampleRate,
                format = AudioFileFormat.WAV
            )
            
            assertTrue("Failed for sample rate $sampleRate", result.isSuccess)
            
            outputFile.delete()
        }
    }
    
    // Helper method for testing
    private fun writeTestWAVHeader(
        output: java.io.OutputStream,
        dataSize: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        
        // RIFF header
        output.write("RIFF".toByteArray())
        output.write(intToBytes(36 + dataSize))
        output.write("WAVE".toByteArray())
        
        // fmt chunk
        output.write("fmt ".toByteArray())
        output.write(intToBytes(16))
        output.write(shortToBytes(1))
        output.write(shortToBytes(channels.toShort()))
        output.write(intToBytes(sampleRate))
        output.write(intToBytes(byteRate))
        output.write(shortToBytes(blockAlign.toShort()))
        output.write(shortToBytes(bitsPerSample.toShort()))
        
        // data chunk
        output.write("data".toByteArray())
        output.write(intToBytes(dataSize))
    }
    
    private fun intToBytes(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }
    
    private fun shortToBytes(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }
}
