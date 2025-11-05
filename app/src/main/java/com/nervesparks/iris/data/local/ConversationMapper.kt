package com.nervesparks.iris.data.local

import com.nervesparks.iris.data.Conversation
import java.time.Instant

/**
 * Mapper for converting between Conversation domain models and ConversationEntity database entities.
 */
object ConversationMapper {
    
    /**
     * Convert a domain Conversation to a database ConversationEntity.
     */
    fun toEntity(conversation: Conversation): ConversationEntity {
        return ConversationEntity(
            id = conversation.id,
            title = conversation.title,
            createdAt = conversation.createdAt.toEpochMilli(),
            lastModified = conversation.lastModified.toEpochMilli(),
            messageCount = conversation.messageCount,
            isPinned = conversation.isPinned,
            isArchived = conversation.isArchived
        )
    }
    
    /**
     * Convert a database ConversationEntity to a domain Conversation.
     */
    fun toDomain(entity: ConversationEntity): Conversation {
        return Conversation(
            id = entity.id,
            title = entity.title,
            createdAt = Instant.ofEpochMilli(entity.createdAt),
            lastModified = Instant.ofEpochMilli(entity.lastModified),
            messageCount = entity.messageCount,
            isPinned = entity.isPinned,
            isArchived = entity.isArchived
        )
    }
    
    /**
     * Convert a list of ConversationEntity to a list of Conversation.
     */
    fun toDomainList(entities: List<ConversationEntity>): List<Conversation> {
        return entities.map { toDomain(it) }
    }
    
    /**
     * Convert a list of Conversation to a list of ConversationEntity.
     */
    fun toEntityList(conversations: List<Conversation>): List<ConversationEntity> {
        return conversations.map { toEntity(it) }
    }
}
