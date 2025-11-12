package com.nervesparks.iris.core.multimodal.audio

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AudioBufferPoolTest {
    
    private lateinit var bufferPool: AudioBufferPool
    
    @Before
    fun setup() {
        bufferPool = AudioBufferPool(bufferSize = 1024, maxPoolSize = 5)
    }
    
    // =========================
    // Buffer Acquisition Tests
    // =========================
    
    @Test
    fun `acquireBuffer should return buffer of correct size`() {
        val buffer = bufferPool.acquireBuffer()
        
        assertEquals(1024, buffer.size)
    }
    
    @Test
    fun `acquireBuffer should return cleared buffer`() {
        val buffer = bufferPool.acquireBuffer()
        
        // All elements should be zero
        buffer.forEach { sample ->
            assertEquals(0f, sample, 0.001f)
        }
    }
    
    @Test
    fun `multiple acquireBuffer calls should work`() {
        val buffers = List(10) { bufferPool.acquireBuffer() }
        
        assertEquals(10, buffers.size)
        buffers.forEach { buffer ->
            assertEquals(1024, buffer.size)
        }
    }
    
    // =========================
    // Buffer Release Tests
    // =========================
    
    @Test
    fun `releaseBuffer should add buffer to pool`() {
        val buffer = bufferPool.acquireBuffer()
        val statsBefore = bufferPool.getStats()
        
        bufferPool.releaseBuffer(buffer)
        
        val statsAfter = bufferPool.getStats()
        assertEquals(statsBefore.pooledBuffers + 1, statsAfter.pooledBuffers)
    }
    
    @Test
    fun `releaseBuffer should clear buffer data`() {
        val buffer = bufferPool.acquireBuffer()
        
        // Fill with data
        for (i in buffer.indices) {
            buffer[i] = i.toFloat()
        }
        
        bufferPool.releaseBuffer(buffer)
        
        // Acquire same buffer again
        val reusedBuffer = bufferPool.acquireBuffer()
        
        // Should be cleared
        reusedBuffer.forEach { sample ->
            assertEquals(0f, sample, 0.001f)
        }
    }
    
    @Test
    fun `releaseBuffer should not pool wrong size buffers`() {
        val wrongSizeBuffer = FloatArray(2048) // Wrong size
        val statsBefore = bufferPool.getStats()
        
        bufferPool.releaseBuffer(wrongSizeBuffer)
        
        val statsAfter = bufferPool.getStats()
        assertEquals(statsBefore.pooledBuffers, statsAfter.pooledBuffers)
    }
    
    @Test
    fun `releaseBuffer should respect max pool size`() {
        // Acquire and release more buffers than max pool size
        val buffers = List(10) { bufferPool.acquireBuffer() }
        
        buffers.forEach { bufferPool.releaseBuffer(it) }
        
        val stats = bufferPool.getStats()
        assertEquals(5, stats.pooledBuffers) // Should be limited to maxPoolSize
    }
    
    // =========================
    // Buffer Reuse Tests
    // =========================
    
    @Test
    fun `acquireBuffer should reuse released buffers`() {
        val buffer1 = bufferPool.acquireBuffer()
        bufferPool.releaseBuffer(buffer1)
        
        val buffer2 = bufferPool.acquireBuffer()
        
        // Should be the same buffer instance (reused)
        assertSame(buffer1, buffer2)
    }
    
    @Test
    fun `buffer pool should reduce allocations`() {
        // Acquire and release multiple times
        repeat(20) {
            val buffer = bufferPool.acquireBuffer()
            bufferPool.releaseBuffer(buffer)
        }
        
        val stats = bufferPool.getStats()
        
        // Should have allocated far fewer buffers than total operations
        // due to reuse
        assertTrue(stats.totalAllocated < 20)
    }
    
    // =========================
    // Pool Management Tests
    // =========================
    
    @Test
    fun `clearPool should remove all buffers`() {
        // Add some buffers to pool
        val buffers = List(5) { bufferPool.acquireBuffer() }
        buffers.forEach { bufferPool.releaseBuffer(it) }
        
        val statsBefore = bufferPool.getStats()
        assertTrue(statsBefore.pooledBuffers > 0)
        
        bufferPool.clearPool()
        
        val statsAfter = bufferPool.getStats()
        assertEquals(0, statsAfter.pooledBuffers)
    }
    
    @Test
    fun `getStats should return accurate statistics`() {
        val buffer1 = bufferPool.acquireBuffer()
        val buffer2 = bufferPool.acquireBuffer()
        
        var stats = bufferPool.getStats()
        assertEquals(2, stats.totalAllocated)
        assertEquals(0, stats.pooledBuffers)
        
        bufferPool.releaseBuffer(buffer1)
        
        stats = bufferPool.getStats()
        assertEquals(2, stats.totalAllocated)
        assertEquals(1, stats.pooledBuffers)
        
        bufferPool.releaseBuffer(buffer2)
        
        stats = bufferPool.getStats()
        assertEquals(2, stats.totalAllocated)
        assertEquals(2, stats.pooledBuffers)
    }
    
    // =========================
    // Statistics Tests
    // =========================
    
    @Test
    fun `BufferPoolStats should calculate utilization correctly`() {
        val buffers = List(3) { bufferPool.acquireBuffer() }
        buffers.forEach { bufferPool.releaseBuffer(it) }
        
        val stats = bufferPool.getStats()
        
        // 3 out of 5 max = 60%
        assertEquals(60f, stats.utilizationPercent, 0.1f)
    }
    
    @Test
    fun `BufferPoolStats should calculate memory usage correctly`() {
        val buffers = List(2) { bufferPool.acquireBuffer() }
        buffers.forEach { bufferPool.releaseBuffer(it) }
        
        val stats = bufferPool.getStats()
        
        // 2 buffers * 1024 floats * 4 bytes = 8192 bytes
        assertEquals(8192L, stats.memoryUsageBytes)
    }
    
    @Test
    fun `BufferPoolStats should handle empty pool`() {
        val stats = bufferPool.getStats()
        
        assertEquals(0, stats.pooledBuffers)
        assertEquals(0f, stats.utilizationPercent, 0.001f)
        assertEquals(0L, stats.memoryUsageBytes)
    }
    
    @Test
    fun `BufferPoolStats should handle full pool`() {
        val buffers = List(5) { bufferPool.acquireBuffer() }
        buffers.forEach { bufferPool.releaseBuffer(it) }
        
        val stats = bufferPool.getStats()
        
        assertEquals(5, stats.pooledBuffers)
        assertEquals(100f, stats.utilizationPercent, 0.001f)
        assertEquals(5 * 1024 * 4L, stats.memoryUsageBytes)
    }
    
    // =========================
    // Thread Safety Tests
    // =========================
    
    @Test
    fun `buffer pool should handle concurrent access`() {
        val threads = List(10) {
            Thread {
                repeat(100) {
                    val buffer = bufferPool.acquireBuffer()
                    Thread.sleep(1) // Simulate some work
                    bufferPool.releaseBuffer(buffer)
                }
            }
        }
        
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        
        // Should not crash and should have valid stats
        val stats = bufferPool.getStats()
        assertTrue(stats.totalAllocated > 0)
        assertTrue(stats.pooledBuffers >= 0)
        assertTrue(stats.pooledBuffers <= stats.maxPoolSize)
    }
}
