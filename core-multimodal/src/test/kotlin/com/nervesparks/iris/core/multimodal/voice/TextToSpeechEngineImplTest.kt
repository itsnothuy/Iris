package com.nervesparks.iris.core.multimodal.voice

import android.content.Context
import com.nervesparks.iris.app.events.EventBus
import com.nervesparks.iris.app.events.IrisEvent
import com.nervesparks.iris.common.error.VoiceException
import com.nervesparks.iris.common.models.HardwareCapability
import com.nervesparks.iris.core.hw.DeviceProfile
import com.nervesparks.iris.core.hw.DeviceProfileProvider
import com.nervesparks.iris.core.multimodal.audio.AudioData
import com.nervesparks.iris.core.multimodal.audio.AudioProcessor
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class TextToSpeechEngineImplTest {
    
    private lateinit var context: Context
    private lateinit var audioProcessor: AudioProcessor
    private lateinit var deviceProfileProvider: DeviceProfileProvider
    private lateinit var eventBus: EventBus
    private lateinit var ttsEngine: TextToSpeechEngineImpl
    
    private val testVoice = VoiceDescriptor(
        id = "en-us-male",
        name = "English US Male",
        language = "en",
        gender = VoiceGender.MALE,
        style = VoiceStyle.NEUTRAL
    )
    
    private val testModel = TTSModelDescriptor(
        id = "test-piper-en",
        name = "Test Piper English",
        description = "Test TTS model",
        supportedLanguages = listOf("en", "es"),
        supportedVoices = listOf(testVoice),
        audioFormat = AudioFormat(
            sampleRate = 22050,
            channels = 1,
            bitDepth = 16,
            encoding = AudioEncoding.PCM_16BIT
        ),
        memoryRequirements = MemoryRequirements(
            minRAM = 1L * 1024 * 1024 * 1024, // 1GB
            recommendedRAM = 2L * 1024 * 1024 * 1024, // 2GB
            modelSize = 30L * 1024 * 1024 // 30MB
        ),
        supportedBackends = listOf(TTSBackend.CPU, TTSBackend.GPU),
        quality = 0.88f,
        fileSize = 30L * 1024 * 1024
    )
    
    private val testDeviceProfile = DeviceProfile(
        deviceModel = "Test Device",
        androidVersion = 13,
        manufacturer = "Test Manufacturer",
        totalRAM = 8L * 1024 * 1024 * 1024, // 8GB
        availableRAM = 4L * 1024 * 1024 * 1024, // 4GB
        cpuInfo = "Test CPU",
        gpuInfo = "Test GPU",
        capabilities = setOf(
            HardwareCapability.OPENCL,
            HardwareCapability.GPU_COMPUTE
        )
    )
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        audioProcessor = mockk(relaxed = true)
        deviceProfileProvider = mockk(relaxed = true)
        eventBus = mockk(relaxed = true)
        
        // Mock context file paths
        val externalDir = mockk<File>()
        every { context.getExternalFilesDir(null) } returns externalDir
        every { externalDir.absolutePath } returns "/data/app/files"
        
        // Mock device profile
        every { deviceProfileProvider.getDeviceProfile() } returns testDeviceProfile
        
        // Mock event bus
        coEvery { eventBus.emit(any()) } just Runs
        
        // Mock audio playback
        coEvery { audioProcessor.playAudio(any(), any()) } returns Result.success(Unit)
        coEvery { audioProcessor.stopPlayback() } just Runs
        
        // Create File mock
        mockkConstructor(File::class)
        every { anyConstructed<File>().exists() } returns true
        every { anyConstructed<File>().length() } returns 100L
        
        ttsEngine = TextToSpeechEngineImpl(
            audioProcessor = audioProcessor,
            deviceProfileProvider = deviceProfileProvider,
            eventBus = eventBus,
            context = context
        )
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    // ====================
    // Model Loading Tests
    // ====================
    
    @Test
    fun `isModelLoaded should return false initially`() = runTest {
        assertFalse(ttsEngine.isModelLoaded())
    }
    
    @Test
    fun `getCurrentModel should return null initially`() = runTest {
        assertNull(ttsEngine.getCurrentModel())
    }
    
    @Test
    fun `loadTTSModel should successfully load valid model`() = runTest {
        val result = ttsEngine.loadTTSModel(testModel)
        
        assertTrue(result.isSuccess)
        assertTrue(ttsEngine.isModelLoaded())
        assertEquals(testModel, ttsEngine.getCurrentModel())
        
        coVerify { eventBus.emit(match { it is IrisEvent.TTSModelLoadStarted }) }
        coVerify { eventBus.emit(match { it is IrisEvent.TTSModelLoadCompleted }) }
    }
    
    @Test
    fun `loadTTSModel should emit load started event`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        coVerify { 
            eventBus.emit(match<IrisEvent.TTSModelLoadStarted> { 
                it.modelId == testModel.id 
            }) 
        }
    }
    
    @Test
    fun `loadTTSModel should emit load completed event on success`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        coVerify { 
            eventBus.emit(match<IrisEvent.TTSModelLoadCompleted> { 
                it.modelId == testModel.id 
            }) 
        }
    }
    
    @Test
    fun `loadTTSModel should fail with insufficient RAM`() = runTest {
        val lowRamProfile = testDeviceProfile.copy(
            totalRAM = 500L * 1024 * 1024 // 500MB
        )
        every { deviceProfileProvider.getDeviceProfile() } returns lowRamProfile
        
        val result = ttsEngine.loadTTSModel(testModel)
        
        assertTrue(result.isFailure)
        assertFalse(ttsEngine.isModelLoaded())
        
        coVerify { eventBus.emit(match { it is IrisEvent.TTSModelLoadFailed }) }
    }
    
    @Test
    fun `loadTTSModel should handle already loaded model`() = runTest {
        // Load first time
        ttsEngine.loadTTSModel(testModel)
        
        // Load again
        val result = ttsEngine.loadTTSModel(testModel)
        
        assertTrue(result.isSuccess)
        assertTrue(ttsEngine.isModelLoaded())
    }
    
    // =========================
    // Speech Synthesis Tests
    // =========================
    
    @Test
    fun `isSpeaking should return false initially`() = runTest {
        assertFalse(ttsEngine.isSpeaking())
    }
    
    @Test
    fun `synthesizeSpeech should fail without loaded model`() = runTest {
        val result = ttsEngine.synthesizeSpeech("Hello world")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is VoiceException)
    }
    
    @Test
    fun `synthesizeSpeech should fail with text too long`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        val longText = "a".repeat(6000) // Exceeds MAX_TEXT_LENGTH of 5000
        val result = ttsEngine.synthesizeSpeech(longText)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("too long") == true)
    }
    
    @Test
    fun `synthesizeSpeech should generate audio for valid text`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        val result = ttsEngine.synthesizeSpeech("Hello world")
        
        assertTrue(result.isSuccess)
        val audioData = result.getOrNull()
        assertNotNull(audioData)
        assertTrue(audioData is AudioData.Chunk)
        
        val chunk = audioData as AudioData.Chunk
        assertTrue(chunk.samples.isNotEmpty())
    }
    
    @Test
    fun `synthesizeSpeech should generate longer audio for longer text`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        val shortResult = ttsEngine.synthesizeSpeech("Hi")
        val longResult = ttsEngine.synthesizeSpeech("Hello world, this is a longer sentence")
        
        assertTrue(shortResult.isSuccess)
        assertTrue(longResult.isSuccess)
        
        val shortSamples = (shortResult.getOrNull() as AudioData.Chunk).samples.size
        val longSamples = (longResult.getOrNull() as AudioData.Chunk).samples.size
        
        assertTrue(longSamples > shortSamples)
    }
    
    @Test
    fun `synthesizeSpeech should respect custom parameters`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        val parameters = SpeechParameters(
            speakingRate = 1.5f,
            pitch = 1.2f,
            volume = 0.8f,
            voice = testVoice
        )
        
        val result = ttsEngine.synthesizeSpeech("Hello", parameters)
        
        assertTrue(result.isSuccess)
    }
    
    // =========================
    // Streaming Synthesis Tests
    // =========================
    
    @Test
    fun `streamSpeech should fail without loaded model`() = runTest {
        try {
            ttsEngine.streamSpeech("Hello world").toList()
            fail("Expected VoiceException")
        } catch (e: VoiceException) {
            assertTrue(e.message?.contains("No TTS model loaded") == true)
        }
    }
    
    @Test
    fun `streamSpeech should emit audio chunks for text`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        val chunks = ttsEngine.streamSpeech("Hello world").toList()
        
        assertTrue(chunks.isNotEmpty())
        chunks.forEach { chunk ->
            assertTrue(chunk.samples.isNotEmpty())
            assertEquals(testModel.audioFormat.sampleRate, chunk.sampleRate)
        }
    }
    
    @Test
    fun `streamSpeech should split long text into multiple chunks`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        // Create text longer than CHUNK_SIZE (500 characters)
        val longText = "Hello world. ".repeat(50) // ~650 characters
        val chunks = ttsEngine.streamSpeech(longText).toList()
        
        assertTrue(chunks.size > 1)
    }
    
    @Test
    fun `streamSpeech should fail with text too long`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        val longText = "a".repeat(6000)
        try {
            ttsEngine.streamSpeech(longText).toList()
            fail("Expected VoiceException")
        } catch (e: VoiceException) {
            assertTrue(e.message?.contains("too long") == true)
        }
    }
    
    // =========================
    // Speech Playback Tests
    // =========================
    
    @Test
    fun `speak should fail when already speaking`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        // Mock playback to take time
        coEvery { audioProcessor.playAudio(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(Unit)
        }
        
        // Start speaking (don't wait)
        val job = kotlinx.coroutines.launch {
            ttsEngine.speak("First message")
        }
        
        // Give it time to start
        kotlinx.coroutines.delay(10)
        
        // Try to speak again
        val result = ttsEngine.speak("Second message")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Already speaking") == true)
        
        job.cancel()
    }
    
    @Test
    fun `speak should synthesize and play audio`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        val result = ttsEngine.speak("Hello world")
        
        assertTrue(result.isSuccess)
        coVerify { audioProcessor.playAudio(any(), testModel.audioFormat.sampleRate) }
    }
    
    @Test
    fun `speak should reset speaking state after completion`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        ttsEngine.speak("Hello")
        
        assertFalse(ttsEngine.isSpeaking())
    }
    
    @Test
    fun `speak should handle playback failure`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        coEvery { audioProcessor.playAudio(any(), any()) } returns Result.failure(
            VoiceException("Playback failed")
        )
        
        val result = ttsEngine.speak("Hello")
        
        assertTrue(result.isFailure)
        assertFalse(ttsEngine.isSpeaking())
    }
    
    @Test
    fun `stopSpeaking should stop active speech`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        // Mock long playback
        coEvery { audioProcessor.playAudio(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(1000)
            Result.success(Unit)
        }
        
        // Start speaking
        val job = kotlinx.coroutines.launch {
            ttsEngine.speak("Long message")
        }
        
        // Give it time to start
        kotlinx.coroutines.delay(10)
        
        val result = ttsEngine.stopSpeaking()
        
        assertTrue(result)
        coVerify { audioProcessor.stopPlayback() }
        assertFalse(ttsEngine.isSpeaking())
        
        job.cancel()
    }
    
    @Test
    fun `stopSpeaking should return false when not speaking`() = runTest {
        val result = ttsEngine.stopSpeaking()
        
        assertFalse(result)
    }
    
    // =========================
    // Pause/Resume Tests
    // =========================
    
    @Test
    fun `pause should return false when not speaking`() = runTest {
        val result = ttsEngine.pause()
        
        assertFalse(result)
    }
    
    @Test
    fun `pause should pause active speech`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        // Mock long playback
        coEvery { audioProcessor.playAudio(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(1000)
            Result.success(Unit)
        }
        
        // Start speaking
        val job = kotlinx.coroutines.launch {
            ttsEngine.speak("Long message")
        }
        
        // Give it time to start
        kotlinx.coroutines.delay(10)
        
        val result = ttsEngine.pause()
        
        assertTrue(result)
        
        job.cancel()
    }
    
    @Test
    fun `pause should return false when already paused`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        coEvery { audioProcessor.playAudio(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(1000)
            Result.success(Unit)
        }
        
        val job = kotlinx.coroutines.launch {
            ttsEngine.speak("Message")
        }
        
        kotlinx.coroutines.delay(10)
        
        // Pause first time
        ttsEngine.pause()
        
        // Try to pause again
        val result = ttsEngine.pause()
        
        assertFalse(result)
        
        job.cancel()
    }
    
    @Test
    fun `resume should return false when not paused`() = runTest {
        val result = ttsEngine.resume()
        
        assertFalse(result)
    }
    
    @Test
    fun `resume should resume paused speech`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        coEvery { audioProcessor.playAudio(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(1000)
            Result.success(Unit)
        }
        
        val job = kotlinx.coroutines.launch {
            ttsEngine.speak("Message")
        }
        
        kotlinx.coroutines.delay(10)
        
        // Pause first
        ttsEngine.pause()
        
        // Then resume
        val result = ttsEngine.resume()
        
        assertTrue(result)
        
        job.cancel()
    }
    
    // =========================
    // Voice Support Tests
    // =========================
    
    @Test
    fun `getAvailableVoices should return empty list without model`() = runTest {
        val voices = ttsEngine.getAvailableVoices()
        
        assertTrue(voices.isEmpty())
    }
    
    @Test
    fun `getAvailableVoices should return model voices`() = runTest {
        ttsEngine.loadTTSModel(testModel)
        
        val voices = ttsEngine.getAvailableVoices()
        
        assertEquals(testModel.supportedVoices, voices)
    }
}
