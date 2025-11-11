package com.nervesparks.iris.core.multimodal

import com.nervesparks.iris.core.multimodal.audio.AudioData
import kotlin.math.PI
import kotlin.math.sin

/**
 * Utility class for generating mock audio data for testing
 */
object TestAudioUtils {
    
    /**
     * Generate a sine wave audio signal
     * @param frequency Frequency in Hz
     * @param duration Duration in milliseconds
     * @param sampleRate Sample rate in Hz (default: 16000)
     * @param amplitude Amplitude (0.0 to 1.0, default: 0.5)
     */
    fun generateSineWave(
        frequency: Int,
        duration: Int,
        sampleRate: Int = 16000,
        amplitude: Float = 0.5f
    ): AudioData.Chunk {
        val numSamples = (sampleRate * duration) / 1000
        val samples = FloatArray(numSamples) { index ->
            val time = index.toDouble() / sampleRate
            (amplitude * sin(2.0 * PI * frequency * time)).toFloat()
        }
        return AudioData.Chunk(
            samples = samples,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Generate white noise audio signal
     * @param amplitude Amplitude (0.0 to 1.0, default: 0.5)
     * @param duration Duration in milliseconds
     * @param sampleRate Sample rate in Hz (default: 16000)
     */
    fun generateWhiteNoise(
        amplitude: Float,
        duration: Int,
        sampleRate: Int = 16000
    ): AudioData.Chunk {
        val numSamples = (sampleRate * duration) / 1000
        val samples = FloatArray(numSamples) {
            amplitude * (2 * Math.random().toFloat() - 1)
        }
        return AudioData.Chunk(
            samples = samples,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Generate silence (zeros)
     * @param duration Duration in milliseconds
     * @param sampleRate Sample rate in Hz (default: 16000)
     */
    fun generateSilence(
        duration: Int,
        sampleRate: Int = 16000
    ): AudioData.Chunk {
        val numSamples = (sampleRate * duration) / 1000
        val samples = FloatArray(numSamples) { 0.0f }
        return AudioData.Chunk(
            samples = samples,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Generate speech-like audio with varying frequency and amplitude
     * Simulates realistic speech patterns
     */
    fun generateSpeechLikeAudio(
        text: String,
        sampleRate: Int = 16000
    ): AudioData.Chunk {
        // Estimate duration based on text length (roughly 5 characters per second)
        val durationMs = (text.length * 200).coerceAtLeast(500)
        val numSamples = (sampleRate * durationMs) / 1000
        
        val samples = FloatArray(numSamples) { index ->
            val time = index.toDouble() / sampleRate
            // Mix multiple frequencies to simulate speech formants
            val f1 = 0.3f * sin(2.0 * PI * 200 * time).toFloat()
            val f2 = 0.2f * sin(2.0 * PI * 800 * time).toFloat()
            val f3 = 0.1f * sin(2.0 * PI * 2400 * time).toFloat()
            
            // Add envelope for more natural sound
            val envelope = 0.5f + 0.5f * sin(2.0 * PI * 3 * time).toFloat()
            
            (f1 + f2 + f3) * envelope
        }
        
        return AudioData.Chunk(
            samples = samples,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Generate audio with voice activity
     * Alternates between speech and silence
     */
    fun generateVoiceActivityPattern(
        speechDuration: Int,
        silenceDuration: Int,
        repetitions: Int,
        sampleRate: Int = 16000
    ): List<AudioData.Chunk> {
        val chunks = mutableListOf<AudioData.Chunk>()
        
        for (i in 0 until repetitions) {
            // Add speech chunk
            chunks.add(generateSineWave(440, speechDuration, sampleRate, 0.6f))
            // Add silence chunk
            chunks.add(generateSilence(silenceDuration, sampleRate))
        }
        
        return chunks
    }
    
    /**
     * Calculate RMS (Root Mean Square) of audio samples
     */
    fun calculateRMS(samples: FloatArray): Float {
        if (samples.isEmpty()) return 0f
        val sumSquares = samples.fold(0.0) { acc, sample ->
            acc + sample * sample
        }
        return kotlin.math.sqrt(sumSquares / samples.size).toFloat()
    }
    
    /**
     * Convert decibels to linear amplitude
     */
    fun dbToLinear(db: Float): Float {
        return kotlin.math.pow(10.0, db / 20.0).toFloat()
    }
    
    /**
     * Convert linear amplitude to decibels
     */
    fun linearToDb(linear: Float): Float {
        return (20.0 * kotlin.math.log10(linear.toDouble())).toFloat()
    }
}
