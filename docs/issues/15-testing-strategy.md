# Issue #15: Comprehensive Testing Strategy & CI/CD Pipeline

## 🎯 Epic: Production-Ready Testing Infrastructure
**Priority**: P1 (High)  
**Estimate**: 10-12 days  
**Dependencies**: All previous issues (#00-#14)  
**Architecture Reference**: [docs/architecture.md](../architecture.md) - Section 15 Testing & Quality Assurance

## 📋 Overview
Implement comprehensive testing strategy and CI/CD pipeline for iris_android to ensure production-ready quality across all components. This system provides multi-layered testing coverage, automated quality gates, performance validation, and continuous integration/deployment pipeline for reliable software delivery.

## 🎯 Goals
- **Comprehensive Test Coverage**: Multi-layered testing strategy covering unit, integration, UI, and E2E tests
- **Automated Quality Gates**: Continuous integration with automated testing and quality checks
- **Performance Validation**: Automated performance testing and regression detection
- **Security Testing**: Security vulnerability scanning and penetration testing
- **Cross-Device Testing**: Validation across multiple Android versions and device configurations
- **Production Monitoring**: Real-time monitoring and alerting for production deployments

## 📝 Detailed Tasks

### 1. Unit Testing Framework

#### 1.1 Core Unit Testing Setup
Create `app/src/test/kotlin/TestConfiguration.kt`:

```kotlin
object TestConfiguration {
    
    // Test constants
    const val TIMEOUT_SHORT = 1000L
    const val TIMEOUT_MEDIUM = 5000L
    const val TIMEOUT_LONG = 10000L
    
    // Mock data constants
    const val MOCK_MODEL_SIZE = 1024L * 1024L * 100L // 100MB
    const val MOCK_CONVERSATION_COUNT = 50
    const val MOCK_MESSAGE_COUNT = 100
    
    // Test device profiles
    val LOW_END_DEVICE = DeviceProfile(
        performanceClass = 1,
        cpuCores = 4,
        totalMemoryMB = 2048,
        availableMemoryMB = 1024,
        hasGPU = false,
        gpuPerformanceClass = 0,
        hasFloat16 = false,
        supportedInstructionSets = listOf("arm64-v8a"),
        deviceModel = "Test Low-End Device",
        androidVersion = 29
    )
    
    val MID_RANGE_DEVICE = DeviceProfile(
        performanceClass = 2,
        cpuCores = 6,
        totalMemoryMB = 4096,
        availableMemoryMB = 2048,
        hasGPU = true,
        gpuPerformanceClass = 2,
        hasFloat16 = true,
        supportedInstructionSets = listOf("arm64-v8a"),
        deviceModel = "Test Mid-Range Device",
        androidVersion = 31
    )
    
    val HIGH_END_DEVICE = DeviceProfile(
        performanceClass = 3,
        cpuCores = 8,
        totalMemoryMB = 8192,
        availableMemoryMB = 4096,
        hasGPU = true,
        gpuPerformanceClass = 3,
        hasFloat16 = true,
        supportedInstructionSets = listOf("arm64-v8a"),
        deviceModel = "Test High-End Device",
        androidVersion = 34
    )
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseUnitTest {
    
    protected lateinit var testScope: TestScope
    protected lateinit var testDispatcher: TestDispatcher
    
    @BeforeAll
    fun setUpBase() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        Dispatchers.setMain(testDispatcher)
    }
    
    @AfterAll
    fun tearDownBase() {
        Dispatchers.resetMain()
    }
    
    @BeforeEach
    fun setUpTest() {
        // Override in subclasses for test-specific setup
    }
    
    @AfterEach
    fun tearDownTest() {
        // Override in subclasses for test-specific cleanup
    }
    
    protected fun runTest(block: suspend TestScope.() -> Unit) {
        testScope.runTest(timeout = TestConfiguration.TIMEOUT_MEDIUM.milliseconds) {
            block()
        }
    }
    
    protected fun createMockContext(): Context {
        return mockk<Context>(relaxed = true)
    }
    
    protected fun createMockApplication(): Application {
        return mockk<Application>(relaxed = true)
    }
}

// Core module testing utilities
class CoreTestUtils {
    
    companion object {
        fun createMockChatMessage(
            id: String = "test_${UUID.randomUUID()}",
            content: String = "Test message content",
            sender: MessageSender = MessageSender.USER,
            timestamp: Long = System.currentTimeMillis()
        ): ChatMessage {
            return ChatMessage(
                id = id,
                content = content,
                sender = sender,
                timestamp = timestamp
            )
        }
        
        fun createMockConversation(
            id: String = "conv_${UUID.randomUUID()}",
            title: String = "Test Conversation",
            messageCount: Int = 5
        ): Conversation {
            val messages = (1..messageCount).map { index ->
                createMockChatMessage(
                    id = "msg_$index",
                    content = "Test message $index",
                    sender = if (index % 2 == 0) MessageSender.USER else MessageSender.AI
                )
            }
            return Conversation(
                id = id,
                title = title,
                messages = messages,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        
        fun createMockModelInfo(
            id: String = "model_${UUID.randomUUID()}",
            name: String = "Test Model",
            size: Long = TestConfiguration.MOCK_MODEL_SIZE,
            version: String = "1.0.0"
        ): ModelInfo {
            return ModelInfo(
                id = id,
                name = name,
                size = size,
                version = version,
                type = ModelType.CHAT,
                capabilities = listOf(ModelCapability.TEXT_GENERATION),
                requiredMemory = size / 2,
                supportedPrecisions = listOf(InferencePrecision.FLOAT16, InferencePrecision.INT8),
                metadata = mapOf("test" to "true")
            )
        }
        
        fun assertPerformanceMetrics(
            actualLatency: Long,
            expectedMaxLatency: Long,
            operation: String
        ) {
            if (actualLatency > expectedMaxLatency) {
                fail("Performance test failed for $operation: " +
                     "actual=$actualLatency ms, expected<=$expectedMaxLatency ms")
            }
        }
        
        fun assertMemoryUsage(
            actualMemoryMB: Long,
            maxAllowedMemoryMB: Long,
            operation: String
        ) {
            if (actualMemoryMB > maxAllowedMemoryMB) {
                fail("Memory test failed for $operation: " +
                     "actual=$actualMemoryMB MB, max=$maxAllowedMemoryMB MB")
            }
        }
    }
}
```

#### 1.2 Model Management Tests
Create `app/src/test/kotlin/core/model/ModelManagerTest.kt`:

```kotlin
@ExtendWith(MockKExtension::class)
class ModelManagerTest : BaseUnitTest() {
    
    @MockK
    private lateinit var modelStorage: ModelStorage
    
    @MockK
    private lateinit var deviceProfileProvider: DeviceProfileProvider
    
    @MockK
    private lateinit var eventBus: EventBus
    
    @MockK
    private lateinit var context: Context
    
    private lateinit var modelManager: ModelManagerImpl
    
    @BeforeEach
    override fun setUpTest() {
        super.setUpTest()
        
        // Setup common mocks
        every { deviceProfileProvider.getDeviceProfile() } returns TestConfiguration.MID_RANGE_DEVICE
        every { eventBus.emit(any()) } returns Unit
        
        modelManager = ModelManagerImpl(
            modelStorage = modelStorage,
            deviceProfileProvider = deviceProfileProvider,
            eventBus = eventBus,
            context = context
        )
    }
    
    @Test
    fun `initialize should succeed with valid configuration`() = runTest {
        // Arrange
        coEvery { modelStorage.initialize() } returns Result.success(Unit)
        coEvery { modelStorage.getAvailableModels() } returns Result.success(emptyList())
        
        // Act
        val result = modelManager.initialize()
        
        // Assert
        assertTrue(result.isSuccess)
        verify { eventBus.emit(ofType<IrisEvent.ModelManagerInitialized>()) }
    }
    
    @Test
    fun `initialize should fail when storage initialization fails`() = runTest {
        // Arrange
        val storageError = Exception("Storage initialization failed")
        coEvery { modelStorage.initialize() } returns Result.failure(storageError)
        
        // Act
        val result = modelManager.initialize()
        
        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ModelException)
    }
    
    @Test
    fun `loadModel should succeed for valid model on capable device`() = runTest {
        // Arrange
        val modelInfo = CoreTestUtils.createMockModelInfo()
        val mockModelData = ByteArray(1024)
        
        coEvery { modelStorage.loadModelData(modelInfo.id) } returns Result.success(mockModelData)
        coEvery { modelStorage.getModelInfo(modelInfo.id) } returns Result.success(modelInfo)
        
        // Act
        val startTime = System.currentTimeMillis()
        val result = modelManager.loadModel(modelInfo.id)
        val endTime = System.currentTimeMillis()
        
        // Assert
        assertTrue(result.isSuccess)
        val loadedModel = result.getOrNull()
        assertNotNull(loadedModel)
        assertEquals(modelInfo.id, loadedModel!!.id)
        
        // Performance assertion
        CoreTestUtils.assertPerformanceMetrics(
            actualLatency = endTime - startTime,
            expectedMaxLatency = 5000L, // 5 seconds max for model loading
            operation = "model loading"
        )
        
        verify { eventBus.emit(ofType<IrisEvent.ModelLoaded>()) }
    }
    
    @Test
    fun `loadModel should fail for insufficient memory`() = runTest {
        // Arrange
        val largeModelInfo = CoreTestUtils.createMockModelInfo(
            size = 10L * 1024L * 1024L * 1024L // 10GB - larger than test device memory
        )
        
        coEvery { modelStorage.getModelInfo(largeModelInfo.id) } returns Result.success(largeModelInfo)
        
        // Act
        val result = modelManager.loadModel(largeModelInfo.id)
        
        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ModelException)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("insufficient memory"))
    }
    
    @Test
    fun `unloadModel should free memory correctly`() = runTest {
        // Arrange
        val modelInfo = CoreTestUtils.createMockModelInfo()
        val mockModelData = ByteArray(1024)
        
        coEvery { modelStorage.loadModelData(modelInfo.id) } returns Result.success(mockModelData)
        coEvery { modelStorage.getModelInfo(modelInfo.id) } returns Result.success(modelInfo)
        
        // Load model first
        val loadResult = modelManager.loadModel(modelInfo.id)
        assertTrue(loadResult.isSuccess)
        
        // Act
        val unloadResult = modelManager.unloadModel(modelInfo.id)
        
        // Assert
        assertTrue(unloadResult.isSuccess)
        verify { eventBus.emit(ofType<IrisEvent.ModelUnloaded>()) }
        
        // Verify model is no longer loaded
        val currentModels = modelManager.getLoadedModels()
        assertFalse(currentModels.any { it.id == modelInfo.id })
    }
    
    @Test
    fun `getRecommendedModels should return appropriate models for device`() = runTest {
        // Arrange
        val lowEndModel = CoreTestUtils.createMockModelInfo(
            id = "low_end",
            size = 100L * 1024L * 1024L // 100MB
        )
        val highEndModel = CoreTestUtils.createMockModelInfo(
            id = "high_end", 
            size = 2L * 1024L * 1024L * 1024L // 2GB
        )
        
        coEvery { modelStorage.getAvailableModels() } returns Result.success(
            listOf(lowEndModel, highEndModel)
        )
        
        // Test for low-end device
        every { deviceProfileProvider.getDeviceProfile() } returns TestConfiguration.LOW_END_DEVICE
        
        // Act
        val recommendations = modelManager.getRecommendedModels()
        
        // Assert
        assertTrue(recommendations.isSuccess)
        val models = recommendations.getOrNull()!!
        assertTrue(models.any { it.id == "low_end" })
        assertFalse(models.any { it.id == "high_end" })
    }
    
    @Test
    fun `concurrent model operations should be handled safely`() = runTest {
        // Arrange
        val modelInfo1 = CoreTestUtils.createMockModelInfo(id = "model1")
        val modelInfo2 = CoreTestUtils.createMockModelInfo(id = "model2")
        val mockModelData = ByteArray(1024)
        
        coEvery { modelStorage.loadModelData(any()) } returns Result.success(mockModelData)
        coEvery { modelStorage.getModelInfo(any()) } returns Result.success(modelInfo1)
        
        // Act - Launch concurrent load operations
        val job1 = async { modelManager.loadModel("model1") }
        val job2 = async { modelManager.loadModel("model2") }
        
        val result1 = job1.await()
        val result2 = job2.await()
        
        // Assert - Both operations should complete successfully
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        
        // Verify thread safety - no corruption of internal state
        val loadedModels = modelManager.getLoadedModels()
        assertEquals(2, loadedModels.size)
    }
}
```

#### 1.3 Chat Engine Tests
Create `app/src/test/kotlin/core/chat/ChatEngineTest.kt`:

```kotlin
@ExtendWith(MockKExtension::class)
class ChatEngineTest : BaseUnitTest() {
    
    @MockK
    private lateinit var modelManager: ModelManager
    
    @MockK
    private lateinit var conversationManager: ConversationManager
    
    @MockK
    private lateinit var performanceManager: PerformanceManager
    
    @MockK
    private lateinit var eventBus: EventBus
    
    private lateinit var chatEngine: ChatEngineImpl
    
    @BeforeEach
    override fun setUpTest() {
        super.setUpTest()
        
        every { eventBus.emit(any()) } returns Unit
        
        chatEngine = ChatEngineImpl(
            modelManager = modelManager,
            conversationManager = conversationManager,
            performanceManager = performanceManager,
            eventBus = eventBus
        )
    }
    
    @Test
    fun `sendMessage should generate response successfully`() = runTest {
        // Arrange
        val conversationId = "test_conversation"
        val userMessage = "Hello, AI!"
        val expectedResponse = "Hello! How can I help you today?"
        
        val mockModel = mockk<LoadedModel>()
        every { mockModel.id } returns "test_model"
        
        coEvery { modelManager.getActiveModel() } returns mockModel
        coEvery { conversationManager.getConversation(conversationId) } returns 
            Result.success(CoreTestUtils.createMockConversation(id = conversationId))
        
        // Mock inference response
        val inferenceResponse = InferenceResponse(
            content = expectedResponse,
            confidence = 0.95f,
            latency = 1000L,
            metadata = mapOf("tokens_generated" to "10")
        )
        
        every { runBlocking { mockModel.generateResponse(any()) } } returns 
            Result.success(inferenceResponse)
        
        coEvery { conversationManager.addMessage(any(), any()) } returns Result.success(Unit)
        
        // Act
        val startTime = System.currentTimeMillis()
        val result = chatEngine.sendMessage(conversationId, userMessage)
        val endTime = System.currentTimeMillis()
        
        // Assert
        assertTrue(result.isSuccess)
        val response = result.getOrNull()!!
        assertEquals(expectedResponse, response.content)
        assertTrue(response.confidence > 0.9f)
        
        // Performance assertion
        CoreTestUtils.assertPerformanceMetrics(
            actualLatency = endTime - startTime,
            expectedMaxLatency = 3000L, // 3 seconds max for response generation
            operation = "message generation"
        )
        
        verify { eventBus.emit(ofType<IrisEvent.MessageSent>()) }
        verify { eventBus.emit(ofType<IrisEvent.ResponseGenerated>()) }
    }
    
    @Test
    fun `sendMessage should handle model generation failure gracefully`() = runTest {
        // Arrange
        val conversationId = "test_conversation"
        val userMessage = "Test message"
        
        val mockModel = mockk<LoadedModel>()
        every { mockModel.id } returns "test_model"
        
        coEvery { modelManager.getActiveModel() } returns mockModel
        coEvery { conversationManager.getConversation(conversationId) } returns 
            Result.success(CoreTestUtils.createMockConversation(id = conversationId))
        
        // Mock inference failure
        every { runBlocking { mockModel.generateResponse(any()) } } returns 
            Result.failure(InferenceException("Model inference failed"))
        
        // Act
        val result = chatEngine.sendMessage(conversationId, userMessage)
        
        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ChatException)
        verify { eventBus.emit(ofType<IrisEvent.MessageSent>()) }
        verify { eventBus.emit(ofType<IrisEvent.ResponseGenerationFailed>()) }
    }
    
    @Test
    fun `streaming response should emit updates progressively`() = runTest {
        // Arrange
        val conversationId = "test_conversation"
        val userMessage = "Generate a long response"
        val streamingTokens = listOf("Hello", " there", "!", " How", " can", " I", " help", "?")
        
        val mockModel = mockk<LoadedModel>()
        every { mockModel.id } returns "test_model"
        
        coEvery { modelManager.getActiveModel() } returns mockModel
        coEvery { conversationManager.getConversation(conversationId) } returns 
            Result.success(CoreTestUtils.createMockConversation(id = conversationId))
        
        // Mock streaming response
        val streamingFlow = flow {
            streamingTokens.forEachIndexed { index, token ->
                emit(StreamingToken(
                    token = token,
                    accumulated = streamingTokens.take(index + 1).joinToString(""),
                    isComplete = index == streamingTokens.size - 1
                ))
                delay(100) // Simulate streaming delay
            }
        }
        
        every { runBlocking { mockModel.generateStreamingResponse(any()) } } returns streamingFlow
        
        // Act
        val responses = mutableListOf<StreamingResponse>()
        val result = chatEngine.sendStreamingMessage(conversationId, userMessage)
        
        result.getOrNull()?.collect { response ->
            responses.add(response)
        }
        
        // Assert
        assertEquals(streamingTokens.size, responses.size)
        assertTrue(responses.last().isComplete)
        assertEquals(streamingTokens.joinToString(""), responses.last().content)
        
        // Verify progressive updates
        responses.forEachIndexed { index, response ->
            assertEquals(streamingTokens.take(index + 1).joinToString(""), response.content)
        }
    }
    
    @Test
    fun `conversation context should be managed correctly`() = runTest {
        // Arrange
        val conversationId = "test_conversation"
        val messages = listOf(
            "What is the capital of France?",
            "Tell me more about Paris",
            "What's the population?"
        )
        
        val mockModel = mockk<LoadedModel>()
        val mockConversation = CoreTestUtils.createMockConversation(id = conversationId)
        
        coEvery { modelManager.getActiveModel() } returns mockModel
        coEvery { conversationManager.getConversation(conversationId) } returns 
            Result.success(mockConversation)
        
        // Mock responses that use context
        val responses = listOf(
            "Paris is the capital of France.",
            "Paris is known for the Eiffel Tower and rich culture.",
            "Paris has approximately 2.1 million people."
        )
        
        messages.forEachIndexed { index, message ->
            every { runBlocking { mockModel.generateResponse(any()) } } returns
                Result.success(InferenceResponse(
                    content = responses[index],
                    confidence = 0.9f,
                    latency = 1000L
                ))
            
            coEvery { conversationManager.addMessage(any(), any()) } returns Result.success(Unit)
            
            // Act
            val result = chatEngine.sendMessage(conversationId, message)
            
            // Assert
            assertTrue(result.isSuccess)
            assertEquals(responses[index], result.getOrNull()!!.content)
        }
        
        // Verify context was passed correctly (conversation history included)
        verify(exactly = 3) { 
            runBlocking { 
                mockModel.generateResponse(match { request ->
                    request.conversationHistory.isNotEmpty() || request.message == messages[0]
                })
            }
        }
    }
    
    @Test
    fun `performance optimization should be applied based on device capabilities`() = runTest {
        // Arrange
        val conversationId = "test_conversation"
        val userMessage = "Test message"
        
        val optimalConfig = InferenceConfig(
            threads = 4,
            batchSize = 1,
            contextLength = 2048,
            precision = InferencePrecision.FLOAT16,
            useGPU = true,
            enableCaching = true
        )
        
        coEvery { performanceManager.getOptimalInferenceConfig(any()) } returns optimalConfig
        
        val mockModel = mockk<LoadedModel>()
        coEvery { modelManager.getActiveModel() } returns mockModel
        coEvery { conversationManager.getConversation(conversationId) } returns 
            Result.success(CoreTestUtils.createMockConversation(id = conversationId))
        
        every { runBlocking { mockModel.generateResponse(any()) } } returns 
            Result.success(InferenceResponse("Response", 0.9f, 1000L))
        
        coEvery { conversationManager.addMessage(any(), any()) } returns Result.success(Unit)
        
        // Act
        val result = chatEngine.sendMessage(conversationId, userMessage)
        
        // Assert
        assertTrue(result.isSuccess)
        
        // Verify performance optimization was requested
        verify { 
            runBlocking { 
                performanceManager.getOptimalInferenceConfig(any())
            }
        }
        
        // Verify optimal config was applied to model
        verify { 
            runBlocking { 
                mockModel.generateResponse(match { request ->
                    request.config == optimalConfig
                })
            }
        }
    }
}
```

### 2. Integration Testing

#### 2.1 System Integration Tests
Create `app/src/test/kotlin/integration/SystemIntegrationTest.kt`:

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SystemIntegrationTest {
    
    private lateinit var testApplication: TestApplication
    private lateinit var testDatabase: TestDatabase
    private lateinit var testScope: TestScope
    
    @BeforeAll
    fun setUpIntegration() {
        testScope = TestScope()
        testApplication = TestApplication()
        testDatabase = TestDatabase()
        
        // Initialize test environment
        testDatabase.initialize()
        testApplication.initialize()
    }
    
    @AfterAll
    fun tearDownIntegration() {
        testDatabase.cleanup()
        testApplication.cleanup()
    }
    
    @Test
    fun `full conversation flow should work end-to-end`() = testScope.runTest {
        // Arrange
        val modelId = "test_model"
        val conversationId = "integration_test_conversation"
        val userMessage = "Hello, can you help me with a math problem?"
        
        // Initialize model manager
        val modelManager = testApplication.getModelManager()
        val chatEngine = testApplication.getChatEngine()
        val conversationManager = testApplication.getConversationManager()
        
        // Load a test model
        val modelLoadResult = modelManager.loadModel(modelId)
        assertTrue(modelLoadResult.isSuccess, "Model loading should succeed")
        
        // Create a new conversation
        val conversationResult = conversationManager.createConversation("Math Help")
        assertTrue(conversationResult.isSuccess, "Conversation creation should succeed")
        val conversation = conversationResult.getOrNull()!!
        
        // Act - Send message and get response
        val responseResult = chatEngine.sendMessage(conversation.id, userMessage)
        
        // Assert
        assertTrue(responseResult.isSuccess, "Message should be processed successfully")
        val response = responseResult.getOrNull()!!
        
        assertNotNull(response.content)
        assertTrue(response.content.isNotEmpty())
        assertTrue(response.confidence > 0.0f)
        
        // Verify conversation was updated
        val updatedConversation = conversationManager.getConversation(conversation.id)
        assertTrue(updatedConversation.isSuccess)
        val messages = updatedConversation.getOrNull()!!.messages
        
        assertEquals(2, messages.size) // User message + AI response
        assertEquals(userMessage, messages[0].content)
        assertEquals(response.content, messages[1].content)
    }
    
    @Test
    fun `model switching should preserve conversation context`() = testScope.runTest {
        // Arrange
        val model1Id = "model_small"
        val model2Id = "model_large"
        val conversationId = "context_test_conversation"
        
        val modelManager = testApplication.getModelManager()
        val chatEngine = testApplication.getChatEngine()
        val conversationManager = testApplication.getConversationManager()
        
        // Create conversation and send first message with model 1
        val conversation = conversationManager.createConversation("Context Test").getOrNull()!!
        modelManager.loadModel(model1Id)
        modelManager.setActiveModel(model1Id)
        
        val firstResponse = chatEngine.sendMessage(conversation.id, "My name is Alice").getOrNull()!!
        
        // Switch to model 2
        modelManager.loadModel(model2Id)
        modelManager.setActiveModel(model2Id)
        
        // Act - Send follow-up message that requires context
        val secondResponse = chatEngine.sendMessage(conversation.id, "What's my name?").getOrNull()!!
        
        // Assert - Second model should have access to conversation history
        assertTrue(secondResponse.content.contains("Alice", ignoreCase = true))
        
        // Verify conversation integrity
        val finalConversation = conversationManager.getConversation(conversation.id).getOrNull()!!
        assertEquals(4, finalConversation.messages.size) // 2 user + 2 AI messages
    }
    
    @Test
    fun `memory pressure should trigger appropriate responses`() = testScope.runTest {
        // Arrange
        val memoryManager = testApplication.getMemoryManager()
        val performanceManager = testApplication.getPerformanceManager()
        val chatEngine = testApplication.getChatEngine()
        
        // Simulate high memory usage
        val largeAllocation = memoryManager.allocateMemory(
            size = 500 * 1024 * 1024, // 500MB
            type = ResourceType.GENERAL,
            priority = AllocationPriority.HIGH,
            tag = "test_allocation"
        ).getOrNull()!!
        
        // Act - Trigger memory optimization
        val optimizationResult = memoryManager.optimizeMemoryUsage()
        
        // Assert
        assertTrue(optimizationResult.success)
        assertTrue(optimizationResult.freedMemory > 0)
        
        // Verify performance mode adjusted
        val performanceMetrics = performanceManager.getCurrentPerformanceMetrics()
        assertNotNull(performanceMetrics)
        
        // Clean up
        memoryManager.deallocateMemory(largeAllocation.id)
    }
    
    @Test
    fun `concurrent operations should be handled safely`() = testScope.runTest {
        // Arrange
        val chatEngine = testApplication.getChatEngine()
        val conversationManager = testApplication.getConversationManager()
        val modelManager = testApplication.getModelManager()
        
        modelManager.loadModel("concurrent_test_model")
        
        val conversations = (1..5).map { index ->
            conversationManager.createConversation("Concurrent Test $index").getOrNull()!!
        }
        
        // Act - Send concurrent messages to different conversations
        val jobs = conversations.map { conversation ->
            async {
                chatEngine.sendMessage(conversation.id, "Concurrent message to ${conversation.title}")
            }
        }
        
        val results = jobs.awaitAll()
        
        // Assert - All operations should succeed
        results.forEach { result ->
            assertTrue(result.isSuccess, "Concurrent operation should succeed")
        }
        
        // Verify data integrity
        conversations.forEach { conversation ->
            val updated = conversationManager.getConversation(conversation.id).getOrNull()!!
            assertEquals(2, updated.messages.size) // User + AI message
        }
    }
    
    @Test
    fun `error recovery should restore system to stable state`() = testScope.runTest {
        // Arrange
        val modelManager = testApplication.getModelManager()
        val chatEngine = testApplication.getChatEngine()
        
        // Cause a model error by loading invalid model
        val invalidModelResult = modelManager.loadModel("invalid_model")
        assertTrue(invalidModelResult.isFailure)
        
        // System should still be functional
        val validModelResult = modelManager.loadModel("valid_test_model")
        assertTrue(validModelResult.isSuccess)
        
        // Act - Normal operations should work
        val conversationManager = testApplication.getConversationManager()
        val conversation = conversationManager.createConversation("Recovery Test").getOrNull()!!
        val response = chatEngine.sendMessage(conversation.id, "Test recovery").getOrNull()!!
        
        // Assert
        assertNotNull(response.content)
        assertTrue(response.content.isNotEmpty())
    }
}

// Test application and utilities
class TestApplication {
    private val serviceLocator = TestServiceLocator()
    
    fun initialize() {
        // Initialize test dependencies
        serviceLocator.initialize()
    }
    
    fun cleanup() {
        serviceLocator.cleanup()
    }
    
    fun getModelManager(): ModelManager = serviceLocator.modelManager
    fun getChatEngine(): ChatEngine = serviceLocator.chatEngine
    fun getConversationManager(): ConversationManager = serviceLocator.conversationManager
    fun getMemoryManager(): MemoryManager = serviceLocator.memoryManager
    fun getPerformanceManager(): PerformanceManager = serviceLocator.performanceManager
}

class TestDatabase {
    private lateinit var database: IrisDatabase
    
    fun initialize() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            IrisDatabase::class.java
        ).build()
    }
    
    fun cleanup() {
        database.close()
    }
    
    fun getDatabase(): IrisDatabase = database
}
```

### 3. UI Testing with Compose

#### 3.1 Chat Screen UI Tests
Create `app/src/androidTest/kotlin/ui/ChatScreenTest.kt`:

```kotlin
@HiltAndroidTest
class ChatScreenTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun chatScreen_displaysCorrectly() {
        composeTestRule.setContent {
            IrisTheme {
                ChatScreen(
                    viewModel = createMockViewModel(),
                    onNavigateToSettings = {}
                )
            }
        }
        
        // Verify initial state
        composeTestRule.onNodeWithText("Iris AI").assertIsDisplayed()
        composeTestRule.onNodeWithText("Message Iris...").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Send").assertIsDisplayed()
    }
    
    @Test
    fun chatScreen_sendMessage_updatesUI() {
        val mockViewModel = createMockViewModel()
        val testMessage = "Hello, AI!"
        
        composeTestRule.setContent {
            IrisTheme {
                ChatScreen(
                    viewModel = mockViewModel,
                    onNavigateToSettings = {}
                )
            }
        }
        
        // Type message
        composeTestRule.onNodeWithText("Message Iris...")
            .performTextInput(testMessage)
        
        // Send message
        composeTestRule.onNodeWithContentDescription("Send")
            .performClick()
        
        // Verify message appears in chat
        composeTestRule.onNodeWithText(testMessage)
            .assertIsDisplayed()
        
        // Verify input is cleared
        composeTestRule.onNodeWithText("Message Iris...")
            .assertTextEquals("")
    }
    
    @Test
    fun chatScreen_voiceInput_opensVoiceInterface() {
        composeTestRule.setContent {
            IrisTheme {
                ChatScreen(
                    viewModel = createMockViewModel(),
                    onNavigateToSettings = {}
                )
            }
        }
        
        // Click voice input button
        composeTestRule.onNodeWithContentDescription("Voice Input")
            .performClick()
        
        // Verify voice interface opens (implementation dependent)
        // This would verify the voice input UI state
    }
    
    @Test
    fun chatScreen_accessibility_screenReaderSupport() {
        composeTestRule.setContent {
            IrisTheme {
                ChatScreen(
                    viewModel = createMockViewModel(),
                    onNavigateToSettings = {}
                )
            }
        }
        
        // Verify accessibility properties
        composeTestRule.onNodeWithContentDescription("Send")
            .assert(hasClickAction())
        
        composeTestRule.onNodeWithContentDescription("Voice Input")
            .assert(hasClickAction())
        
        composeTestRule.onNodeWithContentDescription("Settings")
            .assert(hasClickAction())
    }
    
    @Test
    fun chatScreen_darkTheme_rendersCorrectly() {
        composeTestRule.setContent {
            IrisTheme(darkTheme = true) {
                ChatScreen(
                    viewModel = createMockViewModel(),
                    onNavigateToSettings = {}
                )
            }
        }
        
        // Verify dark theme colors are applied
        composeTestRule.onRoot()
            .captureToImage()
            .assertColorEquals(
                expectedColor = Color.Black,
                tolerance = 0.1f
            )
    }
    
    @Test
    fun chatScreen_messageList_scrollsCorrectly() {
        val mockViewModel = createMockViewModelWithMessages(50)
        
        composeTestRule.setContent {
            IrisTheme {
                ChatScreen(
                    viewModel = mockViewModel,
                    onNavigateToSettings = {}
                )
            }
        }
        
        // Scroll to top
        composeTestRule.onNodeWithTag("MessageList")
            .performScrollToIndex(0)
        
        // Verify first message is visible
        composeTestRule.onNodeWithText("Message 1")
            .assertIsDisplayed()
        
        // Scroll to bottom
        composeTestRule.onNodeWithTag("MessageList")
            .performScrollToIndex(49)
        
        // Verify last message is visible
        composeTestRule.onNodeWithText("Message 50")
            .assertIsDisplayed()
    }
    
    @Test
    fun chatScreen_adaptiveLayout_respondsToScreenSize() {
        // Test compact layout
        composeTestRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        composeTestRule.setContent {
            IrisTheme {
                val windowSizeClass = WindowSizeClass.calculateFromSize(
                    DpSize(400.dp, 800.dp)
                )
                IrisApp(
                    viewModel = createMockViewModel(),
                    windowSizeClass = windowSizeClass
                )
            }
        }
        
        // Verify bottom navigation is visible in compact mode
        composeTestRule.onNodeWithText("Chat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Models").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        
        // Test expanded layout
        composeTestRule.setContent {
            IrisTheme {
                val windowSizeClass = WindowSizeClass.calculateFromSize(
                    DpSize(1200.dp, 800.dp)
                )
                IrisApp(
                    viewModel = createMockViewModel(),
                    windowSizeClass = windowSizeClass
                )
            }
        }
        
        // Verify navigation drawer is visible in expanded mode
        composeTestRule.onNodeWithText("Iris AI").assertIsDisplayed()
    }
    
    private fun createMockViewModel(): MainViewModel {
        return mockk<MainViewModel>(relaxed = true).apply {
            every { uiState } returns MutableStateFlow(
                MainUiState(
                    chatState = ChatState(
                        messages = emptyList(),
                        currentInput = "",
                        isGenerating = false,
                        currentModel = "Test Model"
                    ),
                    currentDestination = "chat"
                )
            ).asStateFlow()
        }
    }
    
    private fun createMockViewModelWithMessages(count: Int): MainViewModel {
        val messages = (1..count).map { index ->
            ChatMessage(
                id = "message_$index",
                content = "Message $index",
                sender = if (index % 2 == 0) MessageSender.USER else MessageSender.AI,
                timestamp = System.currentTimeMillis()
            )
        }
        
        return mockk<MainViewModel>(relaxed = true).apply {
            every { uiState } returns MutableStateFlow(
                MainUiState(
                    chatState = ChatState(
                        messages = messages,
                        currentInput = "",
                        isGenerating = false,
                        currentModel = "Test Model"
                    ),
                    currentDestination = "chat"
                )
            ).asStateFlow()
        }
    }
}
```

### 4. Performance Testing

#### 4.1 Performance Benchmarks
Create `app/src/androidTest/kotlin/performance/PerformanceBenchmarks.kt`:

```kotlin
@RunWith(AndroidJUnit4::class)
class PerformanceBenchmarks {
    
    @get:Rule
    val benchmarkRule = BenchmarkRule()
    
    @Test
    fun benchmarkModelLoading() {
        val modelManager = createTestModelManager()
        val modelInfo = CoreTestUtils.createMockModelInfo()
        
        benchmarkRule.measureRepeated {
            runBlocking {
                val result = modelManager.loadModel(modelInfo.id)
                assertTrue(result.isSuccess)
                modelManager.unloadModel(modelInfo.id)
            }
        }
    }
    
    @Test
    fun benchmarkMessageGeneration() {
        val chatEngine = createTestChatEngine()
        val conversationId = "benchmark_conversation"
        val testMessage = "Generate a response for benchmarking"
        
        benchmarkRule.measureRepeated {
            runBlocking {
                val result = chatEngine.sendMessage(conversationId, testMessage)
                assertTrue(result.isSuccess)
            }
        }
    }
    
    @Test
    fun benchmarkUIRecomposition() {
        val composeTestRule = createComposeRule()
        var counter by mutableIntStateOf(0)
        
        composeTestRule.setContent {
            MessageBubble(
                message = ChatMessage(
                    id = "test",
                    content = "Benchmark message $counter",
                    sender = MessageSender.AI,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        benchmarkRule.measureRepeated {
            composeTestRule.runOnUiThread {
                counter++
            }
            composeTestRule.waitForIdle()
        }
    }
    
    @Test
    fun benchmarkMemoryAllocation() {
        val memoryManager = createTestMemoryManager()
        
        benchmarkRule.measureRepeated {
            runBlocking {
                val allocation = memoryManager.allocateMemory(
                    size = 1024 * 1024, // 1MB
                    type = ResourceType.GENERAL,
                    priority = AllocationPriority.MEDIUM,
                    tag = "benchmark"
                )
                assertTrue(allocation.isSuccess)
                memoryManager.deallocateMemory(allocation.getOrNull()!!.id)
            }
        }
    }
    
    @Test
    fun benchmarkConversationScrolling() {
        val composeTestRule = createComposeRule()
        val messages = (1..1000).map { index ->
            ChatMessage(
                id = "msg_$index",
                content = "Message $index content for scrolling benchmark",
                sender = if (index % 2 == 0) MessageSender.USER else MessageSender.AI,
                timestamp = System.currentTimeMillis()
            )
        }
        
        composeTestRule.setContent {
            LazyColumn {
                items(messages) { message ->
                    MessageBubble(message = message)
                }
            }
        }
        
        benchmarkRule.measureRepeated {
            composeTestRule.onNodeWithTag("MessageList")
                .performScrollToIndex(999)
            
            composeTestRule.onNodeWithTag("MessageList")
                .performScrollToIndex(0)
        }
    }
    
    private fun createTestModelManager(): ModelManager {
        // Create test implementation
        return mockk<ModelManager>(relaxed = true)
    }
    
    private fun createTestChatEngine(): ChatEngine {
        // Create test implementation
        return mockk<ChatEngine>(relaxed = true)
    }
    
    private fun createTestMemoryManager(): MemoryManager {
        // Create test implementation
        return mockk<MemoryManager>(relaxed = true)
    }
}
```

### 5. CI/CD Pipeline Configuration

#### 5.1 GitHub Actions Workflow
Create `.github/workflows/ci.yml`:

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]
  release:
    types: [ published ]

env:
  GRADLE_OPTS: -Dorg.gradle.daemon=false -Dorg.gradle.workers.max=2

jobs:
  lint:
    name: Lint Check
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Cache Gradle dependencies
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Run lint
      run: ./gradlew lint
      
    - name: Upload lint results
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: lint-results
        path: app/build/reports/lint-results-*.html

  unit-tests:
    name: Unit Tests
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Cache Gradle dependencies
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Run unit tests
      run: ./gradlew testDebugUnitTest
      
    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: always()
      with:
        name: Unit Test Results
        path: '**/build/test-results/testDebugUnitTest/TEST-*.xml'
        reporter: java-junit
        
    - name: Upload test results
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: unit-test-results
        path: |
          app/build/reports/tests/testDebugUnitTest/
          app/build/test-results/testDebugUnitTest/

  instrumented-tests:
    name: Instrumented Tests
    runs-on: macos-latest
    strategy:
      matrix:
        api-level: [26, 29, 33]
        target: [default, google_apis]
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Cache Gradle dependencies
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: AVD cache
      uses: actions/cache@v3
      id: avd-cache
      with:
        path: |
          ~/.android/avd/*
          ~/.android/adb*
        key: avd-${{ matrix.api-level }}-${{ matrix.target }}
        
    - name: Create AVD and generate snapshot for caching
      if: steps.avd-cache.outputs.cache-hit != 'true'
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: ${{ matrix.api-level }}
        target: ${{ matrix.target }}
        arch: x86_64
        force-avd-creation: false
        emulator-options: -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -camera-back none
        disable-animations: false
        script: echo "Generated AVD snapshot for caching."
        
    - name: Run instrumented tests
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: ${{ matrix.api-level }}
        target: ${{ matrix.target }}
        arch: x86_64
        force-avd-creation: false
        emulator-options: -no-snapshot-save -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -camera-back none
        disable-animations: true
        script: ./gradlew connectedDebugAndroidTest
        
    - name: Upload instrumented test results
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: instrumented-test-results-${{ matrix.api-level }}-${{ matrix.target }}
        path: |
          app/build/reports/androidTests/connected/
          app/build/outputs/androidTest-results/connected/

  security-scan:
    name: Security Scan
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Run OSEA security scan
      uses: securecodewarrior/github-action-osea-scan@v1
      with:
        languages: 'kotlin,java'
        
    - name: Run dependency check
      uses: dependency-check/Dependency-Check_Action@main
      with:
        project: 'iris-android'
        path: '.'
        format: 'ALL'
        
    - name: Upload security scan results
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: security-scan-results
        path: reports/

  build:
    name: Build APK
    runs-on: ubuntu-latest
    needs: [lint, unit-tests]
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Cache Gradle dependencies
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Build debug APK
      run: ./gradlew assembleDebug
      
    - name: Upload debug APK
      uses: actions/upload-artifact@v3
      with:
        name: debug-apk
        path: app/build/outputs/apk/debug/app-debug.apk

  release:
    name: Release Build
    runs-on: ubuntu-latest
    if: github.event_name == 'release'
    needs: [lint, unit-tests, instrumented-tests, security-scan]
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Cache Gradle dependencies
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Build release AAB
      run: ./gradlew bundleRelease
      env:
        KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
        KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
        KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        
    - name: Upload release AAB
      uses: actions/upload-artifact@v3
      with:
        name: release-aab
        path: app/build/outputs/bundle/release/app-release.aab
        
    - name: Deploy to Play Store
      uses: r0adkll/upload-google-play@v1
      with:
        serviceAccountJsonPlainText: ${{ secrets.PLAY_SERVICE_ACCOUNT_JSON }}
        packageName: com.iris.android
        releaseFiles: app/build/outputs/bundle/release/app-release.aab
        track: production
        status: completed
```

## 🧪 Testing Strategy Summary

### Test Pyramid Structure
1. **Unit Tests (70%)**: Fast, isolated component testing
2. **Integration Tests (20%)**: Module interaction testing  
3. **UI Tests (10%)**: End-to-end user interaction testing

### Quality Gates
- **Code Coverage**: Minimum 80% line coverage for core modules
- **Performance**: All operations meet latency requirements
- **Accessibility**: 100% accessibility compliance
- **Security**: No high/critical vulnerabilities

### Automated Testing
- **Continuous Integration**: Every PR triggers full test suite
- **Performance Regression**: Automated performance benchmarking
- **Cross-Device Testing**: Matrix testing across Android versions
- **Security Scanning**: Automated vulnerability detection

## ✅ Acceptance Criteria

### Primary Criteria
- [ ] **Comprehensive Coverage**: 80%+ test coverage across all modules
- [ ] **Automated Pipeline**: Complete CI/CD pipeline with quality gates
- [ ] **Performance Validation**: Automated performance regression testing
- [ ] **Security Testing**: Regular security scans and vulnerability assessment
- [ ] **Cross-Device Support**: Testing across multiple Android versions and devices

### Technical Criteria
- [ ] **Test Execution Speed**: Full test suite completes in <30 minutes
- [ ] **Reliability**: Test suite has <1% flakiness rate
- [ ] **Coverage**: Unit tests achieve 80%+ line coverage
- [ ] **Performance**: Performance tests catch >95% of regressions

### Quality Criteria
- [ ] **Maintainability**: Test code follows best practices and is well-documented
- [ ] **Reliability**: Tests are deterministic and don't depend on external services
- [ ] **Efficiency**: Test suite provides fast feedback to developers
- [ ] **Completeness**: Critical user journeys are covered by E2E tests

## 🔗 Related Issues
- **Depends on**: All previous issues (#00-#14)
- **Enables**: #16 (Deployment & Release)
- **Related**: All implementation issues for testing coverage

## 📋 Definition of Done
- [ ] Complete test suite with unit, integration, and UI tests
- [ ] Automated CI/CD pipeline with quality gates
- [ ] Performance testing and benchmarking framework
- [ ] Security testing and vulnerability scanning
- [ ] Cross-device testing matrix setup
- [ ] Test coverage reporting and monitoring
- [ ] Automated test execution on every commit
- [ ] Performance regression detection system
- [ ] Documentation complete with testing guidelines
- [ ] Code review completed and approved

---

**Note**: This comprehensive testing strategy ensures production-ready quality for iris_android with automated quality gates and continuous validation across all supported Android devices.