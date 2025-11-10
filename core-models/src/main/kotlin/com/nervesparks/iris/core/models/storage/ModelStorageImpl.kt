package com.nervesparks.iris.core.models.storage

import android.content.Context
import android.os.StatFs
import android.util.Log
import com.google.gson.Gson
import com.nervesparks.iris.core.models.ModelDescriptor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ModelStorage interface
 */
@Singleton
class ModelStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ModelStorage {
    
    companion object {
        private const val TAG = "ModelStorage"
        private const val MODELS_DIR = "models"
        private const val METADATA_SUFFIX = ".metadata.json"
        private const val BUFFER_SIZE = 8192
    }
    
    private val gson = Gson()
    
    override fun getModelsDirectory(): String {
        return File(context.getExternalFilesDir(null), MODELS_DIR).also { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }.absolutePath
    }
    
    override suspend fun isModelStored(modelId: String): Boolean = withContext(Dispatchers.IO) {
        val modelFile = File(getModelsDirectory(), "$modelId.gguf")
        modelFile.exists()
    }
    
    override suspend fun getModelPath(modelId: String): String? = withContext(Dispatchers.IO) {
        val modelFile = File(getModelsDirectory(), "$modelId.gguf")
        if (modelFile.exists()) modelFile.absolutePath else null
    }
    
    override suspend fun saveModelMetadata(
        modelDescriptor: ModelDescriptor,
        filePath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val metadataFile = File(getModelsDirectory(), "${modelDescriptor.id}$METADATA_SUFFIX")
            val json = gson.toJson(modelDescriptor)
            metadataFile.writeText(json)
            Log.i(TAG, "Saved metadata for model ${modelDescriptor.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save metadata for model ${modelDescriptor.id}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getModelMetadata(modelId: String): ModelDescriptor? = withContext(Dispatchers.IO) {
        try {
            val metadataFile = File(getModelsDirectory(), "$modelId$METADATA_SUFFIX")
            if (!metadataFile.exists()) {
                return@withContext null
            }
            val json = metadataFile.readText()
            gson.fromJson(json, ModelDescriptor::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read metadata for model $modelId", e)
            null
        }
    }
    
    override suspend fun deleteModel(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(getModelsDirectory(), "$modelId.gguf")
            val metadataFile = File(getModelsDirectory(), "$modelId$METADATA_SUFFIX")
            
            var deleted = false
            if (modelFile.exists()) {
                deleted = modelFile.delete()
            }
            
            if (metadataFile.exists()) {
                metadataFile.delete()
            }
            
            if (deleted || !modelFile.exists()) {
                Log.i(TAG, "Deleted model $modelId")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete model file"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete model $modelId", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getStoredModels(): List<ModelDescriptor> = withContext(Dispatchers.IO) {
        try {
            val modelsDir = File(getModelsDirectory())
            modelsDir.listFiles { file ->
                file.name.endsWith(METADATA_SUFFIX)
            }?.mapNotNull { metadataFile ->
                try {
                    val json = metadataFile.readText()
                    gson.fromJson(json, ModelDescriptor::class.java)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse metadata file ${metadataFile.name}", e)
                    null
                }
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get stored models", e)
            emptyList()
        }
    }
    
    override suspend fun getAvailableSpace(): Long = withContext(Dispatchers.IO) {
        try {
            val modelsDir = File(getModelsDirectory())
            val stat = StatFs(modelsDir.path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get available space", e)
            0L
        }
    }
    
    override suspend fun verifyModelIntegrity(
        modelId: String,
        expectedSha256: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(getModelsDirectory(), "$modelId.gguf")
            if (!modelFile.exists()) {
                Log.w(TAG, "Model file does not exist: $modelId")
                return@withContext false
            }
            
            val actualSha256 = calculateSha256(modelFile)
            val match = actualSha256.equals(expectedSha256, ignoreCase = true)
            
            if (!match) {
                Log.w(TAG, "SHA-256 mismatch for model $modelId. Expected: $expectedSha256, Actual: $actualSha256")
            } else {
                Log.i(TAG, "SHA-256 verification passed for model $modelId")
            }
            
            match
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify model integrity for $modelId", e)
            false
        }
    }
    
    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
