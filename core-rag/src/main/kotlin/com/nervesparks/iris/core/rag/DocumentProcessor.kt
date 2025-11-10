package com.nervesparks.iris.core.rag

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * Interface for document processing operations
 */
interface DocumentProcessor {
    /**
     * Process a single document from URI
     */
    suspend fun processDocument(
        uri: Uri,
        metadata: DocumentMetadata? = null
    ): Result<ProcessingResult>
    
    /**
     * Process multiple documents with progress tracking
     */
    suspend fun processMultipleDocuments(
        uris: List<Uri>,
        batchMetadata: Map<String, DocumentMetadata>? = null
    ): Flow<BatchProcessingResult>
    
    /**
     * Reprocess an existing document
     */
    suspend fun reprocessDocument(documentId: String): Result<ProcessingResult>
    
    /**
     * Delete a document and its chunks
     */
    suspend fun deleteDocument(documentId: String): Boolean
    
    /**
     * Get processing status of a document
     */
    suspend fun getDocumentStatus(documentId: String): DocumentStatus?
    
    /**
     * Get all documents
     */
    suspend fun getAllDocuments(): List<DocumentStatus>
}

/**
 * Interface for text extraction from various formats
 */
interface TextExtractor {
    /**
     * Extract text from a document URI
     */
    suspend fun extractText(uri: Uri, documentInfo: DocumentInfo): Result<String>
}

/**
 * Interface for text chunking operations
 */
interface ChunkingService {
    /**
     * Chunk text into segments with overlap
     */
    suspend fun chunkText(
        text: String,
        maxChunkSize: Int,
        overlap: Int,
        documentId: String
    ): List<DocumentChunk>
    
    /**
     * Smart chunking that respects semantic boundaries
     */
    suspend fun smartChunkText(
        text: String,
        maxChunkSize: Int,
        overlap: Int,
        documentId: String
    ): List<DocumentChunk>
}

/**
 * Interface for embedding generation
 */
interface EmbeddingService {
    /**
     * Generate embedding for text
     */
    suspend fun generateEmbedding(text: String): FloatArray
    
    /**
     * Batch generate embeddings
     */
    suspend fun generateEmbeddings(texts: List<String>): List<FloatArray>
}

/**
 * Interface for vector storage operations
 */
interface VectorStore {
    /**
     * Save a document
     */
    suspend fun saveDocument(document: StoredDocument)
    
    /**
     * Update a document
     */
    suspend fun updateDocument(document: StoredDocument)
    
    /**
     * Get a document by ID
     */
    suspend fun getDocument(documentId: String): StoredDocument?
    
    /**
     * Get all documents
     */
    suspend fun getAllDocuments(): List<StoredDocument>
    
    /**
     * Delete a document
     */
    suspend fun deleteDocument(documentId: String): Boolean
    
    /**
     * Save chunks with embeddings
     */
    suspend fun saveChunks(chunks: List<EmbeddedChunk>)
    
    /**
     * Delete chunks by document ID
     */
    suspend fun deleteChunksByDocumentId(documentId: String): Boolean
    
    /**
     * Search for similar chunks
     */
    suspend fun searchSimilar(
        queryEmbedding: FloatArray,
        limit: Int,
        threshold: Float
    ): List<ScoredChunk>
}

// Data classes

/**
 * Document metadata
 */
data class DocumentMetadata(
    val title: String? = null,
    val author: String? = null,
    val tags: List<String> = emptyList(),
    val properties: Map<String, String> = emptyMap()
)

/**
 * Document information extracted from URI
 */
data class DocumentInfo(
    val fileName: String,
    val size: Long,
    val mimeType: String,
    val lastModified: Long
)

/**
 * Stored document entity
 */
data class StoredDocument(
    val id: String,
    val title: String,
    val uri: String,
    val mimeType: String,
    val size: Long,
    val textContent: String,
    val metadata: Map<String, String>,
    val createdAt: Long,
    val lastModified: Long,
    val chunkCount: Int,
    val isProcessed: Boolean
)

/**
 * Text chunk from a document
 */
data class DocumentChunk(
    val content: String,
    val startIndex: Int,
    val endIndex: Int,
    val metadata: Map<String, String>
)

/**
 * Chunk with embedding
 */
data class EmbeddedChunk(
    val id: String,
    val documentId: String,
    val content: String,
    val embedding: FloatArray,
    val startIndex: Int,
    val endIndex: Int,
    val metadata: Map<String, String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EmbeddedChunk
        
        if (id != other.id) return false
        if (documentId != other.documentId) return false
        if (content != other.content) return false
        if (!embedding.contentEquals(other.embedding)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + documentId.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}

/**
 * Chunk with similarity score
 */
data class ScoredChunk(
    val chunk: EmbeddedChunk,
    val score: Float
)

/**
 * Processing result
 */
data class ProcessingResult(
    val documentId: String,
    val title: String,
    val chunkCount: Int,
    val characterCount: Int,
    val processingTime: Long,
    val success: Boolean
)

/**
 * Document status
 */
data class DocumentStatus(
    val documentId: String,
    val title: String,
    val isProcessed: Boolean,
    val chunkCount: Int,
    val size: Long,
    val createdAt: Long,
    val lastModified: Long
)

/**
 * Processing error
 */
data class ProcessingError(
    val uri: Uri,
    val error: String
)

/**
 * Batch processing results
 */
sealed class BatchProcessingResult {
    data class Started(val totalDocuments: Int) : BatchProcessingResult()
    data class DocumentCompleted(
        val uri: Uri,
        val result: ProcessingResult,
        val progress: Int
    ) : BatchProcessingResult()
    data class DocumentFailed(val error: ProcessingError) : BatchProcessingResult()
    data class Completed(
        val totalDocuments: Int,
        val successCount: Int,
        val errorCount: Int,
        val errors: List<ProcessingError>
    ) : BatchProcessingResult()
}

/**
 * Exception for document processing errors
 */
class DocumentProcessingException(message: String, cause: Throwable? = null) : Exception(message, cause)
