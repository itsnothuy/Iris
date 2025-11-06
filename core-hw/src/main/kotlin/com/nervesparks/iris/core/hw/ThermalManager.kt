package com.nervesparks.iris.core.hw

import com.nervesparks.iris.common.config.ThermalState
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for monitoring and managing device thermal state
 */
interface ThermalManager {
    /**
     * Current thermal state
     */
    val thermalState: StateFlow<ThermalState>
    
    /**
     * Start thermal monitoring
     */
    fun startMonitoring()
    
    /**
     * Stop thermal monitoring
     */
    fun stopMonitoring()
    
    /**
     * Get current device temperature (Celsius)
     */
    fun getCurrentTemperature(): Float
    
    /**
     * Check if thermal throttling is recommended
     */
    fun shouldThrottle(): Boolean
}
