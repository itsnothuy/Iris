#ifndef IRIS_GENERATION_ENGINE_H
#define IRIS_GENERATION_ENGINE_H

#include <string>
#include <vector>
#include "llama.h"
#include "model_manager.h"

/**
 * Manages text generation with llama.cpp
 */
class GenerationEngine {
public:
    /**
     * Constructor
     * @param modelManager Model manager instance
     * @param temperature Sampling temperature
     * @param topK Top-K sampling parameter
     * @param topP Top-P sampling parameter
     * @param maxTokens Maximum tokens to generate
     */
    GenerationEngine(ModelManager* modelManager, 
                    float temperature, int topK, float topP, int maxTokens);
    ~GenerationEngine();
    
    /**
     * Start generation with a prompt
     * @param prompt Input prompt
     * @return Session ID
     */
    long startGeneration(const std::string& prompt);
    
    /**
     * Generate next token
     * @return Generated token, empty if complete
     */
    std::string generateNextToken();
    
    /**
     * Get the model ID this engine is using
     */
    std::string getModelId() const;
    
    /**
     * Cancel ongoing generation
     */
    void cancel();
    
private:
    ModelManager* modelManager;
    llama_context* context;
    std::vector<llama_token> tokens;
    size_t currentTokenIndex;
    int maxTokens;
    bool isComplete;
    
    // Sampling parameters
    float temperature;
    int topK;
    float topP;
    
    /**
     * Sample next token using configured parameters
     */
    llama_token sampleToken();
};

#endif // IRIS_GENERATION_ENGINE_H
