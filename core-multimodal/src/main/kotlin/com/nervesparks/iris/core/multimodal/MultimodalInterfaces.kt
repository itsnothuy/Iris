package com.nervesparks.iris.core.multimodal

import android.net.Uri
import com.nervesparks.iris.core.multimodal.types.*
import kotlinx.coroutines.flow.Flow

/**
 * Registry for multimodal AI models supporting vision-language capabilities
 */
interface MultimodalModelRegistry {
    /**
     * Get recommended model for a specific vision task
     */
    suspend fun getRecommendedModel(visionTask: VisionTask): Result<MultimodalModelDescriptor>
    
    /**
     * Assess compatibility of a model with current device
     */
    suspend fun assessModelCompatibility(model: MultimodalModelDescriptor): Result<MultimodalModelCompatibilityAssessment>
    
    /**
     * Get all available models
     */
    suspend fun getAvailableModels(): Result<List<MultimodalModelDescriptor>>
    
    /**
     * Get model by ID
     */
    suspend fun getModelById(modelId: String): Result<MultimodalModelDescriptor>
}

/**
 * Vision processing engine for image understanding and analysis
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
     * Analyze an image with a text prompt
     */
    suspend fun analyzeImage(
        imageUri: Uri,
        prompt: String,
        model: MultimodalModelDescriptor,
        parameters: VisionParameters
    ): Result<VisionResult.AnalysisResult>
    
    /**
     * Process an image with a text prompt (simplified interface)
     */
    suspend fun processImageWithPrompt(
        imageUri: Uri,
        prompt: String
    ): Result<String>
    
    /**
     * Process screenshot data
     */
    suspend fun processScreenshot(
        screenshotData: ByteArray,
        prompt: String,
        model: MultimodalModelDescriptor,
        parameters: VisionParameters
    ): Result<VisionResult.ScreenshotResult>
    
    /**
     * Extract text from image using OCR
     */
    suspend fun extractTextFromImage(
        imageUri: Uri,
        model: MultimodalModelDescriptor,
        parameters: VisionParameters
    ): Result<VisionResult.OCRResult>
    
    /**
     * Analyze document with specific type handling
     */
    suspend fun analyzeDocument(
        imageUri: Uri,
        documentType: DocumentType,
        prompt: String,
        model: MultimodalModelDescriptor,
        parameters: VisionParameters
    ): Result<VisionResult.DocumentResult>
    
    /**
     * Stream vision analysis results
     */
    fun streamVisionAnalysis(
        imageUri: Uri,
        prompt: String,
        model: MultimodalModelDescriptor,
        parameters: VisionParameters
    ): Flow<VisionResult.StreamResult>
}

/**
 * Image processor for preparing images for vision models
 */
interface ImageProcessor {
    
    /**
     * Preprocess image for vision models
     */
    suspend fun preprocessImage(
        uri: Uri,
        targetSize: Int,
        format: ImageFormat
    ): Result<ProcessedImageData>
    
    /**
     * Validate image requirements
     */
    suspend fun validateImage(
        uri: Uri
    ): Result<Boolean>
}
