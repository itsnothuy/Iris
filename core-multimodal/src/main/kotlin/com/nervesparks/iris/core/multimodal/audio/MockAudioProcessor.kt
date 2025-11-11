package com.nervesparks.iris.core.multimodal.audio

import com.nervesparks.iris.core.multimodal.voice.AudioConfig
import com.nervesparks.iris.core.multimodal.voice.AudioProcessor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject

/**
 * Mock implementation of AudioProcessor for testing
 */
class MockAudioProcessor @Inject constructor() : AudioProcessor {
    
    private var recording = false
    
    override suspend fun startRecording(
        sampleRate: Int,
        channels: Int,
        config: AudioConfig
    ): Flow<AudioData> = flow {
        recording = true
        // Emit some mock audio chunks
        repeat(5) {
            val samples = FloatArray(1000) { index -> (index % 100) / 100.0f }
            emit(AudioData.Chunk(samples, System.currentTimeMillis()))
            kotlinx.coroutines.delay(100)
        }
        emit(AudioData.Ended)
        recording = false
    }
    
    override suspend fun stopRecording() {
        recording = false
    }
    
    override suspend fun playAudio(audioData: FloatArray, sampleRate: Int): Result<Unit> {
        // Simulate playback
        kotlinx.coroutines.delay(100)
        return Result.success(Unit)
    }
    
    override suspend fun stopPlayback() {
        // No-op for mock
    }
    
    override suspend fun loadAudioFile(file: File): Result<FloatArray> {
        val samples = FloatArray(1000) { 0.0f }
        return Result.success(samples)
    }
    
    override suspend fun saveAudioFile(
        audioData: FloatArray,
        file: File,
        sampleRate: Int,
        format: AudioFileFormat
    ): Result<Unit> {
        return Result.success(Unit)
    }
}
