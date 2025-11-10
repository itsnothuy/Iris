package com.nervesparks.iris.core.models.registry

import android.content.Context
import com.nervesparks.iris.common.models.DeviceClass
import com.nervesparks.iris.common.models.DeviceProfile
import com.nervesparks.iris.common.models.GPUVendor
import com.nervesparks.iris.common.models.HardwareCapability
import com.nervesparks.iris.common.models.SoCVendor
import com.nervesparks.iris.common.models.ThermalCapability
import com.nervesparks.iris.core.hw.DeviceProfileProvider
import com.nervesparks.iris.core.models.ModelType
import io.mockk.every
import io.mockk.mockk
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

/**
 * Unit tests for ModelRegistryImpl
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [28],
    manifest = Config.NONE,
    application = android.app.Application::class,
    assetDir = "../core-models/src/main/assets"
)
class ModelRegistryImplTest {
    
    private lateinit var context: Context
    private lateinit var deviceProfileProvider: DeviceProfileProvider
    private lateinit var modelRegistry: ModelRegistryImpl
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        deviceProfileProvider = mockk()
        
        // Setup default device profile (flagship device)
        val defaultProfile = DeviceProfile(
            socVendor = SoCVendor.QUALCOMM,
            socModel = "Snapdragon 8 Gen 2",
            gpuVendor = GPUVendor.ADRENO,
            gpuModel = "Adreno 740",
            totalRAM = 12L * 1024 * 1024 * 1024, // 12GB
            availableRAM = 8L * 1024 * 1024 * 1024, // 8GB
            androidVersion = 33,
            capabilities = setOf(
                HardwareCapability.OPENCL,
                HardwareCapability.VULKAN,
                HardwareCapability.QNN
            ),
            deviceClass = DeviceClass.FLAGSHIP,
            thermalCapability = ThermalCapability.ADVANCED_MONITORING
        )
        
        every { deviceProfileProvider.getDeviceProfile() } returns defaultProfile
        
        modelRegistry = ModelRegistryImpl(context, deviceProfileProvider)
    }
    
    @Test
    fun `getAvailableModels returns all models when type is null`() = runTest {
        val models = modelRegistry.getAvailableModels(null)
        
        assertTrue("Should have models", models.isNotEmpty())
        assertTrue("Should have LLM models", models.any { it.type == ModelType.LLM })
        assertTrue("Should have embedding models", models.any { it.type == ModelType.EMBEDDING })
        assertTrue("Should have safety models", models.any { it.type == ModelType.SAFETY })
    }
    
    @Test
    fun `getAvailableModels returns only LLM models when type is LLM`() = runTest {
        val models = modelRegistry.getAvailableModels(ModelType.LLM)
        
        assertTrue("Should have LLM models", models.isNotEmpty())
        assertTrue("All models should be LLM type", models.all { it.type == ModelType.LLM })
    }
    
    @Test
    fun `getModelById returns correct model`() = runTest {
        val modelId = "tinyllama-1.1b-q4_0"
        val model = modelRegistry.getModelById(modelId)
        
        assertNotNull("Model should be found", model)
        assertEquals("Model ID should match", modelId, model?.id)
        assertEquals("Model type should be LLM", ModelType.LLM, model?.type)
    }
    
    @Test
    fun `getModelById returns null for non-existent model`() = runTest {
        val model = modelRegistry.getModelById("non-existent-model")
        
        assertEquals("Model should not be found", null, model)
    }
    
    @Test
    fun `getRecommendedModels returns sorted recommendations for flagship device`() = runTest {
        val recommendations = modelRegistry.getRecommendedModels()
        
        assertTrue("Should have recommendations", recommendations.isNotEmpty())
        
        // Check that recommendations are sorted by score (descending)
        val scores = recommendations.map { it.compatibilityScore }
        assertEquals("Scores should be sorted descending", 
            scores.sortedDescending(), scores)
    }
    
    @Test
    fun `validateModel passes for compatible model on flagship device`() = runTest {
        val model = modelRegistry.getModelById("tinyllama-1.1b-q4_0")
        assertNotNull("Model should exist", model)
        
        val result = modelRegistry.validateModel(model!!)
        
        assertTrue("Validation should pass", result.isValid)
        assertTrue("Should have no issues", result.issues.isEmpty())
    }
    
    @Test
    fun `validateModel fails for model requiring more RAM than available`() = runTest {
        // Setup device with low RAM
        val lowRamProfile = DeviceProfile(
            socVendor = SoCVendor.QUALCOMM,
            socModel = "Snapdragon 665",
            gpuVendor = GPUVendor.ADRENO,
            gpuModel = "Adreno 610",
            totalRAM = 1L * 1024 * 1024 * 1024, // 1GB - too low
            availableRAM = 512L * 1024 * 1024, // 512MB
            androidVersion = 29,
            capabilities = setOf(HardwareCapability.OPENCL),
            deviceClass = DeviceClass.BUDGET,
            thermalCapability = ThermalCapability.BASIC_MONITORING
        )
        
        every { deviceProfileProvider.getDeviceProfile() } returns lowRamProfile
        
        val model = modelRegistry.getModelById("phi-3-mini-4k-q4_k_m")
        assertNotNull("Model should exist", model)
        
        val result = modelRegistry.validateModel(model!!)
        
        assertFalse("Validation should fail", result.isValid)
        assertTrue("Should have device incompatible issue", 
            result.issues.contains(ValidationIssue.DEVICE_INCOMPATIBLE))
    }
    
    @Test
    fun `refreshCatalog succeeds`() = runTest {
        val result = modelRegistry.refreshCatalog()
        
        assertTrue("Refresh should succeed", result.isSuccess)
    }
}
