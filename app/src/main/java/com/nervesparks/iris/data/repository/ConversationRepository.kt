package com.nervesparks.iris.data.repository

import com.nervesparks.iris.data.Conversation
import com.nervesparks.iris.data.local.AppDatabase
import com.nervesparks.iris.data.local.ConversationMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Repository for managing conversation persistence operations.
 *
 * Provides a clean abstraction over the database layer, handling
 * conversion between domain models and database entities.
 */
class ConversationRepository(private val database: AppDatabase) {

    private val conversationDao = database.conversationDao()

    /**
     * Create a new conversation.
     */
    suspend fun createConversation(conversation: Conversation) {
        val entity = ConversationMapper.toEntity(conversation)
        conversationDao.insertConversation(entity)
    }

    /**
     * Update an existing conversation.
     */
    suspend fun updateConversation(conversation: Conversation) {
        val entity = ConversationMapper.toEntity(conversation)
        conversationDao.updateConversation(entity)
    }

    /**
     * Get all non-archived conversations as a Flow for reactive updates.
     * Ordered by pinned status and last modified date.
     */
    fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations().map { entities ->
            ConversationMapper.toDomainList(entities)
        }
    }

    /**
     * Get all conversations including archived ones.
     */
    fun getAllConversationsIncludingArchived(): Flow<List<Conversation>> {
        return conversationDao.getAllConversationsIncludingArchived().map { entities ->
            ConversationMapper.toDomainList(entities)
        }
    }

    /**
     * Get archived conversations.
     */
    fun getArchivedConversations(): Flow<List<Conversation>> {
        return conversationDao.getArchivedConversations().map { entities ->
            ConversationMapper.toDomainList(entities)
        }
    }

    /**
     * Get a specific conversation by ID.
     */
    suspend fun getConversationById(conversationId: String): Conversation? {
        val entity = conversationDao.getConversationById(conversationId)
        return entity?.let { ConversationMapper.toDomain(it) }
    }

    /**
     * Search conversations by title.
     */
    fun searchConversations(query: String): Flow<List<Conversation>> {
        return conversationDao.searchConversations(query).map { entities ->
            ConversationMapper.toDomainList(entities)
        }
    }

    /**
     * Delete a specific conversation by ID.
     * This will cascade delete all messages in the conversation.
     */
    suspend fun deleteConversation(conversationId: String) {
        conversationDao.deleteConversation(conversationId)
    }

    /**
     * Delete multiple conversations by IDs.
     */
    suspend fun deleteConversations(conversationIds: List<String>) {
        conversationDao.deleteConversations(conversationIds)
    }

    /**
     * Delete all conversations.
     */
    suspend fun deleteAllConversations() {
        conversationDao.deleteAllConversations()
    }

    /**
     * Get the count of non-archived conversations.
     */
    suspend fun getConversationCount(): Int {
        return conversationDao.getConversationCount()
    }

    /**
     * Update conversation metadata (last modified timestamp and message count).
     */
    suspend fun updateConversationMetadata(conversationId: String, messageCount: Int) {
        val timestamp = Instant.now().toEpochMilli()
        conversationDao.updateConversationMetadata(conversationId, timestamp, messageCount)
    }

    /**
     * Toggle pin status for a conversation.
     */
    suspend fun togglePin(conversationId: String, isPinned: Boolean) {
        conversationDao.updatePinStatus(conversationId, isPinned)
    }

    /**
     * Toggle archive status for a conversation.
     */
    suspend fun toggleArchive(conversationId: String, isArchived: Boolean) {
        conversationDao.updateArchiveStatus(conversationId, isArchived)
    }
}
