package com.nervesparks.iris.app.core

import com.nervesparks.iris.app.events.EventBus
import com.nervesparks.iris.app.events.IrisEvent
import com.nervesparks.iris.app.state.StateManager
import com.nervesparks.iris.common.logging.IrisLogger
import com.nervesparks.iris.core.hw.DeviceProfileProvider
import com.nervesparks.iris.core.hw.ThermalManager
import com.nervesparks.iris.core.llm.LLMEngine
import com.nervesparks.iris.core.rag.RAGEngine
import com.nervesparks.iris.core.safety.SafetyEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central coordinator for application operations
 */
@Singleton
class AppCoordinator @Inject constructor(
    private val stateManager: StateManager,
    private val eventBus: EventBus,
    private val llmEngine: LLMEngine,
    private val ragEngine: RAGEngine,
    private val safetyEngine: SafetyEngine,
    private val thermalManager: ThermalManager,
    private val deviceProfileProvider: DeviceProfileProvider,
) {

    private val _appState = MutableStateFlow<AppState>(AppState.Initializing)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    /**
     * Initialize the application
     */
    suspend fun initialize(): Result<Unit> {
        return try {
            IrisLogger.info("Initializing application")

            // Initialize hardware detection
            val deviceProfile = deviceProfileProvider.getDeviceProfile()
            stateManager.updateDeviceProfile(deviceProfile)
            IrisLogger.info("Device profile: ${deviceProfile.socVendor} ${deviceProfile.socModel}")

            // Initialize thermal monitoring
            thermalManager.startMonitoring()
            IrisLogger.info("Thermal monitoring started")

            _appState.value = AppState.Ready
            IrisLogger.info("Application ready")
            Result.success(Unit)
        } catch (e: Exception) {
            IrisLogger.error("Failed to initialize application", e)
            _appState.value = AppState.Error(e)
            Result.failure(e)
        }
    }

    /**
     * Process user input through the AI pipeline
     */
    suspend fun processUserInput(input: UserInput): Flow<ProcessingResult> = flow {
        try {
            emit(ProcessingResult.Started)

            // Safety check on input
            val safetyResult = safetyEngine.checkInput(input.text)
            if (!safetyResult.isAllowed) {
                emit(ProcessingResult.Blocked(safetyResult.reason ?: "Content blocked by safety filter"))
                eventBus.emit(IrisEvent.SafetyViolation(input.text, safetyResult.reason ?: "Unknown"))
                return@flow
            }

            // RAG retrieval if enabled
            val context = if (input.enableRAG) {
                val chunks = ragEngine.search(input.text)
                chunks.joinToString("\n") { it.content }
            } else {
                ""
            }

            // Build prompt with context
            val prompt = buildPrompt(input.text, context)

            // LLM generation
            llmEngine.generateText(prompt, input.params)
                .collect { token ->
                    emit(ProcessingResult.TokenGenerated(token))
                }

            emit(ProcessingResult.Completed)
        } catch (e: Exception) {
            IrisLogger.error("Error processing user input", e)
            emit(ProcessingResult.Error(e))
        }
    }.catch { e ->
        emit(ProcessingResult.Error(e as? Exception ?: Exception(e)))
    }

    /**
     * Build prompt with optional RAG context
     */
    private fun buildPrompt(userText: String, context: String): String {
        return if (context.isNotEmpty()) {
            "Context:\n$context\n\nUser: $userText\n\nAssistant:"
        } else {
            "User: $userText\n\nAssistant:"
        }
    }

    /**
     * Shutdown the application gracefully
     */
    fun shutdown() {
        IrisLogger.info("Shutting down application")
        thermalManager.stopMonitoring()
    }
}
