package com.nervesparks.iris.core.multimodal.types

import android.net.Uri
import kotlinx.serialization.Serializable

/**
 * Multimodal model descriptor extending base LLM capabilities with vision features
 */
@Serializable
data class MultimodalModelDescriptor(
    val id: String,
    val name: String,
    val baseModel: String,
    val visionRequirements: VisionRequirements,
    val supportedImageFormats: List<ImageFormat>,
    val performance: ModelPerformance,
    val capabilities: List<MultimodalCapability>
)

/**
 * Vision-specific requirements for multimodal models
 */
@Serializable
data class VisionRequirements(
    val maxImageSize: ImageSize,
    val supportedFormats: List<ImageFormat>,
    val minConfidence: Float
)

/**
 * Image size specification
 */
@Serializable
data class ImageSize(
    val width: Int,
    val height: Int
)

/**
 * Model performance characteristics
 */
@Serializable
data class ModelPerformance(
    val inferenceTimeMs: Long,
    val memoryUsageMB: Int,
    val accuracy: Float
)

/**
 * Supported image formats
 */
@Serializable
enum class ImageFormat {
    JPEG, PNG, WEBP, BMP
}

/**
 * Multimodal capabilities
 */
@Serializable
enum class MultimodalCapability {
    VISUAL_QUESTION_ANSWERING,
    IMAGE_CLASSIFICATION,
    OBJECT_DETECTION,
    TEXT_RECOGNITION,
    SCENE_ANALYSIS,
    DOCUMENT_ANALYSIS
}

/**
 * Vision processing tasks
 */
enum class VisionTask {
    GENERAL_QA,
    OBJECT_DETECTION,
    TEXT_RECOGNITION,
    IMAGE_CLASSIFICATION,
    SCENE_ANALYSIS
}

/**
 * Processed image data ready for inference
 */
data class ProcessedImageData(
    val data: ByteArray,
    val format: ImageFormat,
    val width: Int,
    val height: Int,
    val channels: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProcessedImageData

        if (!data.contentEquals(other.data)) return false
        if (format != other.format) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (channels != other.channels) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + channels
        return result
    }
}

/**
 * Multimodal model compatibility assessment
 */
data class MultimodalModelCompatibilityAssessment(
    val isSupported: Boolean,
    val compatibilityScore: Double,
    val memoryRequirement: Long,
    val reasonsForIncompatibility: List<String>
)

/**
 * Exception for multimodal inference errors
 */
class MultimodalInferenceException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
