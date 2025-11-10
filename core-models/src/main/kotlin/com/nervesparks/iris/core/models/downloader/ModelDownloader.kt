package com.nervesparks.iris.core.models.downloader

import com.nervesparks.iris.core.models.ModelDescriptor
import kotlinx.coroutines.flow.Flow

/**
 * Interface for downloading AI models
 */
interface ModelDownloader {
    /**
     * Start downloading a model
     * @return Flow of download progress events
     */
    fun downloadModel(
        modelDescriptor: ModelDescriptor,
        destinationPath: String
    ): Flow<DownloadEvent>
    
    /**
     * Cancel an ongoing download
     */
    suspend fun cancelDownload(modelId: String): Result<Unit>
    
    /**
     * Resume a previously interrupted download
     */
    fun resumeDownload(modelId: String): Flow<DownloadEvent>
    
    /**
     * Get current download status for a model
     */
    suspend fun getDownloadStatus(modelId: String): DownloadStatus?
    
    /**
     * Get list of all active downloads
     */
    suspend fun getActiveDownloads(): List<DownloadStatus>
}

/**
 * Download event types
 */
sealed class DownloadEvent {
    data class Started(val modelId: String, val totalBytes: Long) : DownloadEvent()
    data class Progress(val modelId: String, val bytesDownloaded: Long, val totalBytes: Long) : DownloadEvent()
    data class Verifying(val modelId: String) : DownloadEvent()
    data class Completed(val modelId: String, val filePath: String) : DownloadEvent()
    data class Failed(val modelId: String, val error: Throwable) : DownloadEvent()
    data class Cancelled(val modelId: String) : DownloadEvent()
}

/**
 * Download status
 */
data class DownloadStatus(
    val modelId: String,
    val state: DownloadState,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val startTime: Long,
    val error: String? = null
)

/**
 * Download state enumeration
 */
enum class DownloadState {
    QUEUED,
    DOWNLOADING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED
}
