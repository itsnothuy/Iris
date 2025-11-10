package com.nervesparks.iris.core.rag

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * In-memory vector store implementation
 * 
 * This is a simple implementation for the RAG system.
 * In production, this would be backed by a persistent database like SQLite with sqlite-vec.
 */
@Singleton
class VectorStoreImpl @Inject constructor(
    private val embeddingService: EmbeddingService
) : VectorStore {
    
    companion object {
        private const val TAG = "VectorStore"
    }
    
    // Thread-safe storage
    private val mutex = Mutex()
    private val documents = mutableMapOf<String, StoredDocument>()
    private val chunks = mutableMapOf<String, EmbeddedChunk>()
    
    override suspend fun saveDocument(document: StoredDocument): Unit = mutex.withLock {
        documents[document.id] = document
        Log.d(TAG, "Saved document: ${document.id}")
    }
    
    override suspend fun updateDocument(document: StoredDocument): Unit = mutex.withLock {
        documents[document.id] = document
        Log.d(TAG, "Updated document: ${document.id}")
    }
    
    override suspend fun getDocument(documentId: String): StoredDocument? = mutex.withLock {
        documents[documentId]
    }
    
    override suspend fun getAllDocuments(): List<StoredDocument> = mutex.withLock {
        documents.values.toList()
    }
    
    override suspend fun deleteDocument(documentId: String): Boolean = mutex.withLock {
        val removed = documents.remove(documentId) != null
        if (removed) {
            Log.d(TAG, "Deleted document: $documentId")
        }
        removed
    }
    
    override suspend fun saveChunks(chunks: List<EmbeddedChunk>): Unit = mutex.withLock {
        for (chunk in chunks) {
            this.chunks[chunk.id] = chunk
        }
        Log.d(TAG, "Saved ${chunks.size} chunks")
    }
    
    override suspend fun deleteChunksByDocumentId(documentId: String): Boolean = mutex.withLock {
        val chunkIds = chunks.values
            .filter { it.documentId == documentId }
            .map { it.id }
        
        for (chunkId in chunkIds) {
            chunks.remove(chunkId)
        }
        
        Log.d(TAG, "Deleted ${chunkIds.size} chunks for document: $documentId")
        true
    }
    
    override suspend fun searchSimilar(
        queryEmbedding: FloatArray,
        limit: Int,
        threshold: Float
    ): List<ScoredChunk> = mutex.withLock {
        
        if (chunks.isEmpty()) {
            return@withLock emptyList()
        }
        
        // Calculate cosine similarity with all chunks
        val scored = chunks.values.map { chunk ->
            val similarity = cosineSimilarity(queryEmbedding, chunk.embedding)
            ScoredChunk(chunk, similarity)
        }
        
        // Filter by threshold, sort by score, and return top results
        return@withLock scored
            .filter { it.score >= threshold }
            .sortedByDescending { it.score }
            .take(limit)
    }
    
    /**
     * Calculate cosine similarity between two vectors
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) {
            Log.w(TAG, "Vector size mismatch: ${a.size} vs ${b.size}")
            return 0.0f
        }
        
        var dotProduct = 0.0
        var magnitudeA = 0.0
        var magnitudeB = 0.0
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            magnitudeA += a[i] * a[i]
            magnitudeB += b[i] * b[i]
        }
        
        magnitudeA = sqrt(magnitudeA)
        magnitudeB = sqrt(magnitudeB)
        
        return if (magnitudeA > 0 && magnitudeB > 0) {
            (dotProduct / (magnitudeA * magnitudeB)).toFloat()
        } else {
            0.0f
        }
    }
}
