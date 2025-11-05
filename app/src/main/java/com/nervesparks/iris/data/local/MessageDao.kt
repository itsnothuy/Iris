package com.nervesparks.iris.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for message persistence operations.
 * 
 * Provides methods to insert, query, and delete messages from the local database.
 */
@Dao
interface MessageDao {
    
    /**
     * Insert a new message into the database.
     * If a message with the same ID exists, it will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
    
    /**
     * Insert multiple messages into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)
    
    /**
     * Get all messages ordered by timestamp (oldest first).
     * Returns a Flow for reactive updates.
     */
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>
    
    /**
     * Get all messages as a list (for one-time queries).
     */
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesList(): List<MessageEntity>
    
    /**
     * Get messages for a specific conversation.
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>
    
    /**
     * Get messages for a specific conversation as a list.
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesForConversationList(conversationId: String): List<MessageEntity>
    
    /**
     * Delete all messages for a specific conversation.
     */
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)
    
    /**
     * Get the count of messages in a specific conversation.
     */
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCountForConversation(conversationId: String): Int
    
    /**
     * Delete all messages from the database.
     */
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
    
    /**
     * Delete a specific message by ID.
     */
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)
    
    /**
     * Get the count of messages in the database.
     */
    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getMessageCount(): Int
}
