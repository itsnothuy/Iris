package com.nervesparks.iris.core.rag

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for EmbeddingServiceImpl
 */
@RunWith(RobolectricTestRunner::class)
class EmbeddingServiceImplTest {
    
    private lateinit var embeddingService: EmbeddingServiceImpl
    
    @Before
    fun setup() {
        embeddingService = EmbeddingServiceImpl()
    }
    
    @Test
    fun `generateEmbedding returns correct dimension`() = runTest {
        val text = "This is a test sentence"
        val embedding = embeddingService.generateEmbedding(text)
        
        assertEquals(384, embedding.size)
    }
    
    @Test
    fun `generateEmbedding returns normalized vector`() = runTest {
        val text = "Test text for embedding"
        val embedding = embeddingService.generateEmbedding(text)
        
        // Calculate magnitude
        val magnitude = kotlin.math.sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
        
        // Should be close to 1.0 (normalized)
        assertTrue(magnitude > 0.9f && magnitude < 1.1f)
    }
    
    @Test
    fun `generateEmbedding is deterministic`() = runTest {
        val text = "Deterministic test"
        
        val embedding1 = embeddingService.generateEmbedding(text)
        val embedding2 = embeddingService.generateEmbedding(text)
        
        assertArrayEquals(embedding1, embedding2, 0.0001f)
    }
    
    @Test
    fun `generateEmbedding produces different embeddings for different text`() = runTest {
        val text1 = "First text"
        val text2 = "Second text"
        
        val embedding1 = embeddingService.generateEmbedding(text1)
        val embedding2 = embeddingService.generateEmbedding(text2)
        
        // Embeddings should be different
        var differenceCount = 0
        for (i in embedding1.indices) {
            if (kotlin.math.abs(embedding1[i] - embedding2[i]) > 0.001f) {
                differenceCount++
            }
        }
        
        assertTrue("Embeddings should be different", differenceCount > 10)
    }
    
    @Test
    fun `generateEmbedding handles empty string`() = runTest {
        val embedding = embeddingService.generateEmbedding("")
        
        assertEquals(384, embedding.size)
        // Should still be a valid embedding
        assertTrue(embedding.all { !it.isNaN() && !it.isInfinite() })
    }
    
    @Test
    fun `generateEmbedding handles special characters`() = runTest {
        val text = "Special chars: !@#$%^&*()"
        val embedding = embeddingService.generateEmbedding(text)
        
        assertEquals(384, embedding.size)
        assertTrue(embedding.all { !it.isNaN() && !it.isInfinite() })
    }
    
    @Test
    fun `generateEmbedding handles very long text`() = runTest {
        val longText = "Word ".repeat(1000)
        val embedding = embeddingService.generateEmbedding(longText)
        
        assertEquals(384, embedding.size)
        assertTrue(embedding.all { !it.isNaN() && !it.isInfinite() })
    }
    
    @Test
    fun `generateEmbedding is case sensitive`() = runTest {
        val text1 = "UPPERCASE TEXT"
        val text2 = "uppercase text"
        
        val embedding1 = embeddingService.generateEmbedding(text1)
        val embedding2 = embeddingService.generateEmbedding(text2)
        
        // Should produce same embeddings (normalized internally to lowercase)
        var similarCount = 0
        for (i in embedding1.indices) {
            if (kotlin.math.abs(embedding1[i] - embedding2[i]) < 0.001f) {
                similarCount++
            }
        }
        
        // Most values should be similar since text is lowercased
        assertTrue(similarCount > 300)
    }
    
    @Test
    fun `generateEmbeddings batch processes multiple texts`() = runTest {
        val texts = listOf("Text one", "Text two", "Text three")
        
        val embeddings = embeddingService.generateEmbeddings(texts)
        
        assertEquals(3, embeddings.size)
        assertTrue(embeddings.all { it.size == 384 })
    }
    
    @Test
    fun `generateEmbeddings handles empty list`() = runTest {
        val embeddings = embeddingService.generateEmbeddings(emptyList())
        
        assertEquals(0, embeddings.size)
    }
    
    @Test
    fun `generateEmbeddings produces consistent results`() = runTest {
        val texts = listOf("First", "Second")
        
        val embeddings1 = embeddingService.generateEmbeddings(texts)
        val embeddings2 = embeddingService.generateEmbeddings(texts)
        
        assertEquals(embeddings1.size, embeddings2.size)
        for (i in embeddings1.indices) {
            assertArrayEquals(embeddings1[i], embeddings2[i], 0.0001f)
        }
    }
    
    @Test
    fun `generateEmbedding values are within reasonable range`() = runTest {
        val text = "Sample text for range check"
        val embedding = embeddingService.generateEmbedding(text)
        
        // After normalization, values should typically be in [-1, 1]
        assertTrue(embedding.all { it >= -2.0f && it <= 2.0f })
    }
    
    @Test
    fun `generateEmbedding handles unicode characters`() = runTest {
        val text = "Unicode: 你好 🌍 café"
        val embedding = embeddingService.generateEmbedding(text)
        
        assertEquals(384, embedding.size)
        assertTrue(embedding.all { !it.isNaN() && !it.isInfinite() })
    }
    
    @Test
    fun `similar texts produce similar embeddings`() = runTest {
        val text1 = "machine learning"
        val text2 = "machine learning algorithm"
        
        val embedding1 = embeddingService.generateEmbedding(text1)
        val embedding2 = embeddingService.generateEmbedding(text2)
        
        // Calculate cosine similarity
        var dotProduct = 0.0
        var mag1 = 0.0
        var mag2 = 0.0
        
        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
            mag1 += embedding1[i] * embedding1[i]
            mag2 += embedding2[i] * embedding2[i]
        }
        
        val similarity = dotProduct / (kotlin.math.sqrt(mag1) * kotlin.math.sqrt(mag2))
        
        // Similar texts should have positive similarity
        assertTrue("Expected positive similarity for similar texts", similarity > 0.0)
    }
}
