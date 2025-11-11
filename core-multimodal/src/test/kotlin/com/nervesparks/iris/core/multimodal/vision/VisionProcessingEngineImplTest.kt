package com.nervesparks.iris.core.multimodal.vision

import android.content.Context
import android.net.Uri
import com.nervesparks.iris.core.multimodal.ImageProcessor
import com.nervesparks.iris.core.multimodal.types.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class VisionProcessingEngineImplTest {
    
    private lateinit var context: Context
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var visionEngine: VisionProcessingEngineImpl
    
    private val testModel = MultimodalModelDescriptor(
        id = "test-model",
        name = "Test Vision Model",
        baseModel = "test-base",
        visionRequirements = VisionRequirements(
            maxImageSize = ImageSize(512, 512),
            supportedFormats = listOf(ImageFormat.JPEG, ImageFormat.PNG),
            minConfidence = 0.6f
        ),
        supportedImageFormats = listOf(ImageFormat.JPEG, ImageFormat.PNG),
        performance = ModelPerformance(
            inferenceTimeMs = 800,
            memoryUsageMB = 2048,
            accuracy = 0.82f
        ),
        capabilities = listOf(
            MultimodalCapability.VISUAL_QUESTION_ANSWERING,
            MultimodalCapability.IMAGE_CLASSIFICATION
        )
    )
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        imageProcessor = mockk(relaxed = true)
        
        // Mock external files directory
        val externalDir = mockk<File>()
        val modelsDir = mockk<File>()
        val modelFile = mockk<File>()
        
        every { context.getExternalFilesDir(null) } returns externalDir
        every { externalDir.absolutePath } returns "/data/app/files"
        mockkConstructor(File::class)
        every { anyConstructed<File>().exists() } returns true
        
        visionEngine = VisionProcessingEngineImpl(
            imageProcessor = imageProcessor,
            context = context,
            ioDispatcher = Dispatchers.Unconfined
        )
    }
    
    @Test
    fun `isModelLoaded should return false initially`() = runTest {
        assertFalse(visionEngine.isModelLoaded())
    }
    
    @Test
    fun `getCurrentModel should return null initially`() = runTest {
        assertNull(visionEngine.getCurrentModel())
    }
    
    @Test
    fun `loadVisionModel should successfully load model`() = runTest {
        val result = visionEngine.loadVisionModel(testModel)
        
        assertTrue(result.isSuccess)
        assertTrue(visionEngine.isModelLoaded())
        assertEquals(testModel, visionEngine.getCurrentModel())
    }
    
    @Test
    fun `loadVisionModel should handle already loaded model`() = runTest {
        // Load first time
        visionEngine.loadVisionModel(testModel)
        
        // Load again
        val result = visionEngine.loadVisionModel(testModel)
        
        assertTrue(result.isSuccess)
        assertTrue(visionEngine.isModelLoaded())
    }
    
    @Test
    fun `unloadVisionModel should successfully unload model`() = runTest {
        // Load model first
        visionEngine.loadVisionModel(testModel)
        assertTrue(visionEngine.isModelLoaded())
        
        // Unload
        val result = visionEngine.unloadVisionModel()
        
        assertTrue(result.isSuccess)
        assertFalse(visionEngine.isModelLoaded())
        assertNull(visionEngine.getCurrentModel())
    }
    
    @Test
    fun `processImageWithPrompt should fail when no model is loaded`() = runTest {
        val uri = mockk<Uri>()
        
        val result = visionEngine.processImageWithPrompt(uri, "Test prompt")
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `processImageWithPrompt should validate image before processing`() = runTest {
        // Load model
        visionEngine.loadVisionModel(testModel)
        
        val uri = mockk<Uri>()
        coEvery { imageProcessor.validateImage(uri) } returns Result.success(false)
        
        val result = visionEngine.processImageWithPrompt(uri, "Test prompt")
        
        assertTrue(result.isFailure)
        coVerify { imageProcessor.validateImage(uri) }
    }
    
    @Test
    fun `processImageWithPrompt should preprocess valid image`() = runTest {
        // Load model
        visionEngine.loadVisionModel(testModel)
        
        val uri = mockk<Uri>()
        val processedData = ProcessedImageData(
            data = ByteArray(100),
            format = ImageFormat.JPEG,
            width = 512,
            height = 512,
            channels = 3
        )
        
        coEvery { imageProcessor.validateImage(uri) } returns Result.success(true)
        coEvery { 
            imageProcessor.preprocessImage(uri, 512, ImageFormat.JPEG) 
        } returns Result.success(processedData)
        
        val result = visionEngine.processImageWithPrompt(uri, "Test prompt")
        
        assertTrue(result.isSuccess)
        coVerify { imageProcessor.validateImage(uri) }
        coVerify { imageProcessor.preprocessImage(uri, 512, ImageFormat.JPEG) }
    }
    
    @Test
    fun `processImageWithPrompt should handle preprocessing failure`() = runTest {
        // Load model
        visionEngine.loadVisionModel(testModel)
        
        val uri = mockk<Uri>()
        coEvery { imageProcessor.validateImage(uri) } returns Result.success(true)
        coEvery { 
            imageProcessor.preprocessImage(any(), any(), any()) 
        } returns Result.failure(Exception("Preprocessing failed"))
        
        val result = visionEngine.processImageWithPrompt(uri, "Test prompt")
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `loadVisionModel should fail when model file does not exist`() = runTest {
        every { anyConstructed<File>().exists() } returns false
        
        val result = visionEngine.loadVisionModel(testModel)
        
        assertTrue(result.isFailure)
    }
}
