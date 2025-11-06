package com.nervesparks.iris.core.llm

import com.nervesparks.iris.common.models.BackendType
import com.nervesparks.iris.common.models.ComputeTask
import com.nervesparks.iris.common.models.GenerationParams
import com.nervesparks.iris.core.hw.BackendRouter
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LLMEngineImpl
 */
class LLMEngineImplTest {
    
    private lateinit var llmEngine: LLMEngineImpl
    private lateinit var backendRouter: BackendRouter
    
    @Before
    fun setup() {
        backendRouter = mockk()
        every { backendRouter.getCurrentBackend() } returns BackendType.CPU_NEON
        llmEngine = LLMEngineImpl(backendRouter)
    }
    
    @Test
    fun `loadModel returns success with valid path`() = runTest {
        val result = llmEngine.loadModel("/path/to/model.gguf")
        
        assertTrue(result.isSuccess)
        val handle = result.getOrNull()
        assertNotNull(handle)
        assertEquals("/path/to/model.gguf", handle?.modelPath)
    }
    
    @Test
    fun `generateText emits tokens`() = runTest {
        val prompt = "Hello world"
        val params = GenerationParams()
        
        val tokens = llmEngine.generateText(prompt, params).toList()
        
        assertTrue(tokens.isNotEmpty())
        assertTrue(tokens.any { it.contains("mock") })
    }
    
    @Test
    fun `embed returns float array`() = runTest {
        val embedding = llmEngine.embed("test text")
        
        assertNotNull(embedding)
        assertEquals(768, embedding.size)
    }
    
    @Test
    fun `isModelLoaded returns false for unloaded model`() {
        assertFalse(llmEngine.isModelLoaded("/nonexistent/model.gguf"))
    }
    
    @Test
    fun `isModelLoaded returns true after loading`() = runTest {
        val modelPath = "/path/to/model.gguf"
        llmEngine.loadModel(modelPath)
        
        assertTrue(llmEngine.isModelLoaded(modelPath))
    }
    
    @Test
    fun `unloadModel removes model from cache`() = runTest {
        val modelPath = "/path/to/model.gguf"
        val result = llmEngine.loadModel(modelPath)
        val handle = result.getOrThrow()
        
        assertTrue(llmEngine.isModelLoaded(modelPath))
        
        llmEngine.unloadModel(handle)
        
        assertFalse(llmEngine.isModelLoaded(modelPath))
    }
}
