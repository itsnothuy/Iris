package com.nervesparks.iris.core.tools.executor

import android.content.Context
import com.nervesparks.iris.core.tools.models.ExecutionResult
import com.nervesparks.iris.core.tools.models.FunctionCall
import com.nervesparks.iris.core.tools.models.ToolDefinition

/**
 * Interface for executing tools of different types
 */
interface ToolExecutor {
    /**
     * Execute a tool with given parameters
     * 
     * @param context Android context
     * @param toolDefinition Tool definition
     * @param functionCall Parsed and validated function call
     * @return Result of execution
     */
    suspend fun execute(
        context: Context,
        toolDefinition: ToolDefinition,
        functionCall: FunctionCall
    ): ExecutionResult
    
    /**
     * Check if executor can handle this tool
     */
    fun canExecute(toolDefinition: ToolDefinition): Boolean
}
