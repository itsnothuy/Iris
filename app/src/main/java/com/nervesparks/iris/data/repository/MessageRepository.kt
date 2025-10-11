package com.nervesparks.iris.data.repository

import com.nervesparks.iris.data.Message
import com.nervesparks.iris.data.local.AppDatabase
import com.nervesparks.iris.data.local.MessageMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing message persistence operations.
 * 
 * Provides a clean abstraction over the database layer, handling
 * conversion between domain models and database entities.
 */
class MessageRepository(private val database: AppDatabase) {
    
    private val messageDao = database.messageDao()
    
    /**
     * Save a message to the database.
     */
    suspend fun saveMessage(message: Message) {
        val entity = MessageMapper.toEntity(message)
        messageDao.insertMessage(entity)
    }
    
    /**
     * Save multiple messages to the database.
     */
    suspend fun saveMessages(messages: List<Message>) {
        val entities = MessageMapper.toEntityList(messages)
        messageDao.insertMessages(entities)
    }
    
    /**
     * Get all messages as a Flow for reactive updates.
     */
    fun getAllMessages(): Flow<List<Message>> {
        return messageDao.getAllMessages().map { entities ->
            MessageMapper.toDomainList(entities)
        }
    }
    
    /**
     * Get all messages as a list (for one-time queries).
     */
    suspend fun getAllMessagesList(): List<Message> {
        val entities = messageDao.getAllMessagesList()
        return MessageMapper.toDomainList(entities)
    }
    
    /**
     * Delete all messages from the database.
     */
    suspend fun deleteAllMessages() {
        messageDao.deleteAllMessages()
    }
    
    /**
     * Delete a specific message by ID.
     */
    suspend fun deleteMessage(messageId: String) {
        messageDao.deleteMessage(messageId)
    }
    
    /**
     * Get the count of messages in the database.
     */
    suspend fun getMessageCount(): Int {
        return messageDao.getMessageCount()
    }
}
