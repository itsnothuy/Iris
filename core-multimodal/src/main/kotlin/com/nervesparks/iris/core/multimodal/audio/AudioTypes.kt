package com.nervesparks.iris.core.multimodal.audio

/**
 * Audio data for streaming
 */
sealed class AudioData {
    data class Chunk(
        val samples: FloatArray,
        val timestamp: Long
    ) : AudioData() {
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

/**
 * Audio chunk for TTS streaming
 */
data class AudioChunk(
    val samples: FloatArray,
    val sampleRate: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as AudioChunk
        
        if (!samples.contentEquals(other.samples)) return false
        if (sampleRate != other.sampleRate) return false
        if (timestamp != other.timestamp) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Audio file formats
 */
enum class AudioFileFormat {
    WAV, MP3, FLAC, OGG
}

/**
 * Audio recording state
 */
enum class RecordingState {
    IDLE, RECORDING, PAUSED, STOPPED
}

/**
 * Audio playback state
 */
enum class PlaybackState {
    IDLE, PLAYING, PAUSED, STOPPED
}
