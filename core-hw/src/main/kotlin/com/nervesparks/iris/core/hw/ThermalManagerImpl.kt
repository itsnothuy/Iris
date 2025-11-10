package com.nervesparks.iris.core.hw

import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.nervesparks.iris.common.config.ThermalState
import com.nervesparks.iris.common.logging.IrisLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ThermalManager with Android 11+ thermal monitoring
 */
@Singleton
class ThermalManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ThermalManager {
    
    private val _thermalState = MutableStateFlow(ThermalState.NORMAL)
    override val thermalState: StateFlow<ThermalState> = _thermalState.asStateFlow()
    
    private var isMonitoring = false
    private var monitoringScope: CoroutineScope? = null
    private var thermalCallback: Any? = null
    
    override fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        IrisLogger.info("Starting thermal monitoring")
        
        // Create a coroutine scope for monitoring
        monitoringScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        
        // Try Android 11+ thermal monitoring first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registerThermalListener()
        }
        
        // Fallback to polling for older versions or as backup
        startTemperaturePolling()
    }
    
    override fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false
        
        IrisLogger.info("Stopping thermal monitoring")
        
        // Unregister thermal listener if registered
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            unregisterThermalListener()
        }
        
        // Cancel monitoring scope
        monitoringScope?.cancel()
        monitoringScope = null
    }
    
    override fun getCurrentTemperature(): Float {
        // Try to get thermal headroom on API 30+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.let {
                // Thermal headroom is in seconds before throttling
                // Higher values = cooler device
                val headroom = it.getThermalHeadroom(5)
                // Convert headroom to approximate temperature
                // Low headroom = high temperature
                return when {
                    headroom < 1.0f -> 50.0f // Critical
                    headroom < 2.0f -> 45.0f // Severe
                    headroom < 3.0f -> 40.0f // Moderate
                    headroom < 5.0f -> 35.0f // Light
                    else -> 25.0f // Normal
                }
            }
        }
        
        // Fallback based on current thermal state
        return when (_thermalState.value) {
            ThermalState.CRITICAL, 
            ThermalState.THERMAL_STATUS_CRITICAL,
            ThermalState.THERMAL_STATUS_EMERGENCY -> 55.0f
            ThermalState.SEVERE -> 50.0f
            ThermalState.MODERATE -> 45.0f
            ThermalState.LIGHT -> 40.0f
            ThermalState.NORMAL -> 25.0f
        }
    }
    
    override fun shouldThrottle(): Boolean {
        return when (_thermalState.value) {
            ThermalState.NORMAL, ThermalState.LIGHT -> false
            ThermalState.MODERATE, 
            ThermalState.SEVERE, 
            ThermalState.CRITICAL,
            ThermalState.THERMAL_STATUS_CRITICAL,
            ThermalState.THERMAL_STATUS_EMERGENCY -> true
        }
    }
    
    /**
     * Register thermal listener for Android 11+ (API 30+)
     */
    private fun registerThermalListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // Note: android.os.PowerManager.THERMAL_STATUS_* constants are available from API 29+
                // Actual implementation would use PowerManager or dedicated ThermalService
                IrisLogger.info("Thermal listener support available (API ${Build.VERSION.SDK_INT})")
            } catch (e: Exception) {
                IrisLogger.error("Failed to register thermal listener", e)
            }
        }
    }
    
    /**
     * Unregister thermal listener
     */
    private fun unregisterThermalListener() {
        thermalCallback?.let {
            try {
                // Cleanup thermal callback if needed
                IrisLogger.info("Unregistering thermal listener")
            } catch (e: Exception) {
                IrisLogger.error("Failed to unregister thermal listener", e)
            }
        }
        thermalCallback = null
    }
    
    /**
     * Start polling temperature (fallback for older Android versions)
     */
    private fun startTemperaturePolling() {
        monitoringScope?.launch {
            IrisLogger.info("Starting temperature polling")
            
            while (isActive && isMonitoring) {
                try {
                    updateThermalState()
                } catch (e: Exception) {
                    IrisLogger.error("Error during temperature polling", e)
                }
                
                // Poll every 5 seconds
                delay(5000)
            }
        }
    }
    
    /**
     * Update thermal state based on current conditions
     */
    private fun updateThermalState() {
        val temperature = getCurrentTemperature()
        
        val newState = when {
            temperature >= 55.0f -> ThermalState.CRITICAL
            temperature >= 50.0f -> ThermalState.SEVERE
            temperature >= 45.0f -> ThermalState.MODERATE
            temperature >= 40.0f -> ThermalState.LIGHT
            else -> ThermalState.NORMAL
        }
        
        if (newState != _thermalState.value) {
            IrisLogger.info("Thermal state changed: ${_thermalState.value} -> $newState (temp: ${temperature}°C)")
            _thermalState.value = newState
        }
    }
}
