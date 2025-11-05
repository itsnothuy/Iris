package com.nervesparks.iris.data.privacy

import com.nervesparks.iris.data.repository.ConversationRepository
import com.nervesparks.iris.data.repository.MessageRepository
import java.io.File
import java.time.Instant

/**
 * Information about data storage and usage.
 *
 * @property totalConversations Total number of conversations stored
 * @property totalMessages Total number of messages stored
 * @property storageBytes Total storage space used in bytes
 * @property oldestConversation Timestamp of the oldest conversation
 * @property newestConversation Timestamp of the newest conversation
 * @property dataEncrypted Whether data is encrypted at rest
 * @property networkActivity Whether any network activity has occurred
 * @property exportHistory List of export events
 */
data class PrivacyAuditInfo(
    val totalConversations: Int,
    val totalMessages: Int,
    val storageBytes: Long,
    val oldestConversation: Instant?,
    val newestConversation: Instant?,
    val dataEncrypted: Boolean,
    val networkActivity: Boolean,
    val exportHistory: List<ExportEvent>
)

/**
 * Record of an export event.
 *
 * @property timestamp When the export occurred
 * @property format Export format used
 * @property conversationCount Number of conversations exported
 */
data class ExportEvent(
    val timestamp: Instant,
    val format: String,
    val conversationCount: Int
)

/**
 * Service for auditing data usage and providing privacy transparency.
 * Analyzes stored data and provides comprehensive privacy information.
 */
class PrivacyAuditService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val databasePath: String
) {
    
    /**
     * Generate a comprehensive privacy audit report.
     *
     * @return PrivacyAuditInfo with complete data usage information
     */
    suspend fun generateAuditReport(): PrivacyAuditInfo {
        val conversationCount = conversationRepository.getConversationCount()
        val messageCount = messageRepository.getMessageCount()
        
        // Get conversation dates
        var oldestDate: Instant? = null
        var newestDate: Instant? = null
        
        conversationRepository.getAllConversationsIncludingArchived().collect { conversations ->
            if (conversations.isNotEmpty()) {
                oldestDate = conversations.minByOrNull { it.createdAt }?.createdAt
                newestDate = conversations.maxByOrNull { it.lastModified }?.lastModified
            }
        }
        
        // Calculate storage size
        val databaseFile = File(databasePath)
        val storageBytes = if (databaseFile.exists()) {
            calculateDirectorySize(databaseFile.parentFile)
        } else {
            0L
        }
        
        // Check encryption status (Room database uses SQLCipher if configured)
        val dataEncrypted = isDataEncrypted()
        
        // Network activity is always false for this on-device app
        val networkActivity = false
        
        // Export history (empty for now, could be tracked in preferences later)
        val exportHistory = emptyList<ExportEvent>()
        
        return PrivacyAuditInfo(
            totalConversations = conversationCount,
            totalMessages = messageCount,
            storageBytes = storageBytes,
            oldestConversation = oldestDate,
            newestConversation = newestDate,
            dataEncrypted = dataEncrypted,
            networkActivity = networkActivity,
            exportHistory = exportHistory
        )
    }
    
    /**
     * Get storage breakdown by type.
     *
     * @return Map of storage type to bytes used
     */
    suspend fun getStorageBreakdown(): Map<String, Long> {
        val breakdown = mutableMapOf<String, Long>()
        
        val databaseFile = File(databasePath)
        if (databaseFile.exists()) {
            breakdown["Database"] = databaseFile.length()
            
            // Check for additional database files (WAL, SHM)
            val walFile = File("$databasePath-wal")
            if (walFile.exists()) {
                breakdown["Database WAL"] = walFile.length()
            }
            
            val shmFile = File("$databasePath-shm")
            if (shmFile.exists()) {
                breakdown["Database SHM"] = shmFile.length()
            }
        }
        
        return breakdown
    }
    
    /**
     * Verify data integrity.
     *
     * @return True if data integrity checks pass
     */
    suspend fun verifyDataIntegrity(): Boolean {
        return try {
            // Check that conversation counts match
            val conversationCount = conversationRepository.getConversationCount()
            
            // Verify messages exist for conversations
            var allValid = true
            conversationRepository.getAllConversations().collect { conversations ->
                for (conversation in conversations) {
                    val messageCount = messageRepository.getMessageCountForConversation(conversation.id)
                    if (messageCount != conversation.messageCount) {
                        allValid = false
                        break
                    }
                }
            }
            
            allValid
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Calculate total size of a directory recursively.
     */
    private fun calculateDirectorySize(directory: File?): Long {
        if (directory == null || !directory.exists()) return 0L
        
        var size = 0L
        directory.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }
    
    /**
     * Check if data is encrypted at rest.
     * For this implementation, we check if SQLCipher is being used.
     */
    private fun isDataEncrypted(): Boolean {
        // In a full implementation, this would check if SQLCipher or similar encryption is enabled
        // For now, we return false as the basic Room setup doesn't include encryption by default
        return false
    }
    
    /**
     * Format bytes to human-readable string.
     */
    fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var value = bytes.toDouble()
        var unitIndex = 0
        
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }
        
        return "%.2f %s".format(value, units[unitIndex])
    }
}
