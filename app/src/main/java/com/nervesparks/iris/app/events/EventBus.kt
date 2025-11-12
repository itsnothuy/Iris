package com.nervesparks.iris.app.events

/**
 * Base event class for inter-module communication
 */
sealed class IrisEvent {
    /** Application initialized and ready */
    object AppInitialized : IrisEvent()
    
    /** Model loaded successfully */
    data class ModelLoaded(val handle: com.nervesparks.iris.common.models.ModelHandle) : IrisEvent()
    
    /** Model unloaded */
    data class ModelUnloaded(val modelPath: String) : IrisEvent()
    
    /** Generation session started */
    data class GenerationStarted(val sessionId: Long) : IrisEvent()
    
    /** Generation session completed */
    data class GenerationCompleted(val sessionId: Long) : IrisEvent()
    
    /** Thermal state changed */
    data class ThermalStateChanged(val state: com.nervesparks.iris.common.config.ThermalState) : IrisEvent()
    
    /** Memory warning triggered */
    data class MemoryWarning(val state: com.nervesparks.iris.common.config.MemoryState) : IrisEvent()
    
    /** Performance profile changed */
    data class PerformanceProfileChanged(val profile: com.nervesparks.iris.common.config.PerformanceProfile) : IrisEvent()
    
    /** Safety violation detected */
    data class SafetyViolation(val input: String, val reason: String) : IrisEvent()
    
    /** RAG index updated */
    data class RAGIndexUpdated(val documentCount: Int) : IrisEvent()
    
    /** STT model load started */
    data class STTModelLoadStarted(val modelId: String) : IrisEvent()
    
    /** STT model load completed */
    data class STTModelLoadCompleted(val modelId: String) : IrisEvent()
    
    /** STT model load failed */
    data class STTModelLoadFailed(val modelId: String, val reason: String) : IrisEvent()
    
    /** TTS model load started */
    data class TTSModelLoadStarted(val modelId: String) : IrisEvent()
    
    /** TTS model load completed */
    data class TTSModelLoadCompleted(val modelId: String) : IrisEvent()
    
    /** TTS model load failed */
    data class TTSModelLoadFailed(val modelId: String, val reason: String) : IrisEvent()
    
    /** TTS speech paused */
    object TTSSpeechPaused : IrisEvent()
    
    /** TTS speech resumed */
    object TTSSpeechResumed : IrisEvent()
    
    /** Error occurred in a component */
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
