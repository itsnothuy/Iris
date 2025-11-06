package com.nervesparks.iris.core.hw

import android.content.Context
import com.nervesparks.iris.common.models.BackendType
import com.nervesparks.iris.common.models.ComputeTask
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BackendRouterImpl
 */
class BackendRouterImplTest {
    
    private lateinit var backendRouter: BackendRouterImpl
    private lateinit var deviceProfileProvider: DeviceProfileProvider
    private lateinit var thermalManager: ThermalManager
    
    @Before
    fun setup() {
        deviceProfileProvider = mockk()
        thermalManager = mockk()
        backendRouter = BackendRouterImpl(deviceProfileProvider, thermalManager)
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

