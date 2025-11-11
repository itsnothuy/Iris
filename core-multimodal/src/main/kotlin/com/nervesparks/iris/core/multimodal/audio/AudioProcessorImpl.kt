package com.nervesparks.iris.core.multimodal.audio

import android.content.Context
import android.media.AudioFormat as AndroidAudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import com.nervesparks.iris.common.error.VoiceException
import com.nervesparks.iris.core.multimodal.voice.AudioConfig
import com.nervesparks.iris.core.multimodal.voice.AudioProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.math.sqrt

/**
 * Implementation of AudioProcessor using Android AudioRecord and AudioTrack APIs
 */
@Singleton
class AudioProcessorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioProcessor {
    
    companion object {
        private const val TAG = "AudioProcessor"
        private const val BUFFER_SIZE_MULTIPLIER = 4
    }
    
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isRecording = false
    private var isPlaying = false
    
    override suspend fun startRecording(
        sampleRate: Int,
        channels: Int,
        config: AudioConfig
    ): Flow<AudioData> = flow {
        val channelConfig = if (channels == 1) {
            AndroidAudioFormat.CHANNEL_IN_MONO
        } else {
            AndroidAudioFormat.CHANNEL_IN_STEREO
        }
        
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            AndroidAudioFormat.ENCODING_PCM_16BIT
        ) * BUFFER_SIZE_MULTIPLIER
        
        if (bufferSize <= 0) {
            Log.e(TAG, "Invalid buffer size: $bufferSize for sample rate $sampleRate")
            emit(AudioData.Error("Invalid buffer size for recording. Device may not support this sample rate."))
            return@flow
        }
        
        // Retry logic for transient failures
        var retryCount = 0
        val maxRetries = 3
        var lastError: Exception? = null
        
        while (retryCount < maxRetries) {
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    AndroidAudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                
                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    val errorMsg = when {
                        retryCount < maxRetries - 1 -> "AudioRecord initialization failed, retrying..."
                        else -> "Failed to initialize AudioRecord after $maxRetries attempts. Check microphone permissions."
                    }
                    
                    Log.w(TAG, errorMsg)
                    
                    if (retryCount < maxRetries - 1) {
                        // Clean up failed instance
                        audioRecord?.release()
                        audioRecord = null
                        retryCount++
                        kotlinx.coroutines.delay(500L * retryCount) // Exponential backoff
                        continue
                    } else {
                        emit(AudioData.Error(errorMsg))
                        return@flow
                    }
                }
                
                // Successfully initialized, break retry loop
                break
                
            } catch (e: SecurityException) {
                Log.e(TAG, "Microphone permission denied", e)
                emit(AudioData.Error("Microphone permission denied. Please grant microphone access."))
                return@flow
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid audio parameters", e)
                emit(AudioData.Error("Invalid audio parameters: ${e.message}"))
                return@flow
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "AudioRecord creation failed (attempt ${retryCount + 1}/$maxRetries)", e)
                
                if (retryCount < maxRetries - 1) {
                    retryCount++
                    kotlinx.coroutines.delay(500L * retryCount)
                } else {
                    emit(AudioData.Error("Failed to create AudioRecord: ${e.message}"))
                    return@flow
                }
            }
        }
        
        try {
            audioRecord?.startRecording()
            isRecording = true
            
            val buffer = ShortArray(bufferSize / 2)
            
            while (coroutineContext.isActive && isRecording) {
                val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                
                if (readResult > 0) {
                    // Convert short samples to float
                    val floatSamples = FloatArray(readResult) { index ->
                        buffer[index] / 32768.0f
                    }
                    
                    // Apply audio processing if enabled
                    val processedSamples = if (config.noiseReduction || config.automaticGainControl) {
                        applyAudioProcessing(floatSamples, config)
                    } else {
                        floatSamples
                    }
                    
                    emit(AudioData.Chunk(processedSamples, System.currentTimeMillis()))
                } else if (readResult < 0) {
                    Log.w(TAG, "Error reading audio: $readResult")
                }
            }
            
            emit(AudioData.Ended)
            
        } catch (e: Exception) {
            Log.e(TAG, "Recording error", e)
            emit(AudioData.Error("Recording error: ${e.message}"))
        } finally {
            stopRecording()
        }
    }
    
    override suspend fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
    }
    
    override suspend fun playAudio(
        audioData: FloatArray,
        sampleRate: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AndroidAudioFormat.CHANNEL_OUT_MONO,
                AndroidAudioFormat.ENCODING_PCM_16BIT
            )
            
            if (bufferSize <= 0) {
                return@withContext Result.failure(VoiceException("Invalid buffer size for playback"))
            }
            
            audioTrack = AudioTrack.Builder()
                .setAudioFormat(
                    android.media.AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(AndroidAudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AndroidAudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()
            
            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                return@withContext Result.failure(VoiceException("Failed to initialize AudioTrack"))
            }
            
            // Convert float samples to short
            val shortBuffer = ShortArray(audioData.size) { index ->
                (audioData[index] * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
            }
            
            audioTrack?.play()
            isPlaying = true
            
            audioTrack?.write(shortBuffer, 0, shortBuffer.size)
            
            // Wait for playback to complete
            while (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING && isPlaying) {
                Thread.sleep(100)
            }
            
            stopPlayback()
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Playback error", e)
            stopPlayback()
            Result.failure(VoiceException("Playback error: ${e.message}", e))
        }
    }
    
    override suspend fun stopPlayback() {
        isPlaying = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback", e)
        }
    }
    
    override suspend fun loadAudioFile(file: File): Result<FloatArray> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) {
                return@withContext Result.failure(VoiceException("Invalid audio file"))
            }
            
            // Simple WAV file loading (assuming PCM 16-bit)
            val bytes = file.readBytes()
            
            // Skip WAV header (44 bytes)
            val dataStart = if (bytes.size > 44) 44 else 0
            val audioBytes = bytes.sliceArray(dataStart until bytes.size)
            
            // Convert to float samples
            val buffer = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN)
            val samples = FloatArray(audioBytes.size / 2) { index ->
                buffer.getShort(index * 2) / 32768.0f
            }
            
            Result.success(samples)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading audio file", e)
            Result.failure(VoiceException("Failed to load audio file: ${e.message}", e))
        }
    }
    
    override suspend fun saveAudioFile(
        audioData: FloatArray,
        file: File,
        sampleRate: Int,
        format: AudioFileFormat
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            when (format) {
                AudioFileFormat.WAV -> {
                    // Simple WAV file writing
                    val shortData = ShortArray(audioData.size) { index ->
                        (audioData[index] * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                    }
                    
                    file.outputStream().use { output ->
                        // Write WAV header
                        writeWAVHeader(output, shortData.size * 2, sampleRate, 1, 16)
                        
                        // Write audio data
                        val buffer = ByteBuffer.allocate(shortData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                        shortData.forEach { buffer.putShort(it) }
                        output.write(buffer.array())
                    }
                    
                    Result.success(Unit)
                }
                else -> {
                    Result.failure(VoiceException("Unsupported audio format: $format"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving audio file", e)
            Result.failure(VoiceException("Failed to save audio file: ${e.message}", e))
        }
    }
    
    // Helper methods
    
    private fun applyAudioProcessing(samples: FloatArray, config: AudioConfig): FloatArray {
        var processed = samples
        
        // Apply processing in optimal order
        if (config.noiseReduction) {
            processed = applyAdvancedNoiseReduction(processed)
        }
        
        if (config.automaticGainControl) {
            processed = applyAutomaticGainControl(processed)
        }
        
        if (config.echoCancellation) {
            processed = applyEchoCancellation(processed)
        }
        
        return processed
    }
    
    private fun applyAutomaticGainControl(samples: FloatArray): FloatArray {
        // Enhanced AGC with adaptive windowing
        // Production: Could use Android AudioEffect AGC or custom implementation
        
        val windowSize = 4096 // ~256ms at 16kHz
        val targetRMS = 0.1f
        val result = samples.copyOf()
        
        for (i in samples.indices step windowSize / 2) {
            val windowEnd = minOf(i + windowSize, samples.size)
            val window = samples.sliceArray(i until windowEnd)
            
            // Calculate RMS for this window
            val rms = sqrt(window.map { it * it }.average().toFloat())
            
            if (rms > 0.001f) {
                // Calculate adaptive gain with smoothing
                val gain = (targetRMS / rms).coerceIn(0.3f, 3.0f)
                
                // Apply gain with fade to avoid clicks
                for (j in i until windowEnd) {
                    // Smooth transition at window boundaries
                    val fadeIn = if (j - i < 100) (j - i) / 100f else 1f
                    val fadeOut = if (windowEnd - j < 100) (windowEnd - j) / 100f else 1f
                    val smoothGain = gain * minOf(fadeIn, fadeOut)
                    
                    result[j] = (result[j] * smoothGain).coerceIn(-1.0f, 1.0f)
                }
            }
        }
        
        return result
    }
    
    private fun applySimpleNoiseReduction(samples: FloatArray): FloatArray {
        // Legacy method - kept for compatibility
        val threshold = 0.01f
        return FloatArray(samples.size) { index ->
            if (kotlin.math.abs(samples[index]) < threshold) 0.0f else samples[index]
        }
    }
    
    private fun applyAdvancedNoiseReduction(samples: FloatArray): FloatArray {
        // Enhanced noise reduction using spectral subtraction approach
        // Production: Could use WebRTC noise suppression or RNNoise
        
        // 1. Estimate noise floor from initial samples
        val noiseEstimateSize = minOf(1600, samples.size / 10) // ~100ms at 16kHz
        val noiseFloor = if (noiseEstimateSize > 0) {
            sqrt(samples.take(noiseEstimateSize).map { it * it }.average().toFloat())
        } else {
            0.01f
        }
        
        // 2. Apply adaptive threshold based on noise floor
        val adaptiveThreshold = noiseFloor * 2.5f
        
        // 3. Apply noise gate with soft knee
        return FloatArray(samples.size) { index ->
            val sample = samples[index]
            val magnitude = kotlin.math.abs(sample)
            
            when {
                magnitude < adaptiveThreshold * 0.5f -> 0.0f // Hard gate for very low signals
                magnitude < adaptiveThreshold -> {
                    // Soft knee - gradual attenuation
                    val ratio = (magnitude - adaptiveThreshold * 0.5f) / (adaptiveThreshold * 0.5f)
                    sample * ratio
                }
                else -> sample // Pass through
            }
        }
    }
    
    private fun applyEchoCancellation(samples: FloatArray): FloatArray {
        // Basic echo cancellation using simple delay line
        // Production: Would use Android AcousticEchoCanceler or advanced AEC
        
        if (samples.size < 100) return samples
        
        val result = samples.copyOf()
        val echoDelay = 80 // samples (~5ms at 16kHz)
        val echoAttenuation = 0.3f
        
        // Simple echo suppression by subtracting delayed signal
        for (i in echoDelay until samples.size) {
            val echoEstimate = samples[i - echoDelay] * echoAttenuation
            result[i] = (samples[i] - echoEstimate).coerceIn(-1.0f, 1.0f)
        }
        
        return result
    }
    
    private fun writeWAVHeader(
        output: java.io.OutputStream,
        dataSize: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        
        // RIFF header
        output.write("RIFF".toByteArray())
        output.write(intToBytes(36 + dataSize))
        output.write("WAVE".toByteArray())
        
        // fmt chunk
        output.write("fmt ".toByteArray())
        output.write(intToBytes(16)) // chunk size
        output.write(shortToBytes(1)) // audio format (PCM)
        output.write(shortToBytes(channels.toShort()))
        output.write(intToBytes(sampleRate))
        output.write(intToBytes(byteRate))
        output.write(shortToBytes(blockAlign.toShort()))
        output.write(shortToBytes(bitsPerSample.toShort()))
        
        // data chunk
        output.write("data".toByteArray())
        output.write(intToBytes(dataSize))
    }
    
    private fun intToBytes(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }
    
    private fun shortToBytes(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }
}
