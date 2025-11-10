package com.nervesparks.iris.core.rag

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sin

/**
 * Simple embedding service implementation
 * 
 * This is a placeholder implementation that generates deterministic embeddings
 * based on text content. In production, this would use a real embedding model
 * like sentence-transformers or a similar on-device model.
 */
@Singleton
class EmbeddingServiceImpl @Inject constructor() : EmbeddingService {
    
    companion object {
        private const val TAG = "EmbeddingService"
        private const val EMBEDDING_DIM = 384 // Standard dimension for many embedding models
    }
    
    override suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        try {
            // Generate a deterministic embedding based on text content
            // This is a simple hash-based approach for testing purposes
            val embedding = FloatArray(EMBEDDING_DIM)
            
            // Use text characteristics to generate features
            val normalized = text.lowercase().trim()
            val words = normalized.split(Regex("\\s+"))
            
            // Create features based on:
            // 1. Text length
            // 2. Word count
            // 3. Character distribution
            // 4. Word hash values
            
            val textLength = normalized.length
            val wordCount = words.size
            
            for (i in 0 until EMBEDDING_DIM) {
                var value = 0.0f
                
                // Add length-based component
                value += sin((textLength * (i + 1) * 0.01).toDouble()).toFloat() * 0.3f
                
                // Add word count component
                value += sin((wordCount * (i + 1) * 0.1).toDouble()).toFloat() * 0.3f
                
                // Add character-based component
                if (i < normalized.length) {
                    val charCode = normalized[i % normalized.length].code
                    value += sin((charCode * (i + 1) * 0.01).toDouble()).toFloat() * 0.2f
                }
                
                // Add word-based component
                if (words.isNotEmpty()) {
                    val wordIndex = i % words.size
                    val wordHash = words[wordIndex].hashCode()
                    value += sin((wordHash * (i + 1) * 0.001).toDouble()).toFloat() * 0.2f
                }
                
                embedding[i] = value
            }
            
            // Normalize the embedding vector
            val magnitude = kotlin.math.sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
            if (magnitude > 0) {
                for (i in embedding.indices) {
                    embedding[i] /= magnitude
                }
            }
            
            embedding
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate embedding", e)
            FloatArray(EMBEDDING_DIM) // Return zero vector on error
        }
    }
    
    override suspend fun generateEmbeddings(texts: List<String>): List<FloatArray> = withContext(Dispatchers.Default) {
        texts.map { text ->
            generateEmbedding(text)
        }
    }
}
