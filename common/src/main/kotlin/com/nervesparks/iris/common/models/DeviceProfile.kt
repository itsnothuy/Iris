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
    val capabilities: Set<HardwareCapability>,
    val deviceClass: DeviceClass = DeviceClass.MID_RANGE,
    val thermalCapability: ThermalCapability = ThermalCapability.LIMITED_MONITORING
)

/**
 * SoC information
 */
data class SoCInfo(
    val vendor: SoCVendor,
    val model: String,
    val cores: Int,
    val generation: Int = 0,
    val architecture: String = "ARM64",
    val maxFrequency: Long = 0L
)

/**
 * GPU information
 */
data class GPUInfo(
    val vendor: GPUVendor,
    val model: String,
    val driverVersion: String,
    val openglVersion: String = "Unknown",
    val vulkanSupported: Boolean = false,
    val openclSupported: Boolean = false
)

/**
 * Memory information
 */
data class MemoryInfo(
    val totalRAM: Long,
    val availableRAM: Long,
    val lowMemory: Boolean,
    val lowMemoryThreshold: Long = 0L
)

/**
 * Benchmark results
 */
data class BenchmarkResults(
    val cpuScore: Double,
    val gpuScore: Double,
    val memoryBandwidth: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val backendResults: Map<BackendType, BenchmarkResult> = emptyMap(),
    val thermalBaseline: com.nervesparks.iris.common.config.ThermalState = com.nervesparks.iris.common.config.ThermalState.NORMAL
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
    POWERVR,
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

/**
 * Device class classification based on performance tier
 */
enum class DeviceClass {
    FLAGSHIP,
    HIGH_END,
    MID_RANGE,
    BUDGET,
    LOW_END
}

/**
 * Thermal monitoring capability level
 */
enum class ThermalCapability {
    ADVANCED_MONITORING,  // Android 11+ ADPF
    BASIC_MONITORING,     // Android 10+ ThermalManager
    LIMITED_MONITORING,   // Legacy thermal detection
    NO_MONITORING         // No thermal APIs available
}

/**
 * Individual benchmark result for a specific backend
 */
data class BenchmarkResult(
    val backend: BackendType,
    val executionTime: Long,
    val performance: Double,
    val memoryUsage: Long,
    val success: Boolean
)
