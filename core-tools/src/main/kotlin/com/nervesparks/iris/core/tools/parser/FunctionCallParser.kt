package com.nervesparks.iris.core.tools.parser

import com.nervesparks.iris.core.tools.models.FunctionCall
import com.nervesparks.iris.core.tools.models.ToolDefinition

/**
 * Parser for extracting function calls from LLM responses
 */
interface FunctionCallParser {
    /**
     * Parse function call JSON from LLM response
     * 
     * @param response Raw LLM response text
     * @return Parsed function call or null if no valid function call found
     */
    fun parse(response: String): FunctionCall?
    
    /**
     * Validate function call against tool definition
     * 
     * @param functionCall Parsed function call
     * @param toolDefinition Tool definition to validate against
     * @return Result with validated function call or error message
     */
    fun validate(
        functionCall: FunctionCall,
        toolDefinition: ToolDefinition
    ): Result<FunctionCall>
    
    /**
     * Extract all function calls from a response (supports multiple calls)
     * 
     * @param response Raw LLM response text
     * @return List of parsed function calls
     */
    fun parseMultiple(response: String): List<FunctionCall>
}
