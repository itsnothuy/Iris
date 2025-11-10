package com.nervesparks.iris.core.models.storage

import com.nervesparks.iris.core.models.ModelDescriptor

/**
 * Interface for managing model file storage
 */
interface ModelStorage {
    /**
     * Get storage directory for models
     */
    fun getModelsDirectory(): String
    
    /**
     * Check if a model is stored locally
     */
    suspend fun isModelStored(modelId: String): Boolean
    
    /**
     * Get file path for a stored model
     */
    suspend fun getModelPath(modelId: String): String?
    
    /**
     * Save model metadata
     */
    suspend fun saveModelMetadata(modelDescriptor: ModelDescriptor, filePath: String): Result<Unit>
    
    /**
     * Get model metadata
     */
    suspend fun getModelMetadata(modelId: String): ModelDescriptor?
    
    /**
     * Delete a stored model
     */
    suspend fun deleteModel(modelId: String): Result<Unit>
    
    /**
     * Get list of all stored models
     */
    suspend fun getStoredModels(): List<ModelDescriptor>
    
    /**
     * Get available storage space in bytes
     */
    suspend fun getAvailableSpace(): Long
    
    /**
     * Verify model file integrity
     */
    suspend fun verifyModelIntegrity(modelId: String, expectedSha256: String): Boolean
}
