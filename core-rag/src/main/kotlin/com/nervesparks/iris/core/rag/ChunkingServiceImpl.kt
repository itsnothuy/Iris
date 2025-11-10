package com.nervesparks.iris.core.rag

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for chunking text into manageable segments
 */
@Singleton
class ChunkingServiceImpl @Inject constructor() : ChunkingService {
    
    companion object {
        private const val TAG = "ChunkingService"
    }
    
    override suspend fun chunkText(
        text: String,
        maxChunkSize: Int,
        overlap: Int,
        documentId: String
    ): List<DocumentChunk> = withContext(Dispatchers.Default) {
        
        val chunks = mutableListOf<DocumentChunk>()
        
        // Split by paragraphs first, then by sentences if needed
        val paragraphs = text.split("\n\n", "\n").filter { it.isNotBlank() }
        
        var currentChunk = StringBuilder()
        var chunkStartIndex = 0
        var currentIndex = 0
        
        for (paragraph in paragraphs) {
            val cleanParagraph = paragraph.trim()
            
            // If adding this paragraph would exceed chunk size, finalize current chunk
            if (currentChunk.isNotEmpty() && 
                currentChunk.length + cleanParagraph.length + 1 > maxChunkSize) {
                
                // Create chunk
                val chunkContent = currentChunk.toString().trim()
                if (chunkContent.isNotEmpty()) {
                    chunks.add(DocumentChunk(
                        content = chunkContent,
                        startIndex = chunkStartIndex,
                        endIndex = chunkStartIndex + chunkContent.length,
                        metadata = mapOf(
                            "chunk_index" to chunks.size.toString(),
                            "document_id" to documentId
                        )
                    ))
                }
                
                // Start new chunk with overlap
                val overlapText = if (overlap > 0 && currentChunk.length > overlap) {
                    currentChunk.substring(currentChunk.length - overlap)
                } else ""
                
                currentChunk = StringBuilder(overlapText)
                chunkStartIndex = currentIndex - overlapText.length
            }
            
            // Add paragraph to current chunk
            if (currentChunk.isNotEmpty()) {
                currentChunk.append("\n\n")
            }
            currentChunk.append(cleanParagraph)
            currentIndex += cleanParagraph.length + 2 // +2 for line breaks
        }
        
        // Add final chunk
        val finalChunkContent = currentChunk.toString().trim()
        if (finalChunkContent.isNotEmpty()) {
            chunks.add(DocumentChunk(
                content = finalChunkContent,
                startIndex = chunkStartIndex,
                endIndex = chunkStartIndex + finalChunkContent.length,
                metadata = mapOf(
                    "chunk_index" to chunks.size.toString(),
                    "document_id" to documentId
                )
            ))
        }
        
        Log.d(TAG, "Created ${chunks.size} chunks from ${text.length} characters")
        chunks
    }
    
    override suspend fun smartChunkText(
        text: String,
        maxChunkSize: Int,
        overlap: Int,
        documentId: String
    ): List<DocumentChunk> = withContext(Dispatchers.Default) {
        
        // More sophisticated chunking that respects semantic boundaries
        val sentences = splitIntoSentences(text)
        val chunks = mutableListOf<DocumentChunk>()
        
        var currentChunk = StringBuilder()
        var chunkStartIndex = 0
        var currentIndex = 0
        
        for (sentence in sentences) {
            val cleanSentence = sentence.trim()
            
            // Check if adding this sentence would exceed chunk size
            if (currentChunk.isNotEmpty() && 
                currentChunk.length + cleanSentence.length + 1 > maxChunkSize) {
                
                // Finalize current chunk
                val chunkContent = currentChunk.toString().trim()
                if (chunkContent.isNotEmpty()) {
                    chunks.add(DocumentChunk(
                        content = chunkContent,
                        startIndex = chunkStartIndex,
                        endIndex = chunkStartIndex + chunkContent.length,
                        metadata = mapOf(
                            "chunk_index" to chunks.size.toString(),
                            "document_id" to documentId,
                            "chunking_method" to "smart_semantic"
                        )
                    ))
                }
                
                // Start new chunk with overlap
                val overlapText = if (overlap > 0) {
                    getLastNCharacters(currentChunk.toString(), overlap)
                } else ""
                
                currentChunk = StringBuilder(overlapText)
                chunkStartIndex = currentIndex - overlapText.length
            }
            
            // Add sentence to current chunk
            if (currentChunk.isNotEmpty()) {
                currentChunk.append(" ")
            }
            currentChunk.append(cleanSentence)
            currentIndex += cleanSentence.length + 1
        }
        
        // Add final chunk
        val finalChunkContent = currentChunk.toString().trim()
        if (finalChunkContent.isNotEmpty()) {
            chunks.add(DocumentChunk(
                content = finalChunkContent,
                startIndex = chunkStartIndex,
                endIndex = chunkStartIndex + finalChunkContent.length,
                metadata = mapOf(
                    "chunk_index" to chunks.size.toString(),
                    "document_id" to documentId,
                    "chunking_method" to "smart_semantic"
                )
            ))
        }
        
        Log.d(TAG, "Smart chunking created ${chunks.size} chunks from ${sentences.size} sentences")
        chunks
    }
    
    private fun splitIntoSentences(text: String): List<String> {
        // Simple sentence splitting - in a real implementation, 
        // you might use a more sophisticated NLP library
        return text.split(Regex("[.!?]+\\s+"))
            .filter { it.isNotBlank() }
            .map { it.trim() }
    }
    
    private fun getLastNCharacters(text: String, n: Int): String {
        return if (text.length <= n) text else text.substring(text.length - n)
    }
}
