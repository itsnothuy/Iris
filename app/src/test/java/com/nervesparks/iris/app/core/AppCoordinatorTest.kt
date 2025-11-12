package com.nervesparks.iris.app.core

import com.nervesparks.iris.app.events.EventBus
import com.nervesparks.iris.app.state.StateManager
import com.nervesparks.iris.common.config.ThermalState
import com.nervesparks.iris.common.models.BenchmarkResults
import com.nervesparks.iris.common.models.DeviceProfile
import com.nervesparks.iris.common.models.GPUInfo
import com.nervesparks.iris.common.models.GPUVendor
import com.nervesparks.iris.common.models.HardwareCapability
import com.nervesparks.iris.common.models.MemoryInfo
import com.nervesparks.iris.common.models.SoCInfo
import com.nervesparks.iris.common.models.SoCVendor
import com.nervesparks.iris.core.hw.DeviceProfileProvider
import com.nervesparks.iris.core.hw.ThermalManager
import com.nervesparks.iris.core.llm.LLMEngine
import com.nervesparks.iris.core.rag.RAGEngine
import com.nervesparks.iris.core.rag.RetrievedChunk
import com.nervesparks.iris.core.safety.SafetyEngine
import com.nervesparks.iris.core.safety.SafetyResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for AppCoordinator
 */
class AppCoordinatorTest {

    private lateinit var appCoordinator: AppCoordinator
    private lateinit var stateManager: StateManager
    private lateinit var eventBus: EventBus
    private lateinit var llmEngine: LLMEngine
    private lateinit var ragEngine: RAGEngine
    private lateinit var safetyEngine: SafetyEngine
    private lateinit var thermalManager: ThermalManager
    private lateinit var deviceProfileProvider: DeviceProfileProvider

    @Before
    fun setup() {
        stateManager = StateManager()
        eventBus = mockk(relaxed = true)
        llmEngine = mockk()
        ragEngine = mockk()
        safetyEngine = mockk()
        thermalManager = mockk()
        deviceProfileProvider = mockk()

        // Setup default mock behaviors
        every { deviceProfileProvider.getDeviceProfile() } returns createMockDeviceProfile()
        every { deviceProfileProvider.getSoCInfo() } returns SoCInfo(SoCVendor.QUALCOMM, "Test", 8)
        every { deviceProfileProvider.getGPUInfo() } returns GPUInfo(GPUVendor.ADRENO, "Test", "1.0")
        every { deviceProfileProvider.getMemoryInfo() } returns MemoryInfo(8192L * 1024 * 1024, 4096L * 1024 * 1024, false)
        coEvery { deviceProfileProvider.runBenchmark() } returns BenchmarkResults(100.0, 100.0, 100.0)

        val thermalStateFlow = MutableStateFlow(ThermalState.NORMAL)
        every { thermalManager.thermalState } returns thermalStateFlow.asStateFlow()
        every { thermalManager.startMonitoring() } returns Unit
        every { thermalManager.stopMonitoring() } returns Unit

        appCoordinator = AppCoordinator(
            stateManager,
            eventBus,
            llmEngine,
            ragEngine,
            safetyEngine,
            thermalManager,
            deviceProfileProvider,
        )
    }

    @Test
    fun `initialize succeeds and sets app state to Ready`() = runTest {
        val result = appCoordinator.initialize()

        assertTrue(result.isSuccess)
        val state = appCoordinator.appState.first()
        assertTrue(state is AppState.Ready)
        verify { thermalManager.startMonitoring() }
    }

    @Test
    fun `processUserInput blocks unsafe content`() = runTest {
        coEvery { safetyEngine.checkInput(any()) } returns SafetyResult(
            isAllowed = false,
            reason = "Test block",
        )

        val input = UserInput(text = "unsafe content")
        val results = appCoordinator.processUserInput(input).toList()

        assertTrue(results.any { it is ProcessingResult.Blocked })
    }

    @Test
    fun `processUserInput generates tokens for safe content`() = runTest {
        coEvery { safetyEngine.checkInput(any()) } returns SafetyResult(isAllowed = true)
        coEvery { llmEngine.generateText(any(), any()) } returns flow {
            emit("Hello")
            emit(" world")
        }

        val input = UserInput(text = "Hello")
        val results = appCoordinator.processUserInput(input).toList()

        assertTrue(results.first() is ProcessingResult.Started)
        assertTrue(results.any { it is ProcessingResult.TokenGenerated })
        assertTrue(results.last() is ProcessingResult.Completed)
    }

    @Test
    fun `processUserInput includes RAG context when enabled`() = runTest {
        coEvery { safetyEngine.checkInput(any()) } returns SafetyResult(isAllowed = true)
        coEvery { ragEngine.search(any(), any()) } returns listOf(
            RetrievedChunk("1", "Context content", 0.9f, "doc1", 0, emptyMap()),
        )
        coEvery { llmEngine.generateText(any(), any()) } returns flow {
            emit("Response")
        }

        val input = UserInput(text = "Question", enableRAG = true)
        val results = appCoordinator.processUserInput(input).toList()

        assertTrue(results.any { it is ProcessingResult.TokenGenerated })
    }

    @Test
    fun `shutdown stops thermal monitoring`() {
        appCoordinator.shutdown()

        verify { thermalManager.stopMonitoring() }
    }

    private fun createMockDeviceProfile(): DeviceProfile {
        return DeviceProfile(
            socVendor = SoCVendor.QUALCOMM,
            socModel = "Test SoC",
            gpuVendor = GPUVendor.ADRENO,
            gpuModel = "Test GPU",
            totalRAM = 8192L * 1024 * 1024,
            availableRAM = 4096L * 1024 * 1024,
            androidVersion = 33,
            capabilities = setOf(HardwareCapability.FP16_SUPPORT, HardwareCapability.NNAPI),
        )
    }
}
