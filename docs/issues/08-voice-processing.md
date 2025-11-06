# Issue #08: Voice Processing & Speech Engine

## 🎯 Epic: Voice AI Capabilities
**Priority**: P2 (Medium)  
**Estimate**: 10-12 days  
**Dependencies**: #01 (Core Architecture), #05 (Chat Engine), #07 (Multimodal Support)  
**Architecture Reference**: [docs/architecture.md](../architecture.md) - Section 8 Voice Processing Engine

## 📋 Overview
Implement comprehensive voice processing capabilities including Speech-to-Text (STT), Text-to-Speech (TTS), and voice activity detection. This system enables hands-free interaction with the AI assistant through natural speech input and audio response output.

## 🎯 Goals
- **Speech Recognition**: On-device STT with multiple language support
- **Voice Synthesis**: High-quality TTS with natural-sounding voices
- **Voice Activity Detection**: Intelligent voice trigger and endpoint detection
- **Audio Processing**: Real-time audio preprocessing and enhancement
- **Conversation Flow**: Seamless voice-based conversations
- **Privacy-First**: All voice processing remains on-device

## 📝 Detailed Tasks

### 1. Speech-to-Text Engine

#### 1.1 STT Engine Implementation
Create `core-voice/src/main/kotlin/SpeechToTextEngine.kt`:

```kotlin
@Singleton
class SpeechToTextEngineImpl @Inject constructor(
    private val nativeEngine: NativeInferenceEngine,
    private val audioProcessor: AudioProcessor,
    private val deviceProfileProvider: DeviceProfileProvider,
    private val eventBus: EventBus,
    @ApplicationContext private val context: Context
) : SpeechToTextEngine {
    
    companion object {
        private const val TAG = "SpeechToTextEngine"
        private const val DEFAULT_SAMPLE_RATE = 16000
        private const val DEFAULT_CHANNELS = 1
        private const val CHUNK_DURATION_MS = 1000
        private const val SILENCE_THRESHOLD_DB = -30.0f
        private const val MAX_RECORDING_DURATION_MS = 60000 // 60 seconds
        private const val VAD_WINDOW_MS = 100
    }
    
    private var currentSTTModel: STTModelDescriptor? = null
    private var isSTTModelLoaded = false
    private var isRecording = false
    private var currentRecordingSession: RecordingSession? = null
    
    override suspend fun loadSTTModel(model: STTModelDescriptor): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Loading STT model: ${model.id}")
                eventBus.emit(IrisEvent.STTModelLoadStarted(model.id))
                
                // Validate model compatibility
                val validation = validateSTTModel(model)
                if (!validation.isValid) {
                    return@withContext Result.failure(
                        VoiceException("STT model validation failed: ${validation.reason}")
                    )
                }
                
                // Load model through native engine
                val loadResult = nativeEngine.loadSTTModel(
                    modelPath = getModelPath(model),
                    config = STTConfig(
                        sampleRate = model.audioRequirements.sampleRate,
                        channels = model.audioRequirements.channels,
                        language = model.language,
                        backend = selectOptimalSTTBackend(model)
                    )
                )
                
                if (loadResult.isSuccess) {
                    currentSTTModel = model
                    isSTTModelLoaded = true
                    
                    Log.i(TAG, "STT model loaded successfully: ${model.id}")
                    eventBus.emit(IrisEvent.STTModelLoadCompleted(model.id))
                    Result.success(Unit)
                } else {
                    val error = loadResult.exceptionOrNull()
                    Log.e(TAG, "STT model loading failed", error)
                    eventBus.emit(IrisEvent.STTModelLoadFailed(model.id, error?.message ?: "Unknown error"))
                    Result.failure(error ?: VoiceException("STT model loading failed"))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception during STT model loading", e)
                eventBus.emit(IrisEvent.STTModelLoadFailed(model.id, e.message ?: "Exception"))
                Result.failure(VoiceException("STT model loading exception", e))
            }
        }
    }
    
    override suspend fun startListening(
        config: ListeningConfig
    ): Flow<SpeechRecognitionResult> = flow {
        
        if (!isSTTModelLoaded) {
            emit(SpeechRecognitionResult.Error("No STT model loaded"))
            return@flow
        }
        
        if (isRecording) {
            emit(SpeechRecognitionResult.Error("Already recording"))
            return@flow
        }
        
        try {
            isRecording = true
            currentRecordingSession = RecordingSession(
                sessionId = generateSessionId(),
                startTime = System.currentTimeMillis(),
                config = config
            )
            
            emit(SpeechRecognitionResult.ListeningStarted(currentRecordingSession!!.sessionId))
            
            // Start audio recording
            val audioFlow = audioProcessor.startRecording(
                sampleRate = currentSTTModel!!.audioRequirements.sampleRate,
                channels = currentSTTModel!!.audioRequirements.channels,
                config = config.audioConfig
            )
            
            var silenceCount = 0
            var hasDetectedSpeech = false
            val audioBuffer = mutableListOf<FloatArray>()
            
            audioFlow.collect { audioData ->
                when (audioData) {
                    is AudioData.Chunk -> {
                        // Voice Activity Detection
                        val vadResult = performVAD(audioData.samples)
                        
                        when (vadResult) {
                            VADResult.SPEECH -> {
                                hasDetectedSpeech = true
                                silenceCount = 0
                                audioBuffer.add(audioData.samples)
                                
                                emit(SpeechRecognitionResult.SpeechDetected())
                                
                                // Process audio chunk through STT
                                processAudioChunk(audioData.samples, config.streamingMode)?.let { partialResult ->
                                    emit(SpeechRecognitionResult.PartialTranscription(
                                        text = partialResult.text,
                                        confidence = partialResult.confidence,
                                        isFinal = partialResult.isFinal
                                    ))
                                }
                            }
                            
                            VADResult.SILENCE -> {
                                if (hasDetectedSpeech) {
                                    silenceCount++
                                    
                                    // End of speech detection
                                    if (silenceCount >= config.endOfSpeechSilenceMs / VAD_WINDOW_MS) {
                                        // Process complete audio buffer
                                        val finalTranscription = processFinalAudio(audioBuffer)
                                        
                                        emit(SpeechRecognitionResult.FinalTranscription(
                                            text = finalTranscription.text,
                                            confidence = finalTranscription.confidence,
                                            duration = System.currentTimeMillis() - currentRecordingSession!!.startTime
                                        ))
                                        
                                        stopListening()
                                        return@collect
                                    }
                                }
                                
                                audioBuffer.add(audioData.samples)
                            }
                            
                            VADResult.NOISE -> {
                                // Ignore noise chunks but keep in buffer for context
                                audioBuffer.add(audioData.samples)
                            }
                        }
                        
                        // Check maximum recording duration
                        val recordingDuration = System.currentTimeMillis() - currentRecordingSession!!.startTime
                        if (recordingDuration > MAX_RECORDING_DURATION_MS) {
                            val finalTranscription = processFinalAudio(audioBuffer)
                            
                            emit(SpeechRecognitionResult.FinalTranscription(
                                text = finalTranscription.text,
                                confidence = finalTranscription.confidence,
                                duration = recordingDuration
                            ))
                            
                            emit(SpeechRecognitionResult.MaxDurationReached())
                            stopListening()
                            return@collect
                        }
                    }
                    
                    is AudioData.Error -> {
                        emit(SpeechRecognitionResult.Error("Audio recording error: ${audioData.message}"))
                        stopListening()
                        return@collect
                    }
                    
                    is AudioData.Ended -> {
                        if (audioBuffer.isNotEmpty()) {
                            val finalTranscription = processFinalAudio(audioBuffer)
                            
                            emit(SpeechRecognitionResult.FinalTranscription(
                                text = finalTranscription.text,
                                confidence = finalTranscription.confidence,
                                duration = System.currentTimeMillis() - currentRecordingSession!!.startTime
                            ))
                        }
                        
                        emit(SpeechRecognitionResult.ListeningStopped())
                        stopListening()
                        return@collect
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Speech recognition failed", e)
            emit(SpeechRecognitionResult.Error("Speech recognition failed: ${e.message}"))
            stopListening()
        }
    }
    
    override suspend fun stopListening(): Boolean {
        return try {
            if (isRecording) {
                audioProcessor.stopRecording()
                isRecording = false
                currentRecordingSession = null
                
                Log.d(TAG, "Speech recognition stopped")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop listening", e)
            false
        }
    }
    
    override suspend fun transcribeAudio(
        audioFile: File,
        language: String?
    ): Result<TranscriptionResult> = withContext(Dispatchers.IO) {
        
        if (!isSTTModelLoaded) {
            return@withContext Result.failure(VoiceException("No STT model loaded"))
        }
        
        try {
            // Validate audio file
            if (!audioFile.exists() || audioFile.length() == 0L) {
                return@withContext Result.failure(VoiceException("Invalid audio file"))
            }
            
            // Process audio file
            val audioData = audioProcessor.loadAudioFile(audioFile)
            if (audioData.isFailure) {
                return@withContext Result.failure(
                    VoiceException("Failed to load audio file: ${audioData.exceptionOrNull()?.message}")
                )
            }
            
            val samples = audioData.getOrNull()!!
            
            // Transcribe through native engine
            val transcriptionResult = nativeEngine.transcribeAudio(
                audioSamples = samples,
                sampleRate = currentSTTModel!!.audioRequirements.sampleRate,
                language = language ?: currentSTTModel!!.language
            )
            
            if (transcriptionResult.isSuccess) {
                Result.success(transcriptionResult.getOrNull()!!)
            } else {
                Result.failure(transcriptionResult.exceptionOrNull() ?: VoiceException("Transcription failed"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Audio transcription failed", e)
            Result.failure(VoiceException("Audio transcription failed", e))
        }
    }
    
    override suspend fun getAvailableLanguages(): List<String> {
        return currentSTTModel?.supportedLanguages ?: emptyList()
    }
    
    override suspend fun getCurrentModel(): STTModelDescriptor? {
        return currentSTTModel
    }
    
    override suspend fun isModelLoaded(): Boolean {
        return isSTTModelLoaded
    }
    
    override suspend fun isListening(): Boolean {
        return isRecording
    }
    
    // Private helper methods
    
    private fun validateSTTModel(model: STTModelDescriptor): ModelValidationResult {
        val deviceProfile = deviceProfileProvider.getDeviceProfile()
        
        // Check memory requirements
        if (deviceProfile.totalRAM < model.memoryRequirements.minRAM) {
            return ModelValidationResult(
                isValid = false,
                reason = "Insufficient RAM for STT model",
                issues = listOf(ValidationIssue.INSUFFICIENT_MEMORY)
            )
        }
        
        // Check audio capabilities
        if (!deviceProfile.capabilities.contains(HardwareCapability.MICROPHONE)) {
            return ModelValidationResult(
                isValid = false,
                reason = "No microphone available",
                issues = listOf(ValidationIssue.HARDWARE_MISSING)
            )
        }
        
        return ModelValidationResult(
            isValid = true,
            reason = "STT model compatible",
            issues = emptyList()
        )
    }
    
    private fun selectOptimalSTTBackend(model: STTModelDescriptor): STTBackend {
        val deviceProfile = deviceProfileProvider.getDeviceProfile()
        
        return when {
            model.supportedBackends.contains(STTBackend.NPU) && 
                deviceProfile.capabilities.contains(HardwareCapability.QNN) -> STTBackend.NPU
            
            model.supportedBackends.contains(STTBackend.GPU) && 
                deviceProfile.capabilities.contains(HardwareCapability.OPENCL) -> STTBackend.GPU
            
            else -> STTBackend.CPU
        }
    }
    
    private suspend fun performVAD(audioSamples: FloatArray): VADResult {
        return withContext(Dispatchers.Default) {
            try {
                // Calculate RMS energy
                val rms = sqrt(audioSamples.map { it * it }.average().toFloat())
                val energyDb = 20 * log10(rms + 1e-8f)
                
                // Simple energy-based VAD
                when {
                    energyDb > SILENCE_THRESHOLD_DB + 10 -> VADResult.SPEECH
                    energyDb > SILENCE_THRESHOLD_DB -> VADResult.NOISE
                    else -> VADResult.SILENCE
                }
            } catch (e: Exception) {
                Log.w(TAG, "VAD processing failed", e)
                VADResult.NOISE
            }
        }
    }
    
    private suspend fun processAudioChunk(
        audioSamples: FloatArray,
        streamingMode: Boolean
    ): PartialTranscriptionResult? {
        return if (streamingMode) {
            try {
                nativeEngine.processSTTChunk(audioSamples)
            } catch (e: Exception) {
                Log.w(TAG, "Streaming STT chunk processing failed", e)
                null
            }
        } else {
            null
        }
    }
    
    private suspend fun processFinalAudio(audioBuffer: List<FloatArray>): FinalTranscriptionResult {
        return try {
            // Concatenate audio chunks
            val totalSamples = audioBuffer.sumOf { it.size }
            val combinedAudio = FloatArray(totalSamples)
            var offset = 0
            
            audioBuffer.forEach { chunk ->
                chunk.copyInto(combinedAudio, offset)
                offset += chunk.size
            }
            
            // Process complete audio
            val result = nativeEngine.transcribeAudioComplete(
                audioSamples = combinedAudio,
                sampleRate = currentSTTModel!!.audioRequirements.sampleRate
            )
            
            result.getOrElse {
                FinalTranscriptionResult(
                    text = "",
                    confidence = 0f,
                    segments = emptyList()
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Final audio processing failed", e)
            FinalTranscriptionResult(
                text = "",
                confidence = 0f,
                segments = emptyList()
            )
        }
    }
    
    private fun generateSessionId(): String {
        return "stt_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    private fun getModelPath(model: STTModelDescriptor): String {
        return File(
            File(context.getExternalFilesDir(null), "models"),
            "${model.id}.bin"
        ).absolutePath
    }
}

// STT data structures
data class STTModelDescriptor(
    val id: String,
    val name: String,
    val description: String,
    val language: String,
    val supportedLanguages: List<String>,
    val audioRequirements: AudioRequirements,
    val memoryRequirements: MemoryRequirements,
    val supportedBackends: List<STTBackend>,
    val accuracy: Float,
    val fileSize: Long
)

data class AudioRequirements(
    val sampleRate: Int,
    val channels: Int,
    val bitDepth: Int,
    val supportedFormats: List<String>
)

data class MemoryRequirements(
    val minRAM: Long,
    val recommendedRAM: Long,
    val modelSize: Long
)

enum class STTBackend {
    CPU, GPU, NPU
}

data class ListeningConfig(
    val streamingMode: Boolean = true,
    val endOfSpeechSilenceMs: Int = 1500,
    val maxDurationMs: Int = MAX_RECORDING_DURATION_MS,
    val language: String? = null,
    val audioConfig: AudioConfig = AudioConfig()
)

data class AudioConfig(
    val noiseReduction: Boolean = true,
    val automaticGainControl: Boolean = true,
    val echoCancellation: Boolean = true
)

data class RecordingSession(
    val sessionId: String,
    val startTime: Long,
    val config: ListeningConfig
)

// Speech recognition results
sealed class SpeechRecognitionResult {
    data class ListeningStarted(val sessionId: String) : SpeechRecognitionResult()
    data class SpeechDetected() : SpeechRecognitionResult()
    data class PartialTranscription(
        val text: String,
        val confidence: Float,
        val isFinal: Boolean = false
    ) : SpeechRecognitionResult()
    data class FinalTranscription(
        val text: String,
        val confidence: Float,
        val duration: Long
    ) : SpeechRecognitionResult()
    data class ListeningStopped() : SpeechRecognitionResult()
    data class MaxDurationReached() : SpeechRecognitionResult()
    data class Error(val message: String) : SpeechRecognitionResult()
}

data class TranscriptionResult(
    val text: String,
    val confidence: Float,
    val segments: List<TranscriptionSegment>,
    val duration: Long,
    val language: String
)

data class TranscriptionSegment(
    val text: String,
    val startTime: Float,
    val endTime: Float,
    val confidence: Float
)

data class PartialTranscriptionResult(
    val text: String,
    val confidence: Float,
    val isFinal: Boolean
)

data class FinalTranscriptionResult(
    val text: String,
    val confidence: Float,
    val segments: List<TranscriptionSegment>
)

enum class VADResult {
    SPEECH, SILENCE, NOISE
}

// Audio data classes
sealed class AudioData {
    data class Chunk(val samples: FloatArray, val timestamp: Long) : AudioData() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as Chunk
            
            if (!samples.contentEquals(other.samples)) return false
            if (timestamp != other.timestamp) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = samples.contentHashCode()
            result = 31 * result + timestamp.hashCode()
            return result
        }
    }
    data class Error(val message: String) : AudioData()
    object Ended : AudioData()
}
```

### 2. Text-to-Speech Engine

#### 2.1 TTS Engine Implementation
Create `core-voice/src/main/kotlin/TextToSpeechEngine.kt`:

```kotlin
@Singleton
class TextToSpeechEngineImpl @Inject constructor(
    private val nativeEngine: NativeInferenceEngine,
    private val audioProcessor: AudioProcessor,
    private val deviceProfileProvider: DeviceProfileProvider,
    private val eventBus: EventBus,
    @ApplicationContext private val context: Context
) : TextToSpeechEngine {
    
    companion object {
        private const val TAG = "TextToSpeechEngine"
        private const val DEFAULT_SAMPLE_RATE = 22050
        private const val DEFAULT_SPEAKING_RATE = 1.0f
        private const val MAX_TEXT_LENGTH = 5000
        private const val CHUNK_SIZE = 500 // characters
    }
    
    private var currentTTSModel: TTSModelDescriptor? = null
    private var isTTSModelLoaded = false
    private var currentSpeechSession: SpeechSession? = null
    private var isPlaying = false
    
    override suspend fun loadTTSModel(model: TTSModelDescriptor): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Loading TTS model: ${model.id}")
                eventBus.emit(IrisEvent.TTSModelLoadStarted(model.id))
                
                // Validate model compatibility
                val validation = validateTTSModel(model)
                if (!validation.isValid) {
                    return@withContext Result.failure(
                        VoiceException("TTS model validation failed: ${validation.reason}")
                    )
                }
                
                // Load model through native engine
                val loadResult = nativeEngine.loadTTSModel(
                    modelPath = getModelPath(model),
                    config = TTSConfig(
                        sampleRate = model.audioOutput.sampleRate,
                        voiceId = model.defaultVoice.id,
                        language = model.language,
                        backend = selectOptimalTTSBackend(model)
                    )
                )
                
                if (loadResult.isSuccess) {
                    currentTTSModel = model
                    isTTSModelLoaded = true
                    
                    Log.i(TAG, "TTS model loaded successfully: ${model.id}")
                    eventBus.emit(IrisEvent.TTSModelLoadCompleted(model.id))
                    Result.success(Unit)
                } else {
                    val error = loadResult.exceptionOrNull()
                    Log.e(TAG, "TTS model loading failed", error)
                    eventBus.emit(IrisEvent.TTSModelLoadFailed(model.id, error?.message ?: "Unknown error"))
                    Result.failure(error ?: VoiceException("TTS model loading failed"))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception during TTS model loading", e)
                eventBus.emit(IrisEvent.TTSModelLoadFailed(model.id, e.message ?: "Exception"))
                Result.failure(VoiceException("TTS model loading exception", e))
            }
        }
    }
    
    override suspend fun speak(
        text: String,
        config: SpeechConfig
    ): Flow<SpeechSynthesisResult> = flow {
        
        if (!isTTSModelLoaded) {
            emit(SpeechSynthesisResult.Error("No TTS model loaded"))
            return@flow
        }
        
        if (isPlaying) {
            emit(SpeechSynthesisResult.Error("Already speaking"))
            return@flow
        }
        
        try {
            // Validate and prepare text
            val cleanText = text.trim()
            if (cleanText.isEmpty()) {
                emit(SpeechSynthesisResult.Error("Empty text"))
                return@flow
            }
            
            if (cleanText.length > MAX_TEXT_LENGTH) {
                emit(SpeechSynthesisResult.Error("Text too long (max $MAX_TEXT_LENGTH characters)"))
                return@flow
            }
            
            isPlaying = true
            currentSpeechSession = SpeechSession(
                sessionId = generateSessionId(),
                text = cleanText,
                config = config,
                startTime = System.currentTimeMillis()
            )
            
            emit(SpeechSynthesisResult.SynthesisStarted(currentSpeechSession!!.sessionId))
            
            // Split text into chunks for streaming synthesis
            val textChunks = splitTextIntoChunks(cleanText)
            var totalAudioDuration = 0L
            
            for ((index, chunk) in textChunks.withIndex()) {
                // Synthesize audio for chunk
                val synthesisResult = nativeEngine.synthesizeText(
                    text = chunk,
                    voiceConfig = config.toVoiceConfig(),
                    outputFormat = AudioFormat.PCM_16BIT
                )
                
                if (synthesisResult.isFailure) {
                    emit(SpeechSynthesisResult.Error("Synthesis failed for chunk $index"))
                    stopSpeaking()
                    return@flow
                }
                
                val audioData = synthesisResult.getOrNull()!!
                
                // Play audio chunk
                val playbackResult = audioProcessor.playAudio(
                    audioData = audioData,
                    sampleRate = currentTTSModel!!.audioOutput.sampleRate
                )
                
                playbackResult.collect { playbackEvent ->
                    when (playbackEvent) {
                        is AudioPlaybackEvent.Started -> {
                            emit(SpeechSynthesisResult.AudioStarted(chunk))
                        }
                        
                        is AudioPlaybackEvent.Progress -> {
                            val chunkProgress = (index.toFloat() / textChunks.size) + 
                                (playbackEvent.progress / textChunks.size)
                            
                            emit(SpeechSynthesisResult.SpeechProgress(
                                progress = chunkProgress,
                                currentText = chunk,
                                elapsedTime = System.currentTimeMillis() - currentSpeechSession!!.startTime
                            ))
                        }
                        
                        is AudioPlaybackEvent.Completed -> {
                            totalAudioDuration += playbackEvent.duration
                            
                            if (index == textChunks.size - 1) {
                                // All chunks completed
                                emit(SpeechSynthesisResult.SynthesisCompleted(
                                    sessionId = currentSpeechSession!!.sessionId,
                                    totalDuration = totalAudioDuration,
                                    characterCount = cleanText.length
                                ))
                                
                                stopSpeaking()
                            }
                        }
                        
                        is AudioPlaybackEvent.Error -> {
                            emit(SpeechSynthesisResult.Error("Playback error: ${playbackEvent.message}"))
                            stopSpeaking()
                            return@collect
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Speech synthesis failed", e)
            emit(SpeechSynthesisResult.Error("Speech synthesis failed: ${e.message}"))
            stopSpeaking()
        }
    }
    
    override suspend fun generateAudio(
        text: String,
        config: SpeechConfig,
        outputFile: File
    ): Result<AudioGenerationResult> = withContext(Dispatchers.IO) {
        
        if (!isTTSModelLoaded) {
            return@withContext Result.failure(VoiceException("No TTS model loaded"))
        }
        
        try {
            val cleanText = text.trim()
            if (cleanText.isEmpty()) {
                return@withContext Result.failure(VoiceException("Empty text"))
            }
            
            // Synthesize complete audio
            val synthesisResult = nativeEngine.synthesizeTextToFile(
                text = cleanText,
                voiceConfig = config.toVoiceConfig(),
                outputFile = outputFile,
                outputFormat = AudioFormat.WAV
            )
            
            if (synthesisResult.isSuccess) {
                val result = synthesisResult.getOrNull()!!
                Result.success(AudioGenerationResult(
                    outputFile = outputFile,
                    duration = result.duration,
                    sampleRate = result.sampleRate,
                    fileSize = outputFile.length(),
                    characterCount = cleanText.length
                ))
            } else {
                Result.failure(synthesisResult.exceptionOrNull() ?: VoiceException("Audio generation failed"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Audio generation failed", e)
            Result.failure(VoiceException("Audio generation failed", e))
        }
    }
    
    override suspend fun stopSpeaking(): Boolean {
        return try {
            if (isPlaying) {
                audioProcessor.stopPlayback()
                isPlaying = false
                currentSpeechSession = null
                
                Log.d(TAG, "Speech synthesis stopped")
                eventBus.emit(IrisEvent.SpeechStopped())
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop speaking", e)
            false
        }
    }
    
    override suspend fun pauseSpeaking(): Boolean {
        return try {
            if (isPlaying) {
                audioProcessor.pausePlayback()
                Log.d(TAG, "Speech synthesis paused")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause speaking", e)
            false
        }
    }
    
    override suspend fun resumeSpeaking(): Boolean {
        return try {
            if (isPlaying) {
                audioProcessor.resumePlayback()
                Log.d(TAG, "Speech synthesis resumed")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume speaking", e)
            false
        }
    }
    
    override suspend fun getAvailableVoices(): List<VoiceDescriptor> {
        return currentTTSModel?.availableVoices ?: emptyList()
    }
    
    override suspend fun getCurrentModel(): TTSModelDescriptor? {
        return currentTTSModel
    }
    
    override suspend fun isModelLoaded(): Boolean {
        return isTTSModelLoaded
    }
    
    override suspend fun isSpeaking(): Boolean {
        return isPlaying
    }
    
    // Private helper methods
    
    private fun validateTTSModel(model: TTSModelDescriptor): ModelValidationResult {
        val deviceProfile = deviceProfileProvider.getDeviceProfile()
        
        // Check memory requirements
        if (deviceProfile.totalRAM < model.memoryRequirements.minRAM) {
            return ModelValidationResult(
                isValid = false,
                reason = "Insufficient RAM for TTS model",
                issues = listOf(ValidationIssue.INSUFFICIENT_MEMORY)
            )
        }
        
        // Check audio capabilities
        if (!deviceProfile.capabilities.contains(HardwareCapability.SPEAKER)) {
            return ModelValidationResult(
                isValid = false,
                reason = "No audio output available",
                issues = listOf(ValidationIssue.HARDWARE_MISSING)
            )
        }
        
        return ModelValidationResult(
            isValid = true,
            reason = "TTS model compatible",
            issues = emptyList()
        )
    }
    
    private fun selectOptimalTTSBackend(model: TTSModelDescriptor): TTSBackend {
        val deviceProfile = deviceProfileProvider.getDeviceProfile()
        
        return when {
            model.supportedBackends.contains(TTSBackend.NPU) && 
                deviceProfile.capabilities.contains(HardwareCapability.QNN) -> TTSBackend.NPU
            
            model.supportedBackends.contains(TTSBackend.GPU) && 
                deviceProfile.capabilities.contains(HardwareCapability.OPENCL) -> TTSBackend.GPU
            
            else -> TTSBackend.CPU
        }
    }
    
    private fun splitTextIntoChunks(text: String): List<String> {
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()
        
        // Split by sentences first
        val sentences = text.split(Regex("[.!?]+\\s+"))
        
        for (sentence in sentences) {
            if (currentChunk.length + sentence.length <= CHUNK_SIZE) {
                if (currentChunk.isNotEmpty()) {
                    currentChunk.append(" ")
                }
                currentChunk.append(sentence)
            } else {
                // Finalize current chunk
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk.clear()
                }
                
                // Handle long sentences
                if (sentence.length > CHUNK_SIZE) {
                    val words = sentence.split(" ")
                    for (word in words) {
                        if (currentChunk.length + word.length + 1 <= CHUNK_SIZE) {
                            if (currentChunk.isNotEmpty()) {
                                currentChunk.append(" ")
                            }
                            currentChunk.append(word)
                        } else {
                            if (currentChunk.isNotEmpty()) {
                                chunks.add(currentChunk.toString().trim())
                                currentChunk.clear()
                            }
                            currentChunk.append(word)
                        }
                    }
                } else {
                    currentChunk.append(sentence)
                }
            }
        }
        
        // Add final chunk
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }
        
        return chunks.ifEmpty { listOf(text) }
    }
    
    private fun generateSessionId(): String {
        return "tts_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    private fun getModelPath(model: TTSModelDescriptor): String {
        return File(
            File(context.getExternalFilesDir(null), "models"),
            "${model.id}.bin"
        ).absolutePath
    }
}

// TTS data structures
data class TTSModelDescriptor(
    val id: String,
    val name: String,
    val description: String,
    val language: String,
    val supportedLanguages: List<String>,
    val audioOutput: AudioOutputSpec,
    val memoryRequirements: MemoryRequirements,
    val supportedBackends: List<TTSBackend>,
    val availableVoices: List<VoiceDescriptor>,
    val defaultVoice: VoiceDescriptor,
    val quality: TTSQuality,
    val fileSize: Long
)

data class AudioOutputSpec(
    val sampleRate: Int,
    val bitDepth: Int,
    val channels: Int,
    val supportedFormats: List<String>
)

data class VoiceDescriptor(
    val id: String,
    val name: String,
    val gender: VoiceGender,
    val age: VoiceAge,
    val language: String,
    val accent: String?,
    val description: String
)

enum class VoiceGender {
    MALE, FEMALE, NEUTRAL
}

enum class VoiceAge {
    CHILD, YOUNG_ADULT, ADULT, ELDERLY
}

enum class TTSBackend {
    CPU, GPU, NPU
}

enum class TTSQuality {
    LOW, MEDIUM, HIGH, PREMIUM
}

data class SpeechConfig(
    val voiceId: String? = null,
    val speakingRate: Float = DEFAULT_SPEAKING_RATE,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f,
    val language: String? = null,
    val enableProsody: Boolean = true
) {
    fun toVoiceConfig(): VoiceConfig {
        return VoiceConfig(
            voiceId = voiceId,
            speakingRate = speakingRate,
            pitch = pitch,
            volume = volume,
            language = language,
            enableProsody = enableProsody
        )
    }
}

data class SpeechSession(
    val sessionId: String,
    val text: String,
    val config: SpeechConfig,
    val startTime: Long
)

// Speech synthesis results
sealed class SpeechSynthesisResult {
    data class SynthesisStarted(val sessionId: String) : SpeechSynthesisResult()
    data class AudioStarted(val currentText: String) : SpeechSynthesisResult()
    data class SpeechProgress(
        val progress: Float,
        val currentText: String,
        val elapsedTime: Long
    ) : SpeechSynthesisResult()
    data class SynthesisCompleted(
        val sessionId: String,
        val totalDuration: Long,
        val characterCount: Int
    ) : SpeechSynthesisResult()
    data class Error(val message: String) : SpeechSynthesisResult()
}

data class AudioGenerationResult(
    val outputFile: File,
    val duration: Long,
    val sampleRate: Int,
    val fileSize: Long,
    val characterCount: Int
)

// Audio playback events
sealed class AudioPlaybackEvent {
    object Started : AudioPlaybackEvent()
    data class Progress(val progress: Float, val elapsedTime: Long) : AudioPlaybackEvent()
    data class Completed(val duration: Long) : AudioPlaybackEvent()
    data class Error(val message: String) : AudioPlaybackEvent()
}

enum class AudioFormat {
    PCM_16BIT, WAV, MP3, OGG
}

class VoiceException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

## 🧪 Testing Strategy

### Unit Tests
- [ ] **Voice Engine Logic**
  - STT model loading and validation
  - TTS synthesis accuracy
  - VAD algorithm effectiveness
  - Audio processing pipeline

### Integration Tests
- [ ] **Voice Workflows**
  - End-to-end speech recognition
  - Text-to-speech generation
  - Voice conversation flow
  - Audio quality validation

### Performance Tests
- [ ] **Voice Processing Performance**
  - STT latency benchmarks
  - TTS generation speed
  - Memory usage optimization
  - Audio playback quality

### UI Tests
- [ ] **Voice Interface**
  - Voice input controls
  - Speech visualization
  - Audio playback controls
  - Error state handling

## ✅ Acceptance Criteria

### Primary Criteria
- [ ] **Speech Recognition**: Accurate STT with <3s latency
- [ ] **Voice Synthesis**: Natural-sounding TTS output
- [ ] **Voice Activity Detection**: Reliable speech detection
- [ ] **Audio Quality**: Clear audio input/output processing
- [ ] **Multi-language Support**: STT/TTS for major languages

### Technical Criteria
- [ ] **STT Accuracy**: >95% accuracy for clear speech
- [ ] **TTS Quality**: Natural-sounding voices with proper prosody
- [ ] **Latency**: STT processing <1s for short phrases
- [ ] **Memory Efficiency**: Voice processing <1GB peak memory

### User Experience Criteria
- [ ] **Intuitive Controls**: Easy voice input activation
- [ ] **Visual Feedback**: Clear recording and playback indicators
- [ ] **Error Handling**: Graceful handling of audio issues
- [ ] **Accessibility**: Voice controls for hands-free operation

## 🔗 Related Issues
- **Depends on**: #01 (Core Architecture), #05 (Chat Engine), #07 (Multimodal Support)
- **Enables**: #14 (UI/UX Implementation)
- **Related**: #04 (Model Management), #12 (Performance Optimization)

## 📋 Definition of Done
- [ ] Complete STT engine with streaming recognition
- [ ] High-quality TTS engine with multiple voices
- [ ] Voice activity detection and audio processing
- [ ] Voice-based conversation integration
- [ ] Multi-language support for STT/TTS
- [ ] Comprehensive test suite covering all voice scenarios
- [ ] Performance benchmarks meet acceptance criteria
- [ ] Voice UI components functional
- [ ] Documentation complete with supported models and languages
- [ ] Code review completed and approved

---

**Note**: This voice processing system enables natural speech interaction while maintaining privacy through on-device speech recognition and synthesis.