# MVP Slice 1: Architecture Diagram

## Component Relationships

```
┌─────────────────────────────────────────────────────────────┐
│                      MainChatScreen                          │
│  (Existing - Will integrate new components in future)        │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ uses
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      ChatMessageList                         │
│            (Existing - in ChatSection.kt)                    │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  LazyColumn with message rendering                   │   │
│  │  • Currently uses Map<String, String> format         │   │
│  │  • Can migrate to Message data class using helper    │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │                   │
                    ▼                   ▼
         ┌──────────────────┐  ┌──────────────────┐
         │  MessageBubble   │  │ ProcessingIndicator│
         │      [NEW]       │  │      [NEW]       │
         └──────────────────┘  └──────────────────┘
                    │
                    │ displays
                    ▼
         ┌──────────────────┐
         │    Message       │
         │  (data class)    │
         │     [NEW]        │
         │ ┌──────────────┐ │
         │ │ MessageRole  │ │
         │ │    (enum)    │ │
         │ │    [NEW]     │ │
         │ └──────────────┘ │
         └──────────────────┘
```

## Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                     User Input                               │
│          (Existing MainViewModel logic)                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              Map<String, String> Format                      │
│         (Current: {"role": "user", "content": "..."})        │
│                                                               │
│         ┌─────────────────────────────────┐                 │
│         │  toMessage() Helper Function    │                 │
│         │         [NEW]                    │                 │
│         └─────────────────────────────────┘                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Message Data Class                        │
│  Message(                                                     │
│    id: String,                                               │
│    content: String,                                          │
│    role: MessageRole,                                        │
│    timestamp: Instant,                                       │
│    processingTimeMs: Long?,                                  │
│    tokenCount: Int?                                          │
│  )                                                           │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    MessageBubble UI                          │
│  • Role-based styling                                        │
│  • Copy to clipboard                                         │
│  • Timestamp display                                         │
│  • Processing metrics                                        │
└─────────────────────────────────────────────────────────────┘
```

## Test Coverage Map

```
┌────────────────────────────────────────────────────────────┐
│                    Message.kt                              │
│                  (Data Layer)                              │
│                                                            │
│  ┌──────────────────────────────────────────────────┐    │
│  │           MessageTest.kt                         │    │
│  │        (13 Unit Tests)                           │    │
│  │  ✓ Creation with defaults                        │    │
│  │  ✓ Creation with all values                      │    │
│  │  ✓ isFromUser property                           │    │
│  │  ✓ Equality & hashCode                           │    │
│  │  ✓ Copy functionality                            │    │
│  │  ✓ Role enum values                              │    │
│  │  ✓ Processing metrics                            │    │
│  │  ✓ Edge cases (empty, long, special chars)      │    │
│  └──────────────────────────────────────────────────┘    │
└────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────┐
│              MessageBubble.kt & ProcessingIndicator.kt     │
│                     (UI Layer)                             │
│                                                            │
│  ┌──────────────────────────────────────────────────┐    │
│  │      MessageBubbleTest.kt                        │    │
│  │        (11 UI Tests)                             │    │
│  │  ✓ User message display                          │    │
│  │  ✓ Assistant message display                     │    │
│  │  ✓ System message display                        │    │
│  │  ✓ Timestamp visibility                          │    │
│  │  ✓ Processing metrics                            │    │
│  │  ✓ Long-click handling                           │    │
│  │  ✓ Icon placement                                │    │
│  │  ✓ Edge cases                                    │    │
│  └──────────────────────────────────────────────────┘    │
│                                                            │
│  ┌──────────────────────────────────────────────────┐    │
│  │    ProcessingIndicatorTest.kt                    │    │
│  │         (4 UI Tests)                             │    │
│  │  ✓ Icon display                                  │    │
│  │  ✓ Text visibility                               │    │
│  │  ✓ Component rendering                           │    │
│  └──────────────────────────────────────────────────┘    │
└────────────────────────────────────────────────────────────┘
```

## Integration Points

### Current Integration (Backward Compatible)
```kotlin
// ChatSection.kt - Existing Code
itemsIndexed(messages.drop(3)) { index, messageMap ->
    val role = messageMap["role"] ?: ""
    val content = messageMap["content"] ?: ""
    
    // Current rendering (still works)
    UserOrAssistantMessage(role, content, onLongClick)
}
```

### Future Integration (Recommended)
```kotlin
// ChatSection.kt - New Approach
itemsIndexed(messages.drop(3)) { index, messageMap ->
    messageMap.toMessage()?.let { message ->
        when (message.role) {
            MessageRole.SYSTEM -> {} // Skip or render differently
            else -> MessageBubble(
                message = message,
                showTimestamp = true,
                onLongClick = { /* handle */ }
            )
        }
    }
}
```

### Processing State Integration
```kotlin
// In MainChatScreen or ChatMessageList
LazyColumn {
    // ... existing messages ...
    
    // Show processing indicator
    if (viewModel.isProcessing) {
        item {
            ProcessingIndicator(showMetrics = true)
        }
    }
}
```

## File Dependencies

```
Message.kt
  ├── java.time.Instant
  ├── java.util.UUID
  └── (no other dependencies)

MessageBubble.kt
  ├── Message.kt (data model)
  ├── MessageRole (enum)
  ├── androidx.compose.* (UI framework)
  ├── ClipboardManager (copy functionality)
  └── R.drawable.* (icons)

ProcessingIndicator.kt
  ├── androidx.compose.* (UI framework)
  ├── androidx.compose.animation.* (animations)
  └── R.drawable.logo (icon)

ChatSection.kt (Modified)
  ├── Message.kt (import)
  ├── MessageRole (import)
  └── java.time.Instant (import)
```

## Build & Test Pipeline

```
┌────────────────────────────────────────────────────────────┐
│                    Source Files                            │
│  • Message.kt                                              │
│  • MessageBubble.kt                                        │
│  • ProcessingIndicator.kt                                 │
└────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────┐
│                  Kotlin Compilation                        │
│  kotlinc (syntax validated ✓)                             │
└────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────┐
│                   Gradle Build                             │
│  ./gradlew assembleDebug                                   │
│  (blocked by network - pending dl.google.com access)      │
└────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────┐
│                    Unit Tests                              │
│  ./gradlew testDebugUnitTest                              │
│  • MessageTest.kt (13 tests)                              │
└────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────┐
│                 Instrumented Tests                         │
│  ./gradlew connectedDebugAndroidTest                      │
│  • MessageBubbleTest.kt (11 tests)                        │
│  • ProcessingIndicatorTest.kt (4 tests)                   │
│  (requires device/emulator)                                │
└────────────────────────────────────────────────────────────┘
```

## Migration Strategy

```
Phase 1: Current (Completed ✓)
├── Create Message data class
├── Create MessageBubble component
├── Create ProcessingIndicator component
├── Add helper functions for compatibility
└── Write comprehensive tests

Phase 2: Integration (Next Steps)
├── Update MainViewModel to use Message class
├── Modify message storage from Map to Message
├── Replace UserOrAssistantMessage with MessageBubble
└── Add ProcessingIndicator to LazyColumn

Phase 3: Enhancement (Future)
├── Add message persistence with Room
├── Implement conversation management
├── Add streaming response support
└── Enhance UI with animations
```

## Summary

✅ **Data Layer**: Clean, type-safe Message data class with MessageRole enum  
✅ **UI Layer**: Reusable MessageBubble and ProcessingIndicator components  
✅ **Tests**: 28 tests (13 unit + 15 UI) with comprehensive coverage  
✅ **Documentation**: Complete specifications and usage guides  
✅ **Compatibility**: Backward compatible with existing code  
⏳ **Build**: Pending network access to Google Maven repository  

The implementation follows the MVP slice specification in `docs/pages/chat-interface.md` and adheres to the project's architectural guidelines.
