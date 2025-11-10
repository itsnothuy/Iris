package com.nervesparks.iris.core.llm.inference

import android.content.Context
import com.nervesparks.iris.common.config.ThermalState
import com.nervesparks.iris.common.models.BackendType
import com.nervesparks.iris.common.models.DeviceClass
import com.nervesparks.iris.common.models.DeviceProfile
import com.nervesparks.iris.common.models.GPUVendor
import com.nervesparks.iris.common.models.ModelHandle
import com.nervesparks.iris.common.models.SoCVendor
import com.nervesparks.iris.core.hw.DeviceProfileProvider
import com.nervesparks.iris.core.hw.ThermalManager
import com.nervesparks.iris.core.llm.LLMEngine
import com.nervesparks.iris.core.safety.SafetyEngine
import com.nervesparks.iris.core.safety.SafetyResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for InferenceSessionImpl
 */
class InferenceSessionImplTest {
    
    private lateinit var inferenceSession: InferenceSessionImpl
    private lateinit var llmEngine: LLMEngine
    private lateinit var deviceProfileProvider: DeviceProfileProvider
    private lateinit var thermalManager: ThermalManager
    private lateinit var safetyEngine: SafetyEngine
    private lateinit var context: Context
    
    private val testModelDescriptor = ModelDescriptor(
        id = "test-model-1",
        path = "/test/model.gguf",
        name = "Test Model",
        deviceRequirements = DeviceRequirements()
    )
    
    private val testModelHandle = ModelHandle(
        id = "test-model-1",
        modelPath = "/test/model.gguf",
        contextSize = 2048,
        vocabSize = 32000,
        backend = BackendType.CPU_NEON
    )
    
    private val testDeviceProfile = DeviceProfile(
        socVendor = SoCVendor.QUALCOMM,
        socModel = "Test SoC",
        gpuVendor = GPUVendor.ADRENO,
        gpuModel = "Test GPU",
        totalRAM = 8L * 1024 * 1024 * 1024,
        availableRAM = 4L * 1024 * 1024 * 1024,
        androidVersion = 33,
        capabilities = emptySet(),
        deviceClass = DeviceClass.MID_RANGE
    )
    
    @Before
    fun setup() {
        llmEngine = mockk()
        deviceProfileProvider = mockk()
        thermalManager = mockk()
        safetyEngine = mockk()
        context = mockk(relaxed = true)
        
        every { deviceProfileProvider.getDeviceProfile() } returns testDeviceProfile
        every { thermalManager.thermalState } returns MutableStateFlow(ThermalState.NORMAL)
        coEvery { safetyEngine.checkInput(any()) } returns SafetyResult(isAllowed = true)
        coEvery { safetyEngine.checkOutput(any()) } returns SafetyResult(isAllowed = true)
        
        inferenceSession = InferenceSessionImpl(
            llmEngine = llmEngine,
            deviceProfileProvider = deviceProfileProvider,
            thermalManager = thermalManager,
            safetyEngine = safetyEngine,
            context = context
        )
    }
    
    @Test
    fun `loadModel successfully loads model`() = runTest {
        coEvery { llmEngine.loadModel(any()) } returns Result.success(testModelHandle)
        
        val result = inferenceSession.loadModel(
            testModelDescriptor,
            InferenceParameters()
        )
        
        assertTrue(result.isSuccess)
        val loadResult = result.getOrNull()
        assertNotNull(loadResult)
        assertEquals("test-model-1", loadResult?.modelId)
        assertEquals(BackendType.CPU_NEON, loadResult?.backend)
    }
    
    @Test
    fun `loadModel returns failure when LLM engine fails`() = runTest {
        coEvery { llmEngine.loadModel(any()) } returns Result.failure(Exception("Load failed"))
        
        val result = inferenceSession.loadModel(
            testModelDescriptor,
            InferenceParameters()
        )
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `createSession returns success when model is loaded`() = runTest {
        coEvery { llmEngine.loadModel(any()) } returns Result.success(testModelHandle)
        
        inferenceSession.loadModel(testModelDescriptor, InferenceParameters())
        
        val result = inferenceSession.createSession("test-conversation-1")
        
        assertTrue(result.isSuccess)
        val sessionContext = result.getOrNull()
        assertNotNull(sessionContext)
        assertEquals("test-conversation-1", sessionContext?.sessionId)
        assertEquals("test-model-1", sessionContext?.modelId)
        assertTrue(sessionContext?.isActive ?: false)
    }
    
    @Test
    fun `createSession returns failure when no model is loaded`() = runTest {
        val result = inferenceSession.createSession("test-conversation-1")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InferenceException)
    }
    
    @Test
    fun `getActiveSessionCount returns correct count`() = runTest {
        coEvery { llmEngine.loadModel(any()) } returns Result.success(testModelHandle)
        
        inferenceSession.loadModel(testModelDescriptor, InferenceParameters())
        
        assertEquals(0, inferenceSession.getActiveSessionCount())
        
        inferenceSession.createSession("session-1")
        assertEquals(1, inferenceSession.getActiveSessionCount())
        
        inferenceSession.createSession("session-2")
        assertEquals(2, inferenceSession.getActiveSessionCount())
    }
    
    @Test
    fun `closeSession closes specific session`() = runTest {
        coEvery { llmEngine.loadModel(any()) } returns Result.success(testModelHandle)
        
        inferenceSession.loadModel(testModelDescriptor, InferenceParameters())
        inferenceSession.createSession("session-1")
        inferenceSession.createSession("session-2")
        
        assertEquals(2, inferenceSession.getActiveSessionCount())
        
        val closed = inferenceSession.closeSession("session-1")
        assertTrue(closed)
        assertEquals(1, inferenceSession.getActiveSessionCount())
        
        val notFound = inferenceSession.closeSession("non-existent")
        assertFalse(notFound)
    }
    
    @Test
    fun `closeAllSessions closes all sessions`() = runTest {
        coEvery { llmEngine.loadModel(any()) } returns Result.success(testModelHandle)
        
        inferenceSession.loadModel(testModelDescriptor, InferenceParameters())
        inferenceSession.createSession("session-1")
        inferenceSession.createSession("session-2")
        inferenceSession.createSession("session-3")
        
        assertEquals(3, inferenceSession.getActiveSessionCount())
        
        inferenceSession.closeAllSessions()
        
        assertEquals(0, inferenceSession.getActiveSessionCount())
    }
    
    @Test
    fun `getSessionContext returns correct context`() = runTest {
        coEvery { llmEngine.loadModel(any()) } returns Result.success(testModelHandle)
        
        inferenceSession.loadModel(testModelDescriptor, InferenceParameters())
        inferenceSession.createSession("session-1")
        
        val context = inferenceSession.getSessionContext("session-1")
        
        assertNotNull(context)
        assertEquals("session-1", context?.sessionId)
        assertEquals("test-model-1", context?.modelId)
        assertEquals(0, context?.tokenCount)
        assertEquals(0, context?.conversationTurns)
    }
    
    @Test
    fun `getSessionContext returns null for non-existent session`() = runTest {
        val context = inferenceSession.getSessionContext("non-existent")
        
        assertNull(context)
    }
    
    @Test
    fun `unloadModel closes all sessions and unloads model`() = runTest {
        coEvery { llmEngine.loadModel(any()) } returns Result.success(testModelHandle)
        
        inferenceSession.loadModel(testModelDescriptor, InferenceParameters())
        inferenceSession.createSession("session-1")
        
        assertEquals(1, inferenceSession.getActiveSessionCount())
        
        val result = inferenceSession.unloadModel()
        
        assertTrue(result.isSuccess)
        assertEquals(0, inferenceSession.getActiveSessionCount())
    }
}
