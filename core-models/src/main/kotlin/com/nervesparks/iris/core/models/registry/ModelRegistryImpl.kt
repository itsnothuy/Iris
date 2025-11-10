package com.nervesparks.iris.core.models.registry

import android.content.Context
import android.os.StatFs
import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nervesparks.iris.common.models.BackendType
import com.nervesparks.iris.common.models.DeviceClass
import com.nervesparks.iris.common.models.DeviceProfile
import com.nervesparks.iris.common.models.GPUVendor
import com.nervesparks.iris.common.models.HardwareCapability
import com.nervesparks.iris.common.models.SoCVendor
import com.nervesparks.iris.core.hw.DeviceProfileProvider
import com.nervesparks.iris.core.models.ModelDescriptor
import com.nervesparks.iris.core.models.ModelType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ModelRegistry interface
 */
@Singleton
class ModelRegistryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceProfileProvider: DeviceProfileProvider
) : ModelRegistry {
    
    companion object {
        private const val TAG = "ModelRegistry"
        private const val MODELS_CATALOG_FILE = "models.json"
        private const val REGISTRY_CACHE_KEY = "model_registry_cache"
        private const val CACHE_VALIDITY_HOURS = 24
        private const val STORAGE_BUFFER = 500L * 1024 * 1024 // 500MB buffer
        
        private val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
        
        private fun formatBytes(bytes: Long): String {
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var size = bytes.toDouble()
            var unitIndex = 0
            
            while (size >= 1024 && unitIndex < units.size - 1) {
                size /= 1024
                unitIndex++
            }
            
            return "%.1f %s".format(size, units[unitIndex])
        }
    }
    
    private var cachedRegistry: ModelCatalog? = null
    private val preferences = context.getSharedPreferences("model_registry", Context.MODE_PRIVATE)
    
    override suspend fun getAvailableModels(type: ModelType?): List<ModelDescriptor> {
        val catalog = getModelCatalog()
        return when (type) {
            ModelType.LLM -> catalog.models.llm
            ModelType.EMBEDDING -> catalog.models.embedding
            ModelType.SAFETY -> catalog.models.safety
            null -> catalog.models.llm + catalog.models.embedding + catalog.models.safety
        }
    }
    
    override suspend fun getRecommendedModels(): List<ModelRecommendation> {
        val deviceProfile = deviceProfileProvider.getDeviceProfile()
        val availableModels = getAvailableModels()
        
        return availableModels.mapNotNull { model ->
            val compatibility = assessModelCompatibility(model, deviceProfile)
            if (compatibility.isCompatible) {
                ModelRecommendation(
                    model = model,
                    compatibilityScore = compatibility.score,
                    recommendationReason = compatibility.reason,
                    estimatedPerformance = estimatePerformance(model, deviceProfile),
                    category = determineRecommendationCategory(compatibility.score)
                )
            } else null
        }.sortedByDescending { it.compatibilityScore }
    }
    
    override suspend fun getModelById(modelId: String): ModelDescriptor? {
        return getAvailableModels().find { it.id == modelId }
    }
    
    override suspend fun validateModel(modelDescriptor: ModelDescriptor): ModelValidationResult {
        return try {
            // Check device compatibility
            val deviceProfile = deviceProfileProvider.getDeviceProfile()
            val compatibility = assessModelCompatibility(modelDescriptor, deviceProfile)
            
            if (!compatibility.isCompatible) {
                return ModelValidationResult(
                    isValid = false,
                    reason = compatibility.reason,
                    issues = listOf(ValidationIssue.DEVICE_INCOMPATIBLE)
                )
            }
            
            // Check storage space
            val availableSpace = getAvailableStorageSpace()
            if (availableSpace < modelDescriptor.fileSize + STORAGE_BUFFER) {
                return ModelValidationResult(
                    isValid = false,
                    reason = "Insufficient storage space. Need ${formatBytes(modelDescriptor.fileSize)} but only ${formatBytes(availableSpace)} available.",
                    issues = listOf(ValidationIssue.INSUFFICIENT_STORAGE)
                )
            }
            
            ModelValidationResult(
                isValid = true,
                reason = "Model is compatible and ready for download",
                issues = emptyList()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Model validation failed for ${modelDescriptor.id}", e)
            ModelValidationResult(
                isValid = false,
                reason = "Validation failed: ${e.message}",
                issues = listOf(ValidationIssue.VALIDATION_ERROR)
            )
        }
    }
    
    override suspend fun refreshCatalog(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // For now, just reload from bundled catalog
            // TODO: Implement remote catalog fetching
            val bundledCatalog = loadBundledCatalog()
            cachedRegistry = bundledCatalog
            cacheCatalog(bundledCatalog)
            Log.i(TAG, "Model catalog refreshed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh model catalog", e)
            Result.failure(e)
        }
    }
    
    private suspend fun getModelCatalog(): ModelCatalog {
        // Return cached if available and fresh
        cachedRegistry?.let { cached ->
            if (isCacheValid()) {
                return cached
            }
        }
        
        // Try to load from cache
        val cachedCatalog = loadCachedCatalog()
        if (cachedCatalog != null && isCacheValid()) {
            cachedRegistry = cachedCatalog
            return cachedCatalog
        }
        
        // Refresh catalog in background
        CoroutineScope(Dispatchers.IO).launch {
            refreshCatalog()
        }
        
        val bundledCatalog = loadBundledCatalog()
        cachedRegistry = bundledCatalog
        return bundledCatalog
    }
    
    private fun loadBundledCatalog(): ModelCatalog {
        return try {
            val inputStream = context.assets.open(MODELS_CATALOG_FILE)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            gson.fromJson(jsonString, ModelCatalog::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bundled model catalog", e)
            // Return empty catalog as fallback
            ModelCatalog(
                version = "1.0",
                lastUpdated = System.currentTimeMillis().toString(),
                models = ModelCollections(
                    llm = emptyList(),
                    embedding = emptyList(),
                    safety = emptyList()
                ),
                categories = emptyMap()
            )
        }
    }
    
    private fun loadCachedCatalog(): ModelCatalog? {
        return try {
            val cachedJson = preferences.getString(REGISTRY_CACHE_KEY, null) ?: return null
            gson.fromJson(cachedJson, ModelCatalog::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load cached catalog", e)
            null
        }
    }
    
    private fun cacheCatalog(catalog: ModelCatalog) {
        try {
            val json = gson.toJson(catalog)
            preferences.edit()
                .putString(REGISTRY_CACHE_KEY, json)
                .putLong("cache_timestamp", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache catalog", e)
        }
    }
    
    private fun isCacheValid(): Boolean {
        val cacheTimestamp = preferences.getLong("cache_timestamp", 0)
        val ageHours = (System.currentTimeMillis() - cacheTimestamp) / (1000 * 60 * 60)
        return ageHours < CACHE_VALIDITY_HOURS
    }
    
    private fun assessModelCompatibility(
        model: ModelDescriptor,
        deviceProfile: DeviceProfile
    ): CompatibilityAssessment {
        val issues = mutableListOf<String>()
        var score = 100
        
        // Check RAM requirements
        if (deviceProfile.totalRAM < model.deviceRequirements.minRAM) {
            return CompatibilityAssessment(
                isCompatible = false,
                score = 0,
                reason = "Insufficient RAM: need ${formatBytes(model.deviceRequirements.minRAM)}, have ${formatBytes(deviceProfile.totalRAM)}"
            )
        }
        
        if (deviceProfile.totalRAM < model.deviceRequirements.recommendedRAM) {
            score -= 20
            issues.add("Below recommended RAM")
        }
        
        // Check Android version
        if (deviceProfile.androidVersion < model.deviceRequirements.minAndroidVersion) {
            return CompatibilityAssessment(
                isCompatible = false,
                score = 0,
                reason = "Android version too old: need API ${model.deviceRequirements.minAndroidVersion}, have ${deviceProfile.androidVersion}"
            )
        }
        
        // Check device class
        val deviceClassName = deviceProfile.deviceClass.name
        if (!model.deviceRequirements.deviceClass.contains(deviceClassName)) {
            score -= 30
            issues.add("Not optimized for $deviceClassName devices")
        }
        
        // Check backend support
        val availableBackends = getAvailableBackends(deviceProfile)
        val supportedBackends = model.deviceRequirements.supportedBackends.mapNotNull { backendName ->
            try {
                BackendType.valueOf(backendName)
            } catch (e: IllegalArgumentException) {
                null
            }
        }.toSet()
        
        val compatibleBackends = availableBackends.intersect(supportedBackends)
        
        if (compatibleBackends.isEmpty()) {
            return CompatibilityAssessment(
                isCompatible = false,
                score = 0,
                reason = "No compatible backends available on this device"
            )
        }
        
        // Bonus for optimal backend availability
        val optimalBackend = getOptimalBackend(deviceProfile)
        if (supportedBackends.contains(optimalBackend)) {
            score += 10
        }
        
        val reason = when {
            score >= 90 -> "Excellent compatibility - optimal for your device"
            score >= 70 -> "Good compatibility" + if (issues.isNotEmpty()) " with minor limitations: ${issues.joinToString(", ")}" else ""
            score >= 50 -> "Fair compatibility with some limitations: ${issues.joinToString(", ")}"
            else -> "Poor compatibility: ${issues.joinToString(", ")}"
        }
        
        return CompatibilityAssessment(
            isCompatible = true,
            score = score,
            reason = reason
        )
    }
    
    private fun estimatePerformance(
        model: ModelDescriptor,
        deviceProfile: DeviceProfile
    ): PerformanceEstimate {
        val backendType = getOptimalBackend(deviceProfile)
        val deviceClassName = deviceProfile.deviceClass.name
        
        val tokensPerSecond = model.performance?.tokensPerSecond
            ?.get(backendType.name)
            ?.get(deviceClassName)
            ?: 1.0
        
        val powerConsumption = try {
            PowerConsumption.valueOf(model.performance?.powerConsumption ?: "UNKNOWN")
        } catch (e: IllegalArgumentException) {
            PowerConsumption.UNKNOWN
        }
        
        val thermalProfile = try {
            ThermalProfile.valueOf(model.performance?.thermalProfile ?: "UNKNOWN")
        } catch (e: IllegalArgumentException) {
            ThermalProfile.UNKNOWN
        }
        
        return PerformanceEstimate(
            expectedTokensPerSecond = tokensPerSecond,
            powerConsumption = powerConsumption,
            thermalProfile = thermalProfile,
            backend = backendType
        )
    }
    
    private fun determineRecommendationCategory(score: Int): RecommendationCategory {
        return when {
            score >= 90 -> RecommendationCategory.RECOMMENDED
            score >= 70 -> RecommendationCategory.COMPATIBLE
            score >= 50 -> RecommendationCategory.EXPERIMENTAL
            else -> RecommendationCategory.NOT_RECOMMENDED
        }
    }
    
    private fun getAvailableBackends(deviceProfile: DeviceProfile): Set<BackendType> {
        val backends = mutableSetOf(BackendType.CPU_NEON)
        
        if (deviceProfile.capabilities.contains(HardwareCapability.OPENCL)) {
            backends.add(BackendType.OPENCL_ADRENO)
        }
        
        if (deviceProfile.capabilities.contains(HardwareCapability.VULKAN)) {
            backends.add(BackendType.VULKAN_MALI)
        }
        
        if (deviceProfile.capabilities.contains(HardwareCapability.QNN)) {
            backends.add(BackendType.QNN_HEXAGON)
        }
        
        return backends
    }
    
    private fun getOptimalBackend(deviceProfile: DeviceProfile): BackendType {
        return when (deviceProfile.socVendor) {
            SoCVendor.QUALCOMM -> {
                when (deviceProfile.deviceClass) {
                    DeviceClass.FLAGSHIP -> {
                        if (deviceProfile.capabilities.contains(HardwareCapability.QNN)) {
                            BackendType.QNN_HEXAGON
                        } else {
                            BackendType.OPENCL_ADRENO
                        }
                    }
                    DeviceClass.HIGH_END -> BackendType.OPENCL_ADRENO
                    else -> BackendType.CPU_NEON
                }
            }
            SoCVendor.SAMSUNG, SoCVendor.GOOGLE -> {
                when (deviceProfile.deviceClass) {
                    DeviceClass.FLAGSHIP, DeviceClass.HIGH_END -> {
                        if (deviceProfile.capabilities.contains(HardwareCapability.VULKAN)) {
                            BackendType.VULKAN_MALI
                        } else {
                            BackendType.CPU_NEON
                        }
                    }
                    else -> BackendType.CPU_NEON
                }
            }
            else -> BackendType.CPU_NEON
        }
    }
    
    private fun getAvailableStorageSpace(): Long {
        val modelsDir = getModelsDirectory()
        val stat = StatFs(modelsDir.path)
        return stat.availableBlocksLong * stat.blockSizeLong
    }
    
    private fun getModelsDirectory(): File {
        return File(context.getExternalFilesDir(null), "models").also { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }
}

/**
 * Internal data classes for catalog parsing
 */
internal data class ModelCatalog(
    val version: String,
    val lastUpdated: String,
    val models: ModelCollections,
    val categories: Map<String, CategoryInfo>
)

internal data class ModelCollections(
    val llm: List<ModelDescriptor>,
    val embedding: List<ModelDescriptor>,
    val safety: List<ModelDescriptor>
)

internal data class CategoryInfo(
    val name: String,
    val description: String,
    val icon: String
)

internal data class CompatibilityAssessment(
    val isCompatible: Boolean,
    val score: Int,
    val reason: String
)
