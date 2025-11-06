package com.nervesparks.iris.common.models

/**
 * Comprehensive device profile with hardware capabilities
 */
data class DeviceProfile(
    val socVendor: SoCVendor,
    val socModel: String,
    val gpuVendor: GPUVendor,
    val gpuModel: String,
    val totalRAM: Long,
    val availableRAM: Long,
    val androidVersion: Int,
    val capabilities: Set<HardwareCapability>
)

/**
 * SoC information
 */
data class SoCInfo(
    val vendor: SoCVendor,
    val model: String,
    val cores: Int
)

/**
 * GPU information
 */
data class GPUInfo(
    val vendor: GPUVendor,
    val model: String,
    val driverVersion: String
)

/**
 * Memory information
 */
data class MemoryInfo(
    val totalRAM: Long,
    val availableRAM: Long,
    val lowMemory: Boolean
)

/**
 * Benchmark results
 */
data class BenchmarkResults(
    val cpuScore: Double,
    val gpuScore: Double,
    val memoryBandwidth: Double,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * SoC vendor enumeration
 */
enum class SoCVendor {
    QUALCOMM,
    SAMSUNG,
    MEDIATEK,
    GOOGLE,
    OTHER
}

/**
 * GPU vendor enumeration
 */
enum class GPUVendor {
    ADRENO,
    MALI,
    XCLIPSE,
    OTHER
}

/**
 * Hardware capabilities
 */
enum class HardwareCapability {
    OPENCL,
    VULKAN,
    NNAPI,
    QNN,
    FP16_SUPPORT,
    INT8_SUPPORT
}
