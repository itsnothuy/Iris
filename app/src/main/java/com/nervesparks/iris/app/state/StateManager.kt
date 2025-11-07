package com.nervesparks.iris.app.state

import com.nervesparks.iris.common.config.DeviceState
import com.nervesparks.iris.common.config.MemoryState
import com.nervesparks.iris.common.config.PerformanceProfile
import com.nervesparks.iris.common.models.DeviceProfile
import com.nervesparks.iris.common.models.ModelHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized state management for the application
 */
@Singleton
class StateManager @Inject constructor() {
    
    private val _currentModel = MutableStateFlow<ModelHandle?>(null)
    val currentModel: StateFlow<ModelHandle?> = _currentModel.asStateFlow()
    
    private val _deviceProfile = MutableStateFlow<DeviceProfile?>(null)
    val deviceProfile: StateFlow<DeviceProfile?> = _deviceProfile.asStateFlow()
    
    private val _performanceProfile = MutableStateFlow(PerformanceProfile.BALANCED)
    val performanceProfile: StateFlow<PerformanceProfile> = _performanceProfile.asStateFlow()
    
    private val _deviceState = MutableStateFlow(DeviceState.NORMAL)
    val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()
    
    private val _memoryState = MutableStateFlow(MemoryState.NORMAL)
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()
    
    /**
     * Update currently loaded model
     */
    fun updateCurrentModel(model: ModelHandle?) {
        _currentModel.value = model
    }
    
    /**
     * Update device profile
     */
    fun updateDeviceProfile(profile: DeviceProfile) {
        _deviceProfile.value = profile
    }
    
    /**
     * Update performance profile
     */
    fun updatePerformanceProfile(profile: PerformanceProfile) {
        _performanceProfile.value = profile
    }
    
    /**
     * Update thermal state based on temperature
     * @param temperature Current device temperature in Celsius
     */
    suspend fun updateThermalState(temperature: Float) {
        _deviceState.value = when {
            temperature > 45.0f -> DeviceState.OVERHEATING
            temperature > 40.0f -> DeviceState.HOT
            else -> DeviceState.NORMAL
        }
        
        // Automatically adjust performance profile based on thermal state
        when (_deviceState.value) {
            DeviceState.OVERHEATING -> _performanceProfile.value = PerformanceProfile.EMERGENCY
            DeviceState.HOT -> _performanceProfile.value = PerformanceProfile.BATTERY_SAVER
            DeviceState.NORMAL -> {
                // Only restore to BALANCED if we were in a thermal-limited state
                if (_performanceProfile.value == PerformanceProfile.EMERGENCY ||
                    _performanceProfile.value == PerformanceProfile.BATTERY_SAVER) {
                    _performanceProfile.value = PerformanceProfile.BALANCED
                }
            }
        }
    }
    
    /**
     * Update memory state based on available memory
     * @param availableMemory Available memory in bytes
     * @param totalMemory Total memory in bytes
     */
    suspend fun updateMemoryState(availableMemory: Long, totalMemory: Long) {
        val memoryPercentage = if (totalMemory > 0) {
            (availableMemory.toDouble() / totalMemory) * 100
        } else {
            100.0
        }
        
        _memoryState.value = when {
            memoryPercentage < 10 -> MemoryState.CRITICAL
            memoryPercentage < 25 -> MemoryState.LOW
            else -> MemoryState.NORMAL
        }
    }
}
