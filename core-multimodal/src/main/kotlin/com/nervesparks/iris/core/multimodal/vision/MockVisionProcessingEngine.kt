package com.nervesparks.iris.core.multimodal.vision

import android.content.Context
import android.net.Uri
import com.nervesparks.iris.core.multimodal.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Mock implementation of VisionProcessingEngine for initial module compilation
 */
class MockVisionProcessingEngine(
    private val context: Context
) : VisionProcessingEngine {

    override suspend fun analyzeImage(
        imageUri: Uri,
        prompt: String,
        model: MultimodalModelDescriptor,
        parameters: VisionParameters
    ): Result<VisionResult.AnalysisResult> = withContext(Dispatchers.Default) {
        Result.success(
            VisionResult.AnalysisResult(
                text = "Mock analysis: This image contains objects relevant to the prompt '$prompt'",
                confidence = 0.85f,
                processingTimeMs = 500L
            )
        )
    }

    override suspend fun processScreenshot(
        screenshotData: ByteArray,
        prompt: String,
        model: MultimodalModelDescriptor,
        parameters: VisionParameters
    ): Result<VisionResult.ScreenshotResult> = withContext(Dispatchers.Default) {
        Result.success(
            VisionResult.ScreenshotResult(
                text = "Mock screenshot analysis: UI elements detected",
                confidence = 0.75f,
                processingTimeMs = 300L,
                uiElements = listOf("Button", "Text Field", "Image"),
                textRegions = listOf("Welcome", "Submit", "Cancel")
            )
        )
    }

    override suspend fun extractTextFromImage(
        imageUri: Uri,
        model: MultimodalModelDescriptor,
        parameters: VisionParameters
    ): Result<VisionResult.OCRResult> = withContext(Dispatchers.Default) {
        Result.success(
            VisionResult.OCRResult(
                extractedText = "Mock extracted text from image",
                confidence = 0.90f,
                processingTimeMs = 200L,
                textRegions = listOf("Line 1", "Line 2")
            )
        )
    }

    override suspend fun analyzeDocument(
        imageUri: Uri,
        documentType: DocumentType,
        prompt: String,
        model: MultimodalModelDescriptor,
        parameters: VisionParameters
    ): Result<VisionResult.DocumentResult> = withContext(Dispatchers.Default) {
        Result.success(
            VisionResult.DocumentResult(
                extractedText = "Mock document analysis",
                confidence = 0.80f,
                processingTimeMs = 400L,
                documentType = documentType,
                structuredData = mapOf("title" to "Mock Document", "pages" to 1)
            )
        )
    }

    override fun streamVisionAnalysis(
        imageUri: Uri,
        prompt: String,
        model: MultimodalModelDescriptor,
        parameters: VisionParameters
    ): Flow<VisionResult.StreamResult> = flow {
        emit(VisionResult.StreamResult.Started)
        emit(VisionResult.StreamResult.TextChunk("Processing", false))
        emit(VisionResult.StreamResult.TextChunk("image", false))
        emit(VisionResult.StreamResult.TextChunk("analysis", true))
        emit(VisionResult.StreamResult.Completed(
            VisionResult.AnalysisResult(
                text = "Mock streaming analysis complete",
                confidence = 0.85f,
                processingTimeMs = 600L
            )
        ))
    }
}