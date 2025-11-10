package com.nervesparks.iris.core.hw

import android.util.Log
import com.nervesparks.iris.common.config.ThermalState
import com.nervesparks.iris.common.models.BackendType
import com.nervesparks.iris.common.models.BenchmarkResult
import com.nervesparks.iris.common.models.BenchmarkResults
import com.nervesparks.iris.common.models.ComputeTask
import com.nervesparks.iris.common.models.DeviceClass
import com.nervesparks.iris.common.models.DeviceProfile
import com.nervesparks.iris.common.models.GPUVendor
import com.nervesparks.iris.common.models.HardwareCapability
import com.nervesparks.iris.common.models.SoCVendor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of BackendRouter with intelligent backend selection
 */
@Singleton
class BackendRouterImpl @Inject constructor(
    private val deviceProfileProvider: DeviceProfileProvider,
    private val thermalManager: ThermalManager,
    private val preferences: BackendPreferences
) : BackendRouter {
    
    companion object {
        private const val TAG = "BackendRouter"
        private const val BENCHMARK_CACHE_VALIDITY_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
    
    private var currentBackend: BackendType = BackendType.CPU_NEON
    private var cachedBenchmarks: BenchmarkResults? = null
    private val backendSelectionMatrix = createBackendSelectionMatrix()
    
    override suspend fun selectOptimalBackend(task: ComputeTask): BackendType {
        val deviceProfile = deviceProfileProvider.getDeviceProfile()
        val thermalState = thermalManager.thermalState.value
        
        // Check for cached selection
        val cachedBackend = preferences.getCachedBackend(task, deviceProfile)
        if (cachedBackend != null && isBackendValidForCurrentState(cachedBackend, thermalState)) {
            currentBackend = cachedBackend
            return cachedBackend
        }
        
        // Run benchmarks if needed
        val benchmarks = getCachedOrRunBenchmarks()
        
        // Select optimal backend based on task, device, and thermal state
        val selectedBackend = selectBackendFromMatrix(task, deviceProfile, thermalState, benchmarks)
        
        // Validate and fallback if necessary
        val validatedBackend = validateAndFallback(selectedBackend, deviceProfile)
        
        // Cache the selection
        preferences.cacheBackendSelection(task, deviceProfile, validatedBackend)
        currentBackend = validatedBackend
        
        Log.i(TAG, "Selected backend: $validatedBackend for task: $task")
        return validatedBackend
    }
    
    private fun createBackendSelectionMatrix(): Map<SoCVendor, Map<DeviceClass, List<BackendType>>> {
        return mapOf(
            SoCVendor.QUALCOMM to mapOf(
                DeviceClass.FLAGSHIP to listOf(
                    BackendType.QNN_HEXAGON,
                    BackendType.OPENCL_ADRENO,
                    BackendType.CPU_NEON
                ),
                DeviceClass.HIGH_END to listOf(
                    BackendType.OPENCL_ADRENO,
                    BackendType.QNN_HEXAGON,
                    BackendType.CPU_NEON
                ),
                DeviceClass.MID_RANGE to listOf(
                    BackendType.OPENCL_ADRENO,
                    BackendType.CPU_NEON
                ),
                DeviceClass.BUDGET to listOf(
                    BackendType.CPU_NEON
                ),
                DeviceClass.LOW_END to listOf(
                    BackendType.CPU_NEON
                )
            ),
            SoCVendor.SAMSUNG to mapOf(
                DeviceClass.FLAGSHIP to listOf(
                    BackendType.VULKAN_MALI,
                    BackendType.CPU_NEON
                ),
                DeviceClass.HIGH_END to listOf(
                    BackendType.VULKAN_MALI,
                    BackendType.CPU_NEON
                ),
                DeviceClass.MID_RANGE to listOf(
                    BackendType.CPU_NEON,
                    BackendType.VULKAN_MALI
                ),
                DeviceClass.BUDGET to listOf(
                    BackendType.CPU_NEON
                ),
                DeviceClass.LOW_END to listOf(
                    BackendType.CPU_NEON
                )
            ),
            SoCVendor.GOOGLE to mapOf(
                DeviceClass.FLAGSHIP to listOf(
                    BackendType.VULKAN_MALI,
                    BackendType.CPU_NEON
                ),
                DeviceClass.HIGH_END to listOf(
                    BackendType.VULKAN_MALI,
                    BackendType.CPU_NEON
                )
            ),
            SoCVendor.MEDIATEK to mapOf(
                DeviceClass.HIGH_END to listOf(
                    BackendType.VULKAN_MALI,
                    BackendType.CPU_NEON
                ),
                DeviceClass.MID_RANGE to listOf(
                    BackendType.CPU_NEON,
                    BackendType.VULKAN_MALI
                ),
                DeviceClass.BUDGET to listOf(
                    BackendType.CPU_NEON
                ),
                DeviceClass.LOW_END to listOf(
                    BackendType.CPU_NEON
                )
            )
        )
    }
    
    private fun selectBackendFromMatrix(
        task: ComputeTask,
        deviceProfile: DeviceProfile,
        thermalState: ThermalState,
        benchmarks: BenchmarkResults?
    ): BackendType {
        // Get preferred backends for this SoC and device class
        val preferredBackends = backendSelectionMatrix[deviceProfile.socVendor]
            ?.get(deviceProfile.deviceClass)
            ?: listOf(BackendType.CPU_NEON)
        
        // Filter based on thermal state
        val thermalFilteredBackends = when (thermalState) {
            ThermalState.THERMAL_STATUS_CRITICAL,
            ThermalState.THERMAL_STATUS_EMERGENCY,
            ThermalState.CRITICAL -> {
                // Only CPU in critical thermal state
                preferredBackends.filter { it == BackendType.CPU_NEON }
            }
            ThermalState.SEVERE -> {
                // Prefer CPU, but allow GPU if significantly faster
                preferredBackends.sortedBy { 
                    if (it == BackendType.CPU_NEON) 0 else 1 
                }
            }
            else -> preferredBackends
        }
        
        // Use benchmark results if available
        return if (benchmarks != null && benchmarks.backendResults.isNotEmpty()) {
            selectBestPerformingBackend(thermalFilteredBackends, benchmarks, task)
        } else {
            thermalFilteredBackends.firstOrNull() ?: BackendType.CPU_NEON
        }
    }
    
    private fun selectBestPerformingBackend(
        candidates: List<BackendType>,
        benchmarks: BenchmarkResults,
        task: ComputeTask
    ): BackendType {
        val validCandidates = candidates.filter { backend ->
            benchmarks.backendResults[backend]?.success == true
        }
        
        if (validCandidates.isEmpty()) {
            return BackendType.CPU_NEON
        }
        
        // Score backends based on performance and efficiency
        val scoredBackends = validCandidates.map { backend ->
            val result = benchmarks.backendResults[backend]!!
            val score = calculateBackendScore(result, task)
            backend to score
        }.sortedByDescending { it.second }
        
        return scoredBackends.first().first
    }
    
    private fun calculateBackendScore(result: BenchmarkResult, task: ComputeTask): Double {
        // Higher performance is better, lower execution time is better
        val performanceScore = result.performance
        val timeScore = 1000.0 / (result.executionTime + 1) // Avoid division by zero
        val memoryScore = 1000.0 / (result.memoryUsage / 1024 / 1024 + 1) // MB
        
        // Weight factors based on task type
        val weights = when (task) {
            ComputeTask.LLM_INFERENCE -> Triple(0.6, 0.3, 0.1) // Performance > Time > Memory
            ComputeTask.EMBEDDING_GENERATION -> Triple(0.4, 0.4, 0.2) // Balanced
            ComputeTask.SAFETY_CHECK -> Triple(0.2, 0.6, 0.2) // Time > Performance > Memory
            ComputeTask.ASR_TRANSCRIPTION -> Triple(0.3, 0.5, 0.2) // Time is critical
        }
        
        return performanceScore * weights.first + 
               timeScore * weights.second + 
               memoryScore * weights.third
    }
    
    override suspend fun switchBackend(newBackend: BackendType): Result<Unit> {
        return try {
            // Validate backend is supported on this device
            val deviceProfile = deviceProfileProvider.getDeviceProfile()
            if (!isBackendSupported(newBackend, deviceProfile)) {
                return Result.failure(
                    UnsupportedBackendException("Backend $newBackend not supported on this device")
                )
            }
            
            // Test backend functionality
            val testResult = testBackend(newBackend)
            if (!testResult) {
                return Result.failure(
                    BackendTestException("Backend $newBackend failed functionality test")
                )
            }
            
            currentBackend = newBackend
            Log.i(TAG, "Successfully switched to backend: $newBackend")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch to backend: $newBackend", e)
            Result.failure(e)
        }
    }
    
    override fun getCurrentBackend(): BackendType {
        return currentBackend
    }
    
    override suspend fun validateBackend(backend: BackendType): Boolean {
        val deviceProfile = deviceProfileProvider.getDeviceProfile()
        return isBackendSupported(backend, deviceProfile) && testBackend(backend)
    }
    
    private fun isBackendSupported(backend: BackendType, deviceProfile: DeviceProfile): Boolean {
        return when (backend) {
            BackendType.CPU_NEON -> true // Always supported
            BackendType.OPENCL_ADRENO -> {
                deviceProfile.gpuVendor == GPUVendor.ADRENO &&
                deviceProfile.capabilities.contains(HardwareCapability.OPENCL)
            }
            BackendType.VULKAN_MALI -> {
                (deviceProfile.gpuVendor == GPUVendor.MALI || 
                 deviceProfile.gpuVendor == GPUVendor.XCLIPSE) &&
                deviceProfile.capabilities.contains(HardwareCapability.VULKAN)
            }
            BackendType.QNN_HEXAGON -> {
                deviceProfile.socVendor == SoCVendor.QUALCOMM &&
                deviceProfile.capabilities.contains(HardwareCapability.QNN)
            }
        }
    }
    
    private suspend fun testBackend(backend: BackendType): Boolean = withContext(Dispatchers.IO) {
        try {
            // Run a simple computation test
            when (backend) {
                BackendType.CPU_NEON -> testCPUBackend()
                BackendType.OPENCL_ADRENO -> testOpenCLBackend()
                BackendType.VULKAN_MALI -> testVulkanBackend()
                BackendType.QNN_HEXAGON -> testQNNBackend()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend test failed for $backend", e)
            false
        }
    }
    
    private fun testCPUBackend(): Boolean {
        // Simple CPU computation test
        val size = 100
        val array = FloatArray(size) { it.toFloat() }
        val sum = array.sum()
        return sum > 0
    }
    
    private fun testOpenCLBackend(): Boolean {
        // TODO: Implement OpenCL test kernel
        return false
    }
    
    private fun testVulkanBackend(): Boolean {
        // TODO: Implement Vulkan compute test
        return false
    }
    
    private fun testQNNBackend(): Boolean {
        // TODO: Implement QNN test
        return false
    }
    
    private suspend fun getCachedOrRunBenchmarks(): BenchmarkResults? {
        val cached = cachedBenchmarks ?: preferences.getCachedBenchmarkResults()
        if (cached != null && 
            System.currentTimeMillis() - cached.timestamp < BENCHMARK_CACHE_VALIDITY_MS) {
            cachedBenchmarks = cached
            return cached
        }
        
        return try {
            val benchmarks = deviceProfileProvider.runBenchmark()
            cachedBenchmarks = benchmarks
            preferences.cacheBenchmarkResults(benchmarks)
            benchmarks
        } catch (e: Exception) {
            Log.w(TAG, "Failed to run benchmarks", e)
            null
        }
    }
    
    private fun validateAndFallback(
        selectedBackend: BackendType,
        deviceProfile: DeviceProfile
    ): BackendType {
        return if (isBackendSupported(selectedBackend, deviceProfile)) {
            selectedBackend
        } else {
            Log.w(TAG, "Selected backend $selectedBackend not supported, falling back to CPU")
            BackendType.CPU_NEON
        }
    }
    
    private fun isBackendValidForCurrentState(
        backend: BackendType,
        thermalState: ThermalState
    ): Boolean {
        return when (thermalState) {
            ThermalState.THERMAL_STATUS_CRITICAL,
            ThermalState.THERMAL_STATUS_EMERGENCY,
            ThermalState.CRITICAL -> {
                backend == BackendType.CPU_NEON
            }
            ThermalState.SEVERE -> {
                backend == BackendType.CPU_NEON || 
                preferences.isGPUAllowedInSevereThermal()
            }
            else -> true
        }
    }
}

// Exception classes
class UnsupportedBackendException(message: String) : Exception(message)
class BackendTestException(message: String) : Exception(message)
