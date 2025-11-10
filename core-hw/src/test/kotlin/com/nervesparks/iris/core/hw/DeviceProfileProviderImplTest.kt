package com.nervesparks.iris.core.hw

import com.nervesparks.iris.common.config.ThermalState
import com.nervesparks.iris.common.models.BackendType
import com.nervesparks.iris.common.models.ComputeTask
import com.nervesparks.iris.common.models.DeviceClass
import com.nervesparks.iris.common.models.DeviceProfile
import com.nervesparks.iris.common.models.GPUVendor
import com.nervesparks.iris.common.models.HardwareCapability
import com.nervesparks.iris.common.models.SoCVendor
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for BackendRouterImpl
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackendRouterImplTest {
    
    private lateinit var backendRouter: BackendRouterImpl
    private lateinit var deviceProfileProvider: DeviceProfileProvider
    private lateinit var thermalManager: ThermalManager
    private lateinit var preferences: BackendPreferences
    
    private val mockDeviceProfile = DeviceProfile(
        socVendor = SoCVendor.QUALCOMM,
        socModel = "Snapdragon 8 Gen 2",
        gpuVendor = GPUVendor.ADRENO,
        gpuModel = "Adreno 740",
        totalRAM = 8L * 1024 * 1024 * 1024,
        availableRAM = 4L * 1024 * 1024 * 1024,
        androidVersion = 33,
        capabilities = setOf(HardwareCapability.OPENCL, HardwareCapability.VULKAN),
        deviceClass = DeviceClass.FLAGSHIP
    )
    
    @Before
    fun setup() {
        deviceProfileProvider = mockk()
        thermalManager = mockk()
        preferences = mockk(relaxed = true)
        
        // Setup default mock responses
        every { deviceProfileProvider.getDeviceProfile() } returns mockDeviceProfile
        every { thermalManager.thermalState } returns MutableStateFlow(ThermalState.NORMAL)
        every { preferences.getCachedBackend(any(), any()) } returns null
        every { preferences.getCachedBenchmarkResults() } returns null
        every { preferences.isGPUAllowedInSevereThermal() } returns false
        
        backendRouter = BackendRouterImpl(deviceProfileProvider, thermalManager, preferences)
    }
    
    @Test
    fun `getCurrentBackend returns CPU_NEON by default`() {
        assertEquals(BackendType.CPU_NEON, backendRouter.getCurrentBackend())
    }
    
    @Test
    fun `selectOptimalBackend returns valid backend`() = runTest {
        val backend = backendRouter.selectOptimalBackend(ComputeTask.LLM_INFERENCE)
        
        assertNotNull(backend)
    }
    
    @Test
    fun `validateBackend returns true for CPU_NEON`() = runTest {
        val result = backendRouter.validateBackend(BackendType.CPU_NEON)
        
        assertTrue(result)
    }
}
