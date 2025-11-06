package com.nervesparks.iris.core.hw

import com.nervesparks.iris.common.models.BenchmarkResults
import com.nervesparks.iris.common.models.DeviceProfile
import com.nervesparks.iris.common.models.GPUInfo
import com.nervesparks.iris.common.models.MemoryInfo
import com.nervesparks.iris.common.models.SoCInfo

/**
 * Interface for detecting and profiling device hardware capabilities
 */
interface DeviceProfileProvider {
    /**
     * Get comprehensive device profile
     */
    fun getDeviceProfile(): DeviceProfile
    
    /**
     * Get SoC information
     */
    fun getSoCInfo(): SoCInfo
    
    /**
     * Get GPU information
     */
    fun getGPUInfo(): GPUInfo
    
    /**
     * Get current memory information
     */
    fun getMemoryInfo(): MemoryInfo
    
    /**
     * Run hardware benchmark
     */
    suspend fun runBenchmark(): BenchmarkResults
}
