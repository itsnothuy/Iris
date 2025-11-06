package com.nervesparks.iris.core.tools.parser

import com.nervesparks.iris.core.tools.models.FunctionCall
import com.nervesparks.iris.core.tools.models.ToolDefinition
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of FunctionCallParser using JSON parsing
 */
@Singleton
class FunctionCallParserImpl @Inject constructor() : FunctionCallParser {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    override fun parse(response: String): FunctionCall? {
        // Try to find JSON object in response
        val jsonMatch = extractJsonObject(response) ?: return null
        
        return try {
            json.decodeFromString<FunctionCall>(jsonMatch)
        } catch (e: SerializationException) {
            null
        }
    }
    
    override fun validate(
        functionCall: FunctionCall,
        toolDefinition: ToolDefinition
    ): Result<FunctionCall> {
        // Check if function name matches
        if (functionCall.name != toolDefinition.name) {
            return Result.failure(
                IllegalArgumentException("Function name '${functionCall.name}' does not match tool '${toolDefinition.name}'")
            )
        }
        
        // Check required parameters
        val missingParams = toolDefinition.parameters
            .filter { (_, spec) -> spec.required }
            .filter { (name, _) -> !functionCall.arguments.containsKey(name) }
            .keys
        
        if (missingParams.isNotEmpty()) {
            return Result.failure(
                IllegalArgumentException("Missing required parameters: ${missingParams.joinToString(", ")}")
            )
        }
        
        // Validate parameter types
        for ((paramName, paramValue) in functionCall.arguments) {
            val paramSpec = toolDefinition.parameters[paramName]
            if (paramSpec == null) {
                // Unknown parameter - could warn but not fail
                continue
            }
            
            // Type validation
            val validationType = validateParameterType(paramValue, paramSpec.type)
            if (!validationType) {
                return Result.failure(
                    IllegalArgumentException("Parameter '$paramName' has invalid type. Expected: ${paramSpec.type}")
                )
            }
            
            // Enum validation
            if (paramSpec.enum != null && paramValue !in paramSpec.enum) {
                return Result.failure(
                    IllegalArgumentException("Parameter '$paramName' value '$paramValue' not in allowed values: ${paramSpec.enum}")
                )
            }
        }
        
        return Result.success(functionCall)
    }
    
    override fun parseMultiple(response: String): List<FunctionCall> {
        val functionCalls = mutableListOf<FunctionCall>()
        
        // Find all JSON objects in response
        val jsonObjects = extractAllJsonObjects(response)
        
        for (jsonStr in jsonObjects) {
            try {
                val functionCall = json.decodeFromString<FunctionCall>(jsonStr)
                functionCalls.add(functionCall)
            } catch (e: SerializationException) {
                // Skip invalid JSON
                continue
            }
        }
        
        return functionCalls
    }
    
    /**
     * Extract first JSON object from text
     */
    private fun extractJsonObject(text: String): String? {
        val startIndex = text.indexOf('{')
        if (startIndex == -1) return null
        
        var braceCount = 0
        var inString = false
        var escapeNext = false
        
        for (i in startIndex until text.length) {
            val char = text[i]
            
            when {
                escapeNext -> escapeNext = false
                char == '\\' -> escapeNext = true
                char == '"' && !escapeNext -> inString = !inString
                char == '{' && !inString -> braceCount++
                char == '}' && !inString -> {
                    braceCount--
                    if (braceCount == 0) {
                        return text.substring(startIndex, i + 1)
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * Extract all JSON objects from text
     */
    private fun extractAllJsonObjects(text: String): List<String> {
        val objects = mutableListOf<String>()
        var remainingText = text
        
        while (true) {
            val obj = extractJsonObject(remainingText) ?: break
            objects.add(obj)
            
            val endIndex = remainingText.indexOf(obj) + obj.length
            if (endIndex >= remainingText.length) break
            
            remainingText = remainingText.substring(endIndex)
        }
        
        return objects
    }
    
    /**
     * Validate parameter type
     */
    private fun validateParameterType(value: String, expectedType: String): Boolean {
        return when (expectedType.lowercase()) {
            "string" -> true // Any string is valid
            "integer", "int" -> value.toIntOrNull() != null
            "number", "float", "double" -> value.toDoubleOrNull() != null
            "boolean", "bool" -> value.lowercase() in listOf("true", "false")
            "array" -> value.startsWith("[") && value.endsWith("]")
            "object" -> value.startsWith("{") && value.endsWith("}")
            else -> true // Unknown types pass validation
        }
    }
}
