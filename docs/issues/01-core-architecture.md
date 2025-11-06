# Issue #01: Core Architecture & Module Structure

## 🎯 Epic: Foundation Architecture
**Priority**: P0 (Blocking)  
**Estimate**: 5-7 days  
**Dependencies**: #00 (Project Foundation)  
**Architecture Reference**: [docs/architecture.md](../architecture.md) - Section 3 Module Architecture

## 📋 Overview
Establish the core modular architecture for iris_android based on the comprehensive architecture document. This implements the foundational layer structure, dependency injection framework, and module interfaces that all subsequent features will build upon.

## 🎯 Goals
- **Modular Architecture**: Implement clean separation between UI, business logic, and hardware layers
- **Dependency Injection**: Establish robust DI framework for testability and maintainability  
- **Interface Contracts**: Define clear APIs between modules for loose coupling
- **Plugin Foundation**: Create extensible architecture for future enhancements
- **Performance**: Optimize module loading and initialization for mobile constraints

## 📝 Detailed Tasks

### 1. Gradle Module Structure

#### 1.1 Core Logic Modules (`core-*`)
Create the following modules with proper Gradle configuration:

- [ ] **`core-llm`** - LLM Engine Module
```kotlin
// core-llm/build.gradle.kts
plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.nervesparks.iris.core.llm"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    externalNativeBuild {
        cmake {
            path = "src/main/cpp/CMakeLists.txt"
            version = "3.22.1"
        }
    }
    
    ndkVersion = "25.1.8937393"
}

dependencies {
    implementation(project(":core-hw"))
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    
    testImplementation(libs.junit5)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.compose.ui.test)
}
```

- [ ] **`core-rag`** - RAG Engine Module
- [ ] **`core-asr`** - ASR Engine Module  
- [ ] **`core-vision`** - Vision Engine Module
- [ ] **`core-tools`** - Tool Engine Module
- [ ] **`core-safety`** - Safety Engine Module

#### 1.2 Hardware Abstraction Layer (`core-hw`)
- [ ] **`core-hw-detection`** - Hardware Detection Module
- [ ] **`core-hw-router`** - Backend Router Module  
- [ ] **`core-thermal`** - Thermal Manager Module

#### 1.3 UI Layer Modules (`app-ui`)
- [ ] **`ui-chat`** - Chat Interface Module
- [ ] **`ui-models`** - Model Manager Module
- [ ] **`ui-settings`** - Settings & Privacy Module

#### 1.4 Common/Shared Module
- [ ] **`:common`** - Shared Utilities and Contracts
```kotlin
// common/src/main/kotlin/com/nervesparks/iris/common/
├── config/
│   ├── ModelConfig.kt
│   ├── DeviceConfig.kt
│   └── SafetyConfig.kt
├── error/
│   ├── IrisException.kt
│   ├── ModelException.kt
│   └── HardwareException.kt
├── logging/
│   ├── IrisLogger.kt
│   └── LogLevel.kt
├── models/
│   ├── ModelHandle.kt
│   ├── GenerationParams.kt
│   └── DeviceProfile.kt
└── utils/
    ├── Extensions.kt
    └── Constants.kt
```

### 2. Dependency Injection Framework

#### 2.1 Hilt Configuration
- [ ] **Application-Level Setup**
```kotlin
@HiltAndroidApp
class IrisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeLogging()
        initializeNativeLibraries()
    }
    
    private fun initializeNativeLibraries() {
        try {
            System.loadLibrary("llama")
            System.loadLibrary("whisper")
            System.loadLibrary("sqlite-vec")
        } catch (e: UnsatisfiedLinkError) {
            IrisLogger.error("Failed to load native libraries", e)
            throw RuntimeException("Critical native libraries not available", e)
        }
    }
}
```

- [ ] **Module-Level DI Configuration**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreEnginesModule {
    
    @Binds
    abstract fun bindLLMEngine(impl: LLMEngineImpl): LLMEngine
    
    @Binds
    abstract fun bindRAGEngine(impl: RAGEngineImpl): RAGEngine
    
    @Binds
    abstract fun bindASREngine(impl: ASREngineImpl): ASREngine
    
    @Binds
    abstract fun bindVisionEngine(impl: VisionEngineImpl): VisionEngine
    
    @Binds
    abstract fun bindToolEngine(impl: ToolEngineImpl): ToolEngine
    
    @Binds
    abstract fun bindSafetyEngine(impl: SafetyEngineImpl): SafetyEngine
}

@Module
@InstallIn(SingletonComponent::class)
object HardwareModule {
    
    @Provides
    @Singleton
    fun provideDeviceProfileProvider(context: Context): DeviceProfileProvider {
        return DeviceProfileProviderImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideBackendRouter(
        deviceProfile: DeviceProfileProvider,
        thermalManager: ThermalManager
    ): BackendRouter {
        return BackendRouterImpl(deviceProfile, thermalManager)
    }
}
```

#### 2.2 Interface Definitions
Based on our architecture document, create comprehensive interface contracts:

- [ ] **LLM Engine Interface**
```kotlin
interface LLMEngine {
    suspend fun loadModel(modelPath: String): Result<ModelHandle>
    suspend fun generateText(prompt: String, params: GenerationParams): Flow<String>
    suspend fun embed(text: String): FloatArray
    fun unloadModel(handle: ModelHandle)
    suspend fun getModelInfo(handle: ModelHandle): ModelInfo
    fun isModelLoaded(modelPath: String): Boolean
}

data class GenerationParams(
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.9f,
    val maxTokens: Int = 512,
    val stopTokens: List<String> = emptyList(),
    val repeatPenalty: Float = 1.1f,
    val seed: Long = -1L
)

data class ModelHandle(
    val id: String,
    val modelPath: String,
    val contextSize: Int,
    val vocabSize: Int,
    val backend: BackendType
)

enum class BackendType {
    CPU_NEON, OPENCL_ADRENO, VULKAN_MALI, QNN_HEXAGON
}
```

- [ ] **RAG Engine Interface**
```kotlin
interface RAGEngine {
    suspend fun indexDocument(document: Document): Result<Unit>
    suspend fun search(query: String, limit: Int = 5): List<RetrievedChunk>
    suspend fun deleteIndex(documentId: String): Result<Unit>
    suspend fun updateDocument(document: Document): Result<Unit>
    suspend fun getIndexStats(): IndexStats
    suspend fun optimizeIndex(): Result<Unit>
}

data class Document(
    val id: String,
    val content: String,
    val source: DataSource,
    val metadata: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

data class RetrievedChunk(
    val id: String,
    val content: String,
    val score: Float,
    val documentId: String,
    val chunkIndex: Int,
    val metadata: Map<String, Any>
)

enum class DataSource {
    NOTE, PDF, SMS, EMAIL, CALENDAR, CONTACT, FILE, MANUAL
}
```

- [ ] **Hardware Abstraction Interfaces**
```kotlin
interface DeviceProfileProvider {
    fun getDeviceProfile(): DeviceProfile
    fun getSoCInfo(): SoCInfo
    fun getGPUInfo(): GPUInfo
    fun getMemoryInfo(): MemoryInfo
    suspend fun runBenchmark(): BenchmarkResults
}

data class DeviceProfile(
    val socVendor: SoCVendor,
    val socModel: String,
    val gpuVendor: GPUVendor,
    val gpuModel: String,
    val totalRAM: Long,
    val availableRAM: Long,
    val androidVersion: Int,
    val capabilities: Set<HardwareCapability>
)

enum class SoCVendor { QUALCOMM, SAMSUNG, MEDIATEK, GOOGLE, OTHER }
enum class GPUVendor { ADRENO, MALI, XCLIPSE, OTHER }
enum class HardwareCapability { 
    OPENCL, VULKAN, NNAPI, QNN, FP16_SUPPORT, INT8_SUPPORT 
}

interface BackendRouter {
    suspend fun selectOptimalBackend(task: ComputeTask): BackendType
    suspend fun switchBackend(newBackend: BackendType): Result<Unit>
    fun getCurrentBackend(): BackendType
    suspend fun validateBackend(backend: BackendType): Boolean
}

enum class ComputeTask {
    LLM_INFERENCE, EMBEDDING_GENERATION, SAFETY_CHECK, ASR_TRANSCRIPTION
}
```

### 3. Application Layer Architecture

#### 3.1 App Coordinator
- [ ] **Central Application Coordinator**
```kotlin
@Singleton
class AppCoordinator @Inject constructor(
    private val stateManager: StateManager,
    private val eventBus: EventBus,
    private val llmEngine: LLMEngine,
    private val ragEngine: RAGEngine,
    private val safetyEngine: SafetyEngine,
    private val thermalManager: ThermalManager
) {
    
    private val _appState = MutableStateFlow(AppState.Initializing)
    val appState: StateFlow<AppState> = _appState.asStateFlow()
    
    suspend fun initialize(): Result<Unit> {
        return try {
            // Initialize hardware detection
            val deviceProfile = deviceProfileProvider.getDeviceProfile()
            stateManager.updateDeviceProfile(deviceProfile)
            
            // Initialize thermal monitoring
            thermalManager.startMonitoring()
            
            // Load default models if available
            loadDefaultModels()
            
            _appState.value = AppState.Ready
            Result.success(Unit)
        } catch (e: Exception) {
            _appState.value = AppState.Error(e)
            Result.failure(e)
        }
    }
    
    suspend fun processUserInput(input: UserInput): Flow<ProcessingResult> = flow {
        emit(ProcessingResult.Started)
        
        // Safety check
        val safetyResult = safetyEngine.checkInput(input.text)
        if (!safetyResult.isAllowed) {
            emit(ProcessingResult.Blocked(safetyResult.reason))
            return@flow
        }
        
        // RAG retrieval if enabled
        val context = if (input.enableRAG) {
            ragEngine.search(input.text)
        } else {
            emptyList()
        }
        
        // LLM generation
        val prompt = buildPrompt(input, context)
        llmEngine.generateText(prompt, input.params).collect { token ->
            emit(ProcessingResult.TokenGenerated(token))
        }
        
        emit(ProcessingResult.Completed)
    }
}

sealed class AppState {
    object Initializing : AppState()
    object Ready : AppState()
    data class Error(val exception: Throwable) : AppState()
}

sealed class ProcessingResult {
    object Started : ProcessingResult()
    data class TokenGenerated(val token: String) : ProcessingResult()
    data class Blocked(val reason: String) : ProcessingResult()
    object Completed : ProcessingResult()
}
```

#### 3.2 State Management
- [ ] **Centralized State Manager**
```kotlin
@Singleton
class StateManager @Inject constructor() {
    
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()
    
    private val _currentModel = MutableStateFlow<ModelHandle?>(null)
    val currentModel: StateFlow<ModelHandle?> = _currentModel.asStateFlow()
    
    private val _deviceProfile = MutableStateFlow<DeviceProfile?>(null)
    val deviceProfile: StateFlow<DeviceProfile?> = _deviceProfile.asStateFlow()
    
    private val _performanceProfile = MutableStateFlow(PerformanceProfile.BALANCED)
    val performanceProfile: StateFlow<PerformanceProfile> = _performanceProfile.asStateFlow()
    
    fun updateCurrentModel(model: ModelHandle?) {
        _currentModel.value = model
    }
    
    fun updateDeviceProfile(profile: DeviceProfile) {
        _deviceProfile.value = profile
    }
    
    fun updatePerformanceProfile(profile: PerformanceProfile) {
        _performanceProfile.value = profile
    }
    
    suspend fun addConversation(conversation: Conversation) {
        _conversations.value = _conversations.value + conversation
    }
    
    suspend fun updateConversation(conversationId: String, updater: (Conversation) -> Conversation) {
        _conversations.value = _conversations.value.map { conversation ->
            if (conversation.id == conversationId) {
                updater(conversation)
            } else {
                conversation
            }
        }
    }
}

enum class PerformanceProfile {
    PERFORMANCE, BALANCED, BATTERY_SAVER, EMERGENCY
}
```

#### 3.3 Event Bus System
- [ ] **Event-Driven Communication**
```kotlin
@Singleton
class EventBus @Inject constructor() {
    
    private val _events = MutableSharedFlow<IrisEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<IrisEvent> = _events.asSharedFlow()
    
    fun emit(event: IrisEvent) {
        _events.tryEmit(event)
    }
    
    inline fun <reified T : IrisEvent> subscribe(): Flow<T> {
        return events.filterIsInstance<T>()
    }
}

sealed class IrisEvent {
    data class ModelLoaded(val handle: ModelHandle) : IrisEvent()
    data class ModelUnloaded(val modelPath: String) : IrisEvent()
    data class ThermalStateChanged(val state: ThermalState) : IrisEvent()
    data class PerformanceProfileChanged(val profile: PerformanceProfile) : IrisEvent()
    data class SafetyViolation(val input: String, val reason: String) : IrisEvent()
    data class RAGIndexUpdated(val documentCount: Int) : IrisEvent()
    data class ErrorOccurred(val error: IrisException, val component: String) : IrisEvent()
}
```

### 4. Module Implementation Stubs

#### 4.1 Engine Implementations (Stubs)
Create basic implementation stubs for all engines:

- [ ] **LLM Engine Stub**
```kotlin
@Singleton
class LLMEngineImpl @Inject constructor(
    private val backendRouter: BackendRouter,
    private val eventBus: EventBus
) : LLMEngine {
    
    override suspend fun loadModel(modelPath: String): Result<ModelHandle> {
        // TODO: Implement native model loading
        return Result.failure(NotImplementedError("LLM loading not yet implemented"))
    }
    
    override suspend fun generateText(prompt: String, params: GenerationParams): Flow<String> = flow {
        // TODO: Implement native text generation
        emit("Mock response: $prompt")
    }
    
    override suspend fun embed(text: String): FloatArray {
        // TODO: Implement native embedding generation
        return FloatArray(768) { 0.0f }
    }
    
    override fun unloadModel(handle: ModelHandle) {
        // TODO: Implement native model unloading
    }
    
    override suspend fun getModelInfo(handle: ModelHandle): ModelInfo {
        // TODO: Implement model info retrieval
        return ModelInfo(
            name = "Mock Model",
            parameterCount = "7B",
            contextSize = 4096,
            vocabSize = 32000
        )
    }
    
    override fun isModelLoaded(modelPath: String): Boolean {
        // TODO: Implement model status check
        return false
    }
}
```

Similar stubs for RAG, ASR, Vision, Tools, and Safety engines.

### 5. Testing Infrastructure

#### 5.1 Module Testing Setup
- [ ] **Unit Test Infrastructure**
```kotlin
// Common test utilities
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [CoreEnginesModule::class]
)
@Module
abstract class TestCoreEnginesModule {
    
    @Binds
    abstract fun bindLLMEngine(impl: MockLLMEngine): LLMEngine
    
    @Binds
    abstract fun bindRAGEngine(impl: MockRAGEngine): RAGEngine
}

class MockLLMEngine @Inject constructor() : LLMEngine {
    var mockResponses = mutableListOf<String>()
    var loadResult: Result<ModelHandle>? = null
    
    override suspend fun loadModel(modelPath: String): Result<ModelHandle> {
        return loadResult ?: Result.success(
            ModelHandle("mock", modelPath, 4096, 32000, BackendType.CPU_NEON)
        )
    }
    
    override suspend fun generateText(prompt: String, params: GenerationParams): Flow<String> = flow {
        mockResponses.forEach { emit(it) }
    }
    
    // ... other mock implementations
}
```

#### 5.2 Integration Tests
- [ ] **Module Integration Tests**
```kotlin
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AppCoordinatorIntegrationTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var appCoordinator: AppCoordinator
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun testAppInitialization() = runTest {
        val result = appCoordinator.initialize()
        assertTrue(result.isSuccess)
        
        val appState = appCoordinator.appState.first()
        assertEquals(AppState.Ready, appState)
    }
    
    @Test
    fun testUserInputProcessing() = runTest {
        appCoordinator.initialize()
        
        val input = UserInput(
            text = "Hello, how are you?",
            enableRAG = false,
            params = GenerationParams()
        )
        
        val results = appCoordinator.processUserInput(input).toList()
        
        assertTrue(results.first() is ProcessingResult.Started)
        assertTrue(results.last() is ProcessingResult.Completed)
    }
}
```

## 🧪 Testing Strategy

### Unit Tests
- [ ] **Module Interface Compliance**
  - All engine interfaces properly implemented
  - DI bindings resolve correctly
  - State management works as expected
  - Event bus communication functions

### Integration Tests
- [ ] **Cross-Module Communication**
  - App coordinator orchestrates engines correctly
  - State changes propagate through system
  - Error handling works across module boundaries
  - Performance metrics are tracked

### Architecture Tests
- [ ] **Module Dependency Validation**
```kotlin
@Test
fun `verify module dependencies are acyclic`() {
    val dependencyRule = SliceRule.builder()
        .addSlice("UI").containing("..ui..")
        .addSlice("Core").containing("..core..")
        .addSlice("Common").containing("..common..")
        .build()
    
    Assertions.assertThat(dependencyRule)
        .check(JavaClasses.importPackages("com.nervesparks.iris"))
}
```

## ✅ Acceptance Criteria

### Primary Criteria
- [ ] **Module Structure Complete**: All modules build successfully with proper separation
- [ ] **DI Framework Functional**: All dependencies resolve and inject properly
- [ ] **Interface Contracts Defined**: All engine interfaces documented and implemented
- [ ] **State Management Working**: Centralized state flows correctly between modules
- [ ] **Event System Functional**: Event bus handles inter-module communication

### Technical Criteria
- [ ] **Build Success**: `./gradlew assembleDebug` succeeds for all modules
- [ ] **Test Coverage**: >95% test coverage on interfaces and DI configuration
- [ ] **Architecture Compliance**: No circular dependencies, proper layering enforced
- [ ] **Performance**: App initialization completes in <2 seconds on mid-range devices

### Code Quality Criteria
- [ ] **Documentation**: All public interfaces documented with KDoc
- [ ] **Error Handling**: Comprehensive error types and handling strategies
- [ ] **Logging**: Structured logging available throughout architecture
- [ ] **Configuration**: All modules configurable via DI and settings

## 🔗 Related Issues
- **Depends on**: #00 (Project Foundation)
- **Enables**: #02 (Native llama.cpp Integration), #03 (Hardware Detection)
- **Architecture Reference**: docs/architecture.md sections 3.1-3.2

## 📋 Definition of Done
- [ ] All modules created with proper Gradle configuration
- [ ] Hilt DI framework integrated and tested
- [ ] All engine interfaces defined and stubbed
- [ ] App coordinator and state management functional
- [ ] Event bus system operational
- [ ] Comprehensive test suite passing
- [ ] Architecture documentation updated
- [ ] Code review completed and approved

---

**Note**: This establishes the architectural foundation. All engines will be stub implementations until their respective detailed implementation issues are completed.