#include "model_manager.h"
#include <android/log.h>
#include <random>
#include <chrono>
#include <sstream>
#include <stdexcept>

#define LOG_TAG "IrisModelManager"
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
    
    std::stringstream ss;
    ss << "model_" << timestamp << "_" << dis(gen);
    modelId = ss.str();
}

ModelManager::~ModelManager() {
    unloadModel();
}

std::string ModelManager::loadModel(const std::string& path, int contextSize, 
                                   long seed, int threads) {
    try {
        LOGI("Loading model from: %s", path.c_str());
        
        // Set up model parameters
        llama_model_params modelParams = llama_model_default_params();
        modelParams.n_gpu_layers = 0; // CPU only for now
        
        // Load model
        model = llama_load_model_from_file(path.c_str(), modelParams);
        if (!model) {
            throw std::runtime_error("Failed to load model from " + path);
        }
        
        // Set up context parameters
        llama_context_params contextParams = llama_context_default_params();
        contextParams.seed = (seed == -1) ? static_cast<uint32_t>(std::time(nullptr)) : static_cast<uint32_t>(seed);
        contextParams.n_ctx = contextSize;
        contextParams.n_threads = (threads <= 0) ? 4 : threads;
        contextParams.n_threads_batch = contextParams.n_threads;
        
        // Create context
        context = llama_new_context_with_model(model, contextParams);
        if (!context) {
            llama_free_model(model);
            model = nullptr;
            throw std::runtime_error("Failed to create context");
        }
        
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
    
    LOGI("Model unloaded: %s", modelId.c_str());
}

std::vector<float> ModelManager::generateEmbedding(const std::string& text) {
    if (!model || !context) {
        throw std::runtime_error("Model not loaded");
    }
    
    try {
        // Tokenize input
        std::vector<llama_token> tokens;
        tokens.resize(text.length() + 1);
        int n_tokens = llama_tokenize(model, text.c_str(), text.length(), 
                                      tokens.data(), tokens.size(), true, false);
        if (n_tokens < 0) {
            tokens.resize(-n_tokens);
            n_tokens = llama_tokenize(model, text.c_str(), text.length(), 
                                     tokens.data(), tokens.size(), true, false);
        }
        tokens.resize(n_tokens);
        
        // Clear context
        llama_kv_cache_clear(context);
        
        // Get embedding dimension
        int n_embd = llama_n_embd(model);
        std::vector<float> embedding(n_embd, 0.0f);
        
        // Create batch
        llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
        
        // Decode
        if (llama_decode(context, batch) != 0) {
            throw std::runtime_error("Failed to decode tokens for embedding");
        }
        
        // Extract embedding
        const float* embeddingData = llama_get_embeddings(context);
        if (!embeddingData) {
            throw std::runtime_error("Failed to get embeddings");
        }
        
        std::copy(embeddingData, embeddingData + n_embd, embedding.begin());
        
        return embedding;
        
    } catch (const std::exception& e) {
        LOGE("Embedding generation failed: %s", e.what());
        throw;
    }
}

int ModelManager::determineGPULayers() {
    // TODO: Implement hardware-specific GPU layer determination
    return 0; // CPU-only for now
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
