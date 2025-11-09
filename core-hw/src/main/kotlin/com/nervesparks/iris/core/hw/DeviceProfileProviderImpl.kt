package com.nervesparks.iris.core.hw

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.Build
import android.util.Log
import com.nervesparks.iris.common.config.ThermalState
import com.nervesparks.iris.common.models.BackendType
import com.nervesparks.iris.common.models.BenchmarkResult
import com.nervesparks.iris.common.models.BenchmarkResults
import com.nervesparks.iris.common.models.DeviceClass
import com.nervesparks.iris.common.models.DeviceProfile
import com.nervesparks.iris.common.models.GPUInfo
import com.nervesparks.iris.common.models.GPUVendor
import com.nervesparks.iris.common.models.HardwareCapability
import com.nervesparks.iris.common.models.MemoryInfo
import com.nervesparks.iris.common.models.SoCInfo
import com.nervesparks.iris.common.models.SoCVendor
import com.nervesparks.iris.common.models.ThermalCapability
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Implementation of DeviceProfileProvider with comprehensive hardware detection
 */
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
        val socInfo = getSoCInfo()
        val gpuInfo = getGPUInfo()
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
            cores = Runtime.getRuntime().availableProcessors(),
            maxFrequency = getMaxCpuFrequency()
        )
    }
    
    private fun extractSoCModel(vendor: SoCVendor, hardware: String): String {
        return when (vendor) {
            SoCVendor.QUALCOMM -> {
                // Extract Snapdragon model number (e.g., SM8550 -> Snapdragon 8 Gen 2)
                when {
                    hardware.contains("sm8") -> {
                        val modelNum = hardware.substringAfter("sm8").take(3)
                        "Snapdragon 8 Gen ${if (modelNum.toIntOrNull()?.let { it >= 550 } == true) "2+" else "2"}"
                    }
                    hardware.contains("sm7") -> "Snapdragon 7 Series"
                    hardware.contains("sd8") -> "Snapdragon 8 Series"
                    else -> hardware
                }
            }
            SoCVendor.SAMSUNG -> {
                // Extract Exynos model
                if (hardware.contains("exynos")) {
                    hardware.substringAfter("exynos").take(4).let { "Exynos $it" }
                } else {
                    hardware
                }
            }
            SoCVendor.GOOGLE -> {
                // Tensor G series
                if (hardware.contains("gs")) {
                    "Google Tensor G${hardware.substringAfter("gs").take(1)}"
                } else {
                    "Google Tensor"
                }
            }
            SoCVendor.MEDIATEK -> {
                // MediaTek Dimensity
                if (hardware.contains("mt")) {
                    "MediaTek ${hardware.uppercase()}"
                } else {
                    hardware
                }
            }
            SoCVendor.OTHER -> hardware
        }
    }
    
    private fun determineSoCGeneration(vendor: SoCVendor, model: String): Int {
        return when (vendor) {
            SoCVendor.QUALCOMM -> {
                when {
                    model.contains("Gen 3") || model.contains("8550") -> 3
                    model.contains("Gen 2") || model.contains("8450") -> 2
                    model.contains("Gen 1") || model.contains("8350") -> 1
                    else -> 0
                }
            }
            SoCVendor.SAMSUNG -> {
                // Extract generation from Exynos number
                model.filter { it.isDigit() }.toIntOrNull()?.let { it / 100 } ?: 0
            }
            SoCVendor.GOOGLE -> {
                model.filter { it.isDigit() }.toIntOrNull() ?: 0
            }
            else -> 0
        }
    }
    
    private fun getMaxCpuFrequency(): Long {
        return try {
            val cpuInfoFile = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
            if (cpuInfoFile.exists()) {
                cpuInfoFile.readText().trim().toLongOrNull() ?: 0L
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read CPU frequency", e)
            0L
        }
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
    
    private fun extractGPUModel(vendor: GPUVendor, renderer: String): String {
        return when (vendor) {
            GPUVendor.ADRENO -> {
                // Extract Adreno model number (e.g., "Adreno (TM) 740")
                renderer.filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.let { "Adreno $it" } 
                    ?: renderer
            }
            GPUVendor.MALI -> {
                // Extract Mali model (e.g., "Mali-G78")
                renderer.substringAfter("mali", "").trim().ifEmpty { renderer }
            }
            GPUVendor.XCLIPSE -> {
                // Samsung Xclipse GPU
                renderer.substringAfter("xclipse", "").trim().ifEmpty { renderer }
            }
            else -> renderer
        }
    }
    
    private fun extractDriverVersion(version: String): String {
        // Extract version number from OpenGL version string
        return version.trim().takeIf { it.isNotEmpty() } ?: "Unknown"
    }
    
    override fun getMemoryInfo(): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) 
            as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        return MemoryInfo(
            totalRAM = memInfo.totalMem,
            availableRAM = memInfo.availMem,
            lowMemory = memInfo.lowMemory,
            lowMemoryThreshold = memInfo.threshold
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
    
    private fun checkFP16Support(): Boolean {
        // FP16 is supported on ARM64 devices with newer architecture
        return Build.SUPPORTED_ABIS.any { it.contains("arm64-v8a") } &&
               Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }
    
    private fun checkINT8Support(): Boolean {
        // INT8 support generally available on modern ARM cores
        return Build.SUPPORTED_ABIS.any { it.contains("arm64-v8a") } &&
               Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }
    
    private fun determineDeviceClass(socInfo: SoCInfo, memInfo: MemoryInfo): DeviceClass {
        val ramGB = memInfo.totalRAM / (1024 * 1024 * 1024).toDouble()
        
        return when {
            // Flagship: Latest gen SoC + 12GB+ RAM
            socInfo.generation >= 2 && ramGB >= 12 -> DeviceClass.FLAGSHIP
            
            // High-end: Recent gen SoC + 8GB+ RAM
            socInfo.generation >= 1 && ramGB >= 8 -> DeviceClass.HIGH_END
            
            // Mid-range: 6-8GB RAM
            ramGB >= 6 -> DeviceClass.MID_RANGE
            
            // Budget: 4-6GB RAM
            ramGB >= 4 -> DeviceClass.BUDGET
            
            // Low-end: <4GB RAM
            else -> DeviceClass.LOW_END
        }
    }
    
    private fun detectThermalCapability(): ThermalCapability {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> ThermalCapability.ADVANCED_MONITORING
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> ThermalCapability.BASIC_MONITORING
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> ThermalCapability.LIMITED_MONITORING
            else -> ThermalCapability.NO_MONITORING
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
        val cpuScore = cpuResult.performance
        val gpuScore = results.values.filter { it.backend != BackendType.CPU_NEON }
            .maxOfOrNull { it.performance } ?: 0.0
        
        BenchmarkResults(
            cpuScore = cpuScore,
            gpuScore = gpuScore,
            memoryBandwidth = memoryBandwidth,
            backendResults = results,
            thermalBaseline = ThermalState.NORMAL
        )
    }
    
    private suspend fun benchmarkCPU(): BenchmarkResult = withContext(Dispatchers.Default) {
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
        
        BenchmarkResult(
            backend = BackendType.CPU_NEON,
            executionTime = duration,
            performance = gflops,
            memoryUsage = estimateMemoryUsage(matrixSize),
            success = true
        )
    }
    
    private fun estimateMemoryUsage(matrixSize: Int): Long {
        // 3 matrices of floats (4 bytes each)
        return (3L * matrixSize * matrixSize * 4)
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
    
    private fun measureMemoryBandwidth(): Double {
        val startTime = System.nanoTime()
        val arraySize = 10_000_000 // 10MB
        val array = ByteArray(arraySize)
        
        // Simple memory copy benchmark
        var sum = 0L
        for (i in array.indices) {
            sum += array[i]
        }
        
        val endTime = System.nanoTime()
        val duration = (endTime - startTime) / 1_000_000_000.0 // Convert to seconds
        
        // Calculate bandwidth in MB/s
        return (arraySize / duration) / (1024 * 1024)
    }
}
