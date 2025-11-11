package com.nervesparks.iris.core.multimodal.voice

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * STT model descriptor
 */
@Parcelize
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
) : Parcelable

/**
 * TTS model descriptor
 */
@Parcelize
data class TTSModelDescriptor(
    val id: String,
    val name: String,
    val description: String,
    val supportedLanguages: List<String>,
    val supportedVoices: List<VoiceDescriptor>,
    val audioFormat: AudioFormat,
    val memoryRequirements: MemoryRequirements,
    val supportedBackends: List<TTSBackend>,
    val quality: Float,
    val fileSize: Long
) : Parcelable

/**
 * Voice descriptor for TTS
 */
@Parcelize
data class VoiceDescriptor(
    val id: String,
    val name: String,
    val language: String,
    val gender: VoiceGender,
    val style: VoiceStyle = VoiceStyle.NEUTRAL
) : Parcelable

/**
 * Audio requirements for voice models
 */
@Parcelize
data class AudioRequirements(
    val sampleRate: Int,
    val channels: Int,
    val bitDepth: Int,
    val supportedFormats: List<String>
) : Parcelable

/**
 * Memory requirements for voice models
 */
@Parcelize
data class MemoryRequirements(
    val minRAM: Long,
    val recommendedRAM: Long,
    val modelSize: Long
) : Parcelable

/**
 * Audio format specification
 */
@Parcelize
data class AudioFormat(
    val sampleRate: Int,
    val channels: Int,
    val bitDepth: Int,
    val encoding: AudioEncoding
) : Parcelable

/**
 * Speech recognition result types
 */
sealed class SpeechRecognitionResult {
    data class ListeningStarted(val sessionId: String) : SpeechRecognitionResult()
    data class SpeechDetected(val timestamp: Long = System.currentTimeMillis()) : SpeechRecognitionResult()
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
    data class ListeningStopped(val timestamp: Long = System.currentTimeMillis()) : SpeechRecognitionResult()
    data class MaxDurationReached(val duration: Long = 0) : SpeechRecognitionResult()
    data class Error(val message: String) : SpeechRecognitionResult()
}

/**
 * Transcription result
 */
data class TranscriptionResult(
    val text: String,
    val confidence: Float,
    val segments: List<TranscriptionSegment>,
    val duration: Long,
    val language: String
)

/**
 * Transcription segment
 */
data class TranscriptionSegment(
    val text: String,
    val startTime: Float,
    val endTime: Float,
    val confidence: Float
)

/**
 * Listening configuration
 */
data class ListeningConfig(
    val streamingMode: Boolean = true,
    val endOfSpeechSilenceMs: Int = 1500,
    val maxDurationMs: Int = 60000,
    val language: String? = null,
    val audioConfig: AudioConfig = AudioConfig()
)

/**
 * Audio configuration
 */
data class AudioConfig(
    val noiseReduction: Boolean = true,
    val automaticGainControl: Boolean = true,
    val echoCancellation: Boolean = true
)

/**
 * Speech synthesis parameters
 */
data class SpeechParameters(
    val speakingRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f,
    val voice: VoiceDescriptor? = null
)

/**
 * Recording session information
 */
data class RecordingSession(
    val sessionId: String,
    val startTime: Long,
    val config: ListeningConfig
)

/**
 * Speech synthesis session
 */
data class SpeechSession(
    val sessionId: String,
    val text: String,
    val startTime: Long,
    val parameters: SpeechParameters
)

/**
 * STT backend types
 */
enum class STTBackend {
    CPU, GPU, NPU
}

/**
 * TTS backend types
 */
enum class TTSBackend {
    CPU, GPU, NPU
}

/**
 * Voice gender
 */
enum class VoiceGender {
    MALE, FEMALE, NEUTRAL
}

/**
 * Voice style
 */
enum class VoiceStyle {
    NEUTRAL, FRIENDLY, PROFESSIONAL, CALM, ENERGETIC
}

/**
 * Audio encoding types
 */
enum class AudioEncoding {
    PCM_16BIT, PCM_8BIT, PCM_FLOAT, MP3, AAC, OPUS
}

/**
 * Voice Activity Detection result
 */
enum class VADResult {
    SPEECH, SILENCE, NOISE
}

/**
 * Model validation result
 */
data class ModelValidationResult(
    val isValid: Boolean,
    val reason: String,
    val issues: List<ValidationIssue>
)

/**
 * Validation issues
 */
enum class ValidationIssue {
    INSUFFICIENT_MEMORY,
    HARDWARE_MISSING,
    UNSUPPORTED_FEATURE,
    MODEL_NOT_FOUND,
    CORRUPTED_MODEL
}

/**
 * STT configuration
 */
data class STTConfig(
    val sampleRate: Int,
    val channels: Int,
    val language: String,
    val backend: STTBackend
)

/**
 * TTS configuration
 */
data class TTSConfig(
    val sampleRate: Int,
    val channels: Int,
    val voice: VoiceDescriptor,
    val backend: TTSBackend
)

/**
 * Partial transcription result (for streaming)
 */
data class PartialTranscriptionResult(
    val text: String,
    val confidence: Float,
    val isFinal: Boolean
)

/**
 * Final transcription result (complete audio)
 */
data class FinalTranscriptionResult(
    val text: String,
    val confidence: Float,
    val segments: List<TranscriptionSegment>
)
