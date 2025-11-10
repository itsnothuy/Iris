package com.nervesparks.iris.core.rag

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for VectorStoreImpl
 */
@RunWith(RobolectricTestRunner::class)
class VectorStoreImplTest {
    
    private lateinit var vectorStore: VectorStoreImpl
    private lateinit var embeddingService: EmbeddingServiceImpl
    
    @Before
    fun setup() {
        embeddingService = EmbeddingServiceImpl()
        vectorStore = VectorStoreImpl(embeddingService)
    }
    
    @Test
    fun `saveDocument stores document successfully`() = runTest {
        val document = StoredDocument(
            id = "doc1",
            title = "Test Document",
            uri = "file://test.txt",
            mimeType = "text/plain",
            size = 1024,
            textContent = "Test content",
            metadata = emptyMap(),
            createdAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis(),
            chunkCount = 0,
            isProcessed = false
        )
        
        vectorStore.saveDocument(document)
        
        val retrieved = vectorStore.getDocument("doc1")
        assertNotNull(retrieved)
        assertEquals("doc1", retrieved?.id)
        assertEquals("Test Document", retrieved?.title)
    }
    
    @Test
    fun `getDocument returns null for non-existent document`() = runTest {
        val document = vectorStore.getDocument("nonexistent")
        
        assertNull(document)
    }
    
    @Test
    fun `updateDocument modifies existing document`() = runTest {
        val document = StoredDocument(
            id = "doc1",
            title = "Original Title",
            uri = "file://test.txt",
            mimeType = "text/plain",
            size = 1024,
            textContent = "Original content",
            metadata = emptyMap(),
            createdAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis(),
            chunkCount = 0,
            isProcessed = false
        )
        
        vectorStore.saveDocument(document)
        
        val updated = document.copy(title = "Updated Title", isProcessed = true)
        vectorStore.updateDocument(updated)
        
        val retrieved = vectorStore.getDocument("doc1")
        assertEquals("Updated Title", retrieved?.title)
        assertTrue(retrieved?.isProcessed == true)
    }
    
    @Test
    fun `deleteDocument removes document`() = runTest {
        val document = StoredDocument(
            id = "doc1",
            title = "Test",
            uri = "file://test.txt",
            mimeType = "text/plain",
            size = 1024,
            textContent = "Test",
            metadata = emptyMap(),
            createdAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis(),
            chunkCount = 0,
            isProcessed = false
        )
        
        vectorStore.saveDocument(document)
        assertTrue(vectorStore.deleteDocument("doc1"))
        
        assertNull(vectorStore.getDocument("doc1"))
    }
    
    @Test
    fun `deleteDocument returns false for non-existent document`() = runTest {
        val result = vectorStore.deleteDocument("nonexistent")
        
        assertFalse(result)
    }
    
    @Test
    fun `getAllDocuments returns all stored documents`() = runTest {
        val doc1 = createTestDocument("doc1", "First")
        val doc2 = createTestDocument("doc2", "Second")
        val doc3 = createTestDocument("doc3", "Third")
        
        vectorStore.saveDocument(doc1)
        vectorStore.saveDocument(doc2)
        vectorStore.saveDocument(doc3)
        
        val all = vectorStore.getAllDocuments()
        
        assertEquals(3, all.size)
        assertTrue(all.any { it.id == "doc1" })
        assertTrue(all.any { it.id == "doc2" })
        assertTrue(all.any { it.id == "doc3" })
    }
    
    @Test
    fun `getAllDocuments returns empty list when no documents`() = runTest {
        val all = vectorStore.getAllDocuments()
        
        assertEquals(0, all.size)
    }
    
    @Test
    fun `saveChunks stores embedded chunks`() = runTest {
        val embedding = embeddingService.generateEmbedding("test content")
        val chunk = EmbeddedChunk(
            id = "chunk1",
            documentId = "doc1",
            content = "test content",
            embedding = embedding,
            startIndex = 0,
            endIndex = 12,
            metadata = emptyMap()
        )
        
        vectorStore.saveChunks(listOf(chunk))
        
        // Verify by searching
        val results = vectorStore.searchSimilar(embedding, 10, 0.0f)
        assertTrue(results.isNotEmpty())
        assertEquals("chunk1", results[0].chunk.id)
    }
    
    @Test
    fun `saveChunks handles multiple chunks`() = runTest {
        val chunks = listOf(
            createEmbeddedChunk("chunk1", "doc1", "content one"),
            createEmbeddedChunk("chunk2", "doc1", "content two"),
            createEmbeddedChunk("chunk3", "doc1", "content three")
        )
        
        vectorStore.saveChunks(chunks)
        
        val queryEmbedding = embeddingService.generateEmbedding("content")
        val results = vectorStore.searchSimilar(queryEmbedding, 10, 0.0f)
        
        assertTrue(results.size >= 3)
    }
    
    @Test
    fun `deleteChunksByDocumentId removes all chunks for document`() = runTest {
        val chunks = listOf(
            createEmbeddedChunk("chunk1", "doc1", "content one"),
            createEmbeddedChunk("chunk2", "doc1", "content two"),
            createEmbeddedChunk("chunk3", "doc2", "content three")
        )
        
        vectorStore.saveChunks(chunks)
        vectorStore.deleteChunksByDocumentId("doc1")
        
        // Search should only find chunk3
        val queryEmbedding = embeddingService.generateEmbedding("content")
        val results = vectorStore.searchSimilar(queryEmbedding, 10, 0.0f)
        
        assertTrue(results.all { it.chunk.documentId != "doc1" })
    }
    
    @Test
    fun `searchSimilar returns relevant chunks`() = runTest {
        val chunks = listOf(
            createEmbeddedChunk("chunk1", "doc1", "machine learning algorithms"),
            createEmbeddedChunk("chunk2", "doc1", "cooking recipes"),
            createEmbeddedChunk("chunk3", "doc1", "artificial intelligence")
        )
        
        vectorStore.saveChunks(chunks)
        
        val queryEmbedding = embeddingService.generateEmbedding("machine learning")
        val results = vectorStore.searchSimilar(queryEmbedding, 3, 0.0f)
        
        assertTrue(results.isNotEmpty())
        // First result should be most similar
        assertTrue(results[0].score >= (results.lastOrNull()?.score ?: 0.0f))
    }
    
    @Test
    fun `searchSimilar respects limit parameter`() = runTest {
        val chunks = (1..10).map { i ->
            createEmbeddedChunk("chunk$i", "doc1", "content $i")
        }
        
        vectorStore.saveChunks(chunks)
        
        val queryEmbedding = embeddingService.generateEmbedding("content")
        val results = vectorStore.searchSimilar(queryEmbedding, 3, 0.0f)
        
        assertEquals(3, results.size)
    }
    
    @Test
    fun `searchSimilar filters by threshold`() = runTest {
        val chunks = listOf(
            createEmbeddedChunk("chunk1", "doc1", "machine learning"),
            createEmbeddedChunk("chunk2", "doc1", "completely different topic")
        )
        
        vectorStore.saveChunks(chunks)
        
        val queryEmbedding = embeddingService.generateEmbedding("machine learning")
        val results = vectorStore.searchSimilar(queryEmbedding, 10, 0.8f)
        
        // High threshold should filter out dissimilar results
        assertTrue(results.all { it.score >= 0.8f })
    }
    
    @Test
    fun `searchSimilar returns empty list when no chunks match`() = runTest {
        val chunks = listOf(
            createEmbeddedChunk("chunk1", "doc1", "specific content")
        )
        
        vectorStore.saveChunks(chunks)
        
        val queryEmbedding = embeddingService.generateEmbedding("different query")
        val results = vectorStore.searchSimilar(queryEmbedding, 10, 0.99f)
        
        // Very high threshold may return no results
        assertTrue(results.isEmpty() || results.all { it.score >= 0.99f })
    }
    
    @Test
    fun `searchSimilar returns empty list when no chunks stored`() = runTest {
        val queryEmbedding = embeddingService.generateEmbedding("query")
        val results = vectorStore.searchSimilar(queryEmbedding, 10, 0.0f)
        
        assertEquals(0, results.size)
    }
    
    @Test
    fun `searchSimilar returns results sorted by score`() = runTest {
        val chunks = listOf(
            createEmbeddedChunk("chunk1", "doc1", "artificial intelligence"),
            createEmbeddedChunk("chunk2", "doc1", "machine learning"),
            createEmbeddedChunk("chunk3", "doc1", "deep neural networks")
        )
        
        vectorStore.saveChunks(chunks)
        
        val queryEmbedding = embeddingService.generateEmbedding("machine learning")
        val results = vectorStore.searchSimilar(queryEmbedding, 3, 0.0f)
        
        // Verify descending order
        for (i in 0 until results.size - 1) {
            assertTrue(results[i].score >= results[i + 1].score)
        }
    }
    
    // Helper methods
    
    private suspend fun createEmbeddedChunk(
        id: String,
        documentId: String,
        content: String
    ): EmbeddedChunk {
        val embedding = embeddingService.generateEmbedding(content)
        return EmbeddedChunk(
            id = id,
            documentId = documentId,
            content = content,
            embedding = embedding,
            startIndex = 0,
            endIndex = content.length,
            metadata = emptyMap()
        )
    }
    
    private fun createTestDocument(id: String, title: String): StoredDocument {
        return StoredDocument(
            id = id,
            title = title,
            uri = "file://test.txt",
            mimeType = "text/plain",
            size = 1024,
            textContent = "Test content",
            metadata = emptyMap(),
            createdAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis(),
            chunkCount = 0,
            isProcessed = false
        )
    }
}
