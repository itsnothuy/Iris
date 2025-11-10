package com.nervesparks.iris.core.llm.inference

import android.content.Context
import android.util.Log
import com.nervesparks.iris.common.config.ThermalState
import com.nervesparks.iris.common.models.BackendType
import com.nervesparks.iris.common.models.DeviceClass
import com.nervesparks.iris.common.models.DeviceProfile
import com.nervesparks.iris.common.models.SoCVendor
import com.nervesparks.iris.core.hw.DeviceProfileProvider
import com.nervesparks.iris.core.hw.ThermalManager
import com.nervesparks.iris.core.llm.LLMEngine
import com.nervesparks.iris.core.llm.ModelLoadParams
import com.nervesparks.iris.core.safety.SafetyEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of InferenceSession that manages model loading, session state,
 * and provides streaming inference with adaptive performance and safety checks
 */
@Singleton
class InferenceSessionImpl @Inject constructor(
    private val llmEngine: LLMEngine,
    private val deviceProfileProvider: DeviceProfileProvider,
    private val thermalManager: ThermalManager,
    private val safetyEngine: SafetyEngine,
    @ApplicationContext private val context: Context
) : InferenceSession {
    
    companion object {
        private const val TAG = "InferenceSession"
        private const val MAX_CONTEXT_LENGTH = 4096
        private const val SLIDING_WINDOW_SIZE = 2048
        private const val THERMAL_CHECK_INTERVAL = 5000L
    }
    
    private val activeSessions = mutableMapOf<String, SessionContext>()
    private var currentModelId: String? = null
    private var isModelLoaded = false
    private val thermalMonitorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    override suspend fun loadModel(
        modelDescriptor: ModelDescriptor,
        parameters: InferenceParameters
    ): Result<ModelLoadResult> = withContext(Dispatchers.IO) {
        
        try {
            Log.i(TAG, "Loading model: ${modelDescriptor.id}")
            
            // Unload previous model if loaded
            if (isModelLoaded) {
                unloadCurrentModel()
            }
            
            // Validate device compatibility
            val deviceProfile = deviceProfileProvider.getDeviceProfile()
            val adaptedParameters = adaptParametersForDevice(parameters, deviceProfile)
            
            val startTime = System.currentTimeMillis()
            
            // Load model with LLM engine
            val loadResult = llmEngine.loadModel(modelDescriptor.path)
            
            if (loadResult.isFailure) {
                val error = loadResult.exceptionOrNull()
                Log.e(TAG, "Failed to load model: ${error?.message}", error)
                return@withContext Result.failure(
                    InferenceException("Model loading failed: ${error?.message}")
                )
            }
            
            val modelHandle = loadResult.getOrNull()!!
            val loadTime = System.currentTimeMillis() - startTime
            
            // Initialize session state
            currentModelId = modelDescriptor.id
            isModelLoaded = true
            
            // Start thermal monitoring
            startThermalMonitoring()
            
            val result = ModelLoadResult(
                modelId = modelDescriptor.id,
                backend = modelHandle.backend,
                contextSize = adaptedParameters.contextSize,
                loadTime = loadTime
            )
            
            Log.i(TAG, "Model loaded successfully: ${modelDescriptor.id}")
            
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception during model loading", e)
            Result.failure(InferenceException("Model loading exception", e))
        }
    }
    
    override suspend fun createSession(conversationId: String): Result<InferenceSessionContext> {
        return try {
            if (!isModelLoaded) {
                return Result.failure(InferenceException("No model loaded"))
            }
            
            val sessionContext = SessionContext(
                conversationId = conversationId,
                modelId = currentModelId!!,
                createdAt = System.currentTimeMillis(),
                lastActivity = System.currentTimeMillis(),
                tokenCount = 0,
                conversationHistory = mutableListOf(),
                systemPrompt = null
            )
            
            activeSessions[conversationId] = sessionContext
            
            val context = InferenceSessionContext(
                sessionId = conversationId,
                modelId = currentModelId!!,
                isActive = true,
                createdAt = sessionContext.createdAt
            )
            
            Log.i(TAG, "Created inference session: $conversationId")
            Result.success(context)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create session", e)
            Result.failure(InferenceException("Session creation failed", e))
        }
    }
    
    override suspend fun generateResponse(
        sessionId: String,
        prompt: String,
        parameters: GenerationParameters
    ): Flow<InferenceResult> = flow {
        
        val session = activeSessions[sessionId]
            ?: throw InferenceException("Session not found: $sessionId")
        
        if (!isModelLoaded) {
            throw InferenceException("No model loaded")
        }
        
        try {
            // Safety check on input
            val safetyResult = safetyEngine.checkInput(prompt)
            if (!safetyResult.isAllowed) {
                emit(InferenceResult.SafetyViolation(safetyResult.reason ?: "Input rejected by safety filter"))
                return@flow
            }
            
            // Prepare conversation context
            val contextPrompt = buildContextPrompt(session, prompt)
            val adaptedParams = adaptGenerationParameters(parameters)
            
            // Start generation
            val startTime = System.currentTimeMillis()
            var tokenCount = 0
            val generatedTokens = mutableListOf<String>()
            
            emit(InferenceResult.GenerationStarted(sessionId))
            
            // Convert GenerationParameters to GenerationParams (existing model)
            val genParams = com.nervesparks.iris.common.models.GenerationParams(
                temperature = adaptedParams.temperature,
                topK = adaptedParams.topK,
                topP = adaptedParams.topP,
                maxTokens = adaptedParams.maxTokens,
                stopTokens = adaptedParams.stopSequences,
                repeatPenalty = adaptedParams.repeatPenalty,
                seed = -1L
            )
            
            // Stream tokens from LLM engine
            llmEngine.generateText(contextPrompt, genParams)
                .collect { token ->
                    tokenCount++
                    generatedTokens.add(token)
                    
                    val partialText = generatedTokens.joinToString("")
                    
                    // Safety check on partial output every 10 tokens
                    if (tokenCount % 10 == 0) {
                        val outputSafety = safetyEngine.checkOutput(partialText)
                        if (!outputSafety.isAllowed) {
                            emit(InferenceResult.SafetyViolation(
                                outputSafety.reason ?: "Output rejected by safety filter"
                            ))
                            return@collect
                        }
                    }
                    
                    emit(InferenceResult.TokenGenerated(
                        sessionId = sessionId,
                        token = token,
                        partialText = partialText,
                        tokenIndex = tokenCount,
                        confidence = 1.0f
                    ))
                }
            
            // Generation completed
            val totalTime = System.currentTimeMillis() - startTime
            val tokensPerSecond = if (totalTime > 0) {
                (tokenCount * 1000.0) / totalTime
            } else 0.0
            
            val fullResponse = generatedTokens.joinToString("")
            
            // Final safety check
            val finalSafety = safetyEngine.checkOutput(fullResponse)
            if (!finalSafety.isAllowed) {
                emit(InferenceResult.SafetyViolation(
                    finalSafety.reason ?: "Final output rejected by safety filter"
                ))
                return@flow
            }
            
            // Update session state
            updateSessionState(session, prompt, fullResponse, tokenCount)
            
            emit(InferenceResult.GenerationCompleted(
                sessionId = sessionId,
                fullText = fullResponse,
                tokenCount = tokenCount,
                generationTime = totalTime,
                tokensPerSecond = tokensPerSecond,
                finishReason = FinishReason.COMPLETED
            ))
                
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed for session $sessionId", e)
            emit(InferenceResult.Error(
                sessionId = sessionId,
                error = "Generation failed: ${e.message}",
                cause = e
            ))
        }
    }
    
    override suspend fun getSessionContext(sessionId: String): InferenceSessionContext? {
        val session = activeSessions[sessionId] ?: return null
        
        return InferenceSessionContext(
            sessionId = sessionId,
            modelId = session.modelId,
            isActive = true,
            createdAt = session.createdAt,
            lastActivity = session.lastActivity,
            tokenCount = session.tokenCount,
            conversationTurns = session.conversationHistory.size
        )
    }
    
    override suspend fun getActiveSessionCount(): Int {
        return activeSessions.size
    }
    
    override suspend fun closeSession(sessionId: String): Boolean {
        val session = activeSessions.remove(sessionId)
        if (session != null) {
            Log.i(TAG, "Closed inference session: $sessionId")
            return true
        }
        return false
    }
    
    override suspend fun closeAllSessions() {
        val closedSessions = activeSessions.keys.toList()
        activeSessions.clear()
        
        Log.i(TAG, "Closed ${closedSessions.size} active sessions")
    }
    
    override suspend fun unloadModel(): Result<Unit> {
        return try {
            if (isModelLoaded) {
                closeAllSessions()
                stopThermalMonitoring()
                
                // Note: LLMEngine doesn't expose unloadModel without handle
                // We'll just mark as unloaded for now
                isModelLoaded = false
                currentModelId = null
                
                Log.i(TAG, "Model unloaded successfully")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unload model", e)
            Result.failure(InferenceException("Model unloading failed", e))
        }
    }
    
    // Private helper methods
    
    private fun adaptParametersForDevice(
        parameters: InferenceParameters,
        deviceProfile: DeviceProfile
    ): InferenceParameters {
        val contextSize = when (deviceProfile.deviceClass) {
            DeviceClass.BUDGET -> minOf(parameters.contextSize, 1024)
            DeviceClass.MID_RANGE -> minOf(parameters.contextSize, 2048)
            DeviceClass.HIGH_END -> minOf(parameters.contextSize, 4096)
            DeviceClass.FLAGSHIP -> parameters.contextSize
            else -> minOf(parameters.contextSize, 2048)
        }
        
        val batchSize = when (deviceProfile.deviceClass) {
            DeviceClass.BUDGET -> 1
            DeviceClass.MID_RANGE -> 2
            DeviceClass.HIGH_END -> 4
            DeviceClass.FLAGSHIP -> 8
            else -> 2
        }
        
        val cpuCores = Runtime.getRuntime().availableProcessors()
        
        return parameters.copy(
            contextSize = contextSize,
            batchSize = batchSize,
            threadsCount = minOf(parameters.threadsCount, cpuCores)
        )
    }
    
    private fun adaptGenerationParameters(parameters: GenerationParameters): GenerationParameters {
        val thermalState = thermalManager.thermalState.value
        
        return when (thermalState) {
            ThermalState.NORMAL, ThermalState.LIGHT -> parameters
            ThermalState.MODERATE -> parameters.copy(
                maxTokens = minOf(parameters.maxTokens, 512),
                temperature = maxOf(parameters.temperature - 0.1f, 0.1f)
            )
            ThermalState.SEVERE -> parameters.copy(
                maxTokens = minOf(parameters.maxTokens, 256),
                temperature = maxOf(parameters.temperature - 0.2f, 0.1f),
                topP = maxOf(parameters.topP - 0.1f, 0.1f)
            )
            ThermalState.CRITICAL, ThermalState.THERMAL_STATUS_CRITICAL, ThermalState.THERMAL_STATUS_EMERGENCY -> 
                parameters.copy(
                    maxTokens = minOf(parameters.maxTokens, 128),
                    temperature = 0.1f,
                    topP = 0.5f
                )
        }
    }
    
    private fun buildContextPrompt(session: SessionContext, newPrompt: String): String {
        val systemPrompt = session.systemPrompt ?: getDefaultSystemPrompt()
        
        val conversationContext = session.conversationHistory
            .takeLast(10) // Limit context to last 10 exchanges
            .joinToString("\n") { exchange ->
                "Human: ${exchange.userMessage}\nAssistant: ${exchange.assistantResponse}\n"
            }
        
        return buildString {
            append(systemPrompt)
            if (conversationContext.isNotEmpty()) {
                append("\n\nPrevious conversation:\n")
                append(conversationContext)
            }
            append("\n\nHuman: $newPrompt\nAssistant:")
        }
    }
    
    private fun updateSessionState(
        session: SessionContext,
        userMessage: String,
        assistantResponse: String,
        tokensGenerated: Int
    ) {
        session.conversationHistory.add(
            ConversationExchange(
                userMessage = userMessage,
                assistantResponse = assistantResponse,
                timestamp = System.currentTimeMillis(),
                tokenCount = tokensGenerated
            )
        )
        
        session.tokenCount += tokensGenerated
        session.lastActivity = System.currentTimeMillis()
        
        // Implement sliding window for context management
        if (session.tokenCount > MAX_CONTEXT_LENGTH) {
            implementSlidingWindow(session)
        }
    }
    
    private fun implementSlidingWindow(session: SessionContext) {
        val targetTokens = SLIDING_WINDOW_SIZE
        var currentTokens = session.tokenCount
        
        while (currentTokens > targetTokens && session.conversationHistory.isNotEmpty()) {
            val removed = session.conversationHistory.removeFirst()
            currentTokens -= removed.tokenCount
        }
        
        session.tokenCount = currentTokens
        Log.d(TAG, "Applied sliding window, reduced context to $currentTokens tokens")
    }
    
    private fun startThermalMonitoring() {
        thermalMonitorScope.launch {
            while (isActive && isModelLoaded) {
                delay(THERMAL_CHECK_INTERVAL)
                
                val thermalState = thermalManager.thermalState.value
                
                if (thermalState == ThermalState.CRITICAL || 
                    thermalState == ThermalState.THERMAL_STATUS_CRITICAL ||
                    thermalState == ThermalState.THERMAL_STATUS_EMERGENCY) {
                    Log.w(TAG, "Critical thermal state detected: $thermalState")
                    // Pause briefly to cool down
                    delay(10000) // 10 seconds
                }
            }
        }
    }
    
    private fun stopThermalMonitoring() {
        thermalMonitorScope.coroutineContext.cancelChildren()
    }
    
    private suspend fun unloadCurrentModel() {
        try {
            if (isModelLoaded) {
                closeAllSessions()
                // Note: Would call llmEngine.unloadModel(handle) if we had the handle
                isModelLoaded = false
                currentModelId = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error during model unloading", e)
        }
    }
    
    private fun getDefaultSystemPrompt(): String {
        return """You are Iris, a helpful AI assistant running locally on the user's Android device. 
You are knowledgeable, concise, and respectful. You maintain conversation context and provide accurate information.
Always be helpful while being mindful of the device's computational limitations."""
    }
}
