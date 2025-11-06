# Issue #03: Hardware Detection & Backend Selection

## 🎯 Epic: Hardware Optimization
**Priority**: P0 (Critical)  
**Estimate**: 4-6 days  
**Dependencies**: #01 (Core Architecture), #02 (Native llama.cpp Integration)  
**Architecture Reference**: [docs/architecture.md](../architecture.md) - Section 5.1-5.3 Hardware & Backend Architecture

## 📋 Overview
Implement comprehensive hardware detection and intelligent backend selection system that automatically identifies device capabilities and selects optimal AI acceleration backends for maximum performance while ensuring stability across diverse Android hardware.

## 🎯 Goals
- **Device Profiling**: Accurate detection of SoC, GPU, and system capabilities
- **Backend Optimization**: Automatic selection of CPU/OpenCL/Vulkan/QNN backends
- **Performance Benchmarking**: Runtime validation of backend performance
- **Thermal Awareness**: Integration with thermal management for adaptive behavior
- **Fallback Strategy**: Robust fallback mechanisms for unsupported hardware

## 📝 Detailed Tasks

### 1. Device Profile Detection

#### 1.1 SoC and Hardware Identification
Create `core-hw-detection/src/main/kotlin/DeviceProfileProviderImpl.kt`:

```kotlin
@Singleton
class DeviceProfileProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceProfileProvider {
    
    companion object {
        private const val TAG = "DeviceProfileProvider"
        
        // Known SoC patterns for identification
        private val SNAPDRAGON_PATTERNS = listOf(
            "qcom", "qualcomm", "sm8", "sd8", "sm7", "sd7", "msm"
        )
        
        private val EXYNOS_PATTERNS = listOf(
            "exynos", "universal", "s5e", "samsung"
        )
        
        private val MEDIATEK_PATTERNS = listOf(
            "mt", "mediatek", "dimensity"
        )
        
        private val TENSOR_PATTERNS = listOf(
            "gs", "google", "tensor", "whitechapel"
        )
    }
    
    private var cachedProfile: DeviceProfile? = null
    
    override fun getDeviceProfile(): DeviceProfile {
        return cachedProfile ?: generateDeviceProfile().also { cachedProfile = it }
    }
    
    private fun generateDeviceProfile(): DeviceProfile {
        val socInfo = detectSoCInfo()
        val gpuInfo = detectGPUInfo()
        val memoryInfo = getMemoryInfo()
        val capabilities = detectHardwareCapabilities()
        
        return DeviceProfile(
            socVendor = socInfo.vendor,
            socModel = socInfo.model,
            gpuVendor = gpuInfo.vendor,
            gpuModel = gpuInfo.model,
            totalRAM = memoryInfo.totalRAM,
            availableRAM = memoryInfo.availableRAM,
            androidVersion = Build.VERSION.SDK_INT,
            capabilities = capabilities,
            deviceClass = determineDeviceClass(socInfo, memoryInfo),
            thermalCapability = detectThermalCapability()
        )
    }
    
    override fun getSoCInfo(): SoCInfo {
        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()
        
        // Combine all identifiers for pattern matching
        val identifiers = listOf(hardware, board, device, product)
        
        val vendor = when {
            identifiers.any { id -> SNAPDRAGON_PATTERNS.any { pattern -> id.contains(pattern) } } -> {
                SoCVendor.QUALCOMM
            }
            identifiers.any { id -> EXYNOS_PATTERNS.any { pattern -> id.contains(pattern) } } -> {
                SoCVendor.SAMSUNG
            }
            identifiers.any { id -> TENSOR_PATTERNS.any { pattern -> id.contains(pattern) } } -> {
                SoCVendor.GOOGLE
            }
            identifiers.any { id -> MEDIATEK_PATTERNS.any { pattern -> id.contains(pattern) } } -> {
                SoCVendor.MEDIATEK
            }
            else -> SoCVendor.OTHER
        }
        
        val model = extractSoCModel(vendor, hardware)
        val generation = determineSoCGeneration(vendor, model)
        
        return SoCInfo(
            vendor = vendor,
            model = model,
            generation = generation,
            architecture = "ARM64",
            coreCount = Runtime.getRuntime().availableProcessors(),
            maxFrequency = getMaxCpuFrequency()
        )
    }
    
    override fun getGPUInfo(): GPUInfo {
        return try {
            // Use OpenGL ES to query GPU information
            val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            EGL14.eglInitialize(eglDisplay, null, 0, null, 0)
            
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            
            val attribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
            )
            
            EGL14.eglChooseConfig(eglDisplay, attribs, 0, configs, 0, 1, numConfigs, 0)
            
            val context = EGL14.eglCreateContext(
                eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0
            )
            
            val surface = EGL14.eglCreatePbufferSurface(
                eglDisplay, configs[0],
                intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0
            )
            
            EGL14.eglMakeCurrent(eglDisplay, surface, surface, context)
            
            // Query GPU information
            val renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown"
            val vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: "Unknown"
            val version = GLES20.glGetString(GLES20.GL_VERSION) ?: "Unknown"
            
            // Cleanup
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(eglDisplay, surface)
            EGL14.eglDestroyContext(eglDisplay, context)
            EGL14.eglTerminate(eglDisplay)
            
            parseGPUInfo(renderer, vendor, version)
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query GPU info via OpenGL", e)
            GPUInfo(
                vendor = GPUVendor.OTHER,
                model = "Unknown",
                driverVersion = "Unknown",
                openglVersion = "Unknown",
                vulkanSupported = false,
                openclSupported = false
            )
        }
    }
    
    private fun parseGPUInfo(renderer: String, vendor: String, version: String): GPUInfo {
        val gpuVendor = when {
            renderer.contains("adreno", ignoreCase = true) -> GPUVendor.ADRENO
            renderer.contains("mali", ignoreCase = true) -> GPUVendor.MALI
            renderer.contains("xclipse", ignoreCase = true) -> GPUVendor.XCLIPSE
            renderer.contains("powervr", ignoreCase = true) -> GPUVendor.POWERVR
            else -> GPUVendor.OTHER
        }
        
        val model = extractGPUModel(gpuVendor, renderer)
        val vulkanSupported = checkVulkanSupport()
        val openclSupported = checkOpenCLSupport(gpuVendor)
        
        return GPUInfo(
            vendor = gpuVendor,
            model = model,
            driverVersion = extractDriverVersion(version),
            openglVersion = version,
            vulkanSupported = vulkanSupported,
            openclSupported = openclSupported
        )
    }
    
    override fun getMemoryInfo(): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        // Get total RAM using various methods
        val totalRAM = getTotalRAM()
        
        return MemoryInfo(
            totalRAM = totalRAM,
            availableRAM = memInfo.availMem,
            lowMemoryThreshold = memInfo.threshold,
            isLowMemory = memInfo.lowMemory
        )
    }
    
    private fun detectHardwareCapabilities(): Set<HardwareCapability> {
        val capabilities = mutableSetOf<HardwareCapability>()
        
        // Check OpenCL support
        if (checkOpenCLSupport()) {
            capabilities.add(HardwareCapability.OPENCL)
        }
        
        // Check Vulkan support
        if (checkVulkanSupport()) {
            capabilities.add(HardwareCapability.VULKAN)
        }
        
        // Check NNAPI support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            capabilities.add(HardwareCapability.NNAPI)
        }
        
        // Check QNN support (Qualcomm devices)
        if (getSoCInfo().vendor == SoCVendor.QUALCOMM) {
            capabilities.add(HardwareCapability.QNN)
        }
        
        // Check FP16 support
        if (checkFP16Support()) {
            capabilities.add(HardwareCapability.FP16_SUPPORT)
        }
        
        // Check INT8 support
        if (checkINT8Support()) {
            capabilities.add(HardwareCapability.INT8_SUPPORT)
        }
        
        return capabilities
    }
    
    private fun checkVulkanSupport(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val packageManager = context.packageManager
                packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }
    
    private fun checkOpenCLSupport(gpuVendor: GPUVendor = getGPUInfo().vendor): Boolean {
        return when (gpuVendor) {
            GPUVendor.ADRENO -> true // Most Adreno GPUs support OpenCL
            GPUVendor.MALI -> checkMaliOpenCLSupport()
            GPUVendor.XCLIPSE -> true // Samsung Xclipse supports OpenCL
            else -> false
        }
    }
    
    private fun checkMaliOpenCLSupport(): Boolean {
        // Mali OpenCL support varies by model and driver version
        val gpuModel = getGPUInfo().model.lowercase()
        return when {
            gpuModel.contains("g71") || gpuModel.contains("g72") || 
            gpuModel.contains("g76") || gpuModel.contains("g77") ||
            gpuModel.contains("g78") || gpuModel.contains("g710") -> true
            else -> false
        }
    }
    
    override suspend fun runBenchmark(): BenchmarkResults = withContext(Dispatchers.IO) {
        val results = mutableMapOf<BackendType, BenchmarkResult>()
        
        // CPU benchmark
        val cpuResult = benchmarkCPU()
        results[BackendType.CPU_NEON] = cpuResult
        
        // GPU benchmarks (if supported)
        val gpuInfo = getGPUInfo()
        if (gpuInfo.openclSupported) {
            val openclResult = benchmarkOpenCL()
            results[BackendType.OPENCL_ADRENO] = openclResult
        }
        
        if (gpuInfo.vulkanSupported) {
            val vulkanResult = benchmarkVulkan()
            results[BackendType.VULKAN_MALI] = vulkanResult
        }
        
        // Memory and thermal info
        val memoryBandwidth = measureMemoryBandwidth()
        val thermalBaseline = getCurrentThermalState()
        
        BenchmarkResults(
            backendResults = results,
            memoryBandwidth = memoryBandwidth,
            thermalBaseline = thermalBaseline,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private suspend fun benchmarkCPU(): BenchmarkResult {
        val startTime = System.nanoTime()
        
        // Simple matrix multiplication benchmark
        val matrixSize = 256
        val a = Array(matrixSize) { FloatArray(matrixSize) { Random.nextFloat() } }
        val b = Array(matrixSize) { FloatArray(matrixSize) { Random.nextFloat() } }
        val c = Array(matrixSize) { FloatArray(matrixSize) }
        
        // Perform computation
        for (i in 0 until matrixSize) {
            for (j in 0 until matrixSize) {
                for (k in 0 until matrixSize) {
                    c[i][j] += a[i][k] * b[k][j]
                }
            }
        }
        
        val endTime = System.nanoTime()
        val duration = (endTime - startTime) / 1_000_000 // Convert to milliseconds
        
        val gflops = (2.0 * matrixSize * matrixSize * matrixSize) / (duration * 1_000_000)
        
        return BenchmarkResult(
            backend = BackendType.CPU_NEON,
            executionTime = duration,
            performance = gflops,
            memoryUsage = estimateMemoryUsage(matrixSize),
            success = true
        )
    }
    
    private suspend fun benchmarkOpenCL(): BenchmarkResult {
        // TODO: Implement OpenCL-specific benchmark
        // This would require OpenCL kernel compilation and execution
        return BenchmarkResult(
            backend = BackendType.OPENCL_ADRENO,
            executionTime = 0L,
            performance = 0.0,
            memoryUsage = 0L,
            success = false
        )
    }
    
    private suspend fun benchmarkVulkan(): BenchmarkResult {
        // TODO: Implement Vulkan-specific benchmark
        // This would require Vulkan compute shader compilation and execution
        return BenchmarkResult(
            backend = BackendType.VULKAN_MALI,
            executionTime = 0L,
            performance = 0.0,
            memoryUsage = 0L,
            success = false
        )
    }
}

// Data classes for hardware information
data class SoCInfo(
    val vendor: SoCVendor,
    val model: String,
    val generation: Int,
    val architecture: String,
    val coreCount: Int,
    val maxFrequency: Long
)

data class GPUInfo(
    val vendor: GPUVendor,
    val model: String,
    val driverVersion: String,
    val openglVersion: String,
    val vulkanSupported: Boolean,
    val openclSupported: Boolean
)

data class MemoryInfo(
    val totalRAM: Long,
    val availableRAM: Long,
    val lowMemoryThreshold: Long,
    val isLowMemory: Boolean
)

data class BenchmarkResults(
    val backendResults: Map<BackendType, BenchmarkResult>,
    val memoryBandwidth: Double,
    val thermalBaseline: ThermalState,
    val timestamp: Long
)

data class BenchmarkResult(
    val backend: BackendType,
    val executionTime: Long,
    val performance: Double,
    val memoryUsage: Long,
    val success: Boolean
)

enum class DeviceClass {
    FLAGSHIP, HIGH_END, MID_RANGE, BUDGET, LOW_END
}

enum class ThermalCapability {
    ADVANCED_MONITORING, // Android 11+ ADPF
    BASIC_MONITORING,    // Android 10+ ThermalManager
    LIMITED_MONITORING,  // Legacy thermal detection
    NO_MONITORING        // No thermal APIs available
}
```

### 2. Backend Selection Logic

#### 2.1 Backend Router Implementation
Create `core-hw-router/src/main/kotlin/BackendRouterImpl.kt`:

```kotlin
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
    
    private var currentBackend: BackendType? = null
    private var cachedBenchmarks: BenchmarkResults? = null
    private val backendSelectionMatrix = createBackendSelectionMatrix()
    
    override suspend fun selectOptimalBackend(task: ComputeTask): BackendType {
        val deviceProfile = deviceProfileProvider.getDeviceProfile()
        val thermalState = thermalManager.getCurrentThermalState()
        
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
            ThermalState.THERMAL_STATUS_EMERGENCY -> {
                // Only CPU in critical thermal state
                preferredBackends.filter { it == BackendType.CPU_NEON }
            }
            ThermalState.THERMAL_STATUS_SEVERE -> {
                // Prefer CPU, but allow GPU if significantly faster
                preferredBackends.sortedBy { 
                    if (it == BackendType.CPU_NEON) 0 else 1 
                }
            }
            else -> preferredBackends
        }
        
        // Use benchmark results if available
        return if (benchmarks != null) {
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
        return currentBackend ?: BackendType.CPU_NEON
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
        val cached = cachedBenchmarks
        if (cached != null && 
            System.currentTimeMillis() - cached.timestamp < BENCHMARK_CACHE_VALIDITY_MS) {
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
            ThermalState.THERMAL_STATUS_EMERGENCY -> {
                backend == BackendType.CPU_NEON
            }
            ThermalState.THERMAL_STATUS_SEVERE -> {
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

// Preferences interface for caching backend selections
interface BackendPreferences {
    fun getCachedBackend(task: ComputeTask, deviceProfile: DeviceProfile): BackendType?
    fun cacheBackendSelection(task: ComputeTask, deviceProfile: DeviceProfile, backend: BackendType)
    fun cacheBenchmarkResults(results: BenchmarkResults)
    fun isGPUAllowedInSevereThermal(): Boolean
}

@Singleton
class BackendPreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BackendPreferences {
    
    private val preferences = context.getSharedPreferences("backend_cache", Context.MODE_PRIVATE)
    
    override fun getCachedBackend(task: ComputeTask, deviceProfile: DeviceProfile): BackendType? {
        val key = "${task.name}_${deviceProfile.socVendor}_${deviceProfile.deviceClass}"
        val backendName = preferences.getString(key, null) ?: return null
        return try {
            BackendType.valueOf(backendName)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
    
    override fun cacheBackendSelection(
        task: ComputeTask,
        deviceProfile: DeviceProfile,
        backend: BackendType
    ) {
        val key = "${task.name}_${deviceProfile.socVendor}_${deviceProfile.deviceClass}"
        preferences.edit().putString(key, backend.name).apply()
    }
    
    override fun cacheBenchmarkResults(results: BenchmarkResults) {
        // Store benchmark results as JSON
        val json = gson.toJson(results)
        preferences.edit()
            .putString("benchmark_results", json)
            .putLong("benchmark_timestamp", results.timestamp)
            .apply()
    }
    
    override fun isGPUAllowedInSevereThermal(): Boolean {
        return preferences.getBoolean("gpu_severe_thermal", false)
    }
    
    companion object {
        private val gson = Gson()
    }
}
```

### 3. Thermal State Integration

#### 3.1 Thermal Manager Implementation
Create `core-thermal/src/main/kotlin/ThermalManagerImpl.kt`:

```kotlin
@Singleton
class ThermalManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventBus: EventBus
) : ThermalManager {
    
    companion object {
        private const val TAG = "ThermalManager"
        private const val THERMAL_CHECK_INTERVAL_MS = 5000L
    }
    
    private var currentThermalState = ThermalState.THERMAL_STATUS_NONE
    private var performanceProfile = PerformanceProfile.BALANCED
    private var thermalMonitoringJob: Job? = null
    
    private val thermalCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        object : PowerManager.OnThermalStatusChangedListener {
            override fun onThermalStatusChanged(status: Int) {
                val thermalState = mapAndroidThermalState(status)
                handleThermalStateChange(thermalState)
            }
        }
    } else null
    
    override fun startMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.addThermalStatusListener(thermalCallback!!)
            currentThermalState = mapAndroidThermalState(powerManager.currentThermalStatus)
        }
        
        // Start periodic monitoring for older devices
        thermalMonitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val legacyThermalState = detectLegacyThermalState()
                    if (legacyThermalState != currentThermalState) {
                        handleThermalStateChange(legacyThermalState)
                    }
                    delay(THERMAL_CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.w(TAG, "Error in thermal monitoring", e)
                }
            }
        }
        
        Log.i(TAG, "Thermal monitoring started, current state: $currentThermalState")
    }
    
    override fun stopMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thermalCallback != null) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.removeThermalStatusListener(thermalCallback)
        }
        
        thermalMonitoringJob?.cancel()
        thermalMonitoringJob = null
        
        Log.i(TAG, "Thermal monitoring stopped")
    }
    
    override fun getCurrentThermalState(): ThermalState {
        return currentThermalState
    }
    
    override fun getCurrentProfile(): PerformanceProfile {
        return performanceProfile
    }
    
    override fun setPerformanceProfile(profile: PerformanceProfile) {
        performanceProfile = profile
        eventBus.emit(IrisEvent.PerformanceProfileChanged(profile))
        Log.i(TAG, "Performance profile changed to: $profile")
    }
    
    override fun shouldThrottle(): Boolean {
        return when (currentThermalState) {
            ThermalState.THERMAL_STATUS_SEVERE,
            ThermalState.THERMAL_STATUS_CRITICAL,
            ThermalState.THERMAL_STATUS_EMERGENCY -> true
            else -> false
        }
    }
    
    override fun adaptGenerationParams(params: GenerationParams): GenerationParams {
        return when (currentThermalState) {
            ThermalState.THERMAL_STATUS_CRITICAL,
            ThermalState.THERMAL_STATUS_EMERGENCY -> {
                params.copy(
                    maxTokens = minOf(params.maxTokens, 128),
                    temperature = 0.1f // Reduce randomness for faster generation
                )
            }
            ThermalState.THERMAL_STATUS_SEVERE -> {
                params.copy(
                    maxTokens = minOf(params.maxTokens, 256),
                    temperature = minOf(params.temperature, 0.5f)
                )
            }
            ThermalState.THERMAL_STATUS_MODERATE -> {
                params.copy(
                    maxTokens = minOf(params.maxTokens, 512)
                )
            }
            else -> params
        }
    }
    
    override fun getOptimalThreadCount(): Int {
        val baseThreadCount = Runtime.getRuntime().availableProcessors()
        return when (currentThermalState) {
            ThermalState.THERMAL_STATUS_CRITICAL,
            ThermalState.THERMAL_STATUS_EMERGENCY -> 1
            ThermalState.THERMAL_STATUS_SEVERE -> maxOf(1, baseThreadCount / 4)
            ThermalState.THERMAL_STATUS_MODERATE -> maxOf(2, baseThreadCount / 2)
            else -> when (performanceProfile) {
                PerformanceProfile.PERFORMANCE -> baseThreadCount
                PerformanceProfile.BALANCED -> maxOf(4, baseThreadCount * 3 / 4)
                PerformanceProfile.BATTERY_SAVER -> maxOf(2, baseThreadCount / 2)
                PerformanceProfile.EMERGENCY -> 1
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun mapAndroidThermalState(androidState: Int): ThermalState {
        return when (androidState) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalState.THERMAL_STATUS_NONE
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.THERMAL_STATUS_LIGHT
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.THERMAL_STATUS_MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.THERMAL_STATUS_SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL -> ThermalState.THERMAL_STATUS_CRITICAL
            PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalState.THERMAL_STATUS_EMERGENCY
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState.THERMAL_STATUS_EMERGENCY
            else -> ThermalState.THERMAL_STATUS_NONE
        }
    }
    
    private fun detectLegacyThermalState(): ThermalState {
        // For devices without proper thermal APIs, use heuristics
        return try {
            val batteryTemp = getBatteryTemperature()
            val cpuUsage = getCPUUsage()
            
            when {
                batteryTemp > 45.0f || cpuUsage > 90.0f -> ThermalState.THERMAL_STATUS_SEVERE
                batteryTemp > 40.0f || cpuUsage > 80.0f -> ThermalState.THERMAL_STATUS_MODERATE
                batteryTemp > 35.0f || cpuUsage > 70.0f -> ThermalState.THERMAL_STATUS_LIGHT
                else -> ThermalState.THERMAL_STATUS_NONE
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to detect legacy thermal state", e)
            ThermalState.THERMAL_STATUS_NONE
        }
    }
    
    private fun getBatteryTemperature(): Float {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        
        return batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)?.let {
            it / 10.0f // Convert from tenths of degrees to degrees Celsius
        } ?: 0.0f
    }
    
    private fun getCPUUsage(): Float {
        return try {
            val process = Runtime.getRuntime().exec("cat /proc/stat")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            reader.close()
            
            // Parse CPU usage from /proc/stat
            val parts = line.split("\\s+".toRegex())
            if (parts.size >= 5) {
                val idle = parts[4].toLong()
                val total = parts.subList(1, 5).sumOf { it.toLong() }
                ((total - idle) * 100.0f / total)
            } else {
                0.0f
            }
        } catch (e: Exception) {
            0.0f
        }
    }
    
    private fun handleThermalStateChange(newState: ThermalState) {
        if (newState != currentThermalState) {
            val previousState = currentThermalState
            currentThermalState = newState
            
            Log.i(TAG, "Thermal state changed: $previousState -> $newState")
            eventBus.emit(IrisEvent.ThermalStateChanged(newState))
            
            // Automatic performance profile adjustment
            when (newState) {
                ThermalState.THERMAL_STATUS_CRITICAL,
                ThermalState.THERMAL_STATUS_EMERGENCY -> {
                    setPerformanceProfile(PerformanceProfile.EMERGENCY)
                }
                ThermalState.THERMAL_STATUS_SEVERE -> {
                    if (performanceProfile == PerformanceProfile.PERFORMANCE) {
                        setPerformanceProfile(PerformanceProfile.BATTERY_SAVER)
                    }
                }
                ThermalState.THERMAL_STATUS_NONE,
                ThermalState.THERMAL_STATUS_LIGHT -> {
                    if (performanceProfile == PerformanceProfile.EMERGENCY) {
                        setPerformanceProfile(PerformanceProfile.BALANCED)
                    }
                }
                else -> {
                    // Keep current profile for moderate state
                }
            }
        }
    }
}

enum class ThermalState {
    THERMAL_STATUS_NONE,
    THERMAL_STATUS_LIGHT,
    THERMAL_STATUS_MODERATE,
    THERMAL_STATUS_SEVERE,
    THERMAL_STATUS_CRITICAL,
    THERMAL_STATUS_EMERGENCY
}
```

## 🧪 Testing Strategy

### Unit Tests
- [ ] **Device Detection Accuracy**
  - SoC vendor identification across known devices
  - GPU model detection and capability mapping
  - Memory information accuracy
  - Hardware capability detection validation

### Integration Tests
- [ ] **Backend Selection Logic**
  - Optimal backend selection for various device profiles
  - Thermal state adaptation behavior
  - Fallback mechanisms for unsupported backends
  - Performance profile impact on backend choice

### Device Testing
- [ ] **Multi-Device Validation**
  - Snapdragon devices (8 Gen1+, 7 Gen1+)
  - Exynos devices (2200+, 1380+)
  - Google Tensor devices
  - MediaTek Dimensity devices
  - Budget devices with limited capabilities

### Performance Tests
- [ ] **Backend Performance**
  - Benchmark accuracy and reliability
  - Backend switching overhead
  - Thermal monitoring responsiveness
  - Memory usage optimization

## ✅ Acceptance Criteria

### Primary Criteria
- [ ] **Accurate Device Detection**: 95%+ accuracy on target device types
- [ ] **Optimal Backend Selection**: Best performing backend selected automatically
- [ ] **Thermal Integration**: Proper response to thermal state changes
- [ ] **Fallback Reliability**: Graceful fallback to CPU when GPU unavailable
- [ ] **Performance Validation**: Backend benchmarks correlate with real inference performance

### Technical Criteria
- [ ] **Detection Speed**: Device profiling completes in <2 seconds
- [ ] **Backend Switch Time**: Backend switching completes in <1 second
- [ ] **Memory Efficiency**: Hardware detection uses <10MB peak memory
- [ ] **Thermal Response**: Thermal state changes detected within 5 seconds

### Quality Criteria
- [ ] **Error Handling**: Comprehensive error recovery for all detection failures
- [ ] **Thread Safety**: All components thread-safe for concurrent access
- [ ] **Cache Management**: Efficient caching of detection results and benchmarks
- [ ] **Battery Impact**: Minimal battery drain from monitoring activities

## 🔗 Related Issues
- **Depends on**: #01 (Core Architecture), #02 (Native llama.cpp Integration)
- **Enables**: #04 (Model Management), #05 (Chat Engine), #12 (Thermal Management)
- **Related**: #10 (Safety Engine), #07 (ASR Engine)

## 📋 Definition of Done
- [ ] Complete device profiling system implemented
- [ ] Backend selection matrix and logic functional
- [ ] Thermal state monitoring integrated
- [ ] Performance benchmarking system operational
- [ ] Comprehensive test suite passing across device types
- [ ] Performance criteria met on target devices
- [ ] Documentation updated with supported devices and backends
- [ ] Code review completed and approved

---

**Note**: This system forms the foundation for all hardware-aware optimizations in iris_android. Subsequent components will rely on accurate device detection and optimal backend selection.