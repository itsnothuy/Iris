package com.nervesparks.iris.core.tools.examples

/**
 * Example integration of ToolEngine with LLM for function calling
 * 
 * This file demonstrates how to:
 * 1. Get tool schemas for LLM prompt
 * 2. Parse function calls from LLM responses
 * 3. Execute tools with user confirmation
 * 4. Handle execution results
 */

/*
// Example 1: Getting tool schemas for LLM prompt
suspend fun getToolSchemasForPrompt(toolEngine: ToolEngine): String {
    val tools = toolEngine.getAvailableTools()
    
    return tools.joinToString("\n\n") { tool ->
        buildString {
            appendLine("Tool: ${tool.name}")
            appendLine("Description: ${tool.description}")
            appendLine("Parameters:")
            tool.parameters.forEach { (name, spec) ->
                appendLine("  - $name (${spec.type}): ${spec.description ?: ""}")
                if (spec.required) appendLine("    Required: Yes")
                if (spec.defaultValue != null) appendLine("    Default: ${spec.defaultValue}")
            }
        }
    }
}

// Example 2: Complete LLM integration with function calling
suspend fun processUserMessageWithTools(
    userMessage: String,
    llmEngine: LLMEngine,
    toolEngine: ToolEngine,
    parser: FunctionCallParser
): String {
    // 1. Build prompt with tool schemas
    val toolSchemas = getToolSchemasForPrompt(toolEngine)
    val systemPrompt = """
        You are an AI assistant with access to the following tools:
        
        $toolSchemas
        
        When you want to use a tool, respond with a JSON object:
        {"name": "tool_name", "arguments": {"param1": "value1", "param2": "value2"}}
        
        Always confirm with the user before executing sensitive actions.
    """.trimIndent()
    
    val fullPrompt = "$systemPrompt\n\nUser: $userMessage\nAssistant:"
    
    // 2. Generate LLM response
    var response = ""
    llmEngine.generateText(fullPrompt, GenerationParams()).collect { token ->
        response += token
    }
    
    // 3. Try to parse function call
    val functionCall = parser.parse(response)
    
    if (functionCall != null) {
        // 4. Execute the function
        val result = toolEngine.executeFunction(functionCall)
        
        // 5. Handle result
        return when (val outcome = result.getOrNull()) {
            is ExecutionResult.Success -> {
                "I've ${outcome.message}. ${outcome.data}"
            }
            is ExecutionResult.Declined -> {
                "I understand you declined that action. Is there anything else I can help with?"
            }
            is ExecutionResult.PermissionDenied -> {
                "I don't have permission to do that. Missing: ${outcome.missingPermissions.joinToString(", ")}"
            }
            is ExecutionResult.Error -> {
                "I encountered an error: ${outcome.error}"
            }
            null -> "Something went wrong while executing that action."
        }
    }
    
    // No function call found, return the response as-is
    return response
}

// Example 3: Multi-turn conversation with tool execution
class ToolAwareConversationManager(
    private val llmEngine: LLMEngine,
    private val toolEngine: ToolEngine,
    private val parser: FunctionCallParser
) {
    private val conversationHistory = mutableListOf<String>()
    
    suspend fun sendMessage(userMessage: String): String {
        conversationHistory.add("User: $userMessage")
        
        // Build context from history
        val context = conversationHistory.takeLast(10).joinToString("\n")
        val toolSchemas = getToolSchemasForPrompt(toolEngine)
        
        val prompt = """
            Available tools:
            $toolSchemas
            
            Conversation:
            $context
            
            Provide a helpful response. Use tools when appropriate.
        """.trimIndent()
        
        // Generate response
        var response = ""
        llmEngine.generateText(prompt, GenerationParams()).collect { token ->
            response += token
        }
        
        // Check for function call
        val functionCall = parser.parse(response)
        var finalResponse = response
        
        if (functionCall != null) {
            val result = toolEngine.executeFunction(functionCall)
            when (val outcome = result.getOrNull()) {
                is ExecutionResult.Success -> {
                    finalResponse = "Done! ${outcome.message}"
                }
                is ExecutionResult.Declined -> {
                    finalResponse = "Okay, I won't do that."
                }
                is ExecutionResult.PermissionDenied -> {
                    finalResponse = "I need permission: ${outcome.missingPermissions}"
                }
                is ExecutionResult.Error -> {
                    finalResponse = "Error: ${outcome.error}"
                }
                null -> {}
            }
        }
        
        conversationHistory.add("Assistant: $finalResponse")
        return finalResponse
    }
}

// Example 4: Batch function call processing
suspend fun processBatchFunctionCalls(
    llmResponse: String,
    toolEngine: ToolEngine,
    parser: FunctionCallParser
): List<ExecutionResult> {
    val functionCalls = parser.parseMultiple(llmResponse)
    val results = mutableListOf<ExecutionResult>()
    
    for (call in functionCalls) {
        val result = toolEngine.executeFunction(call)
        result.getOrNull()?.let { results.add(it) }
    }
    
    return results
}

// Example 5: Conditional tool execution based on context
suspend fun smartToolExecution(
    functionCall: FunctionCall,
    toolEngine: ToolEngine,
    userContext: UserContext
): ExecutionResult {
    val tool = toolEngine.getTool(functionCall.name)
        ?: return ExecutionResult.Error("Tool not found")
    
    // Check if user has auto-approve for this tool
    if (userContext.hasAutoApprove(tool.name)) {
        toolEngine.setConfirmationCallback { true }
    } else {
        // Use normal confirmation flow
        toolEngine.setConfirmationCallback { action ->
            showUserConfirmationDialog(action)
        }
    }
    
    // Check permissions first
    val missingPerms = toolEngine.checkPermissions(tool)
    if (missingPerms.isNotEmpty()) {
        return ExecutionResult.PermissionDenied(missingPerms)
    }
    
    // Execute
    val result = toolEngine.executeFunction(functionCall)
    return result.getOrNull() ?: ExecutionResult.Error("Execution failed")
}

// Example 6: Tool execution with retry logic
suspend fun executeWithRetry(
    functionCall: FunctionCall,
    toolEngine: ToolEngine,
    maxRetries: Int = 3
): ExecutionResult {
    var lastError: String? = null
    
    repeat(maxRetries) { attempt ->
        val result = toolEngine.executeFunction(functionCall)
        
        when (val outcome = result.getOrNull()) {
            is ExecutionResult.Success -> return outcome
            is ExecutionResult.Declined,
            is ExecutionResult.PermissionDenied -> return outcome
            is ExecutionResult.Error -> {
                lastError = outcome.error
                // Wait before retry
                delay(1000L * (attempt + 1))
            }
            null -> {
                lastError = "Unknown error"
            }
        }
    }
    
    return ExecutionResult.Error("Failed after $maxRetries attempts: $lastError")
}
*/
