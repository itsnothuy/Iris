package com.nervesparks.iris.data.export

import com.nervesparks.iris.data.Conversation
import com.nervesparks.iris.data.Message
import com.nervesparks.iris.data.MessageRole
import com.nervesparks.iris.data.repository.ConversationRepository
import com.nervesparks.iris.data.repository.MessageRepository
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * Result of an import operation.
 *
 * @property success Whether the import was successful
 * @property conversationsImported Number of conversations imported
 * @property messagesImported Number of messages imported
 * @property duplicatesSkipped Number of duplicate conversations skipped
 * @property error Error message (if failed)
 */
data class ImportResult(
    val success: Boolean,
    val conversationsImported: Int = 0,
    val messagesImported: Int = 0,
    val duplicatesSkipped: Int = 0,
    val error: String? = null
)

/**
 * Service for importing conversation data from exported files.
 * Handles validation, duplicate detection, and data restoration.
 */
class ImportService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository
) {
    
    /**
     * Import conversations from a JSON file.
     *
     * @param file The JSON file to import from
     * @param skipDuplicates Whether to skip conversations that already exist
     * @return ImportResult with statistics and any errors
     */
    suspend fun importFromJson(file: File, skipDuplicates: Boolean = true): ImportResult {
        return try {
            if (!file.exists()) {
                return ImportResult(success = false, error = "File does not exist")
            }
            
            if (!file.name.endsWith(".json")) {
                return ImportResult(success = false, error = "File must be a JSON file")
            }
            
            val content = file.readText()
            val root = JSONObject(content)
            
            // Validate format
            if (!root.has("version") || !root.has("conversations")) {
                return ImportResult(success = false, error = "Invalid export file format")
            }
            
            val conversations = root.getJSONArray("conversations")
            var conversationsImported = 0
            var messagesImported = 0
            var duplicatesSkipped = 0
            
            for (i in 0 until conversations.length()) {
                val conversationObj = conversations.getJSONObject(i)
                val conversationId = conversationObj.getString("id")
                
                // Check for duplicates
                if (skipDuplicates) {
                    val existing = conversationRepository.getConversationById(conversationId)
                    if (existing != null) {
                        duplicatesSkipped++
                        continue
                    }
                }
                
                // Import conversation
                val conversation = Conversation(
                    id = conversationId,
                    title = conversationObj.getString("title"),
                    createdAt = Instant.parse(conversationObj.getString("createdAt")),
                    lastModified = Instant.parse(conversationObj.getString("lastModified")),
                    messageCount = conversationObj.getInt("messageCount"),
                    isPinned = conversationObj.optBoolean("isPinned", false),
                    isArchived = conversationObj.optBoolean("isArchived", false)
                )
                
                conversationRepository.createConversation(conversation)
                conversationsImported++
                
                // Import messages
                val messagesArray = conversationObj.getJSONArray("messages")
                val messages = mutableListOf<Message>()
                
                for (j in 0 until messagesArray.length()) {
                    val messageObj = messagesArray.getJSONObject(j)
                    val message = Message(
                        id = messageObj.getString("id"),
                        content = messageObj.getString("content"),
                        role = MessageRole.valueOf(messageObj.getString("role")),
                        timestamp = Instant.parse(messageObj.getString("timestamp")),
                        processingTimeMs = if (messageObj.has("processingTimeMs")) 
                            messageObj.getLong("processingTimeMs") else null,
                        tokenCount = if (messageObj.has("tokenCount")) 
                            messageObj.getInt("tokenCount") else null
                    )
                    messages.add(message)
                }
                
                messageRepository.saveMessages(messages, conversationId)
                messagesImported += messages.size
            }
            
            ImportResult(
                success = true,
                conversationsImported = conversationsImported,
                messagesImported = messagesImported,
                duplicatesSkipped = duplicatesSkipped
            )
        } catch (e: Exception) {
            ImportResult(success = false, error = e.message ?: "Unknown error during import")
        }
    }
    
    /**
     * Validate an export file without importing it.
     *
     * @param file The file to validate
     * @return Validation result with any errors
     */
    fun validateExportFile(file: File): ValidationResult {
        return try {
            if (!file.exists()) {
                return ValidationResult(valid = false, error = "File does not exist")
            }
            
            if (!file.name.endsWith(".json")) {
                return ValidationResult(valid = false, error = "Only JSON files are supported for import")
            }
            
            val content = file.readText()
            val root = JSONObject(content)
            
            if (!root.has("version")) {
                return ValidationResult(valid = false, error = "Missing version field")
            }
            
            if (!root.has("conversations")) {
                return ValidationResult(valid = false, error = "Missing conversations field")
            }
            
            val conversations = root.getJSONArray("conversations")
            val conversationCount = conversations.length()
            var totalMessages = 0
            
            for (i in 0 until conversationCount) {
                val conversation = conversations.getJSONObject(i)
                if (!conversation.has("id") || !conversation.has("title") || !conversation.has("messages")) {
                    return ValidationResult(valid = false, error = "Invalid conversation format at index $i")
                }
                totalMessages += conversation.getJSONArray("messages").length()
            }
            
            ValidationResult(
                valid = true,
                conversationCount = conversationCount,
                messageCount = totalMessages
            )
        } catch (e: Exception) {
            ValidationResult(valid = false, error = "Failed to parse file: ${e.message}")
        }
    }
}

/**
 * Result of file validation.
 *
 * @property valid Whether the file is valid
 * @property conversationCount Number of conversations in the file
 * @property messageCount Number of messages in the file
 * @property error Error message (if invalid)
 */
data class ValidationResult(
    val valid: Boolean,
    val conversationCount: Int = 0,
    val messageCount: Int = 0,
    val error: String? = null
)
