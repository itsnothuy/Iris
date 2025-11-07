#include "generation_engine.h"
#include <android/log.h>
#include <chrono>
#include <stdexcept>

#define LOG_TAG "IrisGenerationEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

GenerationEngine::GenerationEngine(ModelManager* modelManager,
                                 float temperature, int topK, float topP, int maxTokens)
    : modelManager(modelManager),
      context(modelManager->getContext()),
      currentTokenIndex(0),
      maxTokens(maxTokens),
      isComplete(false),
      temperature(temperature),
      topK(topK),
      topP(topP) {
}

GenerationEngine::~GenerationEngine() {
    // Nothing to cleanup - context is owned by ModelManager
}

long GenerationEngine::startGeneration(const std::string& prompt) {
    if (!modelManager || !context) {
        throw std::runtime_error("Model not initialized");
    }
    
    try {
        llama_model* model = modelManager->getModel();
        
        // Tokenize prompt
        tokens.resize(prompt.length() + 1);
        int n_tokens = llama_tokenize(model, prompt.c_str(), prompt.length(),
                                      tokens.data(), tokens.size(), true, false);
        if (n_tokens < 0) {
            tokens.resize(-n_tokens);
            n_tokens = llama_tokenize(model, prompt.c_str(), prompt.length(),
                                     tokens.data(), tokens.size(), true, false);
        }
        tokens.resize(n_tokens);
        
        // Clear KV cache
        llama_kv_cache_clear(context);
        
        // Process prompt tokens
        llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
        
        if (llama_decode(context, batch) != 0) {
            throw std::runtime_error("Failed to process prompt");
        }
        
        currentTokenIndex = tokens.size();
        isComplete = false;
        
        // Return session ID based on timestamp
        auto now = std::chrono::system_clock::now();
        long sessionId = std::chrono::duration_cast<std::chrono::milliseconds>(
            now.time_since_epoch()).count();
        
        LOGI("Generation started with session ID: %ld", sessionId);
        return sessionId;
        
    } catch (const std::exception& e) {
        LOGE("Failed to start generation: %s", e.what());
        throw;
    }
}

std::string GenerationEngine::generateNextToken() {
    if (isComplete || !modelManager || !context) {
        return "";
    }
    
    try {
        // Check if we've reached max tokens
        if (currentTokenIndex >= static_cast<size_t>(maxTokens)) {
            isComplete = true;
            return "";
        }
        
        // Sample next token
        llama_token token = sampleToken();
        
        // Check for end of sequence
        llama_model* model = modelManager->getModel();
        if (llama_token_is_eog(model, token)) {
            isComplete = true;
            return "";
        }
        
        // Convert token to text
        char buffer[256];
        int n = llama_token_to_piece(model, token, buffer, sizeof(buffer), 0, false);
        if (n < 0) {
            throw std::runtime_error("Failed to convert token to text");
        }
        
        std::string text(buffer, n);
        
        // Add token to context for next iteration
        tokens.push_back(token);
        llama_batch batch = llama_batch_get_one(&token, 1);
        
        if (llama_decode(context, batch) != 0) {
            LOGE("Failed to decode token");
            isComplete = true;
            return "";
        }
        
        currentTokenIndex++;
        
        return text;
        
    } catch (const std::exception& e) {
        LOGE("Token generation failed: %s", e.what());
        isComplete = true;
        return "";
    }
}

llama_token GenerationEngine::sampleToken() {
    llama_model* model = modelManager->getModel();
    
    // Get logits
    const float* logits = llama_get_logits(context);
    int n_vocab = llama_n_vocab(model);
    
    // Create candidates array
    std::vector<llama_token_data> candidates;
    candidates.reserve(n_vocab);
    
    for (llama_token token_id = 0; token_id < n_vocab; token_id++) {
        candidates.push_back({token_id, logits[token_id], 0.0f});
    }
    
    llama_token_data_array candidates_p = {
        candidates.data(),
        candidates.size(),
        false
    };
    
    // Apply temperature
    llama_sample_temp(context, &candidates_p, temperature);
    
    // Apply top-k sampling
    llama_sample_top_k(context, &candidates_p, topK, 1);
    
    // Apply top-p sampling
    llama_sample_top_p(context, &candidates_p, topP, 1);
    
    // Sample token
    llama_token token = llama_sample_token(context, &candidates_p);
    
    return token;
}

std::string GenerationEngine::getModelId() const {
    return modelManager ? modelManager->getModelId() : "";
}

void GenerationEngine::cancel() {
    isComplete = true;
}
