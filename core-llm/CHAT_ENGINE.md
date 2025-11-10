# Chat Engine & Inference Pipeline

This document provides an overview of the Chat Engine and Inference Pipeline implementation for iris_android.

## Overview

The chat engine provides high-level abstractions for conversational AI interactions, built on top of the core LLM engine. It consists of two main components:

1. **InferenceSession**: Manages model loading, inference session lifecycle, and streaming token generation
2. **ConversationManager**: Coordinates conversations, message history, and inference operations

## Architecture

```
┌─────────────────────────────────────┐
│      ConversationManager            │
│  - Conversation lifecycle           │
│  - Message management               │
│  - Metrics tracking                 │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│        InferenceSession             │
│  - Model loading                    │
│  - Session management               │
│  - Streaming inference              │
│  - Adaptive performance             │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│         LLMEngine                   │
│  - Low-level model operations       │
│  - Native library interface         │
└─────────────────────────────────────┘
```

## Components

### InferenceSession

The `InferenceSession` interface provides methods for:

- **Model Loading**: Load models with device-adaptive parameters
- **Session Creation**: Create isolated inference sessions for conversations
- **Streaming Generation**: Generate responses token-by-token with safety checks
- **Session Management**: Track and manage multiple active sessions
- **Thermal Monitoring**: Background monitoring with automatic throttling

#### Key Features

- **Adaptive Performance**: Automatically adjusts parameters based on device class and thermal state
- **Safety Integration**: Input/output filtering via SafetyEngine
- **Context Management**: Sliding window algorithm for managing long conversations
- **Thermal Protection**: Monitors device temperature and throttles when needed

#### Usage Example

```kotlin
// Inject InferenceSession
@Inject lateinit var inferenceSession: InferenceSession

// Load a model
val modelDescriptor = ModelDescriptor(
    id = "llama-3b",
    path = "/path/to/model.gguf",
    name = "Llama 3B",
    deviceRequirements = DeviceRequirements()
)

val loadResult = inferenceSession.loadModel(
    modelDescriptor,
    InferenceParameters(contextSize = 2048)
)

if (loadResult.isSuccess) {
    // Create a session
    val sessionResult = inferenceSession.createSession("conversation-123")
    
    if (sessionResult.isSuccess) {
        val sessionId = sessionResult.getOrNull()!!.sessionId
        
        // Generate response
        inferenceSession.generateResponse(
            sessionId,
            "Hello, how are you?",
            GenerationParameters(temperature = 0.7f)
        ).collect { result ->
            when (result) {
                is InferenceResult.TokenGenerated -> {
                    // Handle streaming token
                    println(result.token)
                }
                is InferenceResult.GenerationCompleted -> {
                    // Handle completion
                    println("Done: ${result.fullText}")
                }
                is InferenceResult.Error -> {
                    // Handle error
                    println("Error: ${result.error}")
                }
                // ... other cases
            }
        }
    }
}
```

### ConversationManager

The `ConversationManager` interface provides methods for:

- **Conversation Lifecycle**: Create, retrieve, clear, and delete conversations
- **Message Management**: Track user and assistant messages with metadata
- **Streaming Responses**: Integrate with InferenceSession for real-time generation
- **Metrics**: Track token counts, processing times, and conversation statistics

#### Key Features

- **In-Memory State**: Current implementation uses in-memory storage (can be extended for persistence)
- **Automatic Session Management**: Creates and reuses inference sessions per conversation
- **Message Tracking**: Records user and assistant messages with timestamps and token counts
- **Memory Management**: Automatic trimming of old messages when limits are reached

#### Usage Example

```kotlin
// Inject ConversationManager
@Inject lateinit var conversationManager: ConversationManager

// Create a new conversation
val conversationResult = conversationManager.createConversation("My First Chat")
val conversationId = conversationResult.getOrNull()!!

// Send a message and get streaming response
conversationManager.sendMessage(
    conversationId,
    "Tell me about Kotlin coroutines",
    GenerationParameters()
).collect { result ->
    when (result) {
        is InferenceResult.TokenGenerated -> {
            // Update UI with streaming token
            updateChatUI(result.token)
        }
        is InferenceResult.GenerationCompleted -> {
            // Message complete
            println("Response: ${result.fullText}")
            println("Tokens: ${result.tokenCount}")
            println("Speed: ${result.tokensPerSecond} tokens/sec")
        }
        // ... handle other cases
    }
}

// Retrieve conversation messages
conversationManager.getMessages(conversationId).collect { messages ->
    messages.forEach { message ->
        println("${message.role}: ${message.content}")
    }
}

// Get all conversations
conversationManager.getAllConversations().collect { conversations ->
    conversations.forEach { metadata ->
        println("${metadata.title} - ${metadata.messageCount} messages")
    }
}
```

## Data Models

### InferenceResult (Sealed Class)

Represents the result of an inference operation:

- `GenerationStarted`: Generation has begun
- `TokenGenerated`: A new token has been generated (streaming)
- `GenerationCompleted`: Generation finished successfully
- `SafetyViolation`: Safety filter blocked the content
- `Error`: An error occurred during generation

### Message

Represents a single message in a conversation:

```kotlin
data class Message(
    val id: String,
    val content: String,
    val role: MessageRole,  // USER, ASSISTANT, SYSTEM
    val timestamp: Long,
    val tokenCount: Int?,
    val processingTimeMs: Long?
)
```

### ConversationMetadata

Metadata about a conversation:

```kotlin
data class ConversationMetadata(
    val id: String,
    val title: String,
    val createdAt: Long,
    val lastModified: Long,
    val messageCount: Int,
    val totalTokens: Int
)
```

## Performance Characteristics

### Device Adaptation

The InferenceSession automatically adapts parameters based on device class:

| Device Class | Context Size | Batch Size | Performance Mode |
|--------------|--------------|------------|------------------|
| Budget       | 1024         | 1          | Battery Saver    |
| Mid-Range    | 2048         | 2          | Balanced         |
| High-End     | 4096         | 4          | Performance      |
| Flagship     | Full         | 8          | Maximum          |

### Thermal Management

When thermal state changes, generation parameters are automatically adjusted:

| Thermal State | Max Tokens | Temperature Adjustment | Action        |
|---------------|------------|------------------------|---------------|
| Normal/Light  | Full       | None                   | None          |
| Moderate      | 512        | -0.1                   | Reduce params |
| Severe        | 256        | -0.2                   | Throttle      |
| Critical      | 128        | Set to 0.1             | Pause & cool  |

## Testing

Comprehensive unit tests are provided:

- **InferenceSessionImplTest**: 10 tests covering model loading, session management, and lifecycle
- **ConversationManagerImplTest**: 11 tests covering conversation operations and message handling

Run tests with:

```bash
./gradlew :core-llm:testDebugUnitTest
```

## Future Enhancements

### Planned Features

1. **Persistent Storage**: Integrate ConversationManager with Room database for persistence
2. **Conversation Search**: Full-text search across conversation history
3. **Export/Import**: Export conversations to JSON or markdown
4. **Model Hot-Swapping**: Switch models mid-conversation without losing context
5. **Multi-Model Support**: Use different models for different conversations
6. **Conversation Branching**: Support branching conversations at any message
7. **Token Budgeting**: Set per-conversation token limits

### Integration Points

- **App Layer**: ConversationRepository for database persistence
- **UI Layer**: ViewModel integration for reactive UI updates
- **Tools**: Integration with ToolEngine for function calling
- **RAG**: Integration with RAGEngine for knowledge-augmented responses

## Dependencies

- `core-llm`: LLMEngine for low-level model operations
- `core-hw`: Device profiling and thermal management
- `core-safety`: Content safety filtering
- `common`: Shared models and utilities

## See Also

- [Architecture Document](../../docs/architecture.md)
- [Core LLM Module](../README.md)
- Issue #05: Chat Engine & Inference Pipeline
