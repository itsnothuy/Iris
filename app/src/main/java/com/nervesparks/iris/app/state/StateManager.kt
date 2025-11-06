package com.nervesparks.iris.app.state

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
}
