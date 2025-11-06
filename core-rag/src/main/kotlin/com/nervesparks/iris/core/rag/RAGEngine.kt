package com.nervesparks.iris.core.rag

/**
 * Document to be indexed
 */
data class Document(
    val id: String,
    val content: String,
    val source: DataSource,
    val metadata: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Retrieved document chunk
 */
data class RetrievedChunk(
    val id: String,
    val content: String,
    val score: Float,
    val documentId: String,
    val chunkIndex: Int,
    val metadata: Map<String, Any>
)

/**
 * Index statistics
 */
data class IndexStats(
    val totalDocuments: Int,
    val totalChunks: Int,
    val indexSize: Long,
    val lastUpdated: Long
)

/**
 * Data source types
 */
enum class DataSource {
    NOTE,
    PDF,
    SMS,
    EMAIL,
    CALENDAR,
    CONTACT,
    FILE,
    MANUAL
}

/**
 * Interface for RAG operations
 */
interface RAGEngine {
    /**
     * Index a document for retrieval
     */
    suspend fun indexDocument(document: Document): Result<Unit>
    
    /**
     * Search for relevant chunks
     */
    suspend fun search(query: String, limit: Int = 5): List<RetrievedChunk>
    
    /**
     * Delete a document from the index
     */
    suspend fun deleteIndex(documentId: String): Result<Unit>
    
    /**
     * Update an existing document
     */
    suspend fun updateDocument(document: Document): Result<Unit>
    
    /**
     * Get index statistics
     */
    suspend fun getIndexStats(): IndexStats
    
    /**
     * Optimize the index
     */
    suspend fun optimizeIndex(): Result<Unit>
}
