package com.nervesparks.iris.core.rag

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RAGEngineImpl
 */
class RAGEngineImplTest {
    
    private lateinit var ragEngine: RAGEngineImpl
    
    @Before
    fun setup() {
        ragEngine = RAGEngineImpl()
    }
    
    // Basic functionality tests
    @Test
    fun `indexDocument succeeds for valid document`() = runTest {
        val document = Document(
            id = "doc1",
            content = "This is a test document about artificial intelligence and machine learning",
            source = DataSource.NOTE
        )
        
        val result = ragEngine.indexDocument(document)
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `search returns relevant chunks for exact match`() = runTest {
        val document = Document(
            id = "doc1",
            content = "This is a test document about artificial intelligence and machine learning. " +
                    "Machine learning is a subset of artificial intelligence that focuses on data.",
            source = DataSource.NOTE
        )
        ragEngine.indexDocument(document)
        
        val results = ragEngine.search("artificial intelligence", limit = 5)
        
        assertTrue(results.isNotEmpty())
        assertTrue(results.first().score > 0.0f)
        assertEquals(document.id, results.first().documentId)
    }
    
    @Test
    fun `search returns empty list for no matches`() = runTest {
        val document = Document(
            id = "doc1",
            content = "This is about cats and dogs",
            source = DataSource.NOTE
        )
        ragEngine.indexDocument(document)
        
        val results = ragEngine.search("quantum physics", limit = 5)
        
        // Should return empty or very low scores
        assertTrue(results.isEmpty() || results.all { it.score < 0.1f })
    }
    
    @Test
    fun `search handles empty index`() = runTest {
        val results = ragEngine.search("anything", limit = 5)
        
        assertTrue(results.isEmpty())
    }
    
    @Test
    fun `search respects limit parameter`() = runTest {
        val document = Document(
            id = "doc1",
            content = "The quick brown fox jumps over the lazy dog. ".repeat(10), // Create multiple chunks
            source = DataSource.NOTE
        )
        ragEngine.indexDocument(document)
        
        val results = ragEngine.search("quick brown fox", limit = 2)
        
        assertTrue(results.size <= 2)
    }
    
    @Test
    fun `deleteIndex removes document`() = runTest {
        val document = Document(
            id = "doc1",
            content = "Test content for deletion",
            source = DataSource.NOTE
        )
        ragEngine.indexDocument(document)
        
        val result = ragEngine.deleteIndex(document.id)
        
        assertTrue(result.isSuccess)
        
        // Verify document is gone
        val stats = ragEngine.getIndexStats()
        assertEquals(0, stats.totalDocuments)
    }
    
    @Test
    fun `deleteIndex handles non-existent document`() = runTest {
        val result = ragEngine.deleteIndex("nonexistent")
        
        assertTrue(result.isSuccess) // Should not fail
    }
    
    @Test
    fun `updateDocument replaces existing document`() = runTest {
        val document1 = Document(
            id = "doc1",
            content = "Original content",
            source = DataSource.NOTE
        )
        ragEngine.indexDocument(document1)
        
        val document2 = Document(
            id = "doc1",
            content = "Updated content",
            source = DataSource.NOTE
        )
        val result = ragEngine.updateDocument(document2)
        
        assertTrue(result.isSuccess)
        
        // Search should find updated content
        val results = ragEngine.search("updated content", limit = 5)
        assertTrue(results.isNotEmpty())
    }
    
    @Test
    fun `getIndexStats returns valid stats`() = runTest {
        val document = Document(
            id = "doc1",
            content = "Test content ".repeat(100), // Create multiple chunks
            source = DataSource.NOTE
        )
        ragEngine.indexDocument(document)
        
        val stats = ragEngine.getIndexStats()
        
        assertEquals(1, stats.totalDocuments)
        assertTrue(stats.totalChunks >= 1)
        assertTrue(stats.indexSize > 0)
        assertTrue(stats.lastUpdated > 0)
    }
    
    @Test
    fun `getIndexStats handles empty index`() = runTest {
        val stats = ragEngine.getIndexStats()
        
        assertEquals(0, stats.totalDocuments)
        assertEquals(0, stats.totalChunks)
    }
    
    @Test
    fun `optimizeIndex succeeds`() = runTest {
        val result = ragEngine.optimizeIndex()
        
        assertTrue(result.isSuccess)
    }
    
    // Document chunking tests
    @Test
    fun `long documents are split into chunks`() = runTest {
        val longContent = "This is a sentence. ".repeat(100) // Create long document
        val document = Document(
            id = "doc1",
            content = longContent,
            source = DataSource.NOTE
        )
        ragEngine.indexDocument(document)
        
        val stats = ragEngine.getIndexStats()
        
        assertTrue(stats.totalChunks > 1) // Should be split into multiple chunks
    }
    
    @Test
    fun `short documents create single chunk`() = runTest {
        val document = Document(
            id = "doc1",
            content = "Short document",
            source = DataSource.NOTE
        )
        ragEngine.indexDocument(document)
        
        val stats = ragEngine.getIndexStats()
        
        assertEquals(1, stats.totalChunks)
    }
    
    @Test
    fun `chunks have proper metadata`() = runTest {
        val document = Document(
            id = "doc1",
            content = "Test content",
            source = DataSource.NOTE,
            metadata = mapOf("author" to "Test Author")
        )
        ragEngine.indexDocument(document)
        
        val results = ragEngine.search("test content", limit = 1)
        
        assertTrue(results.isNotEmpty())
        assertTrue(results.first().metadata.containsKey("source"))
        assertTrue(results.first().metadata.containsKey("timestamp"))
    }
    
    // Search relevance tests
    @Test
    fun `search ranks more relevant results higher`() = runTest {
        val doc1 = Document(
            id = "doc1",
            content = "Machine learning and artificial intelligence",
            source = DataSource.NOTE
        )
        val doc2 = Document(
            id = "doc2",
            content = "Cooking recipes and kitchen tips",
            source = DataSource.NOTE
        )
        ragEngine.indexDocument(doc1)
        ragEngine.indexDocument(doc2)
        
        val results = ragEngine.search("machine learning", limit = 5)
        
        assertTrue(results.isNotEmpty())
        // First result should be from doc1 with higher score
        assertEquals("doc1", results.first().documentId)
    }
    
    @Test
    fun `search is case insensitive`() = runTest {
        val document = Document(
            id = "doc1",
            content = "Machine Learning and Artificial Intelligence",
            source = DataSource.NOTE
        )
        ragEngine.indexDocument(document)
        
        val results1 = ragEngine.search("MACHINE LEARNING", limit = 5)
        val results2 = ragEngine.search("machine learning", limit = 5)
        val results3 = ragEngine.search("Machine Learning", limit = 5)
        
        assertTrue(results1.isNotEmpty())
        assertTrue(results2.isNotEmpty())
        assertTrue(results3.isNotEmpty())
    }
    
    @Test
    fun `search handles stop words`() = runTest {
        val document = Document(
            id = "doc1",
            content = "The quick brown fox jumps over the lazy dog",
            source = DataSource.NOTE
        )
        ragEngine.indexDocument(document)
        
        // Search with stop words should still work
        val results = ragEngine.search("the quick brown fox", limit = 5)
        
        assertTrue(results.isNotEmpty())
    }
    
    // Multiple documents tests
    @Test
    fun `search across multiple documents`() = runTest {
        val doc1 = Document(
            id = "doc1",
            content = "Python programming language",
            source = DataSource.NOTE
        )
        val doc2 = Document(
            id = "doc2",
            content = "Kotlin programming language",
            source = DataSource.NOTE
        )
        val doc3 = Document(
            id = "doc3",
            content = "Java programming language",
            source = DataSource.NOTE
        )
        
        ragEngine.indexDocument(doc1)
        ragEngine.indexDocument(doc2)
        ragEngine.indexDocument(doc3)
        
        val results = ragEngine.search("programming language", limit = 5)
        
        assertEquals(3, results.size)
        assertTrue(results.all { it.score > 0.0f })
    }
    
    @Test
    fun `deleteIndex does not affect other documents`() = runTest {
        val doc1 = Document(
            id = "doc1",
            content = "First document",
            source = DataSource.NOTE
        )
        val doc2 = Document(
            id = "doc2",
            content = "Second document",
            source = DataSource.NOTE
        )
        
        ragEngine.indexDocument(doc1)
        ragEngine.indexDocument(doc2)
        ragEngine.deleteIndex("doc1")
        
        val stats = ragEngine.getIndexStats()
        assertEquals(1, stats.totalDocuments)
        
        val results = ragEngine.search("second document", limit = 5)
        assertTrue(results.isNotEmpty())
    }
    
    // Edge cases
    @Test
    fun `indexDocument handles empty content`() = runTest {
        val document = Document(
            id = "doc1",
            content = "",
            source = DataSource.NOTE
        )
        
        val result = ragEngine.indexDocument(document)
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `search handles empty query`() = runTest {
        val document = Document(
            id = "doc1",
            content = "Test content",
            source = DataSource.NOTE
        )
        ragEngine.indexDocument(document)
        
        val results = ragEngine.search("", limit = 5)
        
        assertTrue(results.isEmpty())
    }
    
    @Test
    fun `search handles special characters in query`() = runTest {
        val document = Document(
            id = "doc1",
            content = "Email: test@example.com",
            source = DataSource.NOTE
        )
        ragEngine.indexDocument(document)
        
        val results = ragEngine.search("test@example.com", limit = 5)
        
        // Should handle gracefully, may or may not find results
        assertTrue(results.isEmpty() || results.all { it.score >= 0.0f })
    }
}
