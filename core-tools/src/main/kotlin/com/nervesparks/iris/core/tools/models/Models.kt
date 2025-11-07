package com.nervesparks.iris.core.tools.models

import kotlinx.serialization.Serializable

/**
 * Type of execution for a tool
 */
enum class ExecutionType {
    /**
     * Opens an Android app with pre-filled data via Intent
     */
    INTENT_LAUNCH,
    
    /**
     * Executes direct API call with required permissions
     */
    DIRECT_API,
    
    /**
     * Executes as a background operation
     */
    BACKGROUND_TASK
}

/**
 * Specification for a tool parameter
 */
@Serializable
data class ParameterSpec(
    val type: String, // "string", "integer", "boolean", "number", "array", "object"
    val required: Boolean,
    val defaultValue: String? = null,
    val description: String? = null,
    val enum: List<String>? = null // For enumerated values
)

/**
 * Definition of a tool/function that can be called
 */
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, ParameterSpec>,
    val requiredPermissions: List<String> = emptyList(),
    val executionType: ExecutionType,
    val category: String = "general" // For organizing tools
)

/**
 * Parsed function call from LLM response
 */
@Serializable
data class FunctionCall(
    val name: String,
    val arguments: Map<String, String>
)

/**
 * Action to be confirmed by user before execution
 */
data class ToolAction(
    val toolDefinition: ToolDefinition,
    val functionCall: FunctionCall,
    val description: String // Human-readable description of what will happen
)

/**
 * Result of tool execution
 */
sealed class ExecutionResult {
    /**
     * Successful execution with result data
     */
    data class Success(
        val data: String,
        val message: String = "Action completed successfully"
    ) : ExecutionResult()
    
    /**
     * User declined the action
     */
    data class Declined(
        val reason: String = "User declined the action"
    ) : ExecutionResult()
    
    /**
     * Execution failed with error
     */
    data class Error(
        val error: String,
        val throwable: Throwable? = null
    ) : ExecutionResult()
    
    /**
     * Missing required permissions
     */
    data class PermissionDenied(
        val missingPermissions: List<String>
    ) : ExecutionResult()
}

/**
 * Audit log entry for tool execution
 */
data class ToolExecutionLog(
    val timestamp: Long = System.currentTimeMillis(),
    val toolName: String,
    val arguments: Map<String, String>,
    val result: ExecutionResult,
    val userId: String? = null
)
