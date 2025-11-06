# Issue #02: Native llama.cpp Integration & JNI Bridge

## 🎯 Epic: Native AI Engine
**Priority**: P0 (Blocking)  
**Estimate**: 7-10 days  
**Dependencies**: #01 (Core Architecture)  
**Architecture Reference**: [docs/architecture.md](../architecture.md) - Section 5 Hardware & Backend Architecture

## 📋 Overview
Integrate llama.cpp as the core native inference engine with full Android NDK support, multi-backend capability (CPU/OpenCL/Vulkan), and robust JNI bridge. This forms the foundation for all LLM inference capabilities in iris_android.

## 🎯 Goals
- **Multi-Backend Support**: Enable CPU, OpenCL (Adreno), and Vulkan (Mali/Xclipse) backends
- **Production JNI Bridge**: Type-safe, memory-efficient native interface
- **Hardware Optimization**: Device-specific optimizations for Snapdragon and Exynos
- **Memory Management**: Efficient model loading/unloading with mobile constraints
- **Error Handling**: Comprehensive error recovery and diagnostics

## 📝 Detailed Tasks

### 1. llama.cpp Integration Setup

#### 1.1 Submodule Configuration
- [ ] **Add llama.cpp as Git Submodule**
```bash
# Pin to specific tested commit for stability
git submodule add https://github.com/ggerganov/llama.cpp.git core-llm/src/main/cpp/llama.cpp
cd core-llm/src/main/cpp/llama.cpp
git checkout [SPECIFIC_COMMIT_HASH]  # Use tested stable version
```

- [ ] **Verify Backend Support**
  - Ensure OpenCL support is available (`GGML_OPENCL=ON`)
  - Verify Vulkan support is included (`GGML_VULKAN=ON`)
  - Check ARM NEON optimizations are enabled
  - Validate quantization support (Q4_0, Q4_K_M, Q8_0)

#### 1.2 CMake Build Configuration
Create comprehensive CMake setup in `core-llm/src/main/cpp/CMakeLists.txt`:

```cmake
cmake_minimum_required(VERSION 3.22.1)
project(iris_llm)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Android-specific settings
set(ANDROID_STL c++_shared)
set(ANDROID_CPP_FEATURES rtti exceptions)

# llama.cpp configuration
set(GGML_OPENCL ON CACHE BOOL "Enable OpenCL backend")
set(GGML_VULKAN ON CACHE BOOL "Enable Vulkan backend")
set(GGML_NATIVE ON CACHE BOOL "Enable native optimizations")
set(GGML_ACCELERATE OFF CACHE BOOL "Disable Accelerate framework")
set(BUILD_SHARED_LIBS OFF CACHE BOOL "Build static libraries")

# Add llama.cpp subdirectory
add_subdirectory(llama.cpp)

# Include directories
include_directories(
    llama.cpp/
    llama.cpp/include/
    llama.cpp/ggml/include/
    ${ANDROID_NDK}/sources/android/native_app_glue
)

# JNI bridge source files
set(JNI_SOURCES
    jni_bridge.cpp
    model_manager.cpp
    backend_manager.cpp
    generation_engine.cpp
    error_handler.cpp
    memory_pool.cpp
)

# Create shared library
add_library(iris_llm SHARED ${JNI_SOURCES})

# Link libraries
target_link_libraries(iris_llm
    llama
    ggml
    android
    log
    EGL
    GLESv2
)

# Compiler flags for optimization
target_compile_options(iris_llm PRIVATE
    -O3
    -DNDEBUG
    -march=armv8-a+fp+simd
    -mtune=cortex-a75
    -ffast-math
    -funroll-loops
)

# OpenCL linking for Adreno GPUs
if(GGML_OPENCL)
    find_package(OpenCL REQUIRED)
    target_link_libraries(iris_llm OpenCL::OpenCL)
endif()

# Vulkan linking for Mali/Xclipse GPUs
if(GGML_VULKAN)
    find_package(Vulkan REQUIRED)
    target_link_libraries(iris_llm Vulkan::Vulkan)
endif()
```

#### 1.3 Native Library Dependencies
- [ ] **OpenCL ICD Loader**
  - Package `libOpenCL.so` in `jniLibs/arm64-v8a/`
  - Ensure compatibility with Adreno drivers
  - Add runtime detection for OpenCL availability

- [ ] **Vulkan Loader**
  - Verify Android Vulkan API level 29+ support
  - Include validation layers for debug builds
  - Add Mali/Xclipse specific optimizations

### 2. JNI Bridge Implementation

#### 2.1 Core JNI Interface
Create type-safe JNI bridge in `core-llm/src/main/cpp/jni_bridge.cpp`:

```cpp
#include <jni.h>
#include <android/log.h>
#include <memory>
#include <unordered_map>
#include <mutex>
#include "llama.h"
#include "model_manager.h"
#include "generation_engine.h"

#define LOG_TAG "IrisLLM"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global state management
class NativeState {
public:
    std::mutex mutex;
    std::unordered_map<std::string, std::unique_ptr<ModelManager>> models;
    std::unordered_map<std::string, std::unique_ptr<GenerationEngine>> sessions;
    
    static NativeState& getInstance() {
        static NativeState instance;
        return instance;
    }
};

// Helper for exception handling
jclass findExceptionClass(JNIEnv* env, const char* className) {
    jclass clazz = env->FindClass(className);
    if (!clazz) {
        env->ExceptionClear();
        clazz = env->FindClass("java/lang/RuntimeException");
    }
    return clazz;
}

void throwException(JNIEnv* env, const char* exceptionClass, const char* message) {
    jclass clazz = findExceptionClass(env, exceptionClass);
    env->ThrowNew(clazz, message);
}

extern "C" {

// Backend management
JNIEXPORT jint JNICALL
Java_com_nervesparks_iris_core_llm_LLMEngineImpl_nativeInitializeBackend(
    JNIEnv* env, jobject thiz, jint backend_type) {
    
    try {
        llama_backend_init();
        
        // Set backend-specific configurations
        switch (backend_type) {
            case 0: // CPU_NEON
                LOGI("Initializing CPU NEON backend");
                break;
            case 1: // OPENCL_ADRENO
                LOGI("Initializing OpenCL Adreno backend");
                break;
            case 2: // VULKAN_MALI
                LOGI("Initializing Vulkan Mali backend");
                break;
            default:
                throwException(env, "java/lang/IllegalArgumentException", 
                             "Unsupported backend type");
                return -1;
        }
        
        return 0; // Success
    } catch (const std::exception& e) {
        LOGE("Backend initialization failed: %s", e.what());
        throwException(env, "com/nervesparks/iris/core/llm/LLMException", e.what());
        return -1;
    }
}

// Model loading
JNIEXPORT jstring JNICALL
Java_com_nervesparks_iris_core_llm_LLMEngineImpl_nativeLoadModel(
    JNIEnv* env, jobject thiz, jstring model_path, jobject params) {
    
    const char* path = env->GetStringUTFChars(model_path, nullptr);
    
    try {
        auto& state = NativeState::getInstance();
        std::lock_guard<std::mutex> lock(state.mutex);
        
        // Extract parameters from Java object
        jclass paramsClass = env->GetObjectClass(params);
        jfieldID contextSizeField = env->GetFieldID(paramsClass, "contextSize", "I");
        jfieldID seedField = env->GetFieldID(paramsClass, "seed", "J");
        jfieldID threadsField = env->GetFieldID(paramsClass, "threads", "I");
        
        int contextSize = env->GetIntField(params, contextSizeField);
        long seed = env->GetLongField(params, seedField);
        int threads = env->GetIntField(params, threadsField);
        
        // Create model manager
        auto modelManager = std::make_unique<ModelManager>();
        std::string modelId = modelManager->loadModel(path, contextSize, seed, threads);
        
        // Store in global state
        state.models[modelId] = std::move(modelManager);
        
        env->ReleaseStringUTFChars(model_path, path);
        return env->NewStringUTF(modelId.c_str());
        
    } catch (const std::exception& e) {
        LOGE("Model loading failed: %s", e.what());
        env->ReleaseStringUTFChars(model_path, path);
        throwException(env, "com/nervesparks/iris/core/llm/ModelException", e.what());
        return nullptr;
    }
}

// Text generation
JNIEXPORT jlong JNICALL
Java_com_nervesparks_iris_core_llm_LLMEngineImpl_nativeStartGeneration(
    JNIEnv* env, jobject thiz, jstring model_id, jstring prompt, jobject gen_params) {
    
    const char* modelIdStr = env->GetStringUTFChars(model_id, nullptr);
    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    
    try {
        auto& state = NativeState::getInstance();
        std::lock_guard<std::mutex> lock(state.mutex);
        
        // Find model
        auto modelIt = state.models.find(modelIdStr);
        if (modelIt == state.models.end()) {
            throwException(env, "com/nervesparks/iris/core/llm/ModelException", 
                         "Model not found");
            return -1;
        }
        
        // Extract generation parameters
        jclass genParamsClass = env->GetObjectClass(gen_params);
        jfieldID tempField = env->GetFieldID(genParamsClass, "temperature", "F");
        jfieldID topKField = env->GetFieldID(genParamsClass, "topK", "I");
        jfieldID topPField = env->GetFieldID(genParamsClass, "topP", "F");
        jfieldID maxTokensField = env->GetFieldID(genParamsClass, "maxTokens", "I");
        
        float temperature = env->GetFloatField(gen_params, tempField);
        int topK = env->GetIntField(gen_params, topKField);
        float topP = env->GetFloatField(gen_params, topPField);
        int maxTokens = env->GetIntField(gen_params, maxTokensField);
        
        // Create generation engine
        auto genEngine = std::make_unique<GenerationEngine>(
            modelIt->second.get(), temperature, topK, topP, maxTokens);
        
        long sessionId = genEngine->startGeneration(promptStr);
        state.sessions[std::to_string(sessionId)] = std::move(genEngine);
        
        env->ReleaseStringUTFChars(model_id, modelIdStr);
        env->ReleaseStringUTFChars(prompt, promptStr);
        
        return sessionId;
        
    } catch (const std::exception& e) {
        LOGE("Generation start failed: %s", e.what());
        env->ReleaseStringUTFChars(model_id, modelIdStr);
        env->ReleaseStringUTFChars(prompt, promptStr);
        throwException(env, "com/nervesparks/iris/core/llm/GenerationException", e.what());
        return -1;
    }
}

// Token generation
JNIEXPORT jstring JNICALL
Java_com_nervesparks_iris_core_llm_LLMEngineImpl_nativeGenerateNextToken(
    JNIEnv* env, jobject thiz, jlong session_id) {
    
    try {
        auto& state = NativeState::getInstance();
        std::lock_guard<std::mutex> lock(state.mutex);
        
        auto sessionIt = state.sessions.find(std::to_string(session_id));
        if (sessionIt == state.sessions.end()) {
            return nullptr; // Session ended or not found
        }
        
        std::string token = sessionIt->second->generateNextToken();
        if (token.empty()) {
            // Generation complete, cleanup
            state.sessions.erase(sessionIt);
            return nullptr;
        }
        
        return env->NewStringUTF(token.c_str());
        
    } catch (const std::exception& e) {
        LOGE("Token generation failed: %s", e.what());
        throwException(env, "com/nervesparks/iris/core/llm/GenerationException", e.what());
        return nullptr;
    }
}

// Embedding generation
JNIEXPORT jfloatArray JNICALL
Java_com_nervesparks_iris_core_llm_LLMEngineImpl_nativeGenerateEmbedding(
    JNIEnv* env, jobject thiz, jstring model_id, jstring text) {
    
    const char* modelIdStr = env->GetStringUTFChars(model_id, nullptr);
    const char* textStr = env->GetStringUTFChars(text, nullptr);
    
    try {
        auto& state = NativeState::getInstance();
        std::lock_guard<std::mutex> lock(state.mutex);
        
        auto modelIt = state.models.find(modelIdStr);
        if (modelIt == state.models.end()) {
            throwException(env, "com/nervesparks/iris/core/llm/ModelException", 
                         "Model not found");
            return nullptr;
        }
        
        std::vector<float> embedding = modelIt->second->generateEmbedding(textStr);
        
        jfloatArray result = env->NewFloatArray(embedding.size());
        env->SetFloatArrayRegion(result, 0, embedding.size(), embedding.data());
        
        env->ReleaseStringUTFChars(model_id, modelIdStr);
        env->ReleaseStringUTFChars(text, textStr);
        
        return result;
        
    } catch (const std::exception& e) {
        LOGE("Embedding generation failed: %s", e.what());
        env->ReleaseStringUTFChars(model_id, modelIdStr);
        env->ReleaseStringUTFChars(text, textStr);
        throwException(env, "com/nervesparks/iris/core/llm/EmbeddingException", e.what());
        return nullptr;
    }
}

// Model unloading
JNIEXPORT jboolean JNICALL
Java_com_nervesparks_iris_core_llm_LLMEngineImpl_nativeUnloadModel(
    JNIEnv* env, jobject thiz, jstring model_id) {
    
    const char* modelIdStr = env->GetStringUTFChars(model_id, nullptr);
    
    try {
        auto& state = NativeState::getInstance();
        std::lock_guard<std::mutex> lock(state.mutex);
        
        auto modelIt = state.models.find(modelIdStr);
        if (modelIt != state.models.end()) {
            // Cleanup any active sessions for this model
            auto sessionIt = state.sessions.begin();
            while (sessionIt != state.sessions.end()) {
                if (sessionIt->second->getModelId() == modelIdStr) {
                    sessionIt = state.sessions.erase(sessionIt);
                } else {
                    ++sessionIt;
                }
            }
            
            // Unload model
            state.models.erase(modelIt);
            env->ReleaseStringUTFChars(model_id, modelIdStr);
            return JNI_TRUE;
        }
        
        env->ReleaseStringUTFChars(model_id, modelIdStr);
        return JNI_FALSE;
        
    } catch (const std::exception& e) {
        LOGE("Model unloading failed: %s", e.what());
        env->ReleaseStringUTFChars(model_id, modelIdStr);
        return JNI_FALSE;
    }
}

// Cleanup
JNIEXPORT void JNICALL
Java_com_nervesparks_iris_core_llm_LLMEngineImpl_nativeShutdown(
    JNIEnv* env, jobject thiz) {
    
    try {
        auto& state = NativeState::getInstance();
        std::lock_guard<std::mutex> lock(state.mutex);
        
        // Clear all sessions and models
        state.sessions.clear();
        state.models.clear();
        
        llama_backend_free();
        LOGI("Native backend shutdown complete");
        
    } catch (const std::exception& e) {
        LOGE("Shutdown failed: %s", e.what());
    }
}

} // extern "C"
```

#### 2.2 Model Manager Implementation
Create `core-llm/src/main/cpp/model_manager.cpp`:

```cpp
#include "model_manager.h"
#include <android/log.h>
#include <random>
#include <chrono>

#define LOG_TAG "ModelManager"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

ModelManager::ModelManager() : model(nullptr), context(nullptr) {
    // Generate unique model ID
    auto now = std::chrono::system_clock::now();
    auto timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(
        now.time_since_epoch()).count();
    
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dis(1000, 9999);
    
    modelId = "model_" + std::to_string(timestamp) + "_" + std::to_string(dis(gen));
}

ModelManager::~ModelManager() {
    unloadModel();
}

std::string ModelManager::loadModel(const std::string& path, int contextSize, 
                                  long seed, int threads) {
    try {
        // Set up model parameters
        llama_model_params modelParams = llama_model_default_params();
        modelParams.n_gpu_layers = determineGPULayers();
        modelParams.use_mmap = true;
        modelParams.use_mlock = false; // Avoid locking memory on mobile
        
        // Load model
        model = llama_load_model_from_file(path.c_str(), modelParams);
        if (!model) {
            throw std::runtime_error("Failed to load model from " + path);
        }
        
        // Set up context parameters
        llama_context_params contextParams = llama_context_default_params();
        contextParams.seed = (seed == -1) ? static_cast<uint32_t>(std::time(nullptr)) : static_cast<uint32_t>(seed);
        contextParams.n_ctx = contextSize;
        contextParams.n_threads = (threads <= 0) ? std::thread::hardware_concurrency() : threads;
        contextParams.n_threads_batch = contextParams.n_threads;
        
        // Create context
        context = llama_new_context_with_model(model, contextParams);
        if (!context) {
            llama_free_model(model);
            model = nullptr;
            throw std::runtime_error("Failed to create context");
        }
        
        // Initialize tokenizer
        tokenizer = std::make_unique<LlamaTokenizer>(model);
        
        LOGI("Model loaded successfully: %s", modelId.c_str());
        return modelId;
        
    } catch (const std::exception& e) {
        LOGE("Model loading failed: %s", e.what());
        throw;
    }
}

void ModelManager::unloadModel() {
    if (context) {
        llama_free(context);
        context = nullptr;
    }
    
    if (model) {
        llama_free_model(model);
        model = nullptr;
    }
    
    tokenizer.reset();
    LOGI("Model unloaded: %s", modelId.c_str());
}

std::vector<float> ModelManager::generateEmbedding(const std::string& text) {
    if (!model || !context) {
        throw std::runtime_error("Model not loaded");
    }
    
    try {
        // Tokenize input
        std::vector<llama_token> tokens = tokenizer->tokenize(text, true);
        
        // Clear context
        llama_kv_cache_clear(context);
        
        // Process tokens for embedding
        int n_embd = llama_n_embd(model);
        std::vector<float> embedding(n_embd, 0.0f);
        
        // Batch processing for efficiency
        llama_batch batch = llama_batch_init(tokens.size(), 0, 1);
        
        for (size_t i = 0; i < tokens.size(); i++) {
            batch.token[i] = tokens[i];
            batch.pos[i] = i;
            batch.n_seq_id[i] = 1;
            batch.seq_id[i][0] = 0;
            batch.logits[i] = (i == tokens.size() - 1) ? 1 : 0; // Only compute logits for last token
        }
        batch.n_tokens = tokens.size();
        
        // Decode
        if (llama_decode(context, batch) != 0) {
            llama_batch_free(batch);
            throw std::runtime_error("Failed to decode tokens for embedding");
        }
        
        // Extract embedding from last token
        const float* embeddingData = llama_get_embeddings(context);
        if (!embeddingData) {
            llama_batch_free(batch);
            throw std::runtime_error("Failed to get embeddings");
        }
        
        std::copy(embeddingData, embeddingData + n_embd, embedding.begin());
        
        llama_batch_free(batch);
        return embedding;
        
    } catch (const std::exception& e) {
        LOGE("Embedding generation failed: %s", e.what());
        throw;
    }
}

int ModelManager::determineGPULayers() {
    // TODO: Implement hardware-specific GPU layer determination
    // This should integrate with the backend detection system
    return 0; // Start with CPU-only, GPU layers will be determined by backend router
}

llama_model* ModelManager::getModel() const {
    return model;
}

llama_context* ModelManager::getContext() const {
    return context;
}

std::string ModelManager::getModelId() const {
    return modelId;
}
```

### 3. Kotlin Implementation

#### 3.1 LLM Engine Implementation
Update `core-llm/src/main/kotlin/LLMEngineImpl.kt`:

```kotlin
@Singleton
class LLMEngineImpl @Inject constructor(
    private val backendRouter: BackendRouter,
    private val eventBus: EventBus,
    private val thermalManager: ThermalManager,
    @ApplicationContext private val context: Context
) : LLMEngine {
    
    companion object {
        init {
            try {
                System.loadLibrary("iris_llm")
            } catch (e: UnsatisfiedLinkError) {
                throw RuntimeException("Failed to load native LLM library", e)
            }
        }
    }
    
    private val loadedModels = mutableMapOf<String, ModelHandle>()
    private val activeGenerations = mutableMapOf<Long, Job>()
    private var isBackendInitialized = false
    
    override suspend fun loadModel(modelPath: String): Result<ModelHandle> = withContext(Dispatchers.IO) {
        try {
            // Initialize backend if not already done
            if (!isBackendInitialized) {
                val backend = backendRouter.selectOptimalBackend(ComputeTask.LLM_INFERENCE)
                val result = nativeInitializeBackend(backend.ordinal)
                if (result != 0) {
                    return@withContext Result.failure(LLMException("Backend initialization failed"))
                }
                isBackendInitialized = true
            }
            
            // Check if model is already loaded
            if (loadedModels.containsKey(modelPath)) {
                return@withContext Result.success(loadedModels[modelPath]!!)
            }
            
            // Validate model file
            val modelFile = File(modelPath)
            if (!modelFile.exists() || !modelFile.canRead()) {
                return@withContext Result.failure(ModelException("Model file not accessible: $modelPath"))
            }
            
            // Get optimal parameters for device
            val deviceProfile = thermalManager.getCurrentProfile()
            val params = createLoadParams(deviceProfile)
            
            // Load model natively
            val modelId = nativeLoadModel(modelPath, params)
                ?: return@withContext Result.failure(ModelException("Native model loading failed"))
            
            // Create model handle
            val handle = ModelHandle(
                id = modelId,
                modelPath = modelPath,
                contextSize = params.contextSize,
                vocabSize = getModelVocabSize(modelId),
                backend = backendRouter.getCurrentBackend()
            )
            
            loadedModels[modelPath] = handle
            eventBus.emit(IrisEvent.ModelLoaded(handle))
            
            Result.success(handle)
            
        } catch (e: Exception) {
            Result.failure(LLMException("Model loading failed", e))
        }
    }
    
    override suspend fun generateText(prompt: String, params: GenerationParams): Flow<String> = channelFlow {
        try {
            // Find appropriate model
            val modelHandle = loadedModels.values.firstOrNull()
                ?: throw LLMException("No model loaded")
            
            // Adapt parameters based on thermal state
            val adaptedParams = thermalManager.adaptGenerationParams(params)
            
            // Start native generation
            val sessionId = nativeStartGeneration(modelHandle.id, prompt, adaptedParams)
            if (sessionId < 0) {
                throw GenerationException("Failed to start generation")
            }
            
            // Create cancellable generation job
            val generationJob = launch(Dispatchers.IO) {
                try {
                    while (isActive) {
                        val token = nativeGenerateNextToken(sessionId)
                        if (token == null) {
                            // Generation complete
                            break
                        }
                        
                        if (!channel.isClosedForSend) {
                            channel.trySend(token)
                        }
                        
                        // Check thermal throttling
                        if (thermalManager.shouldThrottle()) {
                            delay(10) // Brief pause for thermal management
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        channel.close(GenerationException("Generation failed", e))
                    }
                } finally {
                    activeGenerations.remove(sessionId)
                }
            }
            
            activeGenerations[sessionId] = generationJob
            
            // Handle cancellation
            awaitClose {
                generationJob.cancel()
                nativeCancelGeneration(sessionId)
            }
            
        } catch (e: Exception) {
            throw GenerationException("Text generation failed", e)
        }
    }
    
    override suspend fun embed(text: String): FloatArray = withContext(Dispatchers.IO) {
        val modelHandle = loadedModels.values.firstOrNull()
            ?: throw LLMException("No model loaded")
        
        try {
            nativeGenerateEmbedding(modelHandle.id, text)
                ?: throw EmbeddingException("Embedding generation failed")
        } catch (e: Exception) {
            throw EmbeddingException("Embedding generation failed", e)
        }
    }
    
    override fun unloadModel(handle: ModelHandle) {
        try {
            // Cancel any active generations for this model
            activeGenerations.values.forEach { job ->
                job.cancel()
            }
            activeGenerations.clear()
            
            // Unload native model
            val success = nativeUnloadModel(handle.id)
            if (success) {
                loadedModels.remove(handle.modelPath)
                eventBus.emit(IrisEvent.ModelUnloaded(handle.modelPath))
            }
        } catch (e: Exception) {
            // Log error but don't throw - cleanup should be best effort
            Log.e("LLMEngine", "Error unloading model ${handle.id}", e)
        }
    }
    
    override suspend fun getModelInfo(handle: ModelHandle): ModelInfo {
        return ModelInfo(
            name = File(handle.modelPath).nameWithoutExtension,
            parameterCount = extractParameterCount(handle.modelPath),
            contextSize = handle.contextSize,
            vocabSize = handle.vocabSize
        )
    }
    
    override fun isModelLoaded(modelPath: String): Boolean {
        return loadedModels.containsKey(modelPath)
    }
    
    private fun createLoadParams(profile: PerformanceProfile): ModelLoadParams {
        return when (profile) {
            PerformanceProfile.PERFORMANCE -> ModelLoadParams(
                contextSize = 4096,
                threads = 8,
                seed = -1L
            )
            PerformanceProfile.BALANCED -> ModelLoadParams(
                contextSize = 2048,
                threads = 6,
                seed = -1L
            )
            PerformanceProfile.BATTERY_SAVER -> ModelLoadParams(
                contextSize = 1024,
                threads = 4,
                seed = -1L
            )
            PerformanceProfile.EMERGENCY -> ModelLoadParams(
                contextSize = 512,
                threads = 2,
                seed = -1L
            )
        }
    }
    
    private external fun nativeInitializeBackend(backendType: Int): Int
    private external fun nativeLoadModel(modelPath: String, params: ModelLoadParams): String?
    private external fun nativeStartGeneration(modelId: String, prompt: String, params: GenerationParams): Long
    private external fun nativeGenerateNextToken(sessionId: Long): String?
    private external fun nativeGenerateEmbedding(modelId: String, text: String): FloatArray?
    private external fun nativeUnloadModel(modelId: String): Boolean
    private external fun nativeCancelGeneration(sessionId: Long): Boolean
    private external fun nativeShutdown()
}

// Data classes for JNI
data class ModelLoadParams(
    val contextSize: Int,
    val threads: Int,
    val seed: Long
)

// Exception classes
class LLMException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ModelException(message: String, cause: Throwable? = null) : LLMException(message, cause)
class GenerationException(message: String, cause: Throwable? = null) : LLMException(message, cause)
class EmbeddingException(message: String, cause: Throwable? = null) : LLMException(message, cause)
```

### 4. Testing Infrastructure

#### 4.1 Native Testing
Create `core-llm/src/androidTest/cpp/test_native_bridge.cpp`:

```cpp
#include <gtest/gtest.h>
#include <jni.h>
#include "../main/cpp/model_manager.h"

class NativeBridgeTest : public ::testing::Test {
protected:
    void SetUp() override {
        // Initialize test environment
    }
    
    void TearDown() override {
        // Cleanup
    }
};

TEST_F(NativeBridgeTest, ModelManagerCreation) {
    ModelManager manager;
    EXPECT_FALSE(manager.getModelId().empty());
}

TEST_F(NativeBridgeTest, BackendInitialization) {
    // Test CPU backend initialization
    int result = initialize_backend(0); // CPU_NEON
    EXPECT_EQ(result, 0);
}

TEST_F(NativeBridgeTest, MemoryManagement) {
    // Test memory allocation/deallocation patterns
    const size_t iterations = 1000;
    for (size_t i = 0; i < iterations; ++i) {
        ModelManager manager;
        // Verify no memory leaks
    }
}
```

#### 4.2 Integration Testing
Create comprehensive integration tests:

```kotlin
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class LLMEngineIntegrationTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var llmEngine: LLMEngine
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun testModelLoadingAndUnloading() = runTest {
        // Test requires a small test model (e.g., TinyLlama)
        val testModelPath = "/path/to/test/model.gguf"
        
        val result = llmEngine.loadModel(testModelPath)
        assertTrue(result.isSuccess)
        
        val handle = result.getOrThrow()
        assertNotNull(handle)
        assertEquals(testModelPath, handle.modelPath)
        
        llmEngine.unloadModel(handle)
        assertFalse(llmEngine.isModelLoaded(testModelPath))
    }
    
    @Test
    fun testTextGeneration() = runTest {
        // Load test model
        val result = llmEngine.loadModel("/path/to/test/model.gguf")
        assertTrue(result.isSuccess)
        
        // Generate text
        val params = GenerationParams(maxTokens = 10)
        val tokens = llmEngine.generateText("Hello", params).take(5).toList()
        
        assertTrue(tokens.isNotEmpty())
        tokens.forEach { token ->
            assertFalse(token.isBlank())
        }
    }
    
    @Test
    fun testEmbeddingGeneration() = runTest {
        // Load embedding model
        val result = llmEngine.loadModel("/path/to/embedding/model.gguf")
        assertTrue(result.isSuccess)
        
        val embedding = llmEngine.embed("test text")
        assertTrue(embedding.isNotEmpty())
        assertTrue(embedding.size >= 384) // Minimum embedding size
    }
    
    @Test
    fun testConcurrentGenerations() = runTest {
        val result = llmEngine.loadModel("/path/to/test/model.gguf")
        assertTrue(result.isSuccess)
        
        // Start multiple concurrent generations
        val jobs = (1..3).map { i ->
            async {
                llmEngine.generateText("Prompt $i", GenerationParams(maxTokens = 5))
                    .take(3).toList()
            }
        }
        
        val results = jobs.awaitAll()
        assertEquals(3, results.size)
        results.forEach { tokens ->
            assertTrue(tokens.isNotEmpty())
        }
    }
}
```

## 🧪 Testing Strategy

### Unit Tests
- [ ] **JNI Bridge Validation**
  - Parameter marshaling correctness
  - Memory management (no leaks)
  - Exception handling and propagation
  - Thread safety for concurrent access

### Integration Tests
- [ ] **Model Lifecycle Testing**
  - Loading various GGUF model formats
  - Unloading and memory cleanup
  - Multiple model management
  - Error recovery scenarios

### Performance Tests
- [ ] **Backend Performance**
  - CPU vs GPU inference speed
  - Memory usage patterns
  - Thermal behavior under load
  - Battery consumption metrics

### Device Tests
- [ ] **Multi-Device Validation**
  - Snapdragon devices (OpenCL path)
  - Exynos devices (Vulkan path)
  - Various RAM configurations
  - Different Android API levels

## ✅ Acceptance Criteria

### Primary Criteria
- [ ] **Multi-Backend Support**: CPU, OpenCL, and Vulkan backends functional
- [ ] **Model Loading**: Successfully loads and unloads GGUF models
- [ ] **Text Generation**: Streaming token generation works correctly
- [ ] **Embedding Generation**: Vector embeddings generated accurately
- [ ] **Memory Management**: No memory leaks under sustained usage

### Performance Criteria
- [ ] **Inference Speed**: >5 tokens/second on mid-range devices (CPU)
- [ ] **Model Loading**: <30 seconds for 7B Q4_0 model on flagship devices
- [ ] **Memory Efficiency**: Peak RAM usage <2GB for 7B model
- [ ] **Thermal Stability**: No thermal shutdowns during 10-minute inference

### Quality Criteria
- [ ] **Error Handling**: Graceful error recovery and user feedback
- [ ] **Thread Safety**: Concurrent operations handle correctly
- [ ] **Resource Cleanup**: Proper cleanup on app termination
- [ ] **API Compliance**: All interface contracts fulfilled

## 🔗 Related Issues
- **Depends on**: #01 (Core Architecture)
- **Enables**: #03 (Hardware Detection), #05 (Chat Engine)
- **Related**: #04 (Model Management), #10 (Safety Engine)

## 📋 Definition of Done
- [ ] Native llama.cpp integration complete with all backends
- [ ] JNI bridge implemented with comprehensive error handling
- [ ] Kotlin LLM engine implementation functional
- [ ] Comprehensive test suite passing (unit + integration)
- [ ] Performance benchmarks meet acceptance criteria
- [ ] Memory management validated (no leaks)
- [ ] Documentation updated with API specifications
- [ ] Code review completed and approved

---

**Note**: This implementation provides the core inference capability. Subsequent issues will build upon this foundation to add model management, hardware optimization, and feature-specific functionality.