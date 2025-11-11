package com.nervesparks.iris.core.multimodal.registry

import android.content.Context
import android.util.Log
import com.nervesparks.iris.common.models.DeviceClass
import com.nervesparks.iris.common.models.DeviceProfile
import com.nervesparks.iris.core.hw.DeviceProfileProvider
import com.nervesparks.iris.core.multimodal.MultimodalModelRegistry
import com.nervesparks.iris.core.multimodal.types.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of multimodal model registry with device-aware recommendations
 */
@Singleton
class MultimodalModelRegistryImpl @Inject constructor(
    private val deviceProfileProvider: DeviceProfileProvider,
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) : MultimodalModelRegistry {
    
    companion object {
        private const val TAG = "MultimodalModelRegistry"
        private const val MULTIMODAL_CATALOG_FILE = "multimodal_models.json"
        
        // Model compatibility scoring weights
        private const val MEMORY_WEIGHT = 0.4
        private const val PERFORMANCE_WEIGHT = 0.3
        private const val FEATURE_WEIGHT = 0.2
        private const val DEVICE_CLASS_WEIGHT = 0.1
    }
    
    private val modelCache = ConcurrentHashMap<String, MultimodalModelDescriptor>()
    private val compatibilityCache = ConcurrentHashMap<String, MultimodalModelCompatibilityAssessment>()
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun getRecommendedModel(visionTask: VisionTask): Result<MultimodalModelDescriptor> = 
        withContext(ioDispatcher) {
            try {
                Log.d(TAG, "Getting recommended model for task: $visionTask")
                
                val availableModels = getAvailableModels().getOrThrow()
                val deviceProfile = deviceProfileProvider.getDeviceProfile()
                
                // Filter models that support the requested task
                val supportingModels = availableModels.filter { model ->
                    when (visionTask) {
                        VisionTask.OBJECT_DETECTION -> model.capabilities.contains(MultimodalCapability.OBJECT_DETECTION)
                        VisionTask.TEXT_RECOGNITION -> model.capabilities.contains(MultimodalCapability.TEXT_RECOGNITION)
                        VisionTask.IMAGE_CLASSIFICATION -> model.capabilities.contains(MultimodalCapability.IMAGE_CLASSIFICATION)
                        VisionTask.SCENE_ANALYSIS -> model.capabilities.contains(MultimodalCapability.SCENE_ANALYSIS)
                        VisionTask.GENERAL_QA -> model.capabilities.contains(MultimodalCapability.VISUAL_QUESTION_ANSWERING)
                    }
                }
                
                if (supportingModels.isEmpty()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("No models available for task: $visionTask")
                    )
                }
                
                // Score and rank models
                val scoredModels = supportingModels.mapNotNull { model ->
                    val compatibility = assessModelCompatibility(model).getOrNull()
                    if (compatibility != null && compatibility.isSupported) {
                        Pair(model, compatibility.compatibilityScore)
                    } else null
                }.sortedByDescending { it.second }
                
                val bestModel = scoredModels.firstOrNull()?.first
                    ?: return@withContext Result.failure(
                        IllegalStateException("No compatible model found")
                    )
                
                Log.i(TAG, "Recommended model: ${bestModel.id} for task: $visionTask")
                Result.success(bestModel)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get recommended model", e)
                Result.failure(MultimodalInferenceException("Model recommendation failed", e))
            }
        }
    
    override suspend fun assessModelCompatibility(
        model: MultimodalModelDescriptor
    ): Result<MultimodalModelCompatibilityAssessment> =
        withContext(ioDispatcher) {
            try {
                // Check cache first
                compatibilityCache[model.id]?.let { cached ->
                    return@withContext Result.success(cached)
                }
                
                val deviceProfile = deviceProfileProvider.getDeviceProfile()
                val assessment = calculateCompatibilityScore(model, deviceProfile)
                
                // Cache the result
                compatibilityCache[model.id] = assessment
                
                Result.success(assessment)
                
            } catch (e: Exception) {
                Log.e(TAG, "Compatibility assessment failed for model: ${model.id}", e)
                Result.failure(MultimodalInferenceException("Compatibility assessment failed", e))
            }
        }
    
    override suspend fun getAvailableModels(): Result<List<MultimodalModelDescriptor>> =
        withContext(ioDispatcher) {
            try {
                // Load from catalog if cache is empty
                if (modelCache.isEmpty()) {
                    loadModelCatalog()
                }
                
                Result.success(modelCache.values.toList())
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load available models", e)
                Result.failure(MultimodalInferenceException("Failed to load models", e))
            }
        }
    
    override suspend fun getModelById(modelId: String): Result<MultimodalModelDescriptor> =
        withContext(ioDispatcher) {
            try {
                modelCache[modelId]?.let { model ->
                    return@withContext Result.success(model)
                }
                
                // Try to load from catalog
                loadModelCatalog()
                
                modelCache[modelId]?.let { model ->
                    Result.success(model)
                } ?: Result.failure(
                    IllegalArgumentException("Model not found: $modelId")
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get model by ID: $modelId", e)
                Result.failure(MultimodalInferenceException("Model lookup failed", e))
            }
        }
    
    private suspend fun loadModelCatalog() {
        try {
            val catalogStream: InputStream = context.assets.open(MULTIMODAL_CATALOG_FILE)
            val catalogJson = catalogStream.bufferedReader().use { it.readText() }
            
            // Parse the catalog
            val catalog = json.decodeFromString<MultimodalModelCatalog>(catalogJson)
            
            // Populate cache
            catalog.models.forEach { model ->
                modelCache[model.id] = model
            }
            
            Log.i(TAG, "Loaded ${catalog.models.size} models from catalog")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model catalog", e)
            // Load default fallback models
            loadFallbackModels()
        }
    }
    
    private fun loadFallbackModels() {
        val fallbackModel = MultimodalModelDescriptor(
            id = "llava-1.5-7b-q4",
            name = "LLaVA 1.5 7B (Q4)",
            baseModel = "vicuna-7b-v1.5",
            visionRequirements = VisionRequirements(
                maxImageSize = ImageSize(512, 512),
                supportedFormats = listOf(ImageFormat.JPEG, ImageFormat.PNG),
                minConfidence = 0.6f
            ),
            supportedImageFormats = listOf(ImageFormat.JPEG, ImageFormat.PNG, ImageFormat.WEBP),
            performance = ModelPerformance(
                inferenceTimeMs = 800L,
                memoryUsageMB = 4096,
                accuracy = 0.82f
            ),
            capabilities = listOf(
                MultimodalCapability.VISUAL_QUESTION_ANSWERING,
                MultimodalCapability.IMAGE_CLASSIFICATION,
                MultimodalCapability.SCENE_ANALYSIS
            )
        )
        
        modelCache[fallbackModel.id] = fallbackModel
        Log.w(TAG, "Loaded fallback model: ${fallbackModel.id}")
    }
    
    private fun calculateCompatibilityScore(
        model: MultimodalModelDescriptor,
        deviceProfile: DeviceProfile
    ): MultimodalModelCompatibilityAssessment {
        var score = 0.0
        val issues = mutableListOf<String>()
        
        // Memory compatibility check
        val requiredMemory = model.performance.memoryUsageMB * 1024L * 1024L
        val availableMemory = deviceProfile.availableRAM
        
        when {
            availableMemory >= requiredMemory * 1.5 -> score += MEMORY_WEIGHT * 1.0
            availableMemory >= requiredMemory * 1.2 -> score += MEMORY_WEIGHT * 0.8
            availableMemory >= requiredMemory -> score += MEMORY_WEIGHT * 0.6
            else -> {
                score += MEMORY_WEIGHT * 0.2
                issues.add("Insufficient memory: need ${requiredMemory / (1024*1024)}MB, have ${availableMemory / (1024*1024)}MB")
            }
        }
        
        // Performance compatibility
        val expectedInferenceTime = model.performance.inferenceTimeMs
        when {
            expectedInferenceTime <= 500 -> score += PERFORMANCE_WEIGHT * 1.0
            expectedInferenceTime <= 1000 -> score += PERFORMANCE_WEIGHT * 0.8
            expectedInferenceTime <= 2000 -> score += PERFORMANCE_WEIGHT * 0.6
            else -> {
                score += PERFORMANCE_WEIGHT * 0.4
                issues.add("Slow inference expected: ${expectedInferenceTime}ms")
            }
        }
        
        // Feature compatibility
        val supportedCapabilities = model.capabilities.size
        when {
            supportedCapabilities >= 5 -> score += FEATURE_WEIGHT * 1.0
            supportedCapabilities >= 3 -> score += FEATURE_WEIGHT * 0.8
            supportedCapabilities >= 2 -> score += FEATURE_WEIGHT * 0.6
            else -> score += FEATURE_WEIGHT * 0.4
        }
        
        // Device class bonus
        when (deviceProfile.deviceClass) {
            DeviceClass.FLAGSHIP -> score += DEVICE_CLASS_WEIGHT * 1.0
            DeviceClass.HIGH_END -> score += DEVICE_CLASS_WEIGHT * 0.8
            DeviceClass.MID_RANGE -> score += DEVICE_CLASS_WEIGHT * 0.6
            DeviceClass.BUDGET -> score += DEVICE_CLASS_WEIGHT * 0.4
            DeviceClass.LOW_END -> score += DEVICE_CLASS_WEIGHT * 0.2
        }
        
        val isSupported = score >= 0.5 && availableMemory >= requiredMemory
        
        return MultimodalModelCompatibilityAssessment(
            isSupported = isSupported,
            compatibilityScore = score,
            memoryRequirement = requiredMemory,
            reasonsForIncompatibility = if (isSupported) emptyList() else issues
        )
    }
}

/**
 * Serializable catalog structure
 */
@Serializable
data class MultimodalModelCatalog(
    val version: String,
    val lastUpdated: Long,
    val models: List<MultimodalModelDescriptor>
)
