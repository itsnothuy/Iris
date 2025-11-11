package com.nervesparks.iris.core.multimodal

/**
 * Vision processing tasks supported by multimodal models
 */
enum class VisionTask {
    OBJECT_DETECTION,
    TEXT_RECOGNITION, 
    IMAGE_CLASSIFICATION,
    SCENE_ANALYSIS,
    GENERAL_QA
}

/**
 * Model backend types for AI inference
 */
enum class ModelBackend {
    ONNX, TFLITE, PYTORCH, TENSORFLOW
}

/**
 * Supported image formats for processing
 */
enum class ImageFormat {
    JPEG, PNG, WEBP, BMP
}

/**
 * Image size specification
 */
data class ImageSize(
    val width: Int,
    val height: Int
)

/**
 * Processed image data ready for model input
 */
data class ProcessedImageData(
    val data: ByteArray,
    val format: ImageFormat,
    val width: Int,
    val height: Int,
    val channels: Int
)

/**
 * Parameters for vision processing
 */
data class VisionParameters(
    val maxTokens: Int = 1024,
    val temperature: Float = 0.7f,
    val confidence: Float = 0.5f
)

/**
 * Document types for specialized processing
 */
enum class DocumentType {
    PDF, IMAGE, TEXT, RECEIPT, INVOICE, ID_CARD, FORM
}

/**
 * Vision prompt with image data
 */
data class VisionPrompt(
    val text: String,
    val imageData: ProcessedImageData,
    val systemPrompt: String = ""
)

/**
 * Multimodal model descriptor with capabilities and requirements
 */
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
 * Vision processing requirements for a model
 */
data class VisionRequirements(
    val maxImageSize: ImageSize,
    val supportedFormats: List<ImageFormat>,
    val minConfidence: Float = 0.5f
)

/**
 * Model performance characteristics
 */
data class ModelPerformance(
    val inferenceTimeMs: Long,
    val memoryUsageMB: Int,
    val accuracy: Float
)

/**
 * Capabilities that a multimodal model supports
 */
enum class MultimodalCapability {
    IMAGE_CLASSIFICATION,
    OBJECT_DETECTION,
    TEXT_RECOGNITION,
    SCENE_ANALYSIS,
    DOCUMENT_ANALYSIS,
    VISUAL_QUESTION_ANSWERING
}

/**
 * Sealed class representing different types of vision processing results
 */
sealed class VisionResult {
    
    /**
     * General image analysis result
     */
    data class AnalysisResult(
        val text: String,
        val confidence: Float,
        val processingTimeMs: Long
    ) : VisionResult()
    
    /**
     * Screenshot processing result with UI element detection
     */
    data class ScreenshotResult(
        val text: String,
        val confidence: Float,
        val processingTimeMs: Long,
        val uiElements: List<String>,
        val textRegions: List<String>
    ) : VisionResult()
    
    /**
     * OCR text extraction result
     */
    data class OCRResult(
        val extractedText: String,
        val confidence: Float,
        val processingTimeMs: Long,
        val textRegions: List<String>
    ) : VisionResult()
    
    /**
     * Document analysis result with structured data
     */
    data class DocumentResult(
        val extractedText: String,
        val confidence: Float,
        val processingTimeMs: Long,
        val documentType: DocumentType,
        val structuredData: Map<String, Any>
    ) : VisionResult()
    
    /**
     * Streaming vision analysis results
     */
    sealed class StreamResult : VisionResult() {
        object Started : StreamResult()
        
        data class TextChunk(
            val text: String,
            val isComplete: Boolean
        ) : StreamResult()
        
        data class Completed(
            val finalResult: AnalysisResult
        ) : StreamResult()
        
        data class Error(
            val exception: Exception
        ) : StreamResult()
    }
}

/**
 * Model compatibility assessment for device recommendations
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
class MultimodalInferenceException(message: String, cause: Throwable? = null) : Exception(message, cause)