package com.nervesparks.iris.core.multimodal.voice

import android.content.Context
import android.util.Log
import com.nervesparks.iris.common.error.VoiceException
import com.nervesparks.iris.core.hw.DeviceProfileProvider
import com.nervesparks.iris.common.models.HardwareCapability
import com.nervesparks.iris.core.multimodal.audio.AudioData
import com.nervesparks.iris.core.multimodal.audio.AudioProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Implementation of Speech-to-Text engine
 * 
 * Note: This implementation provides the infrastructure for STT with placeholder
 * native integration points. Full STT functionality requires integration with
 * a native speech recognition library (e.g., Whisper.cpp, Vosk, etc.)
 */
@Singleton
class SpeechToTextEngineImpl @Inject constructor(
    private val audioProcessor: AudioProcessor,
    private val deviceProfileProvider: DeviceProfileProvider,
    @ApplicationContext private val context: Context
) : SpeechToTextEngine {
    
    companion object {
        private const val TAG = "SpeechToTextEngine"
        private const val DEFAULT_SAMPLE_RATE = 16000
        private const val DEFAULT_CHANNELS = 1
        private const val SILENCE_THRESHOLD_DB = -30.0f
        private const val MAX_RECORDING_DURATION_MS = 60000 // 60 seconds
        private const val VAD_WINDOW_MS = 100
        
        // Native library loading - only loads if library exists
        private var nativeLibraryLoaded = false
        
        init {
            try {
                System.loadLibrary("iris_multimodal")
                nativeLibraryLoaded = true
                Log.i(TAG, "Native multimodal library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Native multimodal library not available, using mock mode", e)
                nativeLibraryLoaded = false
            }
        }
    }
    
    private var currentSTTModel: STTModelDescriptor? = null
    private var isSTTModelLoaded = false
    private var isRecording = false
    private var currentRecordingSession: RecordingSession? = null
    
    override suspend fun loadSTTModel(model: STTModelDescriptor): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Loading STT model: ${model.id}")
                Log.d(TAG, "STT model load started: ${model.id}")
                
                // Validate model compatibility
                val validation = validateSTTModel(model)
                if (!validation.isValid) {
                    val error = VoiceException("STT model validation failed: ${validation.reason}")
                    Log.e(TAG, "STT model load failed: ${model.id} - ${validation.reason}")
                    return@withContext Result.failure(error)
                }
                
                // Load STT model (placeholder implementation for on-device inference)
                // Production: This would load Whisper.cpp or similar native STT engine
                // For now, validate model file and store configuration
                val modelPath = getModelPath(model)
                val modelFile = File(modelPath)
                
                if (!modelFile.exists()) {
                    Log.w(TAG, "Model file not found at: $modelPath, using mock mode")
                    // In development, continue with mock mode
                    // In production with native engine: return Result.failure(VoiceException("Model file not found"))
                }
                
                // Select optimal backend for this device
                val selectedBackend = selectOptimalSTTBackend(model)
                Log.i(TAG, "Selected STT backend: $selectedBackend for device capabilities")
                
                // Store model configuration for inference
                currentSTTModel = model
                isSTTModelLoaded = true
                
                Log.i(TAG, "STT model loaded successfully: ${model.id}")
                Log.d(TAG, "STT model load completed: ${model.id}")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception during STT model loading", e)
                Log.e(TAG, "STT model load failed: ${model.id} - ${e.message ?: "Exception"}")
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
                                if (config.streamingMode) {
                                    processAudioChunk(audioData.samples, config.streamingMode)?.let { partialResult ->
                                        emit(SpeechRecognitionResult.PartialTranscription(
                                            text = partialResult.text,
                                            confidence = partialResult.confidence,
                                            isFinal = partialResult.isFinal
                                        ))
                                    }
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
                            
                            emit(SpeechRecognitionResult.MaxDurationReached(recordingDuration))
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
            
            // Transcribe audio through inference engine
            // Production: This would use Whisper.cpp native inference
            // Current: Mock implementation for testing infrastructure
            val durationMs = (samples.size * 1000L) / currentSTTModel!!.audioRequirements.sampleRate
            val durationSec = samples.size / currentSTTModel!!.audioRequirements.sampleRate.toFloat()
            
            // Analyze audio characteristics for realistic mock transcription
            val audioEnergy = sqrt(samples.map { it * it }.average().toFloat())
            val hasSignificantAudio = audioEnergy > 0.01f
            
            // Generate mock transcription based on audio characteristics
            val transcriptionText = if (hasSignificantAudio) {
                "Transcribed audio from ${audioFile.name} (${durationMs}ms, energy: %.3f)".format(audioEnergy)
            } else {
                "[silence detected]"
            }
            
            val result = TranscriptionResult(
                text = transcriptionText,
                confidence = if (hasSignificantAudio) 0.85f else 0.95f, // High confidence for silence detection
                segments = listOf(
                    TranscriptionSegment(
                        text = transcriptionText,
                        startTime = 0.0f,
                        endTime = durationSec,
                        confidence = if (hasSignificantAudio) 0.85f else 0.95f
                    )
                ),
                duration = durationMs,
                language = language ?: currentSTTModel!!.language
            )
            
            Result.success(result)
            
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
                // Enhanced Voice Activity Detection using multiple features
                // Production: Could use WebRTC VAD or Silero VAD for better accuracy
                
                // Feature 1: RMS Energy
                val rms = sqrt(audioSamples.map { it * it }.average().toFloat())
                val energyDb = 20 * log10(rms + 1e-8f)
                
                // Feature 2: Zero Crossing Rate (ZCR)
                var zeroCrossings = 0
                for (i in 1 until audioSamples.size) {
                    if ((audioSamples[i] >= 0 && audioSamples[i - 1] < 0) ||
                        (audioSamples[i] < 0 && audioSamples[i - 1] >= 0)) {
                        zeroCrossings++
                    }
                }
                val zcr = zeroCrossings.toFloat() / audioSamples.size
                
                // Feature 3: Spectral Centroid (simplified estimation)
                val spectralCentroid = calculateSimpleSpectralCentroid(audioSamples)
                
                // Decision logic combining multiple features
                val isSpeech = when {
                    // High energy and moderate ZCR indicates speech
                    energyDb > SILENCE_THRESHOLD_DB + 10 && 
                    zcr > 0.02f && zcr < 0.3f &&
                    spectralCentroid > 200f -> true
                    
                    // Medium energy with good spectral characteristics
                    energyDb > SILENCE_THRESHOLD_DB + 5 &&
                    spectralCentroid > 300f -> true
                    
                    else -> false
                }
                
                val isNoise = !isSpeech && energyDb > SILENCE_THRESHOLD_DB
                
                when {
                    isSpeech -> VADResult.SPEECH
                    isNoise -> VADResult.NOISE
                    else -> VADResult.SILENCE
                }
            } catch (e: Exception) {
                Log.w(TAG, "VAD processing failed", e)
                VADResult.NOISE
            }
        }
    }
    
    /**
     * Calculate a simplified spectral centroid
     * Returns estimated center frequency of the signal
     */
    private fun calculateSimpleSpectralCentroid(samples: FloatArray): Float {
        if (samples.isEmpty()) return 0f
        
        // Simple frequency estimation using zero-crossings in windows
        val windowSize = 100
        var totalCentroid = 0f
        var windowCount = 0
        
        for (i in 0 until samples.size - windowSize step windowSize / 2) {
            val window = samples.sliceArray(i until minOf(i + windowSize, samples.size))
            val windowEnergy = sqrt(window.map { it * it }.average().toFloat())
            
            if (windowEnergy > 0.01f) {
                // Count zero crossings in this window
                var zc = 0
                for (j in 1 until window.size) {
                    if ((window[j] >= 0 && window[j - 1] < 0) ||
                        (window[j] < 0 && window[j - 1] >= 0)) {
                        zc++
                    }
                }
                
                // Estimate frequency from zero-crossings
                val estimatedFreq = (zc * DEFAULT_SAMPLE_RATE.toFloat()) / (2 * window.size)
                totalCentroid += estimatedFreq * windowEnergy
                windowCount++
            }
        }
        
        return if (windowCount > 0) totalCentroid / windowCount else 0f
    }
    
    private suspend fun processAudioChunk(
        audioSamples: FloatArray,
        streamingMode: Boolean
    ): PartialTranscriptionResult? {
        return if (streamingMode) {
            try {
                // Process audio chunk for streaming recognition
                // Production: This would use Whisper.cpp streaming API
                // Current: Mock implementation for partial transcription
                
                // Analyze audio characteristics
                val audioEnergy = sqrt(audioSamples.map { it * it }.average().toFloat())
                
                if (audioEnergy < 0.01f) {
                    // Too quiet, likely silence
                    return null
                }
                
                // Generate mock partial transcription based on audio energy
                val confidenceScore = (audioEnergy * 10).coerceIn(0.3f, 0.9f)
                
                PartialTranscriptionResult(
                    text = "[partial: ${audioSamples.size} samples, energy: %.3f]".format(audioEnergy),
                    confidence = confidenceScore,
                    isFinal = false
                )
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
            
            // Process complete audio for final transcription
            // Production: This would use Whisper.cpp full inference
            // Current: Mock implementation with audio analysis
            
            // Analyze combined audio
            val audioEnergy = sqrt(combinedAudio.map { it * it }.average().toFloat())
            val durationSec = combinedAudio.size / currentSTTModel!!.audioRequirements.sampleRate.toFloat()
            val hasSignificantAudio = audioEnergy > 0.01f
            
            // Generate realistic mock transcription
            val transcriptionText = if (hasSignificantAudio) {
                "Final transcription: ${combinedAudio.size} samples (%.2fs, energy: %.3f)".format(durationSec, audioEnergy)
            } else {
                "[silence]"
            }
            
            // Create segments (split long audio into reasonable chunks)
            val segments = mutableListOf<TranscriptionSegment>()
            val segmentDuration = 5.0f // 5 seconds per segment
            val samplesPerSegment = (segmentDuration * currentSTTModel!!.audioRequirements.sampleRate).toInt()
            var currentTime = 0.0f
            var currentSample = 0
            
            while (currentSample < combinedAudio.size) {
                val segmentEnd = minOf(currentSample + samplesPerSegment, combinedAudio.size)
                val segmentSamples = combinedAudio.sliceArray(currentSample until segmentEnd)
                val segmentEnergy = sqrt(segmentSamples.map { it * it }.average().toFloat())
                val segmentHasAudio = segmentEnergy > 0.01f
                
                if (segmentHasAudio) {
                    val segmentText = "Segment at %.1fs".format(currentTime)
                    segments.add(
                        TranscriptionSegment(
                            text = segmentText,
                            startTime = currentTime,
                            endTime = currentTime + (segmentSamples.size / currentSTTModel!!.audioRequirements.sampleRate.toFloat()),
                            confidence = (segmentEnergy * 10).coerceIn(0.7f, 0.95f)
                        )
                    )
                }
                
                currentSample = segmentEnd
                currentTime += segmentDuration
            }
            
            FinalTranscriptionResult(
                text = transcriptionText,
                confidence = if (hasSignificantAudio) 0.8f else 0.95f,
                segments = if (segments.isEmpty()) {
                    listOf(
                        TranscriptionSegment(
                            text = transcriptionText,
                            startTime = 0.0f,
                            endTime = durationSec,
                            confidence = 0.95f
                        )
                    )
                } else {
                    segments
                }
            )
            
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
    
    // =========================================================================
    // Native Method Declarations (JNI Bridge)
    // =========================================================================
    // These methods are implemented in native C++ code (whisper_android.cpp)
    // They will only be called if nativeLibraryLoaded is true
    
    /**
     * Load a Whisper STT model into native memory
     * @param modelPath Path to the Whisper model file (.bin)
     * @return Native context pointer (0 if failed)
     */
    private external fun nativeLoadWhisperModel(modelPath: String): Long
    
    /**
     * Transcribe audio using the loaded Whisper model
     * @param contextPtr Native context pointer from nativeLoadWhisperModel
     * @param audioData Audio samples as float array (normalized -1.0 to 1.0)
     * @param language Language code (e.g., "en", "es", "fr")
     * @return Transcribed text or null if failed
     */
    private external fun nativeTranscribeAudio(
        contextPtr: Long,
        audioData: FloatArray,
        language: String
    ): String?
    
    /**
     * Unload a Whisper model and free native memory
     * @param contextPtr Native context pointer from nativeLoadWhisperModel
     */
    private external fun nativeUnloadWhisperModel(contextPtr: Long)
}
