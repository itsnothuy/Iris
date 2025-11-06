# Issue #05: Chat Engine & Inference Pipeline

## 🎯 Epic: Conversational AI Engine
**Priority**: P1 (High)  
**Estimate**: 8-10 days  
**Dependencies**: #01 (Core Architecture), #02 (Native llama.cpp), #04 (Model Management)  
**Architecture Reference**: [docs/architecture.md](../architecture.md) - Section 5 Core Chat Engine

## 📋 Overview
Implement the core chat engine with streaming inference, conversation state management, adaptive performance controls, and multi-turn conversation capabilities. This engine serves as the primary interface between users and the AI models.

## 🎯 Goals
- **Streaming Inference**: Real-time token streaming with low latency
- **Conversation Management**: Persistent conversation state and history
- **Adaptive Performance**: Dynamic quality adjustments based on device capabilities
- **Multi-turn Support**: Context-aware conversations with memory management
- **Safety Integration**: Built-in content filtering and safety checks

## 📝 Detailed Tasks

### 1. Core Inference Engine

#### 1.1 Inference Session Management
Create `core-inference/src/main/kotlin/InferenceSessionImpl.kt`:

```kotlin
@Singleton
class InferenceSessionImpl @Inject constructor(
    private val nativeEngine: NativeInferenceEngine,
    private val deviceProfileProvider: DeviceProfileProvider,
    private val performanceManager: PerformanceManager,
    private val safetyEngine: SafetyEngine,
    private val eventBus: EventBus
) : InferenceSession {
    
    companion object {
        private const val TAG = "InferenceSession"
        private const val MAX_CONTEXT_LENGTH = 4096
        private const val SLIDING_WINDOW_SIZE = 2048
        private const val MIN_TOKENS_PER_SECOND = 0.5
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
            eventBus.emit(IrisEvent.ModelLoadStarted(modelDescriptor.id))
            
            // Unload previous model if loaded
            if (isModelLoaded) {
                unloadCurrentModel()
            }
            
            // Validate device compatibility
            val deviceProfile = deviceProfileProvider.getDeviceProfile()
            val backendType = selectOptimalBackend(modelDescriptor, deviceProfile)
            val adaptedParameters = adaptParametersForDevice(parameters, deviceProfile)
            
            // Load model with native engine
            val loadResult = nativeEngine.loadModel(
                modelPath = getModelPath(modelDescriptor),
                backend = backendType,
                parameters = adaptedParameters
            )
            
            if (loadResult.isFailure) {
                val error = loadResult.exceptionOrNull()
                Log.e(TAG, "Failed to load model: ${error?.message}", error)
                eventBus.emit(IrisEvent.ModelLoadFailed(modelDescriptor.id, error?.message ?: "Unknown error"))
                return@withContext Result.failure(
                    InferenceException("Model loading failed: ${error?.message}")
                )
            }
            
            // Initialize session state
            currentModelId = modelDescriptor.id
            isModelLoaded = true
            
            // Start thermal monitoring
            startThermalMonitoring()
            
            val result = ModelLoadResult(
                modelId = modelDescriptor.id,
                backend = backendType,
                contextSize = adaptedParameters.contextSize,
                loadTime = loadResult.getOrNull()?.loadTime ?: 0L
            )
            
            Log.i(TAG, "Model loaded successfully: ${modelDescriptor.id}")
            eventBus.emit(IrisEvent.ModelLoadCompleted(modelDescriptor.id, result))
            
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception during model loading", e)
            eventBus.emit(IrisEvent.ModelLoadFailed(modelDescriptor.id, e.message ?: "Unknown exception"))
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
            val safetyResult = safetyEngine.checkInputSafety(prompt)
            if (!safetyResult.isSafe) {
                emit(InferenceResult.SafetyViolation(safetyResult.reason))
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
            
            // Stream tokens from native engine
            nativeEngine.generateTokens(contextPrompt, adaptedParams)
                .collect { tokenResult ->
                    when (tokenResult) {
                        is NativeTokenResult.Token -> {
                            tokenCount++
                            generatedTokens.add(tokenResult.text)
                            
                            val partialText = generatedTokens.joinToString("")
                            
                            // Safety check on partial output
                            if (tokenCount % 10 == 0) { // Check every 10 tokens
                                val outputSafety = safetyEngine.checkOutputSafety(partialText)
                                if (!outputSafety.isSafe) {
                                    emit(InferenceResult.SafetyViolation(outputSafety.reason))
                                    return@collect
                                }
                            }
                            
                            emit(InferenceResult.TokenGenerated(
                                sessionId = sessionId,
                                token = tokenResult.text,
                                partialText = partialText,
                                tokenIndex = tokenCount,
                                confidence = tokenResult.confidence
                            ))
                        }
                        
                        is NativeTokenResult.Finished -> {
                            val totalTime = System.currentTimeMillis() - startTime
                            val tokensPerSecond = if (totalTime > 0) {
                                (tokenCount * 1000.0) / totalTime
                            } else 0.0
                            
                            val fullResponse = generatedTokens.joinToString("")
                            
                            // Final safety check
                            val finalSafety = safetyEngine.checkOutputSafety(fullResponse)
                            if (!finalSafety.isSafe) {
                                emit(InferenceResult.SafetyViolation(finalSafety.reason))
                                return@collect
                            }
                            
                            // Update session state
                            updateSessionState(session, prompt, fullResponse, tokenCount)
                            
                            emit(InferenceResult.GenerationCompleted(
                                sessionId = sessionId,
                                fullText = fullResponse,
                                tokenCount = tokenCount,
                                generationTime = totalTime,
                                tokensPerSecond = tokensPerSecond,
                                finishReason = tokenResult.reason
                            ))
                        }
                        
                        is NativeTokenResult.Error -> {
                            emit(InferenceResult.Error(
                                sessionId = sessionId,
                                error = tokenResult.message,
                                cause = tokenResult.cause
                            ))
                        }
                    }
                }
                
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
            eventBus.emit(IrisEvent.InferenceSessionClosed(sessionId))
            return true
        }
        return false
    }
    
    override suspend fun closeAllSessions() {
        val closedSessions = activeSessions.keys.toList()
        activeSessions.clear()
        
        closedSessions.forEach { sessionId ->
            eventBus.emit(IrisEvent.InferenceSessionClosed(sessionId))
        }
        
        Log.i(TAG, "Closed ${closedSessions.size} active sessions")
    }
    
    override suspend fun unloadModel(): Result<Unit> {
        return try {
            if (isModelLoaded) {
                closeAllSessions()
                stopThermalMonitoring()
                
                nativeEngine.unloadModel()
                isModelLoaded = false
                currentModelId = null
                
                Log.i(TAG, "Model unloaded successfully")
                eventBus.emit(IrisEvent.ModelUnloaded())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unload model", e)
            Result.failure(InferenceException("Model unloading failed", e))
        }
    }
    
    // Private helper methods
    
    private fun selectOptimalBackend(
        modelDescriptor: ModelDescriptor,
        deviceProfile: DeviceProfile
    ): BackendType {
        val availableBackends = modelDescriptor.deviceRequirements.supportedBackends
        
        return when (deviceProfile.socVendor) {
            SoCVendor.QUALCOMM -> {
                when {
                    availableBackends.contains("QNN_HEXAGON") && 
                        deviceProfile.deviceClass == DeviceClass.FLAGSHIP -> BackendType.QNN_HEXAGON
                    availableBackends.contains("OPENCL_ADRENO") -> BackendType.OPENCL_ADRENO
                    else -> BackendType.CPU_NEON
                }
            }
            SoCVendor.SAMSUNG, SoCVendor.GOOGLE -> {
                when {
                    availableBackends.contains("VULKAN_MALI") &&
                        deviceProfile.deviceClass in listOf(DeviceClass.HIGH_END, DeviceClass.FLAGSHIP) -> 
                        BackendType.VULKAN_MALI
                    else -> BackendType.CPU_NEON
                }
            }
            else -> BackendType.CPU_NEON
        }
    }
    
    private fun adaptParametersForDevice(
        parameters: InferenceParameters,
        deviceProfile: DeviceProfile
    ): InferenceParameters {
        val contextSize = when (deviceProfile.deviceClass) {
            DeviceClass.BUDGET -> minOf(parameters.contextSize, 1024)
            DeviceClass.MID_RANGE -> minOf(parameters.contextSize, 2048)
            DeviceClass.HIGH_END -> minOf(parameters.contextSize, 4096)
            DeviceClass.FLAGSHIP -> parameters.contextSize
        }
        
        val batchSize = when (deviceProfile.deviceClass) {
            DeviceClass.BUDGET -> 1
            DeviceClass.MID_RANGE -> 2
            DeviceClass.HIGH_END -> 4
            DeviceClass.FLAGSHIP -> 8
        }
        
        return parameters.copy(
            contextSize = contextSize,
            batchSize = batchSize,
            threadsCount = minOf(parameters.threadsCount, deviceProfile.cpuCores)
        )
    }
    
    private fun adaptGenerationParameters(parameters: GenerationParameters): GenerationParameters {
        val deviceProfile = deviceProfileProvider.getDeviceProfile()
        val performanceState = performanceManager.getCurrentPerformanceState()
        
        return when (performanceState.thermalState) {
            ThermalState.NORMAL -> parameters
            ThermalState.WARM -> parameters.copy(
                maxTokens = minOf(parameters.maxTokens, 512),
                temperature = maxOf(parameters.temperature - 0.1f, 0.1f)
            )
            ThermalState.HOT -> parameters.copy(
                maxTokens = minOf(parameters.maxTokens, 256),
                temperature = maxOf(parameters.temperature - 0.2f, 0.1f),
                topP = maxOf(parameters.topP - 0.1f, 0.1f)
            )
            ThermalState.CRITICAL -> parameters.copy(
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
            while (isModelLoaded) {
                delay(THERMAL_CHECK_INTERVAL)
                
                val thermalState = performanceManager.getCurrentPerformanceState().thermalState
                
                if (thermalState == ThermalState.CRITICAL) {
                    Log.w(TAG, "Critical thermal state detected, pausing inference")
                    eventBus.emit(IrisEvent.ThermalThrottling(thermalState))
                    
                    // Pause briefly to cool down
                    delay(10000) // 10 seconds
                }
            }
        }
    }
    
    private fun stopThermalMonitoring() {
        thermalMonitorScope.coroutineContext.cancelChildren()
    }
    
    private fun unloadCurrentModel() {
        try {
            if (isModelLoaded) {
                closeAllSessions()
                nativeEngine.unloadModel()
                isModelLoaded = false
                currentModelId = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error during model unloading", e)
        }
    }
    
    private fun getModelPath(modelDescriptor: ModelDescriptor): String {
        return File(
            File(context.getExternalFilesDir(null), "models"),
            "${modelDescriptor.id}.gguf"
        ).absolutePath
    }
    
    private fun getDefaultSystemPrompt(): String {
        return """You are Iris, a helpful AI assistant running locally on the user's Android device. 
You are knowledgeable, concise, and respectful. You maintain conversation context and provide accurate information.
Always be helpful while being mindful of the device's computational limitations."""
    }
}

// Session management data classes
data class SessionContext(
    val conversationId: String,
    val modelId: String,
    val createdAt: Long,
    var lastActivity: Long,
    var tokenCount: Int,
    val conversationHistory: MutableList<ConversationExchange>,
    var systemPrompt: String?
)

data class ConversationExchange(
    val userMessage: String,
    val assistantResponse: String,
    val timestamp: Long,
    val tokenCount: Int
)

data class InferenceSessionContext(
    val sessionId: String,
    val modelId: String,
    val isActive: Boolean,
    val createdAt: Long,
    val lastActivity: Long = createdAt,
    val tokenCount: Int = 0,
    val conversationTurns: Int = 0
)

data class ModelLoadResult(
    val modelId: String,
    val backend: BackendType,
    val contextSize: Int,
    val loadTime: Long
)

data class GenerationParameters(
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,
    val stopSequences: List<String> = emptyList()
)

// Inference results sealed class
sealed class InferenceResult {
    data class GenerationStarted(val sessionId: String) : InferenceResult()
    
    data class TokenGenerated(
        val sessionId: String,
        val token: String,
        val partialText: String,
        val tokenIndex: Int,
        val confidence: Float = 1.0f
    ) : InferenceResult()
    
    data class GenerationCompleted(
        val sessionId: String,
        val fullText: String,
        val tokenCount: Int,
        val generationTime: Long,
        val tokensPerSecond: Double,
        val finishReason: FinishReason
    ) : InferenceResult()
    
    data class SafetyViolation(val reason: String) : InferenceResult()
    
    data class Error(
        val sessionId: String,
        val error: String,
        val cause: Throwable? = null
    ) : InferenceResult()
}

enum class FinishReason {
    COMPLETED, MAX_TOKENS, STOP_SEQUENCE, SAFETY_FILTER, ERROR
}

// Exception class
class InferenceException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

#### 1.2 Conversation State Management
Create `core-chat/src/main/kotlin/ConversationManagerImpl.kt`:

```kotlin
@Singleton
class ConversationManagerImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val inferenceSession: InferenceSession,
    private val eventBus: EventBus,
    @ApplicationContext private val context: Context
) : ConversationManager {
    
    companion object {
        private const val TAG = "ConversationManager"
        private const val MAX_CONVERSATIONS = 100
        private const val MAX_MESSAGES_PER_CONVERSATION = 1000
    }
    
    private val activeConversations = mutableMapOf<String, ConversationState>()
    
    override suspend fun createConversation(
        title: String?,
        modelId: String,
        systemPrompt: String?
    ): Result<Conversation> {
        return try {
            val conversationId = generateConversationId()
            val now = System.currentTimeMillis()
            
            val conversation = Conversation(
                id = conversationId,
                title = title ?: "New Conversation",
                modelId = modelId,
                systemPrompt = systemPrompt,
                createdAt = now,
                updatedAt = now,
                messageCount = 0,
                isActive = true
            )
            
            // Save to database
            conversationDao.insertConversation(conversation.toEntity())
            
            // Create inference session
            val sessionResult = inferenceSession.createSession(conversationId)
            if (sessionResult.isFailure) {
                return Result.failure(
                    ConversationException("Failed to create inference session")
                )
            }
            
            // Track active conversation
            activeConversations[conversationId] = ConversationState(
                conversation = conversation,
                messages = mutableListOf()
            )
            
            eventBus.emit(IrisEvent.ConversationCreated(conversationId))
            
            Result.success(conversation)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create conversation", e)
            Result.failure(ConversationException("Conversation creation failed", e))
        }
    }
    
    override suspend fun sendMessage(
        conversationId: String,
        content: String,
        parameters: GenerationParameters?
    ): Flow<ChatResponse> = flow {
        
        val conversationState = getOrLoadConversationState(conversationId)
            ?: throw ConversationException("Conversation not found: $conversationId")
        
        val genParams = parameters ?: GenerationParameters()
        
        try {
            // Create user message
            val userMessage = ChatMessage(
                id = generateMessageId(),
                conversationId = conversationId,
                content = content,
                role = MessageRole.USER,
                timestamp = System.currentTimeMillis(),
                metadata = emptyMap()
            )
            
            // Save user message
            conversationDao.insertMessage(userMessage.toEntity())
            conversationState.messages.add(userMessage)
            
            emit(ChatResponse.MessageReceived(userMessage))
            
            // Start assistant response
            val assistantMessageId = generateMessageId()
            var assistantContent = ""
            var tokenCount = 0
            val responseStartTime = System.currentTimeMillis()
            
            emit(ChatResponse.ResponseStarted(conversationId))
            
            // Generate response through inference session
            inferenceSession.generateResponse(conversationId, content, genParams)
                .collect { inferenceResult ->
                    when (inferenceResult) {
                        is InferenceResult.GenerationStarted -> {
                            // Response generation started
                        }
                        
                        is InferenceResult.TokenGenerated -> {
                            assistantContent = inferenceResult.partialText
                            tokenCount = inferenceResult.tokenIndex
                            
                            emit(ChatResponse.PartialResponse(
                                conversationId = conversationId,
                                messageId = assistantMessageId,
                                partialContent = assistantContent,
                                tokenCount = tokenCount
                            ))
                        }
                        
                        is InferenceResult.GenerationCompleted -> {
                            assistantContent = inferenceResult.fullText
                            tokenCount = inferenceResult.tokenCount
                            
                            // Create complete assistant message
                            val assistantMessage = ChatMessage(
                                id = assistantMessageId,
                                conversationId = conversationId,
                                content = assistantContent,
                                role = MessageRole.ASSISTANT,
                                timestamp = System.currentTimeMillis(),
                                metadata = mapOf(
                                    "token_count" to tokenCount.toString(),
                                    "generation_time" to inferenceResult.generationTime.toString(),
                                    "tokens_per_second" to inferenceResult.tokensPerSecond.toString(),
                                    "finish_reason" to inferenceResult.finishReason.name
                                )
                            )
                            
                            // Save assistant message
                            conversationDao.insertMessage(assistantMessage.toEntity())
                            conversationState.messages.add(assistantMessage)
                            
                            // Update conversation
                            updateConversationAfterMessage(conversationState, assistantMessage)
                            
                            emit(ChatResponse.ResponseCompleted(
                                message = assistantMessage,
                                generationTime = inferenceResult.generationTime,
                                tokensPerSecond = inferenceResult.tokensPerSecond
                            ))
                        }
                        
                        is InferenceResult.SafetyViolation -> {
                            emit(ChatResponse.SafetyViolation(
                                conversationId = conversationId,
                                reason = inferenceResult.reason
                            ))
                        }
                        
                        is InferenceResult.Error -> {
                            emit(ChatResponse.Error(
                                conversationId = conversationId,
                                error = inferenceResult.error,
                                cause = inferenceResult.cause
                            ))
                        }
                    }
                }
                
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            emit(ChatResponse.Error(
                conversationId = conversationId,
                error = "Failed to send message: ${e.message}",
                cause = e
            ))
        }
    }
    
    override suspend fun getConversation(conversationId: String): Conversation? {
        return try {
            conversationDao.getConversationById(conversationId)?.toDomain()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get conversation", e)
            null
        }
    }
    
    override suspend fun getConversationHistory(
        conversationId: String,
        limit: Int,
        offset: Int
    ): List<ChatMessage> {
        return try {
            conversationDao.getMessagesByConversationId(conversationId, limit, offset)
                .map { it.toDomain() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get conversation history", e)
            emptyList()
        }
    }
    
    override suspend fun getAllConversations(): List<Conversation> {
        return try {
            conversationDao.getAllConversations()
                .map { it.toDomain() }
                .sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all conversations", e)
            emptyList()
        }
    }
    
    override suspend fun updateConversationTitle(
        conversationId: String,
        newTitle: String
    ): Boolean {
        return try {
            conversationDao.updateConversationTitle(conversationId, newTitle)
            
            // Update active conversation if loaded
            activeConversations[conversationId]?.let { state ->
                state.conversation = state.conversation.copy(title = newTitle)
            }
            
            eventBus.emit(IrisEvent.ConversationUpdated(conversationId))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update conversation title", e)
            false
        }
    }
    
    override suspend fun deleteConversation(conversationId: String): Boolean {
        return try {
            // Close inference session
            inferenceSession.closeSession(conversationId)
            
            // Remove from active conversations
            activeConversations.remove(conversationId)
            
            // Delete from database
            conversationDao.deleteConversation(conversationId)
            
            eventBus.emit(IrisEvent.ConversationDeleted(conversationId))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete conversation", e)
            false
        }
    }
    
    override suspend fun clearAllConversations(): Boolean {
        return try {
            // Close all inference sessions
            inferenceSession.closeAllSessions()
            
            // Clear active conversations
            activeConversations.clear()
            
            // Clear database
            conversationDao.deleteAllConversations()
            
            eventBus.emit(IrisEvent.AllConversationsCleared())
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all conversations", e)
            false
        }
    }
    
    override suspend fun exportConversation(conversationId: String): ConversationExport? {
        return try {
            val conversation = getConversation(conversationId) ?: return null
            val messages = getConversationHistory(conversationId, Int.MAX_VALUE, 0)
            
            ConversationExport(
                conversation = conversation,
                messages = messages,
                exportedAt = System.currentTimeMillis(),
                version = "1.0"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export conversation", e)
            null
        }
    }
    
    // Private helper methods
    
    private suspend fun getOrLoadConversationState(conversationId: String): ConversationState? {
        // Return if already active
        activeConversations[conversationId]?.let { return it }
        
        // Load from database
        val conversation = getConversation(conversationId) ?: return null
        val messages = getConversationHistory(conversationId, 50, 0) // Load recent messages
        
        val state = ConversationState(
            conversation = conversation,
            messages = messages.toMutableList()
        )
        
        activeConversations[conversationId] = state
        return state
    }
    
    private suspend fun updateConversationAfterMessage(
        state: ConversationState,
        message: ChatMessage
    ) {
        val updatedConversation = state.conversation.copy(
            updatedAt = message.timestamp,
            messageCount = state.conversation.messageCount + 1
        )
        
        state.conversation = updatedConversation
        
        // Update in database
        conversationDao.updateConversation(updatedConversation.toEntity())
        
        // Clean up old conversations if needed
        cleanupOldConversationsIfNeeded()
    }
    
    private suspend fun cleanupOldConversationsIfNeeded() {
        val conversationCount = conversationDao.getConversationCount()
        
        if (conversationCount > MAX_CONVERSATIONS) {
            val toDelete = conversationCount - MAX_CONVERSATIONS
            val oldestConversations = conversationDao.getOldestConversations(toDelete)
            
            oldestConversations.forEach { conversation ->
                deleteConversation(conversation.id)
            }
            
            Log.i(TAG, "Cleaned up $toDelete old conversations")
        }
    }
    
    private fun generateConversationId(): String {
        return "conv_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

// Data classes for conversation management
data class ConversationState(
    var conversation: Conversation,
    val messages: MutableList<ChatMessage>
)

data class Conversation(
    val id: String,
    val title: String,
    val modelId: String,
    val systemPrompt: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int,
    val isActive: Boolean
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val content: String,
    val role: MessageRole,
    val timestamp: Long,
    val metadata: Map<String, String>
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

data class ConversationExport(
    val conversation: Conversation,
    val messages: List<ChatMessage>,
    val exportedAt: Long,
    val version: String
)

// Chat response sealed class
sealed class ChatResponse {
    data class MessageReceived(val message: ChatMessage) : ChatResponse()
    data class ResponseStarted(val conversationId: String) : ChatResponse()
    data class PartialResponse(
        val conversationId: String,
        val messageId: String,
        val partialContent: String,
        val tokenCount: Int
    ) : ChatResponse()
    data class ResponseCompleted(
        val message: ChatMessage,
        val generationTime: Long,
        val tokensPerSecond: Double
    ) : ChatResponse()
    data class SafetyViolation(
        val conversationId: String,
        val reason: String
    ) : ChatResponse()
    data class Error(
        val conversationId: String,
        val error: String,
        val cause: Throwable? = null
    ) : ChatResponse()
}

class ConversationException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

## 🧪 Testing Strategy

### Unit Tests
- [ ] **Inference Engine Logic**
  - Model loading and unloading
  - Parameter adaptation for devices
  - Session management
  - Error handling scenarios

### Integration Tests
- [ ] **End-to-End Chat Flow**
  - Complete conversation cycle
  - Streaming response handling
  - Context management
  - Safety integration

### Performance Tests
- [ ] **Inference Performance**
  - Token generation speed
  - Memory usage under load
  - Thermal behavior
  - Context window management

### UI Tests
- [ ] **Chat Interface**
  - Message display and scrolling
  - Streaming text animation
  - Error state handling
  - Performance feedback

## ✅ Acceptance Criteria

### Primary Criteria
- [ ] **Streaming Chat**: Real-time token streaming with smooth UI updates
- [ ] **Context Management**: Proper conversation context and memory handling
- [ ] **Performance Adaptation**: Dynamic quality adjustments based on device state
- [ ] **Safety Integration**: Content filtering and safety checks work correctly
- [ ] **Session Management**: Robust session handling with error recovery

### Technical Criteria
- [ ] **Response Latency**: First token in <2 seconds on mid-range devices
- [ ] **Streaming Performance**: Consistent token generation speed
- [ ] **Memory Efficiency**: Peak memory usage <500MB during inference
- [ ] **Context Preservation**: Conversations maintain context across sessions

### User Experience Criteria
- [ ] **Responsive Interface**: Smooth chat experience with no blocking
- [ ] **Error Handling**: Clear error messages and recovery options
- [ ] **Performance Feedback**: Visible performance indicators
- [ ] **Conversation Management**: Easy conversation creation and management

## 🔗 Related Issues
- **Depends on**: #01 (Core Architecture), #02 (Native llama.cpp), #04 (Model Management)
- **Enables**: #06 (RAG Engine), #10 (Safety Engine), #14 (UI/UX Implementation)
- **Related**: #03 (Hardware Detection), #12 (Performance Optimization)

## 📋 Definition of Done
- [ ] Complete chat engine with streaming inference
- [ ] Conversation state management with persistence
- [ ] Adaptive performance controls based on device capabilities
- [ ] Safety integration with content filtering
- [ ] Comprehensive test suite covering all scenarios
- [ ] Performance benchmarks meet acceptance criteria
- [ ] Chat UI components functional with real-time updates
- [ ] Documentation complete with API examples
- [ ] Code review completed and approved

---

**Note**: This chat engine serves as the core conversational interface and must be optimized for device-specific performance while maintaining high quality user experience.