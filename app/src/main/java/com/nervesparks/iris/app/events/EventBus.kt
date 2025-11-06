package com.nervesparks.iris.app.events

/**
 * Base event class for inter-module communication
 */
sealed class IrisEvent {
    data class ModelLoaded(val handle: com.nervesparks.iris.common.models.ModelHandle) : IrisEvent()
    data class ModelUnloaded(val modelPath: String) : IrisEvent()
    data class ThermalStateChanged(val state: com.nervesparks.iris.common.config.ThermalState) : IrisEvent()
    data class PerformanceProfileChanged(val profile: com.nervesparks.iris.common.config.PerformanceProfile) : IrisEvent()
    data class SafetyViolation(val input: String, val reason: String) : IrisEvent()
    data class RAGIndexUpdated(val documentCount: Int) : IrisEvent()
    data class ErrorOccurred(val error: com.nervesparks.iris.common.error.IrisException, val component: String) : IrisEvent()
}

/**
 * Event bus for application-wide event communication
 */
interface EventBus {
    /**
     * Emit an event
     */
    fun emit(event: IrisEvent)
    
    /**
     * Subscribe to all events
     */
    val events: kotlinx.coroutines.flow.SharedFlow<IrisEvent>
}
