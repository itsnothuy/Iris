package com.nervesparks.iris.app.state

import com.nervesparks.iris.common.config.DeviceState
import com.nervesparks.iris.common.config.MemoryState
import com.nervesparks.iris.common.config.PerformanceProfile
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for StateManager thermal and memory state management
 */
class StateManagerTest {

    private lateinit var stateManager: StateManager

    @Before
    fun setup() {
        stateManager = StateManager()
    }

    @Test
    fun `updateThermalState sets NORMAL for low temperature`() {
        // Given
        val normalTemperature = 35.0f

        // When
        stateManager.updateThermalState(normalTemperature)

        // Then
        assertEquals(DeviceState.NORMAL, stateManager.deviceState.value)
    }

    @Test
    fun `updateThermalState sets HOT for elevated temperature`() {
        // Given
        val hotTemperature = 42.0f

        // When
        stateManager.updateThermalState(hotTemperature)

        // Then
        assertEquals(DeviceState.HOT, stateManager.deviceState.value)
        assertEquals(PerformanceProfile.BATTERY_SAVER, stateManager.performanceProfile.value)
    }

    @Test
    fun `updateThermalState sets OVERHEATING for high temperature`() {
        // Given
        val criticalTemperature = 50.0f

        // When
        stateManager.updateThermalState(criticalTemperature)

        // Then
        assertEquals(DeviceState.OVERHEATING, stateManager.deviceState.value)
        assertEquals(PerformanceProfile.EMERGENCY, stateManager.performanceProfile.value)
    }

    @Test
    fun `updateMemoryState sets NORMAL for sufficient memory`() {
        // Given
        val availableMemory = 4L * 1024 * 1024 * 1024 // 4GB
        val totalMemory = 8L * 1024 * 1024 * 1024 // 8GB (50%)

        // When
        stateManager.updateMemoryState(availableMemory, totalMemory)

        // Then
        assertEquals(MemoryState.NORMAL, stateManager.memoryState.value)
    }

    @Test
    fun `updateMemoryState sets LOW for low memory`() {
        // Given
        val availableMemory = 1L * 1024 * 1024 * 1024 // 1GB
        val totalMemory = 8L * 1024 * 1024 * 1024 // 8GB (12.5%)

        // When
        stateManager.updateMemoryState(availableMemory, totalMemory)

        // Then
        assertEquals(MemoryState.LOW, stateManager.memoryState.value)
    }

    @Test
    fun `updateMemoryState sets CRITICAL for very low memory`() {
        // Given
        val availableMemory = 500L * 1024 * 1024 // 500MB
        val totalMemory = 8L * 1024 * 1024 * 1024 // 8GB (6.25%)

        // When
        stateManager.updateMemoryState(availableMemory, totalMemory)

        // Then
        assertEquals(MemoryState.CRITICAL, stateManager.memoryState.value)
    }

    @Test
    fun `thermal state boundary at 40 degrees`() {
        // Test boundary conditions
        stateManager.updateThermalState(39.9f)
        assertEquals(DeviceState.NORMAL, stateManager.deviceState.value)

        stateManager.updateThermalState(40.1f)
        assertEquals(DeviceState.HOT, stateManager.deviceState.value)
    }
}
