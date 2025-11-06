package com.nervesparks.iris.core.tools.registry

import com.nervesparks.iris.core.tools.models.ToolDefinition

/**
 * Registry for managing available tools
 */
interface ToolRegistry {
    /**
     * Get all available tools
     */
    fun getAllTools(): List<ToolDefinition>
    
    /**
     * Get a specific tool by name
     */
    fun getTool(name: String): ToolDefinition?
    
    /**
     * Register a new tool
     */
    fun registerTool(tool: ToolDefinition)
    
    /**
     * Unregister a tool
     */
    fun unregisterTool(name: String): Boolean
    
    /**
     * Get tools by category
     */
    fun getToolsByCategory(category: String): List<ToolDefinition>
    
    /**
     * Check if a tool exists
     */
    fun hasTool(name: String): Boolean
}
