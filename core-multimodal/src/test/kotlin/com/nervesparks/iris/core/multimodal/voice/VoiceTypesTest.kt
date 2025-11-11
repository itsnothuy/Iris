package com.nervesparks.iris.core.multimodal.voice

import org.junit.Assert.*
import org.junit.Test

class VoiceTypesTest {
    
    // =========================
    // STTModelDescriptor Tests
    // =========================
    
    @Test
    fun `STTModelDescriptor should be created with all fields`() {
        val model = STTModelDescriptor(
            id = "whisper-tiny",
            name = "Whisper Tiny",
            description = "Small whisper model",
            language = "en",
            supportedLanguages = listOf("en", "es"),
            audioRequirements = AudioRequirements(
                sampleRate = 16000,
                channels = 1,
                bitDepth = 16,
                supportedFormats = listOf("pcm")
            ),
            memoryRequirements = MemoryRequirements(
                minRAM = 2L * 1024 * 1024 * 1024,
                recommendedRAM = 4L * 1024 * 1024 * 1024,
                modelSize = 75L * 1024 * 1024
            ),
            supportedBackends = listOf(STTBackend.CPU),
            accuracy = 0.92f,
            fileSize = 75L * 1024 * 1024
        )
        
        assertEquals("whisper-tiny", model.id)
        assertEquals("Whisper Tiny", model.name)
        assertEquals(0.92f, model.accuracy, 0.001f)
    }
    
    // =========================
    // TTSModelDescriptor Tests
    // =========================
    
    @Test
    fun `TTSModelDescriptor should be created with all fields`() {
        val voice = VoiceDescriptor(
            id = "en-us-male",
            name = "English US Male",
            language = "en",
            gender = VoiceGender.MALE
        )
        
        val model = TTSModelDescriptor(
            id = "piper-en",
            name = "Piper English",
            description = "English TTS model",
            supportedLanguages = listOf("en"),
            supportedVoices = listOf(voice),
            audioFormat = AudioFormat(
                sampleRate = 22050,
                channels = 1,
                bitDepth = 16,
                encoding = AudioEncoding.PCM_16BIT
            ),
            memoryRequirements = MemoryRequirements(
                minRAM = 1L * 1024 * 1024 * 1024,
                recommendedRAM = 2L * 1024 * 1024 * 1024,
                modelSize = 30L * 1024 * 1024
            ),
            supportedBackends = listOf(TTSBackend.CPU),
            quality = 0.88f,
            fileSize = 30L * 1024 * 1024
        )
        
        assertEquals("piper-en", model.id)
        assertEquals(0.88f, model.quality, 0.001f)
        assertEquals(1, model.supportedVoices.size)
    }
    
    // =========================
    // VoiceDescriptor Tests
    // =========================
    
    @Test
    fun `VoiceDescriptor should support all genders`() {
        val maleVoice = VoiceDescriptor(
            id = "male",
            name = "Male Voice",
            language = "en",
            gender = VoiceGender.MALE
        )
        
        val femaleVoice = VoiceDescriptor(
            id = "female",
            name = "Female Voice",
            language = "en",
            gender = VoiceGender.FEMALE
        )
        
        val neutralVoice = VoiceDescriptor(
            id = "neutral",
            name = "Neutral Voice",
            language = "en",
            gender = VoiceGender.NEUTRAL
        )
        
        assertEquals(VoiceGender.MALE, maleVoice.gender)
        assertEquals(VoiceGender.FEMALE, femaleVoice.gender)
        assertEquals(VoiceGender.NEUTRAL, neutralVoice.gender)
    }
    
    @Test
    fun `VoiceDescriptor should support all styles`() {
        val styles = listOf(
            VoiceStyle.NEUTRAL,
            VoiceStyle.FRIENDLY,
            VoiceStyle.PROFESSIONAL,
            VoiceStyle.CALM,
            VoiceStyle.ENERGETIC
        )
        
        styles.forEach { style ->
            val voice = VoiceDescriptor(
                id = "test",
                name = "Test Voice",
                language = "en",
                gender = VoiceGender.NEUTRAL,
                style = style
            )
            assertEquals(style, voice.style)
        }
    }
    
    // =========================
    // SpeechRecognitionResult Tests
    // =========================
    
    @Test
    fun `SpeechRecognitionResult should support all result types`() {
        val started = SpeechRecognitionResult.ListeningStarted("session-1")
        assertTrue(started is SpeechRecognitionResult.ListeningStarted)
        assertEquals("session-1", started.sessionId)
        
        val detected = SpeechRecognitionResult.SpeechDetected()
        assertTrue(detected is SpeechRecognitionResult.SpeechDetected)
        
        val partial = SpeechRecognitionResult.PartialTranscription("hello", 0.9f)
        assertTrue(partial is SpeechRecognitionResult.PartialTranscription)
        assertEquals("hello", partial.text)
        assertEquals(0.9f, partial.confidence, 0.001f)
        
        val final = SpeechRecognitionResult.FinalTranscription("hello world", 0.95f, 1000L)
        assertTrue(final is SpeechRecognitionResult.FinalTranscription)
        assertEquals("hello world", final.text)
        
        val stopped = SpeechRecognitionResult.ListeningStopped()
        assertTrue(stopped is SpeechRecognitionResult.ListeningStopped)
        
        val maxDuration = SpeechRecognitionResult.MaxDurationReached(60000L)
        assertTrue(maxDuration is SpeechRecognitionResult.MaxDurationReached)
        
        val error = SpeechRecognitionResult.Error("Test error")
        assertTrue(error is SpeechRecognitionResult.Error)
        assertEquals("Test error", error.message)
    }
    
    // =========================
    // TranscriptionResult Tests
    // =========================
    
    @Test
    fun `TranscriptionResult should contain segments`() {
        val segment = TranscriptionSegment(
            text = "hello",
            startTime = 0.0f,
            endTime = 0.5f,
            confidence = 0.92f
        )
        
        val result = TranscriptionResult(
            text = "hello",
            confidence = 0.92f,
            segments = listOf(segment),
            duration = 500L,
            language = "en"
        )
        
        assertEquals(1, result.segments.size)
        assertEquals("hello", result.segments[0].text)
        assertEquals(0.92f, result.segments[0].confidence, 0.001f)
    }
    
    // =========================
    // ListeningConfig Tests
    // =========================
    
    @Test
    fun `ListeningConfig should have sensible defaults`() {
        val config = ListeningConfig()
        
        assertTrue(config.streamingMode)
        assertEquals(1500, config.endOfSpeechSilenceMs)
        assertEquals(60000, config.maxDurationMs)
        assertNull(config.language)
    }
    
    @Test
    fun `ListeningConfig should allow custom values`() {
        val audioConfig = AudioConfig(
            noiseReduction = true,
            automaticGainControl = true,
            echoCancellation = false
        )
        
        val config = ListeningConfig(
            streamingMode = false,
            endOfSpeechSilenceMs = 2000,
            maxDurationMs = 30000,
            language = "es",
            audioConfig = audioConfig
        )
        
        assertFalse(config.streamingMode)
        assertEquals(2000, config.endOfSpeechSilenceMs)
        assertEquals(30000, config.maxDurationMs)
        assertEquals("es", config.language)
        assertTrue(config.audioConfig.noiseReduction)
    }
    
    // =========================
    // SpeechParameters Tests
    // =========================
    
    @Test
    fun `SpeechParameters should have default values`() {
        val params = SpeechParameters()
        
        assertEquals(1.0f, params.speakingRate, 0.001f)
        assertEquals(1.0f, params.pitch, 0.001f)
        assertEquals(1.0f, params.volume, 0.001f)
        assertNull(params.voice)
    }
    
    @Test
    fun `SpeechParameters should support custom values`() {
        val voice = VoiceDescriptor(
            id = "test-voice",
            name = "Test Voice",
            language = "en",
            gender = VoiceGender.FEMALE
        )
        
        val params = SpeechParameters(
            speakingRate = 1.5f,
            pitch = 1.2f,
            volume = 0.8f,
            voice = voice
        )
        
        assertEquals(1.5f, params.speakingRate, 0.001f)
        assertEquals(1.2f, params.pitch, 0.001f)
        assertEquals(0.8f, params.volume, 0.001f)
        assertEquals(voice, params.voice)
    }
    
    // =========================
    // Backend Tests
    // =========================
    
    @Test
    fun `STTBackend should have all expected values`() {
        val backends = listOf(STTBackend.CPU, STTBackend.GPU, STTBackend.NPU)
        assertEquals(3, backends.size)
    }
    
    @Test
    fun `TTSBackend should have all expected values`() {
        val backends = listOf(TTSBackend.CPU, TTSBackend.GPU, TTSBackend.NPU)
        assertEquals(3, backends.size)
    }
    
    // =========================
    // VADResult Tests
    // =========================
    
    @Test
    fun `VADResult should have all detection types`() {
        val results = listOf(VADResult.SPEECH, VADResult.SILENCE, VADResult.NOISE)
        assertEquals(3, results.size)
    }
    
    // =========================
    // ValidationIssue Tests
    // =========================
    
    @Test
    fun `ValidationIssue should have all issue types`() {
        val issues = listOf(
            ValidationIssue.INSUFFICIENT_MEMORY,
            ValidationIssue.HARDWARE_MISSING,
            ValidationIssue.UNSUPPORTED_FEATURE,
            ValidationIssue.MODEL_NOT_FOUND,
            ValidationIssue.CORRUPTED_MODEL
        )
        assertEquals(5, issues.size)
    }
    
    @Test
    fun `ModelValidationResult should indicate validity`() {
        val valid = ModelValidationResult(
            isValid = true,
            reason = "All checks passed",
            issues = emptyList()
        )
        
        assertTrue(valid.isValid)
        assertTrue(valid.issues.isEmpty())
        
        val invalid = ModelValidationResult(
            isValid = false,
            reason = "Insufficient memory",
            issues = listOf(ValidationIssue.INSUFFICIENT_MEMORY)
        )
        
        assertFalse(invalid.isValid)
        assertEquals(1, invalid.issues.size)
    }
    
    // =========================
    // RecordingSession Tests
    // =========================
    
    @Test
    fun `RecordingSession should store session info`() {
        val config = ListeningConfig()
        val session = RecordingSession(
            sessionId = "rec-123",
            startTime = System.currentTimeMillis(),
            config = config
        )
        
        assertEquals("rec-123", session.sessionId)
        assertTrue(session.startTime > 0)
        assertEquals(config, session.config)
    }
    
    // =========================
    // SpeechSession Tests
    // =========================
    
    @Test
    fun `SpeechSession should store session info`() {
        val params = SpeechParameters()
        val session = SpeechSession(
            sessionId = "tts-456",
            text = "Hello world",
            startTime = System.currentTimeMillis(),
            parameters = params
        )
        
        assertEquals("tts-456", session.sessionId)
        assertEquals("Hello world", session.text)
        assertTrue(session.startTime > 0)
        assertEquals(params, session.parameters)
    }
}
