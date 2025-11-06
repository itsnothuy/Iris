package com.nervesparks.iris.core.tools.registry

import com.nervesparks.iris.core.tools.models.ExecutionType
import com.nervesparks.iris.core.tools.models.ParameterSpec
import com.nervesparks.iris.core.tools.models.ToolDefinition
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ToolRegistry with predefined Android tools
 */
@Singleton
class ToolRegistryImpl @Inject constructor() : ToolRegistry {
    
    private val tools = mutableMapOf<String, ToolDefinition>()
    
    init {
        // Register standard Android tools
        registerStandardTools()
    }
    
    override fun getAllTools(): List<ToolDefinition> = tools.values.toList()
    
    override fun getTool(name: String): ToolDefinition? = tools[name]
    
    override fun registerTool(tool: ToolDefinition) {
        tools[tool.name] = tool
    }
    
    override fun unregisterTool(name: String): Boolean {
        return tools.remove(name) != null
    }
    
    override fun getToolsByCategory(category: String): List<ToolDefinition> {
        return tools.values.filter { it.category == category }
    }
    
    override fun hasTool(name: String): Boolean = tools.containsKey(name)
    
    /**
     * Register standard Android tools
     */
    private fun registerStandardTools() {
        // Calendar Event Creation
        registerTool(
            ToolDefinition(
                name = "create_calendar_event",
                description = "Create a new calendar event with specified details",
                parameters = mapOf(
                    "title" to ParameterSpec(
                        type = "string",
                        required = true,
                        description = "Event title"
                    ),
                    "datetime" to ParameterSpec(
                        type = "string",
                        required = true,
                        description = "Event start date and time in ISO format (YYYY-MM-DDTHH:MM:SS)"
                    ),
                    "duration_mins" to ParameterSpec(
                        type = "integer",
                        required = false,
                        defaultValue = "60",
                        description = "Event duration in minutes"
                    ),
                    "location" to ParameterSpec(
                        type = "string",
                        required = false,
                        description = "Event location"
                    ),
                    "description" to ParameterSpec(
                        type = "string",
                        required = false,
                        description = "Event description"
                    )
                ),
                requiredPermissions = listOf("android.permission.WRITE_CALENDAR"),
                executionType = ExecutionType.INTENT_LAUNCH,
                category = "calendar"
            )
        )
        
        // SMS Sending
        registerTool(
            ToolDefinition(
                name = "send_sms",
                description = "Send an SMS message to a phone number",
                parameters = mapOf(
                    "to" to ParameterSpec(
                        type = "string",
                        required = true,
                        description = "Phone number to send SMS to"
                    ),
                    "message" to ParameterSpec(
                        type = "string",
                        required = true,
                        description = "Message content"
                    )
                ),
                requiredPermissions = listOf("android.permission.SEND_SMS"),
                executionType = ExecutionType.INTENT_LAUNCH,
                category = "messaging"
            )
        )
        
        // Alarm Setting
        registerTool(
            ToolDefinition(
                name = "set_alarm",
                description = "Set an alarm for a specific time",
                parameters = mapOf(
                    "hour" to ParameterSpec(
                        type = "integer",
                        required = true,
                        description = "Hour in 24-hour format (0-23)"
                    ),
                    "minute" to ParameterSpec(
                        type = "integer",
                        required = true,
                        description = "Minute (0-59)"
                    ),
                    "message" to ParameterSpec(
                        type = "string",
                        required = false,
                        description = "Alarm message/label"
                    ),
                    "days" to ParameterSpec(
                        type = "string",
                        required = false,
                        description = "Comma-separated list of days (MON,TUE,WED,THU,FRI,SAT,SUN)"
                    )
                ),
                requiredPermissions = listOf("com.android.alarm.permission.SET_ALARM"),
                executionType = ExecutionType.INTENT_LAUNCH,
                category = "time"
            )
        )
        
        // Contact Search
        registerTool(
            ToolDefinition(
                name = "search_contacts",
                description = "Search for contacts by name",
                parameters = mapOf(
                    "query" to ParameterSpec(
                        type = "string",
                        required = true,
                        description = "Contact name to search for"
                    )
                ),
                requiredPermissions = listOf("android.permission.READ_CONTACTS"),
                executionType = ExecutionType.DIRECT_API,
                category = "contacts"
            )
        )
        
        // Timer Setting
        registerTool(
            ToolDefinition(
                name = "set_timer",
                description = "Set a countdown timer",
                parameters = mapOf(
                    "seconds" to ParameterSpec(
                        type = "integer",
                        required = true,
                        description = "Timer duration in seconds"
                    ),
                    "message" to ParameterSpec(
                        type = "string",
                        required = false,
                        description = "Timer message/label"
                    )
                ),
                requiredPermissions = emptyList(),
                executionType = ExecutionType.INTENT_LAUNCH,
                category = "time"
            )
        )
        
        // Web Search
        registerTool(
            ToolDefinition(
                name = "web_search",
                description = "Open web browser to search for a query",
                parameters = mapOf(
                    "query" to ParameterSpec(
                        type = "string",
                        required = true,
                        description = "Search query"
                    )
                ),
                requiredPermissions = emptyList(),
                executionType = ExecutionType.INTENT_LAUNCH,
                category = "web"
            )
        )
    }
}
