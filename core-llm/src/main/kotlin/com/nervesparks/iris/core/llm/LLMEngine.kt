package com.nervesparks.iris.core.llm

import com.nervesparks.iris.common.models.GenerationParams
import com.nervesparks.iris.common.models.ModelHandle
import com.nervesparks.iris.common.models.ModelInfo
import kotlinx.coroutines.flow.Flow

/**
 * Interface for LLM operations
 */
interface LLMEngine {
    /**
     * Load a model from disk
     * @param modelPath Path to the GGUF model file
     * @return Result containing ModelHandle on success
     */
    suspend fun loadModel(modelPath: String): Result<ModelHandle>
    
    /**
     * Generate text from a prompt
     * @param prompt Input prompt
     * @param params Generation parameters
     * @return Flow of generated tokens
     */
    suspend fun generateText(prompt: String, params: GenerationParams): Flow<String>
    
    /**
     * Generate embeddings for text
     * @param text Input text
     * @return Float array of embeddings
     */
    suspend fun embed(text: String): FloatArray
    
    /**
     * Unload a model from memory
     * @param handle Model handle to unload
     */
    fun unloadModel(handle: ModelHandle)
    
    /**
     * Get information about a loaded model
     * @param handle Model handle
     * @return Model information
     */
    suspend fun getModelInfo(handle: ModelHandle): ModelInfo
    
    /**
     * Check if a model is currently loaded
     * @param modelPath Path to check
     * @return True if loaded
     */
    fun isModelLoaded(modelPath: String): Boolean
}
