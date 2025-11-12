package com.nervesparks.iris.core.multimodal.voice

import android.content.Context
import com.nervesparks.iris.app.events.EventBus
import com.nervesparks.iris.app.events.IrisEvent
import com.nervesparks.iris.common.error.VoiceException
import com.nervesparks.iris.common.models.HardwareCapability
import com.nervesparks.iris.core.hw.DeviceProfile
import com.nervesparks.iris.core.hw.DeviceProfileProvider
import com.nervesparks.iris.core.multimodal.TestAudioUtils
import com.nervesparks.iris.core.multimodal.audio.AudioConfig
import com.nervesparks.iris.core.multimodal.audio.AudioData
import com.nervesparks.iris.core.multimodal.audio.AudioProcessor
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SpeechToTextEngineImplTest {
    
    private lateinit var context: Context
    private lateinit var audioProcessor: AudioProcessor
    private lateinit var deviceProfileProvider: DeviceProfileProvider
    private lateinit var eventBus: EventBus
    private lateinit var sttEngine: SpeechToTextEngineImpl
    
    private val testModel = STTModelDescriptor(
        id = "test-whisper-tiny",
        name = "Test Whisper Tiny",
        description = "Test STT model",
        language = "en",
        supportedLanguages = listOf("en", "es", "fr"),
        audioRequirements = AudioRequirements(
            sampleRate = 16000,
            channels = 1,
            bitDepth = 16,
            supportedFormats = listOf("pcm", "wav")
        ),
        memoryRequirements = MemoryRequirements(
            minRAM = 2L * 1024 * 1024 * 1024, // 2GB
            recommendedRAM = 4L * 1024 * 1024 * 1024, // 4GB
            modelSize = 75L * 1024 * 1024 // 75MB
        ),
        supportedBackends = listOf(STTBackend.CPU, STTBackend.GPU),
        accuracy = 0.92f,
        fileSize = 75L * 1024 * 1024
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
            HardwareCapability.MICROPHONE,
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
        
        // Create File mock for exists check
        mockkConstructor(File::class)
        every { anyConstructed<File>().exists() } returns true
        every { anyConstructed<File>().length() } returns 100L
        
        sttEngine = SpeechToTextEngineImpl(
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
        assertFalse(sttEngine.isModelLoaded())
    }
    
    @Test
    fun `getCurrentModel should return null initially`() = runTest {
        assertNull(sttEngine.getCurrentModel())
    }
    
    @Test
    fun `loadSTTModel should successfully load valid model`() = runTest {
        val result = sttEngine.loadSTTModel(testModel)
        
        assertTrue(result.isSuccess)
        assertTrue(sttEngine.isModelLoaded())
        assertEquals(testModel, sttEngine.getCurrentModel())
        
        coVerify { eventBus.emit(match { it is IrisEvent.STTModelLoadStarted }) }
        coVerify { eventBus.emit(match { it is IrisEvent.STTModelLoadCompleted }) }
    }
    
    @Test
    fun `loadSTTModel should emit load started event`() = runTest {
        sttEngine.loadSTTModel(testModel)
        
        coVerify { 
            eventBus.emit(match<IrisEvent.STTModelLoadStarted> { 
                it.modelId == testModel.id 
            }) 
        }
    }
    
    @Test
    fun `loadSTTModel should emit load completed event on success`() = runTest {
        sttEngine.loadSTTModel(testModel)
        
        coVerify { 
            eventBus.emit(match<IrisEvent.STTModelLoadCompleted> { 
                it.modelId == testModel.id 
            }) 
        }
    }
    
    @Test
    fun `loadSTTModel should fail with insufficient RAM`() = runTest {
        val lowRamProfile = testDeviceProfile.copy(
            totalRAM = 1L * 1024 * 1024 * 1024 // 1GB
        )
        every { deviceProfileProvider.getDeviceProfile() } returns lowRamProfile
        
        val result = sttEngine.loadSTTModel(testModel)
        
        assertTrue(result.isFailure)
        assertFalse(sttEngine.isModelLoaded())
        
        coVerify { eventBus.emit(match { it is IrisEvent.STTModelLoadFailed }) }
    }
    
    @Test
    fun `loadSTTModel should fail without microphone capability`() = runTest {
        val noMicProfile = testDeviceProfile.copy(
            capabilities = setOf(HardwareCapability.OPENCL)
        )
        every { deviceProfileProvider.getDeviceProfile() } returns noMicProfile
        
        val result = sttEngine.loadSTTModel(testModel)
        
        assertTrue(result.isFailure)
        assertFalse(sttEngine.isModelLoaded())
        assertTrue(result.exceptionOrNull() is VoiceException)
    }
    
    @Test
    fun `loadSTTModel should handle already loaded model`() = runTest {
        // Load first time
        sttEngine.loadSTTModel(testModel)
        
        // Load again
        val result = sttEngine.loadSTTModel(testModel)
        
        assertTrue(result.isSuccess)
        assertTrue(sttEngine.isModelLoaded())
    }
    
    // ========================
    // Listening/Recognition Tests
    // ========================
    
    @Test
    fun `isListening should return false initially`() = runTest {
        assertFalse(sttEngine.isListening())
    }
    
    @Test
    fun `startListening should fail without loaded model`() = runTest {
        val config = ListeningConfig()
        
        val results = sttEngine.startListening(config).toList()
        
        assertEquals(1, results.size)
        assertTrue(results[0] is SpeechRecognitionResult.Error)
        assertEquals("No STT model loaded", (results[0] as SpeechRecognitionResult.Error).message)
    }
    
    @Test
    fun `startListening should emit listening started event`() = runTest {
        sttEngine.loadSTTModel(testModel)
        
        val audioFlow = flow<AudioData> {
            emit(TestAudioUtils.generateSineWave(440, 100))
            emit(AudioData.Ended)
        }
        coEvery { audioProcessor.startRecording(any(), any(), any()) } returns audioFlow
        
        val config = ListeningConfig(streamingMode = false)
        val results = sttEngine.startListening(config).toList()
        
        assertTrue(results.any { it is SpeechRecognitionResult.ListeningStarted })
    }
    
    @Test
    fun `startListening should detect voice activity`() = runTest {
        sttEngine.loadSTTModel(testModel)
        
        // Generate loud audio to trigger VAD
        val loudAudio = TestAudioUtils.generateSineWave(440, 100, amplitude = 0.8f)
        val audioFlow = flow<AudioData> {
            emit(loudAudio)
            emit(AudioData.Ended)
        }
        coEvery { audioProcessor.startRecording(any(), any(), any()) } returns audioFlow
        
        val config = ListeningConfig(streamingMode = false)
        val results = sttEngine.startListening(config).toList()
        
        assertTrue(results.any { it is SpeechRecognitionResult.SpeechDetected })
    }
    
    @Test
    fun `startListening should emit final transcription`() = runTest {
        sttEngine.loadSTTModel(testModel)
        
        val audioFlow = flow<AudioData> {
            emit(TestAudioUtils.generateSpeechLikeAudio("hello world"))
            emit(AudioData.Ended)
        }
        coEvery { audioProcessor.startRecording(any(), any(), any()) } returns audioFlow
        
        val config = ListeningConfig(streamingMode = false)
        val results = sttEngine.startListening(config).toList()
        
        val finalResult = results.filterIsInstance<SpeechRecognitionResult.FinalTranscription>()
        assertEquals(1, finalResult.size)
        assertTrue(finalResult[0].text.isNotEmpty())
        assertTrue(finalResult[0].confidence > 0f)
    }
    
    @Test
    fun `startListening should handle audio errors`() = runTest {
        sttEngine.loadSTTModel(testModel)
        
        val audioFlow = flow<AudioData> {
            emit(AudioData.Error("Microphone permission denied"))
        }
        coEvery { audioProcessor.startRecording(any(), any(), any()) } returns audioFlow
        
        val config = ListeningConfig()
        val results = sttEngine.startListening(config).toList()
        
        assertTrue(results.any { it is SpeechRecognitionResult.Error })
    }
    
    @Test
    fun `startListening should respect max duration`() = runTest {
        sttEngine.loadSTTModel(testModel)
        
        val audioFlow = flow<AudioData> {
            // Simulate long recording
            repeat(100) {
                emit(TestAudioUtils.generateSineWave(440, 100))
            }
            emit(AudioData.Ended)
        }
        coEvery { audioProcessor.startRecording(any(), any(), any()) } returns audioFlow
        
        val config = ListeningConfig(maxDurationMs = 500)
        val results = sttEngine.startListening(config).toList()
        
        // Should stop before processing all chunks due to max duration
        assertTrue(results.any { 
            it is SpeechRecognitionResult.MaxDurationReached || 
            it is SpeechRecognitionResult.ListeningStopped 
        })
    }
    
    @Test
    fun `stopListening should stop active recording`() = runTest {
        sttEngine.loadSTTModel(testModel)
        coEvery { audioProcessor.stopRecording() } just Runs
        
        // Simulate listening state
        val audioFlow = flow<AudioData> {
            emit(TestAudioUtils.generateSineWave(440, 100))
            // Don't emit Ended to keep recording active
        }
        coEvery { audioProcessor.startRecording(any(), any(), any()) } returns audioFlow
        
        // Start listening in background (don't wait for it)
        val config = ListeningConfig()
        sttEngine.startListening(config)
        
        // Give it a moment to start
        kotlinx.coroutines.delay(50)
        
        val result = sttEngine.stopListening()
        
        assertTrue(result)
        coVerify { audioProcessor.stopRecording() }
    }
    
    @Test
    fun `stopListening should return false when not listening`() = runTest {
        val result = sttEngine.stopListening()
        
        assertFalse(result)
    }
    
    // =========================
    // Transcription Tests
    // =========================
    
    @Test
    fun `transcribeAudio should fail without loaded model`() = runTest {
        val audioFile = mockk<File>()
        every { audioFile.exists() } returns true
        every { audioFile.length() } returns 1000L
        every { audioFile.name } returns "test.wav"
        
        val result = sttEngine.transcribeAudio(audioFile)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is VoiceException)
    }
    
    @Test
    fun `transcribeAudio should fail with non-existent file`() = runTest {
        sttEngine.loadSTTModel(testModel)
        
        val audioFile = mockk<File>()
        every { audioFile.exists() } returns false
        
        val result = sttEngine.transcribeAudio(audioFile)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is VoiceException)
    }
    
    @Test
    fun `transcribeAudio should fail with empty file`() = runTest {
        sttEngine.loadSTTModel(testModel)
        
        val audioFile = mockk<File>()
        every { audioFile.exists() } returns true
        every { audioFile.length() } returns 0L
        
        val result = sttEngine.transcribeAudio(audioFile)
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `transcribeAudio should successfully transcribe valid audio file`() = runTest {
        sttEngine.loadSTTModel(testModel)
        
        val audioFile = mockk<File>()
        every { audioFile.exists() } returns true
        every { audioFile.length() } returns 1000L
        every { audioFile.name } returns "test.wav"
        
        val audioData = TestAudioUtils.generateSpeechLikeAudio("hello world")
        coEvery { audioProcessor.loadAudioFile(audioFile) } returns Result.success(audioData.samples)
        
        val result = sttEngine.transcribeAudio(audioFile)
        
        assertTrue(result.isSuccess)
        val transcription = result.getOrNull()!!
        assertTrue(transcription.text.isNotEmpty())
        assertTrue(transcription.confidence > 0f)
        assertTrue(transcription.segments.isNotEmpty())
        assertEquals("en", transcription.language)
    }
    
    @Test
    fun `transcribeAudio should use specified language`() = runTest {
        sttEngine.loadSTTModel(testModel)
        
        val audioFile = mockk<File>()
        every { audioFile.exists() } returns true
        every { audioFile.length() } returns 1000L
        every { audioFile.name } returns "test.wav"
        
        val audioData = TestAudioUtils.generateSpeechLikeAudio("hello")
        coEvery { audioProcessor.loadAudioFile(audioFile) } returns Result.success(audioData.samples)
        
        val result = sttEngine.transcribeAudio(audioFile, language = "es")
        
        assertTrue(result.isSuccess)
        assertEquals("es", result.getOrNull()!!.language)
    }
    
    @Test
    fun `transcribeAudio should handle audio loading failure`() = runTest {
        sttEngine.loadSTTModel(testModel)
        
        val audioFile = mockk<File>()
        every { audioFile.exists() } returns true
        every { audioFile.length() } returns 1000L
        
        coEvery { audioProcessor.loadAudioFile(audioFile) } returns Result.failure(
            VoiceException("Failed to load audio")
        )
        
        val result = sttEngine.transcribeAudio(audioFile)
        
        assertTrue(result.isFailure)
    }
    
    // =========================
    // Language Support Tests
    // =========================
    
    @Test
    fun `getAvailableLanguages should return empty list without model`() = runTest {
        val languages = sttEngine.getAvailableLanguages()
        
        assertTrue(languages.isEmpty())
    }
    
    @Test
    fun `getAvailableLanguages should return model languages`() = runTest {
        sttEngine.loadSTTModel(testModel)
        
        val languages = sttEngine.getAvailableLanguages()
        
        assertEquals(testModel.supportedLanguages, languages)
    }
    
    // =========================
    // Voice Activity Detection Tests
    // =========================
    
    @Test
    fun `VAD should detect speech in loud audio`() = runTest {
        sttEngine.loadSTTModel(testModel)
        
        val loudAudio = TestAudioUtils.generateSineWave(440, 200, amplitude = 0.9f)
        val audioFlow = flow<AudioData> {
            emit(loudAudio)
            emit(AudioData.Ended)
        }
        coEvery { audioProcessor.startRecording(any(), any(), any()) } returns audioFlow
        
        val config = ListeningConfig(streamingMode = false)
        val results = sttEngine.startListening(config).toList()
        
        assertTrue(results.any { it is SpeechRecognitionResult.SpeechDetected })
    }
    
    @Test
    fun `VAD should not detect speech in silence`() = runTest {
        sttEngine.loadSTTModel(testModel)
        
        val silence = TestAudioUtils.generateSilence(200)
        val audioFlow = flow<AudioData> {
            emit(silence)
            emit(AudioData.Ended)
        }
        coEvery { audioProcessor.startRecording(any(), any(), any()) } returns audioFlow
        
        val config = ListeningConfig(streamingMode = false)
        val results = sttEngine.startListening(config).toList()
        
        assertFalse(results.any { it is SpeechRecognitionResult.SpeechDetected })
    }
    
    @Test
    fun `VAD should handle low-amplitude noise`() = runTest {
        sttEngine.loadSTTModel(testModel)
        
        val noise = TestAudioUtils.generateWhiteNoise(0.1f, 200)
        val audioFlow = flow<AudioData> {
            emit(noise)
            emit(AudioData.Ended)
        }
        coEvery { audioProcessor.startRecording(any(), any(), any()) } returns audioFlow
        
        val config = ListeningConfig(streamingMode = false)
        val results = sttEngine.startListening(config).toList()
        
        // Low amplitude noise should not be detected as speech
        val speechDetections = results.filterIsInstance<SpeechRecognitionResult.SpeechDetected>()
        assertTrue(speechDetections.isEmpty() || speechDetections.size < 2)
    }
}
