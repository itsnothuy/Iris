package com.nervesparks.iris.core.models

/**
 * Complete model descriptor from catalog
 */
data class ModelDescriptor(
    val id: String,
    val name: String,
    val description: String,
    val type: ModelType,
    val parameterCount: String,
    val quantization: String,
    val fileSize: Long,
    val contextSize: Int? = null,
    val vocabSize: Int? = null,
    val dimensions: Int? = null,
    val maxSequenceLength: Int? = null,
    val capabilities: List<String>,
    val license: String,
    val architecture: String,
    val downloadUrl: String,
    val sha256: String,
    val deviceRequirements: DeviceRequirements,
    val performance: PerformanceMetrics? = null
)

/**
 * Device requirements for a model
 */
data class DeviceRequirements(
    val minRAM: Long,
    val recommendedRAM: Long,
    val minAndroidVersion: Int,
    val supportedBackends: List<String>,
    val deviceClass: List<String>
)

/**
 * Performance metrics for a model
 */
data class PerformanceMetrics(
    val tokensPerSecond: Map<String, Map<String, Double>>,
    val powerConsumption: String,
    val thermalProfile: String
)

/**
 * Model type enumeration
 */
enum class ModelType {
    LLM,
    EMBEDDING,
    SAFETY
}
