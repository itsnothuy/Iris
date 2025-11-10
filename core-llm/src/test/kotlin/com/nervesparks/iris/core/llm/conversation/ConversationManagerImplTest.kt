package com.nervesparks.iris.core.llm.conversation

import com.nervesparks.iris.core.llm.inference.FinishReason
import com.nervesparks.iris.core.llm.inference.GenerationParameters
import com.nervesparks.iris.core.llm.inference.InferenceResult
import com.nervesparks.iris.core.llm.inference.InferenceSession
import com.nervesparks.iris.core.llm.inference.InferenceSessionContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ConversationManagerImpl
 */
class ConversationManagerImplTest {
    
    private lateinit var conversationManager: ConversationManagerImpl
    private lateinit var inferenceSession: InferenceSession
    
    @Before
    fun setup() {
        inferenceSession = mockk()
        conversationManager = ConversationManagerImpl(inferenceSession)
    }
    
    @Test
    fun `createConversation successfully creates new conversation`() = runTest {
        val result = conversationManager.createConversation("Test Conversation")
        
        assertTrue(result.isSuccess)
        val conversationId = result.getOrNull()
        assertNotNull(conversationId)
        
        // Verify conversation was created
        val metadata = conversationManager.getConversation(conversationId!!)
        assertNotNull(metadata)
        assertEquals("Test Conversation", metadata?.title)
        assertEquals(0, metadata?.messageCount)
    }
    
    @Test
    fun `createConversation uses default title when none provided`() = runTest {
        val result = conversationManager.createConversation()
        
        assertTrue(result.isSuccess)
        val conversationId = result.getOrNull()
        
        val metadata = conversationManager.getConversation(conversationId!!)
        assertEquals("New Conversation", metadata?.title)
    }
    
    @Test
    fun `getConversation returns null for non-existent conversation`() = runTest {
        val metadata = conversationManager.getConversation("non-existent-id")
        
        assertNull(metadata)
    }
    
    @Test
    fun `getMessages returns empty list for new conversation`() = runTest {
        val result = conversationManager.createConversation()
        val conversationId = result.getOrNull()!!
        
        val messages = conversationManager.getMessages(conversationId).first()
        
        assertTrue(messages.isEmpty())
    }
    
    @Test
    fun `sendMessage creates inference session and generates response`() = runTest {
        // Setup
        val result = conversationManager.createConversation()
        val conversationId = result.getOrNull()!!
        
        coEvery { inferenceSession.createSession(any()) } returns Result.success(
            InferenceSessionContext(
                sessionId = conversationId,
                modelId = "test-model",
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )
        
        coEvery { inferenceSession.generateResponse(any(), any(), any()) } returns flowOf(
            InferenceResult.GenerationStarted(conversationId),
            InferenceResult.TokenGenerated(
                sessionId = conversationId,
                token = "Hello",
                partialText = "Hello",
                tokenIndex = 1
            ),
            InferenceResult.TokenGenerated(
                sessionId = conversationId,
                token = " World",
                partialText = "Hello World",
                tokenIndex = 2
            ),
            InferenceResult.GenerationCompleted(
                sessionId = conversationId,
                fullText = "Hello World",
                tokenCount = 2,
                generationTime = 100L,
                tokensPerSecond = 20.0,
                finishReason = FinishReason.COMPLETED
            )
        )
        
        // Execute
        val results = conversationManager.sendMessage(
            conversationId,
            "Hi",
            GenerationParameters()
        ).toList()
        
        // Verify
        assertEquals(4, results.size)
        assertTrue(results[0] is InferenceResult.GenerationStarted)
        assertTrue(results[1] is InferenceResult.TokenGenerated)
        assertTrue(results[2] is InferenceResult.TokenGenerated)
        assertTrue(results[3] is InferenceResult.GenerationCompleted)
        
        coVerify { inferenceSession.createSession(conversationId) }
        coVerify { inferenceSession.generateResponse(conversationId, "Hi", any()) }
        
        // Check messages were added
        val messages = conversationManager.getMessages(conversationId).first()
        assertEquals(2, messages.size)
        assertEquals("Hi", messages[0].content)
        assertEquals(MessageRole.USER, messages[0].role)
        assertEquals("Hello World", messages[1].content)
        assertEquals(MessageRole.ASSISTANT, messages[1].role)
    }
    
    @Test
    fun `deleteConversation removes conversation and closes session`() = runTest {
        val result = conversationManager.createConversation()
        val conversationId = result.getOrNull()!!
        
        coEvery { inferenceSession.createSession(any()) } returns Result.success(
            InferenceSessionContext(
                sessionId = conversationId,
                modelId = "test-model",
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )
        coEvery { inferenceSession.closeSession(any()) } returns true
        
        // Verify conversation exists
        assertNotNull(conversationManager.getConversation(conversationId))
        
        // Delete
        val deleted = conversationManager.deleteConversation(conversationId)
        
        assertTrue(deleted)
        assertNull(conversationManager.getConversation(conversationId))
    }
    
    @Test
    fun `deleteConversation returns false for non-existent conversation`() = runTest {
        val deleted = conversationManager.deleteConversation("non-existent")
        
        assertFalse(deleted)
    }
    
    @Test
    fun `clearConversation removes all messages`() = runTest {
        val result = conversationManager.createConversation()
        val conversationId = result.getOrNull()!!
        
        coEvery { inferenceSession.createSession(any()) } returns Result.success(
            InferenceSessionContext(
                sessionId = conversationId,
                modelId = "test-model",
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )
        
        coEvery { inferenceSession.generateResponse(any(), any(), any()) } returns flowOf(
            InferenceResult.GenerationCompleted(
                sessionId = conversationId,
                fullText = "Response",
                tokenCount = 1,
                generationTime = 100L,
                tokensPerSecond = 10.0,
                finishReason = FinishReason.COMPLETED
            )
        )
        
        coEvery { inferenceSession.closeSession(any()) } returns true
        
        // Add some messages
        conversationManager.sendMessage(conversationId, "Test", GenerationParameters()).toList()
        
        // Verify messages exist
        var messages = conversationManager.getMessages(conversationId).first()
        assertEquals(2, messages.size)
        
        // Clear
        val cleared = conversationManager.clearConversation(conversationId)
        
        assertTrue(cleared)
        
        // Verify messages are cleared
        messages = conversationManager.getMessages(conversationId).first()
        assertTrue(messages.isEmpty())
        
        // Verify metadata is updated
        val metadata = conversationManager.getConversation(conversationId)
        assertEquals(0, metadata?.messageCount)
        assertEquals(0, metadata?.totalTokens)
    }
    
    @Test
    fun `getAllConversations returns list of conversations`() = runTest {
        conversationManager.createConversation("Conversation 1")
        conversationManager.createConversation("Conversation 2")
        conversationManager.createConversation("Conversation 3")
        
        val conversations = conversationManager.getAllConversations().first()
        
        assertEquals(3, conversations.size)
    }
    
    @Test
    fun `conversations are sorted by last modified`() = runTest {
        val id1 = conversationManager.createConversation("First").getOrNull()!!
        Thread.sleep(10)
        val id2 = conversationManager.createConversation("Second").getOrNull()!!
        Thread.sleep(10)
        val id3 = conversationManager.createConversation("Third").getOrNull()!!
        
        val conversations = conversationManager.getAllConversations().first()
        
        // Most recent should be first
        assertEquals(id3, conversations[0].id)
        assertEquals(id2, conversations[1].id)
        assertEquals(id1, conversations[2].id)
    }
}
