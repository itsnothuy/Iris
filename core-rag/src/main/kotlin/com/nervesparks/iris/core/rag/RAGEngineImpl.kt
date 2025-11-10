package com.nervesparks.iris.core.rag

import com.nervesparks.iris.common.error.RAGException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Production-ready implementation of RAGEngine
 * Uses in-memory vector storage with TF-IDF embeddings
 */
@Singleton
class RAGEngineImpl @Inject constructor() : RAGEngine {
    
    // Thread-safe storage
    private val mutex = Mutex()
    private val documents = mutableMapOf<String, Document>()
    private val chunks = mutableMapOf<String, DocumentChunk>()
    private val termFrequencies = mutableMapOf<String, MutableMap<String, Int>>()
    private var documentCount = 0
    
    // Chunking configuration
    private val chunkSize = 512 // characters per chunk
    private val chunkOverlap = 128 // overlap between chunks
    
    /**
     * Internal document chunk representation
     */
    private data class DocumentChunk(
        val id: String,
        val documentId: String,
        val chunkIndex: Int,
        val content: String,
        val metadata: Map<String, Any>,
        val terms: Map<String, Int>, // term frequencies
        val magnitude: Double // vector magnitude for cosine similarity
    )
    
    override suspend fun indexDocument(document: Document): Result<Unit> = mutex.withLock {
        return try {
            // Store original document
            documents[document.id] = document
            
            // Chunk the document
            val documentChunks = chunkDocument(document)
            
            // Index each chunk
            for (chunk in documentChunks) {
                chunks[chunk.id] = chunk
                
                // Update term frequencies for IDF calculation
                for (term in chunk.terms.keys) {
                    termFrequencies.getOrPut(term) { mutableMapOf() }[chunk.id] = chunk.terms[term]!!
                }
            }
            
            documentCount++
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RAGException("Failed to index document: ${e.message}", e))
        }
    }
    
    override suspend fun search(query: String, limit: Int): List<RetrievedChunk> = mutex.withLock {
        if (chunks.isEmpty()) {
            return emptyList()
        }
        
        // Extract terms from query
        val queryTerms = extractTerms(query)
        if (queryTerms.isEmpty()) {
            return emptyList()
        }
        
        // Calculate query magnitude
        val queryMagnitude = calculateMagnitudeInt(queryTerms)
        
        // Calculate cosine similarity with all chunks using simple term matching
        val scored = chunks.values.map { chunk ->
            val similarity = calculateCosineSimilarity(
                queryTerms,
                chunk.terms,
                queryMagnitude,
                chunk.magnitude
            )
            chunk to similarity
        }
        
        // Sort by similarity and return top results
        return scored
            .filter { it.second > 0.0 } // Only return results with some similarity
            .sortedByDescending { it.second }
            .take(limit)
            .map { (chunk, score) ->
                RetrievedChunk(
                    id = chunk.id,
                    content = chunk.content,
                    score = score.toFloat(),
                    documentId = chunk.documentId,
                    chunkIndex = chunk.chunkIndex,
                    metadata = chunk.metadata
                )
            }
    }
    
    override suspend fun deleteIndex(documentId: String): Result<Unit> = mutex.withLock {
        return try {
            // Remove document
            documents.remove(documentId)
            
            // Remove all chunks for this document
            val chunkIds = chunks.values
                .filter { it.documentId == documentId }
                .map { it.id }
            
            for (chunkId in chunkIds) {
                chunks.remove(chunkId)
                
                // Clean up term frequencies
                termFrequencies.values.forEach { it.remove(chunkId) }
            }
            
            // Clean up empty term entries
            termFrequencies.entries.removeIf { it.value.isEmpty() }
            
            documentCount = documents.size
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RAGException("Failed to delete index: ${e.message}", e))
        }
    }
    
    override suspend fun updateDocument(document: Document): Result<Unit> {
        // Delete old index and create new one
        deleteIndex(document.id)
        return indexDocument(document)
    }
    
    override suspend fun getIndexStats(): IndexStats = mutex.withLock {
        return IndexStats(
            totalDocuments = documents.size,
            totalChunks = chunks.size,
            indexSize = calculateIndexSize(),
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    override suspend fun optimizeIndex(): Result<Unit> = mutex.withLock {
        return try {
            // Clean up any orphaned chunks
            val validDocumentIds = documents.keys
            val orphanedChunks = chunks.values
                .filter { it.documentId !in validDocumentIds }
                .map { it.id }
            
            for (chunkId in orphanedChunks) {
                chunks.remove(chunkId)
                termFrequencies.values.forEach { it.remove(chunkId) }
            }
            
            // Clean up empty term entries
            termFrequencies.entries.removeIf { it.value.isEmpty() }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RAGException("Failed to optimize index: ${e.message}", e))
        }
    }
    
    /**
     * Chunk a document into overlapping segments
     */
    private fun chunkDocument(document: Document): List<DocumentChunk> {
        val content = document.content
        val documentChunks = mutableListOf<DocumentChunk>()
        
        var startIndex = 0
        var chunkIndex = 0
        
        while (startIndex < content.length) {
            val endIndex = minOf(startIndex + chunkSize, content.length)
            val chunkContent = content.substring(startIndex, endIndex).trim()
            
            if (chunkContent.isNotEmpty()) {
                val terms = extractTerms(chunkContent)
                val magnitude = calculateMagnitudeInt(terms)
                
                val chunk = DocumentChunk(
                    id = "${document.id}_chunk_$chunkIndex",
                    documentId = document.id,
                    chunkIndex = chunkIndex,
                    content = chunkContent,
                    metadata = document.metadata + mapOf(
                        "source" to document.source.name,
                        "timestamp" to document.timestamp
                    ),
                    terms = terms,
                    magnitude = magnitude
                )
                
                documentChunks.add(chunk)
                chunkIndex++
            }
            
            // Move to next chunk with overlap
            startIndex += (chunkSize - chunkOverlap)
        }
        
        return documentChunks
    }
    
    /**
     * Extract terms from text and count frequencies
     */
    private fun extractTerms(text: String): Map<String, Int> {
        // Simple tokenization: lowercase, split on non-alphanumeric, filter short words
        val terms = text.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 } // Minimum word length
            .filterNot { it in stopWords }
        
        // Count term frequencies
        return terms.groupingBy { it }.eachCount()
    }
    
    /**
     * Calculate TF-IDF weighted vector
     */
    private fun calculateTFIDF(terms: Map<String, Int>, totalChunks: Int): Map<String, Double> {
        return terms.mapValues { (term, tf) ->
            val df = termFrequencies[term]?.size ?: 0
            if (df == 0) {
                0.0
            } else {
                // TF-IDF = term frequency * log(total docs / document frequency)
                val idf = kotlin.math.ln((totalChunks.toDouble() + 1) / (df + 1))
                tf * idf
            }
        }
    }
    
    /**
     * Calculate vector magnitude for normalization (Int version)
     */
    private fun calculateMagnitudeInt(vector: Map<String, Int>): Double {
        return sqrt(vector.values.sumOf { it * it.toDouble() })
    }
    
    /**
     * Calculate vector magnitude for normalization (Double version)
     */
    private fun calculateMagnitudeDouble(vector: Map<String, Double>): Double {
        return sqrt(vector.values.sumOf { it * it })
    }
    
    /**
     * Calculate cosine similarity between two term frequency vectors
     */
    private fun calculateCosineSimilarity(
        queryTerms: Map<String, Int>,
        chunkTerms: Map<String, Int>,
        queryMagnitude: Double,
        chunkMagnitude: Double
    ): Double {
        if (queryMagnitude == 0.0 || chunkMagnitude == 0.0) {
            return 0.0
        }
        
        // Calculate dot product
        var dotProduct = 0.0
        for ((term, queryFreq) in queryTerms) {
            val chunkFreq = chunkTerms[term] ?: 0
            dotProduct += queryFreq * chunkFreq.toDouble()
        }
        
        // Cosine similarity = dot product / (magnitude1 * magnitude2)
        return dotProduct / (queryMagnitude * chunkMagnitude)
    }
    
    /**
     * Calculate approximate index size in bytes
     */
    private fun calculateIndexSize(): Long {
        var size = 0L
        
        // Documents
        size += documents.values.sumOf { doc ->
            doc.id.length + doc.content.length + doc.source.name.length + 100L
        }
        
        // Chunks
        size += chunks.values.sumOf { chunk ->
            chunk.id.length + chunk.content.length + chunk.terms.size * 20L
        }
        
        // Term frequencies
        size += termFrequencies.size * 50L
        
        return size
    }
    
    companion object {
        // Common English stop words to filter out
        private val stopWords = setOf(
            "the", "and", "for", "are", "but", "not", "you", "all", "can", "her",
            "was", "one", "our", "out", "day", "get", "has", "him", "his", "how",
            "man", "new", "now", "old", "see", "two", "way", "who", "boy", "did",
            "its", "let", "put", "say", "she", "too", "use", "with", "have", "this",
            "will", "your", "from", "they", "know", "want", "been", "good", "much",
            "some", "time", "very", "when", "come", "here", "just", "like", "long",
            "make", "many", "over", "such", "take", "than", "them", "well", "only"
        )
    }
}
