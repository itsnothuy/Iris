package com.nervesparks.iris.core.multimodal.voice

import com.nervesparks.iris.core.multimodal.audio.AudioData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject

/**
 * Mock implementation of SpeechToTextEngine for testing
 */
class MockSpeechToTextEngine @Inject constructor() : SpeechToTextEngine {
    
    private var modelLoaded = false
    private var listening = false
    private var currentModel: STTModelDescriptor? = null
    
    override suspend fun loadSTTModel(model: STTModelDescriptor): Result<Unit> {
        currentModel = model
        modelLoaded = true
        return Result.success(Unit)
    }
    
    override suspend fun startListening(config: ListeningConfig): Flow<SpeechRecognitionResult> = flow {
        listening = true
        emit(SpeechRecognitionResult.ListeningStarted("mock_session"))
        emit(SpeechRecognitionResult.SpeechDetected())
        emit(SpeechRecognitionResult.PartialTranscription("Mock transcription", 0.9f, false))
        emit(SpeechRecognitionResult.FinalTranscription("Mock transcription complete", 0.95f, 1000))
        emit(SpeechRecognitionResult.ListeningStopped())
        listening = false
    }
    
    override suspend fun stopListening(): Boolean {
        listening = false
        return true
    }
    
    override suspend fun transcribeAudio(audioFile: File, language: String?): Result<TranscriptionResult> {
        return Result.success(
            TranscriptionResult(
                text = "Mock transcription from file",
                confidence = 0.9f,
                segments = emptyList(),
                duration = 1000,
                language = language ?: "en"
            )
        )
    }
    
    override suspend fun getAvailableLanguages(): List<String> {
        return listOf("en", "es", "fr")
    }
    
    override suspend fun getCurrentModel(): STTModelDescriptor? {
        return currentModel
    }
    
    override suspend fun isModelLoaded(): Boolean {
        return modelLoaded
    }
    
    override suspend fun isListening(): Boolean {
        return listening
    }
}
