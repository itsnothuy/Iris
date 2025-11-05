package com.nervesparks.iris.data.local

import com.nervesparks.iris.data.Message
import com.nervesparks.iris.data.MessageRole
import java.time.Instant

/**
 * Mapper class to convert between domain Message and database MessageEntity.
 * 
 * Handles the conversion of timestamps (Instant <-> Long) and roles (enum <-> String).
 */
object MessageMapper {
    
    /**
     * Convert a domain Message to a database MessageEntity.
     * @param conversationId The conversation this message belongs to
     */
    fun toEntity(message: Message, conversationId: String = "default"): MessageEntity {
        return MessageEntity(
            id = message.id,
            content = message.content,
            role = message.role.name,
            timestamp = message.timestamp.toEpochMilli(),
            processingTimeMs = message.processingTimeMs,
            tokenCount = message.tokenCount,
            conversationId = conversationId
        )
    }
    
    /**
     * Convert a database MessageEntity to a domain Message.
     */
    fun toDomain(entity: MessageEntity): Message {
        return Message(
            id = entity.id,
            content = entity.content,
            role = MessageRole.valueOf(entity.role),
            timestamp = Instant.ofEpochMilli(entity.timestamp),
            processingTimeMs = entity.processingTimeMs,
            tokenCount = entity.tokenCount
        )
    }
    
    /**
     * Convert a list of MessageEntity to a list of domain Messages.
     */
    fun toDomainList(entities: List<MessageEntity>): List<Message> {
        return entities.map { toDomain(it) }
    }
    
    /**
     * Convert a list of domain Messages to a list of MessageEntity.
     * @param conversationId The conversation these messages belong to
     */
    fun toEntityList(messages: List<Message>, conversationId: String = "default"): List<MessageEntity> {
        return messages.map { toEntity(it, conversationId) }
    }
}
