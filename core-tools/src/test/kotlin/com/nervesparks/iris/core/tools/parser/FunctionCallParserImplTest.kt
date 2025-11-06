package com.nervesparks.iris.core.tools.parser

import com.nervesparks.iris.core.tools.models.ExecutionType
import com.nervesparks.iris.core.tools.models.ParameterSpec
import com.nervesparks.iris.core.tools.models.ToolDefinition
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FunctionCallParserImplTest {
    
    private lateinit var parser: FunctionCallParserImpl
    
    @Before
    fun setup() {
        parser = FunctionCallParserImpl()
    }
    
    @Test
    fun `parse extracts valid function call from JSON`() {
        val json = """{"name": "test_function", "arguments": {"arg1": "value1", "arg2": "value2"}}"""
        
        val result = parser.parse(json)
        
        assertNotNull(result)
        assertEquals("test_function", result?.name)
        assertEquals(2, result?.arguments?.size)
        assertEquals("value1", result?.arguments?.get("arg1"))
        assertEquals("value2", result?.arguments?.get("arg2"))
    }
    
    @Test
    fun `parse extracts function call from text with surrounding content`() {
        val text = """
            Here is the function call:
            {"name": "send_sms", "arguments": {"to": "1234567890", "message": "Hello"}}
            That's the call you requested.
        """.trimIndent()
        
        val result = parser.parse(text)
        
        assertNotNull(result)
        assertEquals("send_sms", result?.name)
        assertEquals("1234567890", result?.arguments?.get("to"))
        assertEquals("Hello", result?.arguments?.get("message"))
    }
    
    @Test
    fun `parse returns null for invalid JSON`() {
        val invalidJson = """{"name": "test", "arguments":"""
        
        val result = parser.parse(invalidJson)
        
        assertNull(result)
    }
    
    @Test
    fun `parse returns null when no JSON found`() {
        val text = "This is just regular text without any JSON"
        
        val result = parser.parse(text)
        
        assertNull(result)
    }
    
    @Test
    fun `parseMultiple extracts multiple function calls`() {
        val text = """
            First call: {"name": "func1", "arguments": {"a": "1"}}
            Second call: {"name": "func2", "arguments": {"b": "2"}}
        """.trimIndent()
        
        val results = parser.parseMultiple(text)
        
        assertEquals(2, results.size)
        assertEquals("func1", results[0].name)
        assertEquals("func2", results[1].name)
    }
    
    @Test
    fun `validate succeeds for valid function call`() {
        val functionCall = parser.parse("""{"name": "test_tool", "arguments": {"param1": "value1"}}""")!!
        val toolDefinition = ToolDefinition(
            name = "test_tool",
            description = "Test tool",
            parameters = mapOf(
                "param1" to ParameterSpec(type = "string", required = true)
            ),
            executionType = ExecutionType.INTENT_LAUNCH
        )
        
        val result = parser.validate(functionCall, toolDefinition)
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `validate fails when function name does not match`() {
        val functionCall = parser.parse("""{"name": "wrong_tool", "arguments": {"param1": "value1"}}""")!!
        val toolDefinition = ToolDefinition(
            name = "test_tool",
            description = "Test tool",
            parameters = mapOf(
                "param1" to ParameterSpec(type = "string", required = true)
            ),
            executionType = ExecutionType.INTENT_LAUNCH
        )
        
        val result = parser.validate(functionCall, toolDefinition)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("does not match") == true)
    }
    
    @Test
    fun `validate fails when required parameter is missing`() {
        val functionCall = parser.parse("""{"name": "test_tool", "arguments": {}}""")!!
        val toolDefinition = ToolDefinition(
            name = "test_tool",
            description = "Test tool",
            parameters = mapOf(
                "required_param" to ParameterSpec(type = "string", required = true)
            ),
            executionType = ExecutionType.INTENT_LAUNCH
        )
        
        val result = parser.validate(functionCall, toolDefinition)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Missing required parameters") == true)
    }
    
    @Test
    fun `validate succeeds when optional parameter is missing`() {
        val functionCall = parser.parse("""{"name": "test_tool", "arguments": {"required_param": "value"}}""")!!
        val toolDefinition = ToolDefinition(
            name = "test_tool",
            description = "Test tool",
            parameters = mapOf(
                "required_param" to ParameterSpec(type = "string", required = true),
                "optional_param" to ParameterSpec(type = "string", required = false)
            ),
            executionType = ExecutionType.INTENT_LAUNCH
        )
        
        val result = parser.validate(functionCall, toolDefinition)
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `validate fails for invalid integer type`() {
        val functionCall = parser.parse("""{"name": "test_tool", "arguments": {"age": "not_a_number"}}""")!!
        val toolDefinition = ToolDefinition(
            name = "test_tool",
            description = "Test tool",
            parameters = mapOf(
                "age" to ParameterSpec(type = "integer", required = true)
            ),
            executionType = ExecutionType.INTENT_LAUNCH
        )
        
        val result = parser.validate(functionCall, toolDefinition)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("invalid type") == true)
    }
    
    @Test
    fun `validate succeeds for valid integer type`() {
        val functionCall = parser.parse("""{"name": "test_tool", "arguments": {"age": "25"}}""")!!
        val toolDefinition = ToolDefinition(
            name = "test_tool",
            description = "Test tool",
            parameters = mapOf(
                "age" to ParameterSpec(type = "integer", required = true)
            ),
            executionType = ExecutionType.INTENT_LAUNCH
        )
        
        val result = parser.validate(functionCall, toolDefinition)
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `validate fails when enum value is not in allowed list`() {
        val functionCall = parser.parse("""{"name": "test_tool", "arguments": {"status": "invalid"}}""")!!
        val toolDefinition = ToolDefinition(
            name = "test_tool",
            description = "Test tool",
            parameters = mapOf(
                "status" to ParameterSpec(
                    type = "string",
                    required = true,
                    enum = listOf("pending", "completed", "failed")
                )
            ),
            executionType = ExecutionType.INTENT_LAUNCH
        )
        
        val result = parser.validate(functionCall, toolDefinition)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not in allowed values") == true)
    }
    
    @Test
    fun `validate succeeds when enum value is in allowed list`() {
        val functionCall = parser.parse("""{"name": "test_tool", "arguments": {"status": "completed"}}""")!!
        val toolDefinition = ToolDefinition(
            name = "test_tool",
            description = "Test tool",
            parameters = mapOf(
                "status" to ParameterSpec(
                    type = "string",
                    required = true,
                    enum = listOf("pending", "completed", "failed")
                )
            ),
            executionType = ExecutionType.INTENT_LAUNCH
        )
        
        val result = parser.validate(functionCall, toolDefinition)
        
        assertTrue(result.isSuccess)
    }
}
