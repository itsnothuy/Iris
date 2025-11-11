package com.nervesparks.iris.core.multimodal.voice

import com.nervesparks.iris.core.multimodal.audio.*
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Speech-to-Text engine for converting voice to text
 */
interface SpeechToTextEngine {
    /**
     * Load a speech-to-text model
     */
    suspend fun loadSTTModel(model: STTModelDescriptor): Result<Unit>
    
    /**
     * Start listening and streaming speech recognition results
     */
    suspend fun startListening(config: ListeningConfig): Flow<SpeechRecognitionResult>
    
    /**
     * Stop listening and end the current recognition session
     */
    suspend fun stopListening(): Boolean
    
    /**
     * Transcribe an audio file
     */
    suspend fun transcribeAudio(audioFile: File, language: String? = null): Result<TranscriptionResult>
    
    /**
     * Get available languages for the current model
     */
    suspend fun getAvailableLanguages(): List<String>
    
    /**
     * Get the currently loaded STT model
     */
    suspend fun getCurrentModel(): STTModelDescriptor?
    
    /**
     * Check if an STT model is loaded
     */
    suspend fun isModelLoaded(): Boolean
    
    /**
     * Check if currently listening
     */
    suspend fun isListening(): Boolean
}

/**
 * Text-to-Speech engine for converting text to voice
 */
interface TextToSpeechEngine {
    /**
     * Load a text-to-speech model
     */
    suspend fun loadTTSModel(model: TTSModelDescriptor): Result<Unit>
    
    /**
     * Synthesize speech from text
     */
    suspend fun synthesizeSpeech(
        text: String,
        parameters: SpeechParameters = SpeechParameters()
    ): Result<AudioData>
    
    /**
     * Stream audio synthesis
     */
    fun streamSpeech(
        text: String,
        parameters: SpeechParameters = SpeechParameters()
    ): Flow<AudioChunk>
    
    /**
     * Speak text with playback
     */
    suspend fun speak(
        text: String,
        parameters: SpeechParameters = SpeechParameters()
    ): Result<Unit>
    
    /**
     * Stop current speech playback
     */
    suspend fun stopSpeaking(): Boolean
    
    /**
     * Pause current speech playback
     */
    suspend fun pause(): Boolean
    
    /**
     * Resume paused speech playback
     */
    suspend fun resume(): Boolean
    
    /**
     * Get available voices for the current model
     */
    suspend fun getAvailableVoices(): List<VoiceDescriptor>
    
    /**
     * Get the currently loaded TTS model
     */
    suspend fun getCurrentModel(): TTSModelDescriptor?
    
    /**
     * Check if a TTS model is loaded
     */
    suspend fun isModelLoaded(): Boolean
    
    /**
     * Check if currently speaking
     */
    suspend fun isSpeaking(): Boolean
}

/**
 * Audio processor for recording and playback
 */
interface AudioProcessor {
    /**
     * Start recording audio
     */
    suspend fun startRecording(
        sampleRate: Int,
        channels: Int,
        config: AudioConfig
    ): Flow<AudioData>
    
    /**
     * Stop recording audio
     */
    suspend fun stopRecording()
    
    /**
     * Play audio data
     */
    suspend fun playAudio(
        audioData: FloatArray,
        sampleRate: Int
    ): Result<Unit>
    
    /**
     * Stop audio playback
     */
    suspend fun stopPlayback()
    
    /**
     * Load audio file
     */
    suspend fun loadAudioFile(file: File): Result<FloatArray>
    
    /**
     * Save audio to file
     */
    suspend fun saveAudioFile(
        audioData: FloatArray,
        file: File,
        sampleRate: Int,
        format: AudioFileFormat
    ): Result<Unit>
}
