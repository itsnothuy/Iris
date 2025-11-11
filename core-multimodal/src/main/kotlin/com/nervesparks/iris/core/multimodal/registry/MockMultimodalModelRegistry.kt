package com.nervesparks.iris.core.multimodal.registry

import android.content.Context
import com.nervesparks.iris.core.multimodal.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mock implementation of MultimodalModelRegistry for initial module compilation
 */
class MockMultimodalModelRegistry(
    private val context: Context
) : MultimodalModelRegistry {

    private val mockModel = MultimodalModelDescriptor(
        id = "mock-vision-v1",
        name = "Mock Vision Model",
        baseModel = "mock-base",
        visionRequirements = VisionRequirements(
            maxImageSize = ImageSize(width = 512, height = 512),
            supportedFormats = listOf(ImageFormat.JPEG, ImageFormat.PNG)
        ),
        supportedImageFormats = listOf(ImageFormat.JPEG, ImageFormat.PNG),
        performance = ModelPerformance(
            inferenceTimeMs = 500L,
            memoryUsageMB = 200,
            accuracy = 0.85f
        ),
        capabilities = listOf(MultimodalCapability.IMAGE_CLASSIFICATION, MultimodalCapability.VISUAL_QUESTION_ANSWERING)
    )

    override suspend fun getRecommendedModel(visionTask: VisionTask): Result<MultimodalModelDescriptor> = withContext(Dispatchers.Default) {
        Result.success(mockModel)
    }

    override suspend fun assessModelCompatibility(model: MultimodalModelDescriptor): Result<MultimodalModelCompatibilityAssessment> = withContext(Dispatchers.Default) {
        Result.success(
            MultimodalModelCompatibilityAssessment(
                isSupported = true,
                compatibilityScore = 0.8,
                memoryRequirement = 200 * 1024 * 1024L,
                reasonsForIncompatibility = emptyList()
            )
        )
    }

    override suspend fun getAvailableModels(): Result<List<MultimodalModelDescriptor>> = withContext(Dispatchers.Default) {
        Result.success(listOf(mockModel))
    }

    override suspend fun getModelById(modelId: String): Result<MultimodalModelDescriptor> = withContext(Dispatchers.Default) {
        if (modelId == mockModel.id) {
            Result.success(mockModel)
        } else {
            Result.failure(IllegalArgumentException("Model not found: $modelId"))
        }
    }
}