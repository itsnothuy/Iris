package com.nervesparks.iris.core.tools

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.nervesparks.iris.core.tools.executor.DirectApiExecutor
import com.nervesparks.iris.core.tools.executor.IntentLaunchExecutor
import com.nervesparks.iris.core.tools.executor.ToolExecutor
import com.nervesparks.iris.core.tools.models.ExecutionResult
import com.nervesparks.iris.core.tools.models.FunctionCall
import com.nervesparks.iris.core.tools.models.ToolAction
import com.nervesparks.iris.core.tools.models.ToolDefinition
import com.nervesparks.iris.core.tools.models.ToolExecutionLog
import com.nervesparks.iris.core.tools.parser.FunctionCallParser
import com.nervesparks.iris.core.tools.registry.ToolRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ToolEngine with dependency injection support
 */
@Singleton
class ToolEngineImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val toolRegistry: ToolRegistry,
    private val functionCallParser: FunctionCallParser,
    private val intentLaunchExecutor: IntentLaunchExecutor,
    private val directApiExecutor: DirectApiExecutor
) : ToolEngine {
    
    private val executors: List<ToolExecutor> = listOf(
        intentLaunchExecutor,
        directApiExecutor
    )
    
    private val executionLogs = mutableListOf<ToolExecutionLog>()
    
    // Callback for user confirmation - can be set by UI layer
    private var confirmationCallback: (suspend (ToolAction) -> Boolean)? = null
    
    /**
     * Set callback for user confirmation
     */
    fun setConfirmationCallback(callback: suspend (ToolAction) -> Boolean) {
        confirmationCallback = callback
    }
    
    override suspend fun executeFunction(functionCall: FunctionCall): Result<ExecutionResult> {
        return withContext(Dispatchers.IO) {
            try {
                // Get tool definition
                val toolDefinition = toolRegistry.getTool(functionCall.name)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Tool '${functionCall.name}' not found")
                    )
                
                // Validate function call
                val validationResult = functionCallParser.validate(functionCall, toolDefinition)
                if (validationResult.isFailure) {
                    return@withContext Result.failure(
                        validationResult.exceptionOrNull() ?: Exception("Validation failed")
                    )
                }
                
                // Check permissions
                val missingPermissions = checkPermissions(toolDefinition)
                if (missingPermissions.isNotEmpty()) {
                    val result = ExecutionResult.PermissionDenied(missingPermissions)
                    logExecution(functionCall.name, functionCall.arguments, result)
                    return@withContext Result.success(result)
                }
                
                // Request user confirmation
                val toolAction = ToolAction(
                    toolDefinition = toolDefinition,
                    functionCall = functionCall,
                    description = buildActionDescription(toolDefinition, functionCall)
                )
                
                val confirmed = requestUserConfirmation(toolAction)
                if (!confirmed) {
                    val result = ExecutionResult.Declined()
                    logExecution(functionCall.name, functionCall.arguments, result)
                    return@withContext Result.success(result)
                }
                
                // Find appropriate executor
                val executor = executors.find { it.canExecute(toolDefinition) }
                    ?: return@withContext Result.failure(
                        IllegalStateException("No executor found for tool '${functionCall.name}'")
                    )
                
                // Execute tool
                val result = executor.execute(context, toolDefinition, functionCall)
                logExecution(functionCall.name, functionCall.arguments, result)
                
                Result.success(result)
            } catch (e: Exception) {
                val result = ExecutionResult.Error("Execution failed: ${e.message}", e)
                Result.success(result)
            }
        }
    }
    
    override fun getAvailableTools(): List<ToolDefinition> {
        return toolRegistry.getAllTools()
    }
    
    override suspend fun requestUserConfirmation(action: ToolAction): Boolean {
        // If no callback is set, default to auto-approve (for testing)
        // In production, this should require explicit confirmation
        return confirmationCallback?.invoke(action) ?: false
    }
    
    override fun getTool(name: String): ToolDefinition? {
        return toolRegistry.getTool(name)
    }
    
    override suspend fun checkPermissions(toolDefinition: ToolDefinition): List<String> {
        return toolDefinition.requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Build human-readable description of action
     */
    private fun buildActionDescription(
        toolDefinition: ToolDefinition,
        functionCall: FunctionCall
    ): String {
        return when (toolDefinition.name) {
            "create_calendar_event" -> {
                val title = functionCall.arguments["title"] ?: "Untitled"
                val datetime = functionCall.arguments["datetime"] ?: "unspecified time"
                "Create calendar event '$title' at $datetime"
            }
            "send_sms" -> {
                val to = functionCall.arguments["to"] ?: "unknown"
                val message = functionCall.arguments["message"] ?: ""
                "Send SMS to $to: '$message'"
            }
            "set_alarm" -> {
                val hour = functionCall.arguments["hour"] ?: "00"
                val minute = functionCall.arguments["minute"] ?: "00"
                "Set alarm for $hour:$minute"
            }
            "set_timer" -> {
                val seconds = functionCall.arguments["seconds"] ?: "0"
                "Set timer for $seconds seconds"
            }
            "search_contacts" -> {
                val query = functionCall.arguments["query"] ?: ""
                "Search contacts for '$query'"
            }
            "web_search" -> {
                val query = functionCall.arguments["query"] ?: ""
                "Search web for '$query'"
            }
            else -> "Execute ${toolDefinition.name}"
        }
    }
    
    /**
     * Log tool execution for audit
     */
    private fun logExecution(
        toolName: String,
        arguments: Map<String, String>,
        result: ExecutionResult
    ) {
        val log = ToolExecutionLog(
            toolName = toolName,
            arguments = arguments,
            result = result
        )
        executionLogs.add(log)
        
        // Keep only last 100 logs to prevent memory issues
        if (executionLogs.size > 100) {
            executionLogs.removeAt(0)
        }
    }
    
    /**
     * Get execution logs (for debugging/audit)
     */
    fun getExecutionLogs(): List<ToolExecutionLog> = executionLogs.toList()
}
