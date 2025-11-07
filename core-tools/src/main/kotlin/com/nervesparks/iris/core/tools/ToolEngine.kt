package com.nervesparks.iris.core.tools

import com.nervesparks.iris.core.tools.models.ExecutionResult
import com.nervesparks.iris.core.tools.models.FunctionCall
import com.nervesparks.iris.core.tools.models.ToolAction
import com.nervesparks.iris.core.tools.models.ToolDefinition

/**
 * Main interface for the Tool Engine
 * Handles function calling, tool execution, and user confirmation
 */
interface ToolEngine {
    /**
     * Execute a function call
     * 
     * @param functionCall Parsed function call from LLM
     * @return Result of execution
     */
    suspend fun executeFunction(functionCall: FunctionCall): Result<ExecutionResult>
    
    /**
     * Get all available tools
     * 
     * @return List of available tool definitions
     */
    fun getAvailableTools(): List<ToolDefinition>
    
    /**
     * Request user confirmation for a tool action
     * 
     * @param action Tool action to confirm
     * @return True if user approved, false if declined
     */
    suspend fun requestUserConfirmation(action: ToolAction): Boolean
    
    /**
     * Get a specific tool definition by name
     * 
     * @param name Tool name
     * @return Tool definition or null if not found
     */
    fun getTool(name: String): ToolDefinition?
    
    /**
     * Check if all required permissions are granted for a tool
     * 
     * @param toolDefinition Tool to check
     * @return List of missing permissions (empty if all granted)
     */
    suspend fun checkPermissions(toolDefinition: ToolDefinition): List<String>
}
