#include <jni.h>
#include <android/log.h>
#include <memory>
#include <unordered_map>
#include <mutex>
#include <string>
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
    
private:
    NativeState() = default;
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

// Backend initialization
JNIEXPORT jint JNICALL
Java_com_nervesparks_iris_core_llm_LLMEngineImpl_nativeInitializeBackend(
    JNIEnv* env, jobject thiz, jint backend_type) {
    
    try {
        llama_backend_init();
        
        // Log backend type
        switch (backend_type) {
            case 0: // CPU_NEON
                LOGI("Initializing CPU NEON backend");
                break;
            case 1: // OPENCL_ADRENO
                LOGI("Initializing OpenCL Adreno backend (not yet supported)");
                break;
            case 2: // VULKAN_MALI
                LOGI("Initializing Vulkan Mali backend (not yet supported)");
                break;
            default:
                LOGE("Unsupported backend type: %d", backend_type);
                return -1;
        }
        
        return 0; // Success
    } catch (const std::exception& e) {
        LOGE("Backend initialization failed: %s", e.what());
        throwException(env, "java/lang/RuntimeException", e.what());
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
        throwException(env, "java/lang/RuntimeException", e.what());
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
            throwException(env, "java/lang/RuntimeException", "Model not found");
            env->ReleaseStringUTFChars(model_id, modelIdStr);
            env->ReleaseStringUTFChars(prompt, promptStr);
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
        throwException(env, "java/lang/RuntimeException", e.what());
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
        throwException(env, "java/lang/RuntimeException", e.what());
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
            throwException(env, "java/lang/RuntimeException", "Model not found");
            env->ReleaseStringUTFChars(model_id, modelIdStr);
            env->ReleaseStringUTFChars(text, textStr);
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
        throwException(env, "java/lang/RuntimeException", e.what());
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

// Shutdown
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
