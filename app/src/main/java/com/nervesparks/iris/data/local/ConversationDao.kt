package com.nervesparks.iris.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for conversation persistence operations.
 *
 * Provides methods to create, read, update, and delete conversations.
 */
@Dao
interface ConversationDao {

    /**
     * Insert a new conversation into the database.
     * If a conversation with the same ID exists, it will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    /**
     * Update an existing conversation.
     */
    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    /**
     * Get all conversations ordered by pinned status and last modified date.
     * Pinned conversations appear first, then sorted by most recent.
     * Returns a Flow for reactive updates.
     */
    @Query(
        """
        SELECT * FROM conversations 
        WHERE isArchived = 0
        ORDER BY isPinned DESC, lastModified DESC
    """,
    )
    fun getAllConversations(): Flow<List<ConversationEntity>>

    /**
     * Get all conversations including archived ones.
     */
    @Query(
        """
        SELECT * FROM conversations 
        ORDER BY isPinned DESC, lastModified DESC
    """,
    )
    fun getAllConversationsIncludingArchived(): Flow<List<ConversationEntity>>

    /**
     * Get archived conversations.
     */
    @Query(
        """
        SELECT * FROM conversations 
        WHERE isArchived = 1
        ORDER BY lastModified DESC
    """,
    )
    fun getArchivedConversations(): Flow<List<ConversationEntity>>

    /**
     * Get a specific conversation by ID.
     */
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: String): ConversationEntity?

    /**
     * Search conversations by title or content (placeholder for now).
     * Full-text search across message content would require FTS extension.
     */
    @Query(
        """
        SELECT * FROM conversations 
        WHERE title LIKE '%' || :query || '%'
        AND isArchived = 0
        ORDER BY isPinned DESC, lastModified DESC
    """,
    )
    fun searchConversations(query: String): Flow<List<ConversationEntity>>

    /**
     * Delete a specific conversation by ID.
     */
    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    /**
     * Delete multiple conversations by IDs.
     */
    @Query("DELETE FROM conversations WHERE id IN (:conversationIds)")
    suspend fun deleteConversations(conversationIds: List<String>)

    /**
     * Delete all conversations from the database.
     */
    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()

    /**
     * Get the count of conversations in the database.
     */
    @Query("SELECT COUNT(*) FROM conversations WHERE isArchived = 0")
    suspend fun getConversationCount(): Int

    /**
     * Update conversation's last modified timestamp and message count.
     */
    @Query(
        """
        UPDATE conversations 
        SET lastModified = :timestamp, messageCount = :messageCount 
        WHERE id = :conversationId
    """,
    )
    suspend fun updateConversationMetadata(conversationId: String, timestamp: Long, messageCount: Int)

    /**
     * Toggle pin status for a conversation.
     */
    @Query("UPDATE conversations SET isPinned = :isPinned WHERE id = :conversationId")
    suspend fun updatePinStatus(conversationId: String, isPinned: Boolean)

    /**
     * Toggle archive status for a conversation.
     */
    @Query("UPDATE conversations SET isArchived = :isArchived WHERE id = :conversationId")
    suspend fun updateArchiveStatus(conversationId: String, isArchived: Boolean)
}
