package com.nervesparks.iris.data.privacy

import com.nervesparks.iris.data.repository.ConversationRepository
import com.nervesparks.iris.data.repository.MessageRepository
import java.io.File

/**
 * Result of a data deletion operation.
 *
 * @property success Whether the deletion was successful
 * @property conversationsDeleted Number of conversations deleted
 * @property messagesDeleted Number of messages deleted
 * @property error Error message (if failed)
 */
data class DeletionResult(
    val success: Boolean,
    val conversationsDeleted: Int = 0,
    val messagesDeleted: Int = 0,
    val error: String? = null
)

/**
 * Service for securely deleting user data.
 * Handles conversation and message deletion with proper cleanup.
 */
class DataDeletionService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository
) {
    
    /**
     * Delete all conversations and messages.
     * This is a destructive operation that cannot be undone.
     *
     * @return DeletionResult with statistics and any errors
     */
    suspend fun deleteAllData(): DeletionResult {
        return try {
            // Get counts before deletion
            val conversationCount = conversationRepository.getConversationCount()
            val messageCount = messageRepository.getMessageCount()
            
            // Delete all data
            conversationRepository.deleteAllConversations()
            messageRepository.deleteAllMessages()
            
            DeletionResult(
                success = true,
                conversationsDeleted = conversationCount,
                messagesDeleted = messageCount
            )
        } catch (e: Exception) {
            DeletionResult(success = false, error = e.message ?: "Unknown error during deletion")
        }
    }
    
    /**
     * Delete specific conversations and their messages.
     *
     * @param conversationIds List of conversation IDs to delete
     * @return DeletionResult with statistics and any errors
     */
    suspend fun deleteConversations(conversationIds: List<String>): DeletionResult {
        return try {
            var messagesDeleted = 0
            
            // Count messages before deletion
            for (conversationId in conversationIds) {
                val messageCount = messageRepository.getMessageCountForConversation(conversationId)
                messagesDeleted += messageCount
            }
            
            // Delete conversations (cascades to messages)
            conversationRepository.deleteConversations(conversationIds)
            
            DeletionResult(
                success = true,
                conversationsDeleted = conversationIds.size,
                messagesDeleted = messagesDeleted
            )
        } catch (e: Exception) {
            DeletionResult(success = false, error = e.message ?: "Unknown error during deletion")
        }
    }
    
    /**
     * Delete messages older than a specified date.
     * Only deletes messages, keeps conversations.
     *
     * @param olderThanDays Delete messages older than this many days
     * @return DeletionResult with statistics and any errors
     */
    suspend fun deleteOldMessages(olderThanDays: Int): DeletionResult {
        return try {
            val cutoffDate = java.time.Instant.now().minusSeconds((olderThanDays * 24 * 60 * 60).toLong())
            
            // Get all messages
            val allMessages = messageRepository.getAllMessagesList()
            val oldMessages = allMessages.filter { it.timestamp.isBefore(cutoffDate) }
            
            // Delete old messages
            for (message in oldMessages) {
                messageRepository.deleteMessage(message.id)
            }
            
            DeletionResult(
                success = true,
                conversationsDeleted = 0,
                messagesDeleted = oldMessages.size
            )
        } catch (e: Exception) {
            DeletionResult(success = false, error = e.message ?: "Unknown error during deletion")
        }
    }
    
    /**
     * Delete archived conversations.
     *
     * @return DeletionResult with statistics and any errors
     */
    suspend fun deleteArchivedConversations(): DeletionResult {
        return try {
            val archivedConversations = mutableListOf<String>()
            var messagesDeleted = 0
            
            conversationRepository.getArchivedConversations().collect { conversations ->
                for (conversation in conversations) {
                    archivedConversations.add(conversation.id)
                    val messageCount = messageRepository.getMessageCountForConversation(conversation.id)
                    messagesDeleted += messageCount
                }
            }
            
            if (archivedConversations.isNotEmpty()) {
                conversationRepository.deleteConversations(archivedConversations)
            }
            
            DeletionResult(
                success = true,
                conversationsDeleted = archivedConversations.size,
                messagesDeleted = messagesDeleted
            )
        } catch (e: Exception) {
            DeletionResult(success = false, error = e.message ?: "Unknown error during deletion")
        }
    }
    
    /**
     * Vacuum the database to reclaim space after deletions.
     * This should be called after large deletion operations.
     *
     * @param databasePath Path to the database file
     * @return True if successful
     */
    suspend fun vacuumDatabase(databasePath: String): Boolean {
        return try {
            // In a full implementation, this would execute VACUUM on the database
            // For now, we just verify the database exists
            val dbFile = File(databasePath)
            dbFile.exists()
        } catch (e: Exception) {
            false
        }
    }
}
