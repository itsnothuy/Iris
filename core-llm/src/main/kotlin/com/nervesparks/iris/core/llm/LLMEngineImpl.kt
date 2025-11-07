package com.nervesparks.iris.core.llm

import android.util.Log
import com.nervesparks.iris.common.error.ModelException
import com.nervesparks.iris.common.models.ComputeTask
import com.nervesparks.iris.common.models.GenerationParams
import com.nervesparks.iris.common.models.ModelHandle
import com.nervesparks.iris.common.models.ModelInfo
import com.nervesparks.iris.core.hw.BackendRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Native llama.cpp implementation of LLMEngine
 */
@Singleton
class LLMEngineImpl @Inject constructor(
    private val backendRouter: BackendRouter
) : LLMEngine {
    
    companion object {
        private const val TAG = "LLMEngineImpl"
        
        init {
            try {
                System.loadLibrary("iris_llm")
                Log.i(TAG, "Native LLM library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native LLM library", e)
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
            
            // Create load parameters with reasonable defaults
            val params = ModelLoadParams(
                contextSize = 2048,
                threads = 4,
                seed = -1L
            )
            
            // Load model natively
            val modelId = nativeLoadModel(modelPath, params)
                ?: return@withContext Result.failure(ModelException("Native model loading failed"))
            
            // Create model handle
            val handle = ModelHandle(
                id = modelId,
                modelPath = modelPath,
                contextSize = params.contextSize,
                vocabSize = 32000, // Default, actual value would need to be queried
                backend = backendRouter.getCurrentBackend()
            )
            
            loadedModels[modelPath] = handle
            Log.i(TAG, "Model loaded successfully: $modelId")
            
            Result.success(handle)
            
        } catch (e: Exception) {
            Log.e(TAG, "Model loading failed", e)
            Result.failure(LLMException("Model loading failed", e))
        }
    }
    
    override suspend fun generateText(prompt: String, params: GenerationParams): Flow<String> = callbackFlow {
        try {
            // Find appropriate model
            val modelHandle = loadedModels.values.firstOrNull()
                ?: throw LLMException("No model loaded")
            
            // Start native generation
            val sessionId = nativeStartGeneration(modelHandle.id, prompt, params)
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
                        
                        // Small delay to prevent tight loop
                        delay(1)
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "Generation failed", e)
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
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Text generation failed", e)
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
            Log.e(TAG, "Embedding generation failed", e)
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
                Log.i(TAG, "Model unloaded: ${handle.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unloading model ${handle.id}", e)
        }
    }
    
    override suspend fun getModelInfo(handle: ModelHandle): ModelInfo {
        // Extract basic info from filename
        val modelName = File(handle.modelPath).nameWithoutExtension
        return ModelInfo(
            name = modelName,
            parameterCount = "Unknown", // Would need native query
            contextSize = handle.contextSize,
            vocabSize = handle.vocabSize
        )
    }
    
    override fun isModelLoaded(modelPath: String): Boolean {
        return loadedModels.containsKey(modelPath)
    }
    
    // Native method declarations
    private external fun nativeInitializeBackend(backendType: Int): Int
    private external fun nativeLoadModel(modelPath: String, params: ModelLoadParams): String?
    private external fun nativeStartGeneration(modelId: String, prompt: String, params: GenerationParams): Long
    private external fun nativeGenerateNextToken(sessionId: Long): String?
    private external fun nativeGenerateEmbedding(modelId: String, text: String): FloatArray?
    private external fun nativeUnloadModel(modelId: String): Boolean
    private external fun nativeShutdown()
}
