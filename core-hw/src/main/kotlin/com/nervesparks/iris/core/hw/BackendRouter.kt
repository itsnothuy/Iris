package com.nervesparks.iris.core.hw

import com.nervesparks.iris.common.models.BackendType
import com.nervesparks.iris.common.models.ComputeTask

/**
 * Interface for selecting and managing inference backends
 */
interface BackendRouter {
    /**
     * Select optimal backend for a compute task
     */
    suspend fun selectOptimalBackend(task: ComputeTask): BackendType
    
    /**
     * Switch to a different backend
     */
    suspend fun switchBackend(newBackend: BackendType): Result<Unit>
    
    /**
     * Get currently active backend
     */
    fun getCurrentBackend(): BackendType
    
    /**
     * Validate if a backend is available and functional
     */
    suspend fun validateBackend(backend: BackendType): Boolean
}
