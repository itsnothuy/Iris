#ifndef IRIS_MODEL_MANAGER_H
#define IRIS_MODEL_MANAGER_H

#include <string>
#include <memory>
#include <vector>
#include "llama.h"

/**
 * Manages llama.cpp model lifecycle
 */
class ModelManager {
public:
    ModelManager();
    ~ModelManager();
    
    /**
     * Load a GGUF model from file
     * @param path Path to model file
     * @param contextSize Context window size
     * @param seed Random seed (-1 for time-based)
     * @param threads Number of threads
     * @return Model ID on success
     */
    std::string loadModel(const std::string& path, int contextSize, 
                         long seed, int threads);
    
    /**
     * Unload the current model
     */
    void unloadModel();
    
    /**
     * Generate embedding for text
     * @param text Input text
     * @return Vector of embeddings
     */
    std::vector<float> generateEmbedding(const std::string& text);
    
    /**
     * Get the model handle
     */
    llama_model* getModel() const;
    
    /**
     * Get the context handle
     */
    llama_context* getContext() const;
    
    /**
     * Get the model ID
     */
    std::string getModelId() const;
    
private:
    llama_model* model;
    llama_context* context;
    std::string modelId;
    
    /**
     * Determine optimal GPU layer count
     */
    int determineGPULayers();
};

#endif // IRIS_MODEL_MANAGER_H
