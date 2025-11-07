package com.nervesparks.iris.core.hw

import android.content.Context
import android.os.Build
import com.nervesparks.iris.common.logging.IrisLogger
import com.nervesparks.iris.common.models.DeviceProfile
import com.nervesparks.iris.common.models.GPUVendor
import com.nervesparks.iris.common.models.HardwareCapability
import com.nervesparks.iris.common.models.SoCVendor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects available compute backends and capabilities
 */
@Singleton
class BackendDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceProfileProvider: DeviceProfileProvider
) {
    
    /**
     * Detect all available backend capabilities
     */
    fun detectCapabilities(): BackendCapabilities {
        IrisLogger.info("Detecting backend capabilities...")
        
        val deviceProfile = deviceProfileProvider.getDeviceProfile()
        val cpuFeatures = detectCPUFeatures()
        
        // OpenCL detection (Adreno GPUs)
        val openclAvailable = checkOpenCLSupport(deviceProfile)
        val openclDevices = if (openclAvailable) {
            enumerateOpenCLDevices(deviceProfile)
        } else {
            emptyList()
        }
        
        // Vulkan detection (Mali/Xclipse GPUs)
        val vulkanAvailable = checkVulkanSupport(deviceProfile)
        val vulkanVersion = if (vulkanAvailable) {
            getVulkanVersion()
        } else {
            null
        }
        
        // QNN/Hexagon detection (Qualcomm devices)
        val qnnAvailable = checkQNNSupport(deviceProfile)
        
        val capabilities = BackendCapabilities(
            openclAvailable = openclAvailable,
            openclDevices = openclDevices,
            vulkanAvailable = vulkanAvailable,
            vulkanVersion = vulkanVersion,
            cpuFeatures = cpuFeatures,
            qnnAvailable = qnnAvailable
        )
        
        IrisLogger.info("Backend capabilities: $capabilities")
        return capabilities
    }
    
    /**
     * Select optimal backend for a given task
     */
    fun selectOptimalBackend(
        capabilities: BackendCapabilities,
        task: ComputeTask,
        modelSizeGB: Float = 3.0f
    ): OptimalBackend {
        return when (task) {
            ComputeTask.LLM_INFERENCE -> selectLLMBackend(capabilities, modelSizeGB)
            ComputeTask.EMBEDDING_GENERATION -> selectEmbeddingBackend(capabilities)
            ComputeTask.SPEECH_RECOGNITION -> selectASRBackend(capabilities)
            ComputeTask.VISION_PROCESSING -> selectVisionBackend(capabilities)
        }
    }
    
    /**
     * Select optimal backend for LLM inference
     */
    private fun selectLLMBackend(capabilities: BackendCapabilities, modelSizeGB: Float): OptimalBackend {
        // Prefer GPU for large models (>3GB)
        if (modelSizeGB > 3.0f) {
            if (capabilities.openclAvailable) {
                return OptimalBackend(
                    type = BackendType.OPENCL_ADRENO,
                    gpuLayers = 32,
                    reason = "Large model, Adreno GPU available"
                )
            }
            
            if (capabilities.vulkanAvailable) {
                return OptimalBackend(
                    type = BackendType.VULKAN_MALI,
                    gpuLayers = 28,
                    reason = "Large model, Mali/Xclipse GPU available"
                )
            }
        }
        
        // Fallback to CPU for small models or if GPU unavailable
        return OptimalBackend(
            type = BackendType.CPU_NEON,
            gpuLayers = 0,
            reason = if (modelSizeGB <= 3.0f) "Small model, CPU efficient" else "GPU unavailable, using CPU"
        )
    }
    
    /**
     * Select optimal backend for embedding generation
     */
    private fun selectEmbeddingBackend(capabilities: BackendCapabilities): OptimalBackend {
        // Embeddings benefit from GPU parallelization
        if (capabilities.openclAvailable) {
            return OptimalBackend(
                type = BackendType.OPENCL_ADRENO,
                gpuLayers = -1,
                reason = "Embedding benefits from GPU parallelization"
            )
        }
        
        if (capabilities.vulkanAvailable) {
            return OptimalBackend(
                type = BackendType.VULKAN_MALI,
                gpuLayers = -1,
                reason = "Embedding benefits from GPU parallelization"
            )
        }
        
        return OptimalBackend(
            type = BackendType.CPU_NEON,
            gpuLayers = 0,
            reason = "GPU unavailable for embeddings"
        )
    }
    
    /**
     * Select optimal backend for ASR
     */
    private fun selectASRBackend(capabilities: BackendCapabilities): OptimalBackend {
        // Small ASR models can use QNN/Hexagon if available
        if (capabilities.qnnAvailable) {
            return OptimalBackend(
                type = BackendType.QNN_HEXAGON,
                gpuLayers = 0,
                reason = "ASR optimized for Hexagon DSP"
            )
        }
        
        return OptimalBackend(
            type = BackendType.CPU_NEON,
            gpuLayers = 0,
            reason = "ASR running on CPU"
        )
    }
    
    /**
     * Select optimal backend for vision processing
     */
    private fun selectVisionBackend(capabilities: BackendCapabilities): OptimalBackend {
        // Vision models benefit from GPU
        if (capabilities.vulkanAvailable) {
            return OptimalBackend(
                type = BackendType.VULKAN_MALI,
                gpuLayers = -1,
                reason = "Vision processing on GPU"
            )
        }
        
        if (capabilities.openclAvailable) {
            return OptimalBackend(
                type = BackendType.OPENCL_ADRENO,
                gpuLayers = -1,
                reason = "Vision processing on GPU"
            )
        }
        
        return OptimalBackend(
            type = BackendType.CPU_NEON,
            gpuLayers = 0,
            reason = "Vision processing on CPU"
        )
    }
    
    /**
     * Detect CPU features
     */
    private fun detectCPUFeatures(): CpuFeatures {
        val coreCount = Runtime.getRuntime().availableProcessors()
        
        // On ARM64, we can assume NEON support
        val neonSupported = Build.SUPPORTED_ABIS.any { it.contains("arm64") || it.contains("armeabi-v7a") }
        
        // FP16 and DotProd are available on newer ARM cores
        // This is a simplified detection - actual feature detection requires JNI
        val fp16Supported = Build.SUPPORTED_ABIS.any { it.contains("arm64-v8a") }
        val dotProdSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && fp16Supported
        
        return CpuFeatures(
            neonSupported = neonSupported,
            fp16Supported = fp16Supported,
            dotProdSupported = dotProdSupported,
            coreCount = coreCount
        )
    }
    
    /**
     * Check OpenCL support (Adreno GPUs)
     */
    private fun checkOpenCLSupport(deviceProfile: DeviceProfile): Boolean {
        // Adreno GPUs (Qualcomm) typically support OpenCL
        return deviceProfile.gpuVendor == GPUVendor.ADRENO ||
               deviceProfile.socVendor == SoCVendor.QUALCOMM ||
               deviceProfile.capabilities.contains(HardwareCapability.OPENCL)
    }
    
    /**
     * Enumerate OpenCL devices
     */
    private fun enumerateOpenCLDevices(deviceProfile: DeviceProfile): List<String> {
        // This would require JNI to actually query OpenCL
        // For now, return device GPU info
        return if (deviceProfile.gpuModel.isNotEmpty()) {
            listOf(deviceProfile.gpuModel)
        } else {
            emptyList()
        }
    }
    
    /**
     * Check Vulkan support (Mali/Xclipse GPUs)
     */
    private fun checkVulkanSupport(deviceProfile: DeviceProfile): Boolean {
        // Mali, Xclipse, and some Adreno GPUs support Vulkan
        return deviceProfile.gpuVendor == GPUVendor.MALI ||
               deviceProfile.gpuVendor == GPUVendor.XCLIPSE ||
               deviceProfile.capabilities.contains(HardwareCapability.VULKAN) ||
               // Modern Adreno also supports Vulkan
               (deviceProfile.gpuVendor == GPUVendor.ADRENO && 
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
    }
    
    /**
     * Get Vulkan version
     */
    private fun getVulkanVersion(): String {
        // This would require JNI to query Vulkan
        // Return approximate version based on Android version
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> "1.1"
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> "1.0"
            else -> "Unknown"
        }
    }
    
    /**
     * Check QNN/Hexagon DSP support (Qualcomm devices)
     */
    private fun checkQNNSupport(deviceProfile: DeviceProfile): Boolean {
        // QNN is available on Snapdragon devices with Hexagon DSP
        return (deviceProfile.socVendor == SoCVendor.QUALCOMM ||
                deviceProfile.capabilities.contains(HardwareCapability.QNN)) &&
               Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }
}
