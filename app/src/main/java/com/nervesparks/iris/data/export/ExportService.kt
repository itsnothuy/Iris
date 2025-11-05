package com.nervesparks.iris.data.export

import com.nervesparks.iris.data.Conversation
import com.nervesparks.iris.data.Message
import com.nervesparks.iris.data.MessageRole
import com.nervesparks.iris.data.repository.ConversationRepository
import com.nervesparks.iris.data.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Service for exporting conversation data in various formats.
 * Handles format conversion, file I/O, and integrity verification.
 */
class ExportService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository
) {
    
    private val dateFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    
    /**
     * Export all conversations to the specified format.
     *
     * @param exportDir Directory where the export file should be saved
     * @param format Export format to use
     * @return ExportResult with file path and checksum or error
     */
    suspend fun exportAllConversations(
        exportDir: File,
        format: ExportFormat
    ): ExportResult {
        return try {
            val conversations = conversationRepository.getAllConversationsIncludingArchived()
            var allConversations: List<Conversation> = emptyList()
            
            // Collect all conversations from the Flow
            conversations.collect { allConversations = it }
            
            exportConversations(allConversations, exportDir, format)
        } catch (e: Exception) {
            ExportResult(success = false, error = e.message ?: "Unknown error during export")
        }
    }
    
    /**
     * Export specific conversations to the specified format.
     *
     * @param conversationIds List of conversation IDs to export
     * @param exportDir Directory where the export file should be saved
     * @param format Export format to use
     * @return ExportResult with file path and checksum or error
     */
    suspend fun exportConversations(
        conversationIds: List<String>,
        exportDir: File,
        format: ExportFormat
    ): ExportResult {
        return try {
            val conversations = conversationIds.mapNotNull { id ->
                conversationRepository.getConversationById(id)
            }
            exportConversations(conversations, exportDir, format)
        } catch (e: Exception) {
            ExportResult(success = false, error = e.message ?: "Unknown error during export")
        }
    }
    
    /**
     * Export conversations in date range.
     *
     * @param startDate Start of date range (inclusive)
     * @param endDate End of date range (inclusive)
     * @param exportDir Directory where the export file should be saved
     * @param format Export format to use
     * @return ExportResult with file path and checksum or error
     */
    suspend fun exportConversationsByDateRange(
        startDate: Instant,
        endDate: Instant,
        exportDir: File,
        format: ExportFormat
    ): ExportResult {
        return try {
            val conversations = conversationRepository.getAllConversationsIncludingArchived()
            var filteredConversations: List<Conversation> = emptyList()
            
            conversations.collect { allConversations ->
                filteredConversations = allConversations.filter { conversation ->
                    conversation.createdAt.isAfter(startDate) && conversation.createdAt.isBefore(endDate)
                }
            }
            
            exportConversations(filteredConversations, exportDir, format)
        } catch (e: Exception) {
            ExportResult(success = false, error = e.message ?: "Unknown error during export")
        }
    }
    
    /**
     * Export conversations with progress reporting.
     *
     * @param conversationIds List of conversation IDs to export
     * @param exportDir Directory where the export file should be saved
     * @param format Export format to use
     * @return Flow of ExportProgress and final ExportResult
     */
    fun exportConversationsWithProgress(
        conversationIds: List<String>,
        exportDir: File,
        format: ExportFormat
    ): Flow<Any> = flow {
        emit(ExportProgress(0, conversationIds.size, "Starting export..."))
        
        val conversations = mutableListOf<Conversation>()
        conversationIds.forEachIndexed { index, id ->
            val conversation = conversationRepository.getConversationById(id)
            conversation?.let { conversations.add(it) }
            emit(ExportProgress(index + 1, conversationIds.size, "Loading conversation ${index + 1}/${conversationIds.size}"))
        }
        
        emit(ExportProgress(conversationIds.size, conversationIds.size, "Generating export file..."))
        val result = exportConversations(conversations, exportDir, format)
        emit(result)
    }
    
    /**
     * Internal method to export a list of conversations.
     */
    private suspend fun exportConversations(
        conversations: List<Conversation>,
        exportDir: File,
        format: ExportFormat
    ): ExportResult {
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        
        val timestamp = Instant.now().toEpochMilli()
        val fileName = "iris_export_${timestamp}.${format.getFileExtension()}"
        val file = File(exportDir, fileName)
        
        val content = when (format) {
            ExportFormat.JSON -> exportToJson(conversations)
            ExportFormat.MARKDOWN -> exportToMarkdown(conversations)
            ExportFormat.PLAIN_TEXT -> exportToPlainText(conversations)
        }
        
        file.writeText(content)
        val checksum = calculateChecksum(file)
        
        return ExportResult(
            success = true,
            filePath = file.absolutePath,
            checksum = checksum
        )
    }
    
    /**
     * Export conversations to JSON format.
     */
    private suspend fun exportToJson(conversations: List<Conversation>): String {
        val root = JSONObject()
        root.put("version", "1.0")
        root.put("exportedAt", Instant.now().toString())
        root.put("conversationCount", conversations.size)
        
        val conversationsArray = JSONArray()
        for (conversation in conversations) {
            val conversationObj = JSONObject()
            conversationObj.put("id", conversation.id)
            conversationObj.put("title", conversation.title)
            conversationObj.put("createdAt", conversation.createdAt.toString())
            conversationObj.put("lastModified", conversation.lastModified.toString())
            conversationObj.put("messageCount", conversation.messageCount)
            conversationObj.put("isPinned", conversation.isPinned)
            conversationObj.put("isArchived", conversation.isArchived)
            
            val messages = messageRepository.getMessagesForConversationList(conversation.id)
            val messagesArray = JSONArray()
            for (message in messages) {
                val messageObj = JSONObject()
                messageObj.put("id", message.id)
                messageObj.put("content", message.content)
                messageObj.put("role", message.role.name)
                messageObj.put("timestamp", message.timestamp.toString())
                message.processingTimeMs?.let { messageObj.put("processingTimeMs", it) }
                message.tokenCount?.let { messageObj.put("tokenCount", it) }
                messagesArray.put(messageObj)
            }
            conversationObj.put("messages", messagesArray)
            conversationsArray.put(conversationObj)
        }
        
        root.put("conversations", conversationsArray)
        return root.toString(2)
    }
    
    /**
     * Export conversations to Markdown format.
     */
    private suspend fun exportToMarkdown(conversations: List<Conversation>): String {
        val builder = StringBuilder()
        builder.appendLine("# Iris Conversations Export")
        builder.appendLine()
        builder.appendLine("**Exported**: ${dateFormatter.format(Instant.now())}")
        builder.appendLine("**Total Conversations**: ${conversations.size}")
        builder.appendLine()
        builder.appendLine("---")
        builder.appendLine()
        
        for (conversation in conversations) {
            builder.appendLine("## ${conversation.title}")
            builder.appendLine()
            builder.appendLine("- **Created**: ${dateFormatter.format(conversation.createdAt)}")
            builder.appendLine("- **Last Modified**: ${dateFormatter.format(conversation.lastModified)}")
            builder.appendLine("- **Messages**: ${conversation.messageCount}")
            if (conversation.isPinned) builder.appendLine("- **Pinned**: Yes")
            if (conversation.isArchived) builder.appendLine("- **Archived**: Yes")
            builder.appendLine()
            
            val messages = messageRepository.getMessagesForConversationList(conversation.id)
            for (message in messages) {
                val roleLabel = when (message.role) {
                    MessageRole.USER -> "👤 User"
                    MessageRole.ASSISTANT -> "🤖 Assistant"
                    MessageRole.SYSTEM -> "⚙️ System"
                }
                builder.appendLine("### $roleLabel")
                builder.appendLine("*${dateFormatter.format(message.timestamp)}*")
                builder.appendLine()
                builder.appendLine(message.content)
                builder.appendLine()
                message.processingTimeMs?.let { 
                    builder.appendLine("*Processing time: ${it}ms*")
                }
                message.tokenCount?.let {
                    builder.appendLine("*Tokens: $it*")
                }
                builder.appendLine()
            }
            
            builder.appendLine("---")
            builder.appendLine()
        }
        
        return builder.toString()
    }
    
    /**
     * Export conversations to plain text format.
     */
    private suspend fun exportToPlainText(conversations: List<Conversation>): String {
        val builder = StringBuilder()
        builder.appendLine("IRIS CONVERSATIONS EXPORT")
        builder.appendLine("=" .repeat(80))
        builder.appendLine()
        builder.appendLine("Exported: ${dateFormatter.format(Instant.now())}")
        builder.appendLine("Total Conversations: ${conversations.size}")
        builder.appendLine()
        
        for (conversation in conversations) {
            builder.appendLine("-".repeat(80))
            builder.appendLine("Conversation: ${conversation.title}")
            builder.appendLine("Created: ${dateFormatter.format(conversation.createdAt)}")
            builder.appendLine("Last Modified: ${dateFormatter.format(conversation.lastModified)}")
            builder.appendLine("Messages: ${conversation.messageCount}")
            builder.appendLine("-".repeat(80))
            builder.appendLine()
            
            val messages = messageRepository.getMessagesForConversationList(conversation.id)
            for (message in messages) {
                val roleLabel = when (message.role) {
                    MessageRole.USER -> "[USER]"
                    MessageRole.ASSISTANT -> "[ASSISTANT]"
                    MessageRole.SYSTEM -> "[SYSTEM]"
                }
                builder.appendLine("$roleLabel ${dateFormatter.format(message.timestamp)}")
                builder.appendLine(message.content)
                builder.appendLine()
            }
            
            builder.appendLine()
        }
        
        return builder.toString()
    }
    
    /**
     * Calculate SHA-256 checksum for a file.
     */
    private fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = file.readBytes()
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Get file extension for export format.
     */
    private fun ExportFormat.getFileExtension(): String = when (this) {
        ExportFormat.JSON -> "json"
        ExportFormat.MARKDOWN -> "md"
        ExportFormat.PLAIN_TEXT -> "txt"
    }
}
