package com.nervesparks.iris.core.multimodal.voice

import com.nervesparks.iris.core.multimodal.audio.AudioChunk
import com.nervesparks.iris.core.multimodal.audio.AudioData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Mock implementation of TextToSpeechEngine for testing
 */
class MockTextToSpeechEngine @Inject constructor() : TextToSpeechEngine {
    
    private var modelLoaded = false
    private var speaking = false
    private var currentModel: TTSModelDescriptor? = null
    
    override suspend fun loadTTSModel(model: TTSModelDescriptor): Result<Unit> {
        currentModel = model
        modelLoaded = true
        return Result.success(Unit)
    }
    
    override suspend fun synthesizeSpeech(
        text: String,
        parameters: SpeechParameters
    ): Result<AudioData> {
        val samples = FloatArray(1000) { 0.0f }
        return Result.success(AudioData.Chunk(samples, System.currentTimeMillis()))
    }
    
    override fun streamSpeech(
        text: String,
        parameters: SpeechParameters
    ): Flow<AudioChunk> = flow {
        val samples = FloatArray(100) { 0.0f }
        emit(AudioChunk(samples, 22050, System.currentTimeMillis()))
    }
    
    override suspend fun speak(
        text: String,
        parameters: SpeechParameters
    ): Result<Unit> {
        speaking = true
        // Simulate speaking
        kotlinx.coroutines.delay(100)
        speaking = false
        return Result.success(Unit)
    }
    
    override suspend fun stopSpeaking(): Boolean {
        speaking = false
        return true
    }
    
    override suspend fun pause(): Boolean {
        return true
    }
    
    override suspend fun resume(): Boolean {
        return true
    }
    
    override suspend fun getAvailableVoices(): List<VoiceDescriptor> {
        return listOf(
            VoiceDescriptor("mock_voice", "Mock Voice", "en", VoiceGender.NEUTRAL)
        )
    }
    
    override suspend fun getCurrentModel(): TTSModelDescriptor? {
        return currentModel
    }
    
    override suspend fun isModelLoaded(): Boolean {
        return modelLoaded
    }
    
    override suspend fun isSpeaking(): Boolean {
        return speaking
    }
}
