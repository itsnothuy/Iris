package com.nervesparks.iris.core.multimodal.audio

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Audio buffer pool for efficient memory management
 * Reduces GC pressure by reusing audio buffers
 */
class AudioBufferPool(
    private val bufferSize: Int,
    private val maxPoolSize: Int = 10
) {
    private val bufferPool = ConcurrentLinkedQueue<FloatArray>()
    private val poolSize = AtomicInteger(0)
    private val allocatedBuffers = AtomicInteger(0)
    
    /**
     * Acquire a buffer from the pool or create a new one
     */
    fun acquireBuffer(): FloatArray {
        val buffer = bufferPool.poll()
        
        return if (buffer != null) {
            poolSize.decrementAndGet()
            // Clear buffer before reuse
            buffer.fill(0f)
            buffer
        } else {
            allocatedBuffers.incrementAndGet()
            FloatArray(bufferSize)
        }
    }
    
    /**
     * Release a buffer back to the pool for reuse
     */
    fun releaseBuffer(buffer: FloatArray) {
        if (buffer.size != bufferSize) {
            // Wrong size, don't pool it
            return
        }
        
        if (poolSize.get() < maxPoolSize) {
            // Clear sensitive audio data
            buffer.fill(0f)
            bufferPool.offer(buffer)
            poolSize.incrementAndGet()
        }
        // If pool is full, let GC handle it
    }
    
    /**
     * Clear all buffers from the pool
     * Call when stopping audio processing to free memory
     */
    fun clearPool() {
        bufferPool.clear()
        poolSize.set(0)
    }
    
    /**
     * Get pool statistics
     */
    fun getStats(): BufferPoolStats {
        return BufferPoolStats(
            pooledBuffers = poolSize.get(),
            totalAllocated = allocatedBuffers.get(),
            maxPoolSize = maxPoolSize,
            bufferSize = bufferSize
        )
    }
}

/**
 * Statistics for buffer pool monitoring
 */
data class BufferPoolStats(
    val pooledBuffers: Int,
    val totalAllocated: Int,
    val maxPoolSize: Int,
    val bufferSize: Int
) {
    val utilizationPercent: Float
        get() = (pooledBuffers.toFloat() / maxPoolSize) * 100f
    
    val memoryUsageBytes: Long
        get() = pooledBuffers.toLong() * bufferSize * 4 // 4 bytes per float
}
