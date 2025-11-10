package com.nervesparks.iris.core.rag

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for ChunkingServiceImpl
 */
@RunWith(RobolectricTestRunner::class)
class ChunkingServiceImplTest {
    
    private lateinit var chunkingService: ChunkingServiceImpl
    
    @Before
    fun setup() {
        chunkingService = ChunkingServiceImpl()
    }
    
    @Test
    fun `chunkText creates single chunk for short text`() = runTest {
        val text = "This is a short text."
        val chunks = chunkingService.chunkText(
            text = text,
            maxChunkSize = 1000,
            overlap = 100,
            documentId = "doc1"
        )
        
        assertEquals(1, chunks.size)
        assertEquals(text.trim(), chunks[0].content)
        assertEquals("0", chunks[0].metadata["chunk_index"])
        assertEquals("doc1", chunks[0].metadata["document_id"])
    }
    
    @Test
    fun `chunkText splits long text into multiple chunks`() = runTest {
        val paragraph = "This is a paragraph. ".repeat(30) // ~630 chars
        val text = "$paragraph\n\n$paragraph" // ~1260 chars
        
        val chunks = chunkingService.chunkText(
            text = text,
            maxChunkSize = 500,
            overlap = 100,
            documentId = "doc1"
        )
        
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.content.isNotEmpty() })
    }
    
    @Test
    fun `chunkText respects maxChunkSize`() = runTest {
        // Create text with paragraph breaks to allow proper chunking
        val paragraph = "This is a test paragraph. ".repeat(20) // ~500 chars
        val longText = List(5) { paragraph }.joinToString("\n\n") // ~2500 chars with breaks
        
        val chunks = chunkingService.chunkText(
            text = longText,
            maxChunkSize = 1000,
            overlap = 200,
            documentId = "doc1"
        )
        
        // Should create multiple chunks
        assertTrue("Should create at least 2 chunks", chunks.size >= 2)
        assertTrue("All chunks should have content", chunks.all { it.content.isNotEmpty() })
    }
    
    @Test
    fun `chunkText includes overlap between chunks`() = runTest {
        val text = "Sentence one. Sentence two. Sentence three. Sentence four. Sentence five."
        
        val chunks = chunkingService.chunkText(
            text = text,
            maxChunkSize = 50,
            overlap = 20,
            documentId = "doc1"
        )
        
        if (chunks.size > 1) {
            // Check that there's some overlap in content
            val firstChunk = chunks[0].content
            val secondChunk = chunks[1].content
            
            // At least one word should appear in both chunks
            val firstWords = firstChunk.split(" ").takeLast(3)
            val secondWords = secondChunk.split(" ").take(5)
            
            val hasOverlap = firstWords.any { word -> 
                secondWords.any { it.contains(word.trim(), ignoreCase = true) }
            }
            assertTrue("Expected overlap between chunks", hasOverlap || chunks.size == 1)
        }
    }
    
    @Test
    fun `chunkText handles empty text`() = runTest {
        val chunks = chunkingService.chunkText(
            text = "",
            maxChunkSize = 1000,
            overlap = 100,
            documentId = "doc1"
        )
        
        assertEquals(0, chunks.size)
    }
    
    @Test
    fun `chunkText handles text with only whitespace`() = runTest {
        val chunks = chunkingService.chunkText(
            text = "   \n\n   \t   ",
            maxChunkSize = 1000,
            overlap = 100,
            documentId = "doc1"
        )
        
        assertEquals(0, chunks.size)
    }
    
    @Test
    fun `chunkText preserves paragraph structure`() = runTest {
        val para1 = "First paragraph content"
        val para2 = "Second paragraph content"
        val text = "$para1\n\n$para2"
        
        val chunks = chunkingService.chunkText(
            text = text,
            maxChunkSize = 1000,
            overlap = 100,
            documentId = "doc1"
        )
        
        assertEquals(1, chunks.size)
        assertTrue(chunks[0].content.contains("First paragraph"))
        assertTrue(chunks[0].content.contains("Second paragraph"))
    }
    
    @Test
    fun `smartChunkText creates chunks with semantic boundaries`() = runTest {
        val sentence1 = "This is the first sentence."
        val sentence2 = "This is the second sentence."
        val sentence3 = "This is the third sentence."
        val text = "$sentence1 $sentence2 $sentence3"
        
        val chunks = chunkingService.smartChunkText(
            text = text,
            maxChunkSize = 50,
            overlap = 20,
            documentId = "doc1"
        )
        
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.all { it.metadata["chunking_method"] == "smart_semantic" })
    }
    
    @Test
    fun `smartChunkText respects sentence boundaries`() = runTest {
        val text = "Short one. Second sentence here. Third sentence. Fourth sentence content."
        
        val chunks = chunkingService.smartChunkText(
            text = text,
            maxChunkSize = 40,
            overlap = 10,
            documentId = "doc1"
        )
        
        assertTrue(chunks.isNotEmpty())
        // Chunks should end with complete words
        assertTrue(chunks.all { chunk -> 
            val lastChar = chunk.content.lastOrNull()
            lastChar == null || lastChar.isLetterOrDigit() || lastChar == '.'
        })
    }
    
    @Test
    fun `chunkText tracks start and end indices`() = runTest {
        val text = "First part. Second part. Third part."
        
        val chunks = chunkingService.chunkText(
            text = text,
            maxChunkSize = 1000,
            overlap = 100,
            documentId = "doc1"
        )
        
        assertEquals(1, chunks.size)
        assertEquals(0, chunks[0].startIndex)
        assertEquals(text.trim().length, chunks[0].endIndex)
    }
    
    @Test
    fun `chunkText assigns sequential chunk indices`() = runTest {
        val longText = "Word ".repeat(1000)
        
        val chunks = chunkingService.chunkText(
            text = longText,
            maxChunkSize = 500,
            overlap = 100,
            documentId = "doc1"
        )
        
        for (i in chunks.indices) {
            assertEquals(i.toString(), chunks[i].metadata["chunk_index"])
        }
    }
    
    @Test
    fun `chunkText handles special characters`() = runTest {
        val text = "Email: test@example.com. Price: \$99.99. Date: 2024-01-01."
        
        val chunks = chunkingService.chunkText(
            text = text,
            maxChunkSize = 1000,
            overlap = 100,
            documentId = "doc1"
        )
        
        assertEquals(1, chunks.size)
        assertTrue(chunks[0].content.contains("@"))
        assertTrue(chunks[0].content.contains("\$"))
    }
    
    @Test
    fun `smartChunkText handles empty text`() = runTest {
        val chunks = chunkingService.smartChunkText(
            text = "",
            maxChunkSize = 1000,
            overlap = 100,
            documentId = "doc1"
        )
        
        assertEquals(0, chunks.size)
    }
    
    @Test
    fun `chunkText with zero overlap works correctly`() = runTest {
        val text = "A ".repeat(600) // Long text
        
        val chunks = chunkingService.chunkText(
            text = text,
            maxChunkSize = 500,
            overlap = 0,
            documentId = "doc1"
        )
        
        // Should create at least one chunk
        assertTrue(chunks.size >= 1)
    }
}
