package com.nervesparks.iris.core.models.registry

import com.nervesparks.iris.core.models.ModelDescriptor
import com.nervesparks.iris.core.models.ModelType

/**
 * Interface for model registry and catalog management
 */
interface ModelRegistry {
    /**
     * Get list of available models, optionally filtered by type
     */
    suspend fun getAvailableModels(type: ModelType? = null): List<ModelDescriptor>
    
    /**
     * Get device-aware model recommendations
     */
    suspend fun getRecommendedModels(): List<ModelRecommendation>
    
    /**
     * Get model by ID
     */
    suspend fun getModelById(modelId: String): ModelDescriptor?
    
    /**
     * Validate if model can be downloaded and run on this device
     */
    suspend fun validateModel(modelDescriptor: ModelDescriptor): ModelValidationResult
    
    /**
     * Refresh model catalog from remote source
     */
    suspend fun refreshCatalog(): Result<Unit>
}

/**
 * Model recommendation with compatibility score
 */
data class ModelRecommendation(
    val model: ModelDescriptor,
    val compatibilityScore: Int,
    val recommendationReason: String,
    val estimatedPerformance: PerformanceEstimate,
    val category: RecommendationCategory
)

/**
 * Performance estimate for a model on this device
 */
data class PerformanceEstimate(
    val expectedTokensPerSecond: Double,
    val powerConsumption: PowerConsumption,
    val thermalProfile: ThermalProfile,
    val backend: com.nervesparks.iris.common.models.BackendType
)

/**
 * Model validation result
 */
data class ModelValidationResult(
    val isValid: Boolean,
    val reason: String,
    val issues: List<ValidationIssue>
)

/**
 * Recommendation category
 */
enum class RecommendationCategory {
    RECOMMENDED,
    COMPATIBLE,
    EXPERIMENTAL,
    NOT_RECOMMENDED
}

/**
 * Power consumption level
 */
enum class PowerConsumption {
    LOW, MEDIUM, HIGH, UNKNOWN
}

/**
 * Thermal profile
 */
enum class ThermalProfile {
    COOL, WARM, HOT, UNKNOWN
}

/**
 * Validation issue types
 */
enum class ValidationIssue {
    DEVICE_INCOMPATIBLE,
    INSUFFICIENT_STORAGE,
    URL_INACCESSIBLE,
    VALIDATION_ERROR
}
