# core-tools Module

Function calling and tool engine implementation for Iris Android AI Assistant.

## Overview

The `core-tools` module provides a comprehensive framework for executing Android system functions through structured function calls. It enables the AI assistant to interact with device capabilities like calendar, SMS, alarms, contacts, and more through a secure, permission-based execution model.

## Architecture

### Key Components

1. **ToolEngine** - Main interface for tool execution with user confirmation
2. **ToolRegistry** - Registry of available tools with their definitions
3. **FunctionCallParser** - JSON parser for extracting function calls from LLM responses
4. **ToolExecutors** - Execution layer for different tool types (intents, direct API)

### Data Models

- `ToolDefinition` - Defines a tool with parameters, permissions, and execution type
- `FunctionCall` - Parsed function call with name and arguments
- `ExecutionResult` - Result of tool execution (Success, Error, Declined, PermissionDenied)
- `ToolAction` - Action requiring user confirmation
- `ParameterSpec` - Parameter specification with type, validation, and defaults

## Standard Tools

### 1. create_calendar_event
Creates a calendar event with title, datetime, duration, location, and description.

```json
{
  "name": "create_calendar_event",
  "arguments": {
    "title": "Team Meeting",
    "datetime": "2025-11-15T14:00:00",
    "duration_mins": "60",
    "location": "Conference Room A"
  }
}
```

### 2. send_sms
Sends an SMS message to a phone number.

```json
{
  "name": "send_sms",
  "arguments": {
    "to": "1234567890",
    "message": "Hello from Iris!"
  }
}
```

### 3. set_alarm
Sets an alarm for a specific time.

```json
{
  "name": "set_alarm",
  "arguments": {
    "hour": "7",
    "minute": "30",
    "message": "Wake up!"
  }
}
```

### 4. set_timer
Sets a countdown timer.

```json
{
  "name": "set_timer",
  "arguments": {
    "seconds": "300",
    "message": "Pizza ready"
  }
}
```

### 5. search_contacts
Searches contacts by name.

```json
{
  "name": "search_contacts",
  "arguments": {
    "query": "John"
  }
}
```

### 6. web_search
Opens web browser to search for a query.

```json
{
  "name": "web_search",
  "arguments": {
    "query": "kotlin coroutines tutorial"
  }
}
```

## Usage

### Basic Usage

```kotlin
@Inject
lateinit var toolEngine: ToolEngine

// Get available tools
val tools = toolEngine.getAvailableTools()

// Parse function call from LLM response
val response = """{"name": "send_sms", "arguments": {"to": "1234567890", "message": "Hello"}}"""
val parser = FunctionCallParserImpl()
val functionCall = parser.parse(response)

// Set confirmation callback
toolEngine.setConfirmationCallback { action ->
    // Show UI dialog and return user's decision
    showConfirmationDialog(action)
}

// Execute function
val result = toolEngine.executeFunction(functionCall!!)
when (val outcome = result.getOrNull()) {
    is ExecutionResult.Success -> println("Success: ${outcome.message}")
    is ExecutionResult.Error -> println("Error: ${outcome.error}")
    is ExecutionResult.Declined -> println("User declined")
    is ExecutionResult.PermissionDenied -> println("Missing: ${outcome.missingPermissions}")
}
```

### Adding Custom Tools

```kotlin
@Inject
lateinit var toolRegistry: ToolRegistry

val customTool = ToolDefinition(
    name = "my_custom_tool",
    description = "My custom functionality",
    parameters = mapOf(
        "param1" to ParameterSpec(
            type = "string",
            required = true,
            description = "First parameter"
        )
    ),
    requiredPermissions = listOf("android.permission.MY_PERMISSION"),
    executionType = ExecutionType.DIRECT_API,
    category = "custom"
)

toolRegistry.registerTool(customTool)
```

## Security Features

### 1. Permission Checking
- Automatically checks required Android permissions before execution
- Returns `PermissionDenied` result if permissions are missing

### 2. User Confirmation
- All tool executions require explicit user confirmation
- Confirmation callback must be set by UI layer
- Human-readable action descriptions for user review

### 3. Audit Logging
- All tool executions are logged with timestamp, arguments, and results
- Access logs via `toolEngine.getExecutionLogs()`
- Automatically limited to last 100 entries

### 4. Validation
- Parameter type validation (string, integer, number, boolean)
- Required parameter checking
- Enum value validation
- Schema validation against tool definitions

## Testing

The module includes comprehensive unit tests:

- **FunctionCallParserImplTest** - 19 tests for JSON parsing and validation
- **ToolRegistryImplTest** - 10 tests for tool registry operations
- **ToolEngineImplTest** - 8 tests for execution workflow

Run tests:
```bash
./gradlew :core-tools:test
```

## Dependencies

- `common` - Shared models and utilities
- `kotlinx.serialization` - JSON parsing
- `hilt` - Dependency injection
- `coroutines` - Async operations

## Integration

### In Application Module

```kotlin
// Add dependency in app/build.gradle.kts
dependencies {
    implementation(project(":core-tools"))
}

// Inject in your ViewModel or Activity
@Inject
lateinit var toolEngine: ToolEngine

// Set up confirmation UI
toolEngine.setConfirmationCallback { action ->
    // Show dialog and wait for user response
    showToolConfirmationDialog(action)
}
```

### With LLM Engine

```kotlin
// Generate text with tool schema
val toolSchemas = toolEngine.getAvailableTools()
    .map { tool -> tool.toJsonSchema() }

val prompt = """
Available tools: ${toolSchemas.joinToString("\n")}

User: Set an alarm for 7am tomorrow
"""

// Parse LLM response for function calls
val llmResponse = llmEngine.generateText(prompt)
val functionCall = parser.parse(llmResponse)

// Execute if function call found
if (functionCall != null) {
    toolEngine.executeFunction(functionCall)
}
```

## Privacy & Safety

- ✅ **No telemetry** - Zero data collection or external transmission
- ✅ **Local execution** - All processing happens on-device
- ✅ **Permission-based** - Respects Android permission model
- ✅ **User consent** - Explicit confirmation required for all actions
- ✅ **Audit trail** - Complete execution logging for transparency

## Future Enhancements

- Background task executor for long-running operations
- Dynamic tool loading from configuration
- Tool execution chaining (multi-step workflows)
- Custom intent filtering and resolution
- Rate limiting and throttling
- Tool execution queuing and scheduling
