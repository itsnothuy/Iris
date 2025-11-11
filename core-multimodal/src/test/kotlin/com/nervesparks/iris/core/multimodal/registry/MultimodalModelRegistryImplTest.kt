package com.nervesparks.iris.core.multimodal.registry

import android.content.Context
import android.content.res.AssetManager
import com.nervesparks.iris.common.models.DeviceClass
import com.nervesparks.iris.common.models.DeviceProfile
import com.nervesparks.iris.common.models.GPUVendor
import com.nervesparks.iris.common.models.HardwareCapability
import com.nervesparks.iris.common.models.SoCVendor
import com.nervesparks.iris.common.models.ThermalCapability
import com.nervesparks.iris.core.hw.DeviceProfileProvider
import com.nervesparks.iris.core.multimodal.types.VisionTask
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

@OptIn(ExperimentalCoroutinesApi::class)
class MultimodalModelRegistryImplTest {
    
    private lateinit var context: Context
    private lateinit var deviceProfileProvider: DeviceProfileProvider
    private lateinit var registry: MultimodalModelRegistryImpl
    
    private val testCatalogJson = """
    {
      "version": "1.0.0",
      "lastUpdated": 1699747200000,
      "models": [
        {
          "id": "test-model-1",
          "name": "Test Model 1",
          "baseModel": "test-base",
          "visionRequirements": {
            "maxImageSize": {"width": 512, "height": 512},
            "supportedFormats": ["JPEG", "PNG"],
            "minConfidence": 0.6
          },
          "supportedImageFormats": ["JPEG", "PNG"],
          "performance": {
            "inferenceTimeMs": 800,
            "memoryUsageMB": 2048,
            "accuracy": 0.82
          },
          "capabilities": [
            "VISUAL_QUESTION_ANSWERING",
            "IMAGE_CLASSIFICATION"
          ]
        }
      ]
    }
    """.trimIndent()
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        deviceProfileProvider = mockk()
        
        val assetManager = mockk<AssetManager>()
        every { context.assets } returns assetManager
        every { assetManager.open(any()) } returns ByteArrayInputStream(testCatalogJson.toByteArray())
        
        // Setup default device profile
        coEvery { deviceProfileProvider.getDeviceProfile() } returns DeviceProfile(
            socVendor = SoCVendor.QUALCOMM,
            socModel = "Snapdragon 8 Gen 2",
            gpuVendor = GPUVendor.ADRENO,
            gpuModel = "Adreno 740",
            totalRAM = 8L * 1024 * 1024 * 1024, // 8GB
            availableRAM = 6L * 1024 * 1024 * 1024, // 6GB available
            androidVersion = 33,
            capabilities = setOf(HardwareCapability.OPENCL, HardwareCapability.VULKAN),
            deviceClass = DeviceClass.FLAGSHIP,
            thermalCapability = ThermalCapability.ADVANCED_MONITORING
        )
        
        registry = MultimodalModelRegistryImpl(
            deviceProfileProvider = deviceProfileProvider,
            context = context,
            ioDispatcher = Dispatchers.Unconfined
        )
    }
    
    @Test
    fun `getAvailableModels should load models from catalog`() = runTest {
        val result = registry.getAvailableModels()
        
        assertTrue(result.isSuccess)
        val models = result.getOrNull()
        assertNotNull(models)
        assertEquals(1, models?.size)
        assertEquals("test-model-1", models?.first()?.id)
    }
    
    @Test
    fun `getModelById should return correct model`() = runTest {
        val result = registry.getModelById("test-model-1")
        
        assertTrue(result.isSuccess)
        val model = result.getOrNull()
        assertNotNull(model)
        assertEquals("test-model-1", model?.id)
        assertEquals("Test Model 1", model?.name)
    }
    
    @Test
    fun `getModelById should fail for non-existent model`() = runTest {
        val result = registry.getModelById("non-existent")
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `assessModelCompatibility should return high score for compatible model`() = runTest {
        // First load the models
        val models = registry.getAvailableModels().getOrThrow()
        val model = models.first()
        
        val result = registry.assessModelCompatibility(model)
        
        assertTrue(result.isSuccess)
        val assessment = result.getOrNull()
        assertNotNull(assessment)
        assertTrue(assessment!!.isSupported)
        assertTrue(assessment.compatibilityScore > 0.7)
    }
    
    @Test
    fun `assessModelCompatibility should detect insufficient memory`() = runTest {
        // Setup device with low memory
        coEvery { deviceProfileProvider.getDeviceProfile() } returns DeviceProfile(
            socVendor = SoCVendor.QUALCOMM,
            socModel = "Snapdragon 8 Gen 2",
            gpuVendor = GPUVendor.ADRENO,
            gpuModel = "Adreno 740",
            totalRAM = 4L * 1024 * 1024 * 1024, // 4GB
            availableRAM = 1L * 1024 * 1024 * 1024, // 1GB available (too low)
            androidVersion = 33,
            capabilities = setOf(HardwareCapability.OPENCL),
            deviceClass = DeviceClass.MID_RANGE,
            thermalCapability = ThermalCapability.BASIC_MONITORING
        )
        
        val models = registry.getAvailableModels().getOrThrow()
        val model = models.first()
        
        val result = registry.assessModelCompatibility(model)
        
        assertTrue(result.isSuccess)
        val assessment = result.getOrNull()
        assertNotNull(assessment)
        assertFalse(assessment!!.isSupported)
        assertTrue(assessment.reasonsForIncompatibility.isNotEmpty())
    }
    
    @Test
    fun `getRecommendedModel should return model for VISUAL_QUESTION_ANSWERING task`() = runTest {
        val result = registry.getRecommendedModel(VisionTask.GENERAL_QA)
        
        assertTrue(result.isSuccess)
        val model = result.getOrNull()
        assertNotNull(model)
        assertEquals("test-model-1", model?.id)
    }
    
    @Test
    fun `getRecommendedModel should fail for unsupported task`() = runTest {
        val result = registry.getRecommendedModel(VisionTask.OBJECT_DETECTION)
        
        assertTrue(result.isFailure)
    }
}
