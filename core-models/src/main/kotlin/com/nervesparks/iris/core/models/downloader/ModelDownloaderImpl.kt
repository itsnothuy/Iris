package com.nervesparks.iris.core.models.downloader

import android.content.Context
import android.util.Log
import com.nervesparks.iris.core.models.ModelDescriptor
import com.nervesparks.iris.core.models.storage.ModelStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ModelDownloader interface
 */
@Singleton
class ModelDownloaderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelStorage: ModelStorage
) : ModelDownloader {
    
    companion object {
        private const val TAG = "ModelDownloader"
        private const val DOWNLOAD_CHUNK_SIZE = 8192
        private const val TEMP_FILE_SUFFIX = ".download"
        private const val CONNECT_TIMEOUT_SECONDS = 30L
        private const val READ_TIMEOUT_SECONDS = 60L
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    private val activeDownloads = ConcurrentHashMap<String, DownloadStatus>()
    
    override fun downloadModel(
        modelDescriptor: ModelDescriptor,
        destinationPath: String
    ): Flow<DownloadEvent> = flow {
        val modelId = modelDescriptor.id
        
        try {
            // Mark as started
            val status = DownloadStatus(
                modelId = modelId,
                state = DownloadState.DOWNLOADING,
                bytesDownloaded = 0L,
                totalBytes = modelDescriptor.fileSize,
                startTime = System.currentTimeMillis()
            )
            activeDownloads[modelId] = status
            
            emit(DownloadEvent.Started(modelId, modelDescriptor.fileSize))
            
            // Create destination file
            val destinationFile = File(destinationPath)
            val tempFile = File(destinationPath + TEMP_FILE_SUFFIX)
            
            // Download the file
            val request = Request.Builder()
                .url(modelDescriptor.downloadUrl)
                .build()
            
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Download failed: HTTP ${response.code}")
                    }
                    
                    val body = response.body ?: throw Exception("Response body is null")
                    val totalBytes = body.contentLength()
                    
                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(DOWNLOAD_CHUNK_SIZE)
                            var bytesDownloaded = 0L
                            var bytesRead: Int
                            
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                bytesDownloaded += bytesRead
                                
                                // Update status and emit progress
                                activeDownloads[modelId] = status.copy(
                                    bytesDownloaded = bytesDownloaded
                                )
                                
                                emit(DownloadEvent.Progress(modelId, bytesDownloaded, totalBytes))
                            }
                        }
                    }
                }
            }
            
            // Verify integrity of temp file
            emit(DownloadEvent.Verifying(modelId))
            activeDownloads[modelId] = status.copy(state = DownloadState.VERIFYING)
            
            val isValid = verifyFileSha256(tempFile, modelDescriptor.sha256)
            
            if (!isValid) {
                tempFile.delete()
                throw Exception("SHA-256 verification failed")
            }
            
            // Move temp file to final destination
            if (tempFile.renameTo(destinationFile)) {
                // Save metadata
                modelStorage.saveModelMetadata(modelDescriptor, destinationFile.absolutePath)
                
                activeDownloads[modelId] = status.copy(
                    state = DownloadState.COMPLETED,
                    bytesDownloaded = modelDescriptor.fileSize
                )
                
                emit(DownloadEvent.Completed(modelId, destinationFile.absolutePath))
                Log.i(TAG, "Successfully downloaded model $modelId")
            } else {
                throw Exception("Failed to move temporary file to destination")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download model $modelId", e)
            activeDownloads[modelId] = DownloadStatus(
                modelId = modelId,
                state = DownloadState.FAILED,
                bytesDownloaded = 0L,
                totalBytes = modelDescriptor.fileSize,
                startTime = System.currentTimeMillis(),
                error = e.message
            )
            emit(DownloadEvent.Failed(modelId, e))
        } finally {
            // Clean up temp file if it exists
            val tempFile = File(destinationPath + TEMP_FILE_SUFFIX)
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }
    
    override suspend fun cancelDownload(modelId: String): Result<Unit> {
        return try {
            val status = activeDownloads[modelId]
            if (status != null) {
                activeDownloads[modelId] = status.copy(state = DownloadState.CANCELLED)
                // Note: OkHttp doesn't support cancellation of ongoing requests easily
                // In a production implementation, we'd need to track Call objects and cancel them
                Log.i(TAG, "Cancelled download for model $modelId")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Download not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel download for $modelId", e)
            Result.failure(e)
        }
    }
    
    override fun resumeDownload(modelId: String): Flow<DownloadEvent> {
        // For now, resuming is not implemented
        // In a production implementation, we'd support range requests
        return flow {
            emit(DownloadEvent.Failed(modelId, Exception("Resume not yet implemented")))
        }
    }
    
    override suspend fun getDownloadStatus(modelId: String): DownloadStatus? {
        return activeDownloads[modelId]
    }
    
    override suspend fun getActiveDownloads(): List<DownloadStatus> {
        return activeDownloads.values.filter { 
            it.state == DownloadState.DOWNLOADING || it.state == DownloadState.QUEUED 
        }
    }
    
    private fun verifyFileSha256(file: File, expectedSha256: String): Boolean {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            java.io.FileInputStream(file).use { fis ->
                val buffer = ByteArray(DOWNLOAD_CHUNK_SIZE)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
            actualSha256.equals(expectedSha256, ignoreCase = true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify SHA-256", e)
            false
        }
    }
}
