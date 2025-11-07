package com.nervesparks.iris.core.tools

import android.content.Context
import com.nervesparks.iris.core.tools.executor.DirectApiExecutor
import com.nervesparks.iris.core.tools.executor.IntentLaunchExecutor
import com.nervesparks.iris.core.tools.models.ExecutionResult
import com.nervesparks.iris.core.tools.models.FunctionCall
import com.nervesparks.iris.core.tools.models.ToolAction
import com.nervesparks.iris.core.tools.parser.FunctionCallParser
import com.nervesparks.iris.core.tools.registry.ToolRegistry
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ToolEngineImplTest {
    
    private lateinit var toolEngine: ToolEngineImpl
    private lateinit var context: Context
    private lateinit var toolRegistry: ToolRegistry
    private lateinit var functionCallParser: FunctionCallParser
    private lateinit var intentLaunchExecutor: IntentLaunchExecutor
    private lateinit var directApiExecutor: DirectApiExecutor
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        toolRegistry = mockk()
        functionCallParser = mockk()
        intentLaunchExecutor = mockk()
        directApiExecutor = mockk()
        
        toolEngine = ToolEngineImpl(
            context,
            toolRegistry,
            functionCallParser,
            intentLaunchExecutor,
            directApiExecutor
        )
    }
    
    @Test
    fun `getAvailableTools returns tools from registry`() {
        val mockTools = listOf(
            mockk<com.nervesparks.iris.core.tools.models.ToolDefinition>(relaxed = true),
            mockk<com.nervesparks.iris.core.tools.models.ToolDefinition>(relaxed = true)
        )
        every { toolRegistry.getAllTools() } returns mockTools
        
        val result = toolEngine.getAvailableTools()
        
        assertEquals(mockTools, result)
        verify { toolRegistry.getAllTools() }
    }
    
    @Test
    fun `getTool returns tool from registry`() {
        val mockTool = mockk<com.nervesparks.iris.core.tools.models.ToolDefinition>(relaxed = true)
        every { toolRegistry.getTool("test_tool") } returns mockTool
        
        val result = toolEngine.getTool("test_tool")
        
        assertEquals(mockTool, result)
        verify { toolRegistry.getTool("test_tool") }
    }
    
    @Test
    fun `executeFunction fails when tool not found`() = runTest {
        val functionCall = FunctionCall("unknown_tool", emptyMap())
        every { toolRegistry.getTool("unknown_tool") } returns null
        
        val result = toolEngine.executeFunction(functionCall)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
    }
    
    @Test
    fun `executeFunction fails when validation fails`() = runTest {
        val functionCall = FunctionCall("test_tool", emptyMap())
        val mockTool = mockk<com.nervesparks.iris.core.tools.models.ToolDefinition>(relaxed = true) {
            every { name } returns "test_tool"
        }
        every { toolRegistry.getTool("test_tool") } returns mockTool
        every { 
            functionCallParser.validate(functionCall, mockTool)
        } returns Result.failure(IllegalArgumentException("Validation failed"))
        
        val result = toolEngine.executeFunction(functionCall)
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `executeFunction returns Declined when user declines`() = runTest {
        val functionCall = FunctionCall("test_tool", emptyMap())
        val mockTool = mockk<com.nervesparks.iris.core.tools.models.ToolDefinition>(relaxed = true) {
            every { name } returns "test_tool"
            every { requiredPermissions } returns emptyList()
        }
        
        every { toolRegistry.getTool("test_tool") } returns mockTool
        every { functionCallParser.validate(functionCall, mockTool) } returns Result.success(functionCall)
        
        // Set confirmation callback to decline
        toolEngine.setConfirmationCallback { false }
        
        val result = toolEngine.executeFunction(functionCall)
        
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is ExecutionResult.Declined)
    }
    
    @Test
    fun `requestUserConfirmation returns false when no callback set`() = runTest {
        val mockAction = mockk<ToolAction>(relaxed = true)
        
        val result = toolEngine.requestUserConfirmation(mockAction)
        
        assertFalse(result)
    }
    
    @Test
    fun `requestUserConfirmation calls callback when set`() = runTest {
        val mockAction = mockk<ToolAction>(relaxed = true)
        var callbackInvoked = false
        
        toolEngine.setConfirmationCallback { 
            callbackInvoked = true
            true
        }
        
        val result = toolEngine.requestUserConfirmation(mockAction)
        
        assertTrue(callbackInvoked)
        assertTrue(result)
    }
    
    @Test
    fun `getExecutionLogs returns execution history`() = runTest {
        // Execute a function that will be declined
        val functionCall = FunctionCall("test_tool", mapOf("param" to "value"))
        val mockTool = mockk<com.nervesparks.iris.core.tools.models.ToolDefinition>(relaxed = true) {
            every { name } returns "test_tool"
            every { requiredPermissions } returns emptyList()
        }
        
        every { toolRegistry.getTool("test_tool") } returns mockTool
        every { functionCallParser.validate(functionCall, mockTool) } returns Result.success(functionCall)
        toolEngine.setConfirmationCallback { false }
        
        toolEngine.executeFunction(functionCall)
        
        val logs = toolEngine.getExecutionLogs()
        
        assertTrue(logs.isNotEmpty())
        assertEquals("test_tool", logs.last().toolName)
        assertTrue(logs.last().result is ExecutionResult.Declined)
    }
}
