package com.nervesparks.iris.core.hw

/**
 * Represents the capabilities of available compute backends
 */
data class BackendCapabilities(
    /** OpenCL availability (typically for Adreno GPUs) */
    val openclAvailable: Boolean = false,
    
    /** List of OpenCL devices if available */
    val openclDevices: List<String> = emptyList(),
    
    /** Vulkan availability (typically for Mali/Xclipse GPUs) */
    val vulkanAvailable: Boolean = false,
    
    /** Vulkan version if available */
    val vulkanVersion: String? = null,
    
    /** CPU feature set */
    val cpuFeatures: CpuFeatures = CpuFeatures(),
    
    /** QNN/Hexagon DSP availability (Qualcomm devices) */
    val qnnAvailable: Boolean = false
)

/**
 * CPU feature detection
 */
data class CpuFeatures(
    /** ARM NEON support */
    val neonSupported: Boolean = false,
    
    /** ARM FP16 support */
    val fp16Supported: Boolean = false,
    
    /** ARM DotProd support */
    val dotProdSupported: Boolean = false,
    
    /** Number of CPU cores */
    val coreCount: Int = Runtime.getRuntime().availableProcessors()
)

/**
 * Backend type enumeration
 */
enum class BackendType {
    /** CPU-only with ARM NEON optimizations */
    CPU_NEON,
    
    /** OpenCL backend for Adreno GPUs */
    OPENCL_ADRENO,
    
    /** Vulkan backend for Mali/Xclipse GPUs */
    VULKAN_MALI,
    
    /** Qualcomm QNN/Hexagon DSP backend */
    QNN_HEXAGON
}

/**
 * Optimal backend selection result
 */
data class OptimalBackend(
    /** Selected backend type */
    val type: BackendType,
    
    /** Number of layers to offload to GPU (-1 for all) */
    val gpuLayers: Int = 0,
    
    /** Reason for selection */
    val reason: String = ""
)

/**
 * Compute task types for backend selection
 */
enum class ComputeTask {
    /** Large language model inference */
    LLM_INFERENCE,
    
    /** Embedding generation */
    EMBEDDING_GENERATION,
    
    /** ASR/Speech recognition */
    SPEECH_RECOGNITION,
    
    /** Vision/Image processing */
    VISION_PROCESSING
}
