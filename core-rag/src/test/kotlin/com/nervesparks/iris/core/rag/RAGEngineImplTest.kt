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
    
    @Test
    fun `indexDocument succeeds for valid document`() = runTest {
        val document = Document(
            id = "doc1",
            content = "This is a test document",
            source = DataSource.NOTE
        )
        
        val result = ragEngine.indexDocument(document)
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `search returns relevant chunks`() = runTest {
        val document = Document(
            id = "doc1",
            content = "This is a test document about AI and machine learning",
            source = DataSource.NOTE
        )
        ragEngine.indexDocument(document)
        
        val results = ragEngine.search("AI machine learning", limit = 5)
        
        assertTrue(results.isNotEmpty())
        assertEquals(document.id, results.first().documentId)
    }
    
    @Test
    fun `deleteIndex removes document`() = runTest {
        val document = Document(
            id = "doc1",
            content = "Test content",
            source = DataSource.NOTE
        )
        ragEngine.indexDocument(document)
        
        val result = ragEngine.deleteIndex(document.id)
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `getIndexStats returns valid stats`() = runTest {
        val document = Document(
            id = "doc1",
            content = "Test content",
            source = DataSource.NOTE
        )
        ragEngine.indexDocument(document)
        
        val stats = ragEngine.getIndexStats()
        
        assertTrue(stats.totalDocuments >= 1)
        assertTrue(stats.lastUpdated > 0)
    }
}
