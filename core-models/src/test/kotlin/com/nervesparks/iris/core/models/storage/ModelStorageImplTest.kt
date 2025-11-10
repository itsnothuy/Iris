package com.nervesparks.iris.core.models.storage

import android.content.Context
import com.nervesparks.iris.core.models.DeviceRequirements
import com.nervesparks.iris.core.models.ModelDescriptor
import com.nervesparks.iris.core.models.ModelType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for ModelStorageImpl
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ModelStorageImplTest {
    
    private lateinit var context: Context
    private lateinit var modelStorage: ModelStorageImpl
    private lateinit var testModelsDir: File
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        modelStorage = ModelStorageImpl(context)
        testModelsDir = File(modelStorage.getModelsDirectory())
        
        // Clean up test directory
        testModelsDir.listFiles()?.forEach { it.delete() }
    }
    
    @Test
    fun `getModelsDirectory creates directory if not exists`() {
        val dir = modelStorage.getModelsDirectory()
        
        assertNotNull("Directory path should not be null", dir)
        assertTrue("Directory should exist", File(dir).exists())
        assertTrue("Should be a directory", File(dir).isDirectory)
    }
    
    @Test
    fun `isModelStored returns false for non-existent model`() = runTest {
        val isStored = modelStorage.isModelStored("non-existent-model")
        
        assertFalse("Model should not be stored", isStored)
    }
    
    @Test
    fun `getModelPath returns null for non-existent model`() = runTest {
        val path = modelStorage.getModelPath("non-existent-model")
        
        assertEquals("Path should be null", null, path)
    }
    
    @Test
    fun `saveModelMetadata creates metadata file`() = runTest {
        val testModel = createTestModelDescriptor("test-model-1")
        val testPath = File(testModelsDir, "test-model-1.gguf").absolutePath
        
        val result = modelStorage.saveModelMetadata(testModel, testPath)
        
        assertTrue("Save should succeed", result.isSuccess)
        
        val metadataFile = File(testModelsDir, "test-model-1.metadata.json")
        assertTrue("Metadata file should exist", metadataFile.exists())
    }
    
    @Test
    fun `getModelMetadata returns saved metadata`() = runTest {
        val testModel = createTestModelDescriptor("test-model-2")
        val testPath = File(testModelsDir, "test-model-2.gguf").absolutePath
        
        modelStorage.saveModelMetadata(testModel, testPath)
        
        val retrievedModel = modelStorage.getModelMetadata("test-model-2")
        
        assertNotNull("Model metadata should be retrieved", retrievedModel)
        assertEquals("Model ID should match", testModel.id, retrievedModel?.id)
        assertEquals("Model name should match", testModel.name, retrievedModel?.name)
        assertEquals("Model type should match", testModel.type, retrievedModel?.type)
    }
    
    @Test
    fun `getModelMetadata returns null for non-existent model`() = runTest {
        val model = modelStorage.getModelMetadata("non-existent")
        
        assertEquals("Model should not be found", null, model)
    }
    
    @Test
    fun `deleteModel removes model and metadata files`() = runTest {
        val testModel = createTestModelDescriptor("test-model-3")
        val modelFile = File(testModelsDir, "test-model-3.gguf")
        val testPath = modelFile.absolutePath
        
        // Create dummy model file
        modelFile.writeText("test content")
        modelStorage.saveModelMetadata(testModel, testPath)
        
        assertTrue("Model file should exist", modelFile.exists())
        
        val result = modelStorage.deleteModel("test-model-3")
        
        assertTrue("Delete should succeed", result.isSuccess)
        assertFalse("Model file should be deleted", modelFile.exists())
        
        val metadataFile = File(testModelsDir, "test-model-3.metadata.json")
        assertFalse("Metadata file should be deleted", metadataFile.exists())
    }
    
    @Test
    fun `getStoredModels returns all stored models`() = runTest {
        // Create multiple test models
        val model1 = createTestModelDescriptor("test-model-4")
        val model2 = createTestModelDescriptor("test-model-5")
        
        modelStorage.saveModelMetadata(model1, "path1")
        modelStorage.saveModelMetadata(model2, "path2")
        
        val storedModels = modelStorage.getStoredModels()
        
        assertEquals("Should have 2 stored models", 2, storedModels.size)
        assertTrue("Should contain model1", storedModels.any { it.id == "test-model-4" })
        assertTrue("Should contain model2", storedModels.any { it.id == "test-model-5" })
    }
    
    @Test
    fun `getAvailableSpace returns non-negative value`() = runTest {
        val space = modelStorage.getAvailableSpace()
        
        assertTrue("Available space should be non-negative", space >= 0)
    }
    
    private fun createTestModelDescriptor(id: String): ModelDescriptor {
        return ModelDescriptor(
            id = id,
            name = "Test Model $id",
            description = "Test description",
            type = ModelType.LLM,
            parameterCount = "1B",
            quantization = "Q4_0",
            fileSize = 1024L * 1024 * 100, // 100MB
            contextSize = 2048,
            vocabSize = 32000,
            capabilities = listOf("test"),
            license = "test",
            architecture = "test",
            downloadUrl = "https://example.com/model.gguf",
            sha256 = "abc123",
            deviceRequirements = DeviceRequirements(
                minRAM = 1024L * 1024 * 1024 * 2,
                recommendedRAM = 1024L * 1024 * 1024 * 4,
                minAndroidVersion = 28,
                supportedBackends = listOf("CPU_NEON"),
                deviceClass = listOf("BUDGET", "MID_RANGE")
            )
        )
    }
}
