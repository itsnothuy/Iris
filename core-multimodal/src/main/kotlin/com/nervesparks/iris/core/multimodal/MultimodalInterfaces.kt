package com.nervesparks.iris.core.multimodal

import android.net.Uri
import com.nervesparks.iris.core.multimodal.types.*

/**
 * Registry for multimodal models with device compatibility assessment
 */
interface MultimodalModelRegistry {
    /**
     * Get recommended model for a specific vision task
     */
    suspend fun getRecommendedModel(visionTask: VisionTask): Result<MultimodalModelDescriptor>
    
    /**
     * Assess compatibility of a multimodal model with current device
     */
    suspend fun assessModelCompatibility(
        model: MultimodalModelDescriptor
    ): Result<MultimodalModelCompatibilityAssessment>
    
    /**
     * Get list of available multimodal models
     */
    suspend fun getAvailableModels(): Result<List<MultimodalModelDescriptor>>
    
    /**
     * Get model by ID
     */
    suspend fun getModelById(modelId: String): Result<MultimodalModelDescriptor>
}

/**
 * Image preprocessing and validation
 */
interface ImageProcessor {
    /**
     * Preprocess image for model inference
     * @param uri Image URI
     * @param targetSize Target size for resizing (maintaining aspect ratio)
     * @param format Target image format
     * @return Processed image data ready for inference
     */
    suspend fun preprocessImage(
        uri: Uri,
        targetSize: Int,
        format: ImageFormat
    ): Result<ProcessedImageData>
    
    /**
     * Validate if image is supported
     * @param uri Image URI
     * @return true if image is valid and supported
     */
    suspend fun validateImage(uri: Uri): Result<Boolean>
}

/**
 * Vision processing engine for multimodal inference
 */
interface VisionProcessingEngine {
    /**
     * Load a vision-language model
     */
    suspend fun loadVisionModel(model: MultimodalModelDescriptor): Result<Unit>
    
    /**
     * Check if a vision model is currently loaded
     */
    suspend fun isModelLoaded(): Boolean
    
    /**
     * Get currently loaded model
     */
    suspend fun getCurrentModel(): MultimodalModelDescriptor?
    
    /**
     * Unload the current vision model
     */
    suspend fun unloadVisionModel(): Result<Unit>
    
    /**
     * Process an image with a text prompt
     * Note: This is a simplified interface. Full streaming support would require
     * Flow<VisionResult> but keeping it simple for now
     */
    suspend fun processImageWithPrompt(
        imageUri: Uri,
        prompt: String
    ): Result<String>
}
