# Chat Interface Specification

## Business Goals

Create an intuitive, responsive chat interface that feels natural for AI conversation while providing clear feedback about system state and model performance. The interface should accommodate both novice users seeking simple chat and power users wanting detailed control.

**Primary Objectives**:
- Enable natural conversation flow with minimal friction
- Provide clear feedback on AI processing state and performance
- Support conversation history and context management
- Maintain responsive UI even during intensive AI processing

## User Stories & Acceptance Tests

### Epic: Basic Conversation Flow

**US-001: Send and receive messages**
- *As a user, I want to type a message and receive an AI response so that I can have a conversation*
- **AC1**: User can type message in input field and send via button or Enter key
- **AC2**: Message appears immediately in conversation with timestamp
- **AC3**: AI processing indicator shows while response is being generated
- **AC4**: AI response appears in conversation thread with clear visual distinction
- **AC5**: Conversation scrolls automatically to show latest message

**US-002: Conversation History**
- *As a user, I want to see my previous messages so that I can maintain context*
- **AC1**: Previous messages persist between app sessions
- **AC2**: Conversation history scrollable with smooth performance
- **AC3**: Message timestamps shown for reference
- **AC4**: Clear visual distinction between user and AI messages

**US-003: Conversation Management**
- *As a user, I want to start new conversations and manage existing ones*
- **AC1**: "New Chat" button clears current conversation
- **AC2**: Confirmation dialog for clearing when messages exist
- **AC3**: Conversation state preserved during navigation to other screens

### Epic: Input and Interaction

**US-004: Rich Text Input**
- *As a user, I want flexible input options for different types of queries*
- **AC1**: Multi-line text input with auto-expanding height
- **AC2**: Voice input button triggers speech-to-text
- **AC3**: Paste button for clipboard content
- **AC4**: Input field maintains focus and cursor position appropriately

**US-005: Message Actions**
- *As a user, I want to interact with messages for better conversation control*
- **AC1**: Long-press message to copy text to clipboard
- **AC2**: Copy button available for AI responses
- **AC3**: Regenerate response option for AI messages
- **AC4**: Toast feedback for successful actions

### Epic: System Feedback

**US-006: Processing Status**
- *As a user, I want clear feedback about what the AI is doing*
- **AC1**: Loading indicator shows during model inference
- **AC2**: Processing time displayed after response completion
- **AC3**: Token generation rate shown for power users
- **AC4**: Error messages provide actionable guidance

**US-007: Model Information**
- *As a user, I want to know which model is responding to my queries*
- **AC1**: Current model name visible in interface
- **AC2**: Model performance metrics available on request
- **AC3**: Quick model switching option accessible

## UI States & Navigation

### Primary States

```mermaid
stateDiagram-v2
    [*] --> Empty: App Launch
    Empty --> FirstMessage: User types message
    FirstMessage --> Thinking: Send button pressed
    Thinking --> WithHistory: Response received
    WithHistory --> Thinking: New message sent
    WithHistory --> Empty: New chat started
    WithHistory --> ModelSelection: Model switch requested
    ModelSelection --> WithHistory: Model selected
    
    Thinking --> Error: Processing failed
    Error --> WithHistory: Retry successful
    Error --> Empty: Clear conversation
```

### Screen Layouts

**Empty State**:
- Welcome message with app branding
- Suggested conversation starters (3-4 examples)
- Model selection indicator
- Input field with placeholder text

**Active Conversation**:
- Scrollable message list taking majority of screen
- Fixed input area at bottom with auto-expanding text field
- Send button enabled only when text present
- Voice input and paste buttons available

**Processing State**:
- Loading animation in AI message bubble
- Disable input during processing
- Cancel option for long-running requests
- Real-time processing metrics (optional)

### Responsive Design

- **Phone Portrait**: Single column, full-width messages
- **Phone Landscape**: Slightly wider message bubbles for readability
- **Tablet**: Centered conversation with max width for optimal reading
- **Accessibility**: Large text support, screen reader compatibility

## Data Flow & Boundaries

### Input Processing Flow

```mermaid
sequenceDiagram
    participant UI as Chat UI
    participant VM as ViewModel
    participant Repo as MessageRepository
    participant AI as LLaMA Engine
    participant DB as Room Database
    
    UI->>VM: sendMessage(text)
    VM->>Repo: addUserMessage(text)
    Repo->>DB: insertMessage(user, text)
    VM->>AI: generateResponse(context)
    AI-->>VM: responseToken (streaming)
    VM-->>UI: updatePartialResponse(token)
    AI->>VM: responseComplete(fullText)
    VM->>Repo: addAIMessage(response)
    Repo->>DB: insertMessage(ai, response)
    VM-->>UI: conversationUpdated()
```

### State Management

**ViewModel State**:
```kotlin
data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val currentInput: String = "",
    val isProcessing: Boolean = false,
    val currentModel: String = "",
    val error: String? = null,
    val processingMetrics: ProcessingMetrics? = null
)
```

**Message Data Model**:
```kotlin
data class Message(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Instant,
    val processingTimeMs: Long? = null,
    val tokenCount: Int? = null
)
```

### Component Boundaries

- **MainChatScreen**: Orchestrates overall chat experience
- **MessageList**: Handles conversation display and scrolling
- **MessageBubble**: Individual message rendering with actions
- **InputArea**: Text input, voice, and send functionality
- **ProcessingIndicator**: AI thinking state and metrics

## Non-Functional Requirements

### Performance

- **Message Rendering**: 60 FPS scrolling with 1000+ messages
- **Input Responsiveness**: <100ms delay from typing to display
- **Memory Usage**: <100MB for conversation with 500 messages
- **AI Response Time**: First token within 2 seconds, complete response within 30 seconds

### Internationalization (i18n)

- **Text Strings**: All UI text externalized to string resources
- **RTL Support**: Proper layout for right-to-left languages
- **Date/Time**: Locale-appropriate formatting
- **Cultural Sensitivity**: Appropriate conversation starters for different regions

### Accessibility

- **Screen Reader**: Full TalkBack/VoiceOver support
- **Keyboard Navigation**: Complete keyboard-only operation
- **Color Contrast**: WCAG AA compliance for all text
- **Large Text**: Scaling support up to 200%
- **Motor Accessibility**: Minimum 44dp touch targets

### Error Handling

- **Network Failures**: N/A (on-device processing)
- **Model Loading Errors**: Clear guidance and retry options
- **Memory Constraints**: Graceful degradation, conversation truncation
- **Input Validation**: Handle special characters, excessive length
- **Crash Recovery**: Restore conversation state after app restart

## Test Plan

### Unit Tests

**ViewModel Tests**:
- Message sending and state updates
- Error handling scenarios
- Input validation and sanitization
- Conversation management operations

**Repository Tests**:
- Message persistence and retrieval
- Database migration scenarios
- Conversation history management

**Utility Tests**:
- Message formatting and timestamps
- Text processing and validation
- Performance metrics calculation

### UI Tests (Compose)

**Message Display Tests**:
```kotlin
@Test
fun messageList_displays_user_and_ai_messages_correctly() {
    // Test message bubble appearance, timestamps, action buttons
}

@Test
fun conversation_scrolls_to_latest_message_automatically() {
    // Test auto-scroll behavior
}
```

**Input Tests**:
```kotlin
@Test
fun input_field_sends_message_on_enter_key() {
    // Test keyboard interaction
}

@Test
fun send_button_enabled_only_with_text() {
    // Test button state management
}
```

**State Tests**:
```kotlin
@Test
fun processing_indicator_shows_during_ai_response() {
    // Test loading states
}

@Test
fun error_state_displays_retry_option() {
    // Test error handling UI
}
```

### Integration Tests

**Chat Flow Tests**:
- Complete send→process→receive cycle
- Conversation persistence across app lifecycle
- Model switching during active conversation

**Performance Tests**:
- Large conversation rendering performance
- Memory usage during extended sessions
- Scroll performance with mixed content types

### Instrumented Tests

**Device Compatibility**:
- Test on various screen sizes and densities
- Verify performance on different hardware configurations
- Validate accessibility features across Android versions

## Telemetry **NOT** Collected

In accordance with our privacy-first approach, the following telemetry will **NOT** be collected:

❌ **User Messages**: No conversation content or user input  
❌ **AI Responses**: No generated content or model outputs  
❌ **Personal Identifiers**: No device IDs, user accounts, or tracking  
❌ **Usage Patterns**: No conversation frequency or timing data  
❌ **Performance Data**: No inference metrics transmitted externally  
❌ **Error Details**: No crash reports with sensitive context  

**Local Metrics Only**: Performance and error information remains on-device for debugging and optimization.

## Merge Checklist

### Development Complete
- [ ] All user stories implemented with acceptance criteria met
- [ ] Unit tests written and passing (≥80% coverage)
- [ ] UI tests covering critical user interactions
- [ ] Integration tests for ViewModel-Repository communication
- [ ] Performance benchmarks meet requirements

### Code Quality
- [ ] Kotlin code follows project style guide (ktlint passing)
- [ ] No lint warnings or static analysis issues
- [ ] All public APIs documented with KDoc
- [ ] Complex business logic has explanatory comments
- [ ] Error handling implemented for all failure modes

### UI/UX Standards
- [ ] Compose UI follows Material Design 3 guidelines
- [ ] Accessibility features tested with TalkBack
- [ ] All text externalized to string resources
- [ ] Color contrast meets WCAG AA standards
- [ ] Touch targets minimum 44dp in size

### Testing & Validation
- [ ] Manual testing on multiple device configurations
- [ ] Conversation persistence verified across app restarts
- [ ] Memory usage acceptable during extended sessions
- [ ] No memory leaks detected in UI tests
- [ ] Voice input integration working correctly

### Documentation
- [ ] Implementation matches specification requirements
- [ ] Any deviations from spec documented and justified
- [ ] Public API changes reflected in documentation
- [ ] Screenshots updated for UI changes

### Security & Privacy
- [ ] No conversation data transmitted externally
- [ ] Local storage properly encrypted
- [ ] Input validation prevents injection attacks
- [ ] No hardcoded secrets or credentials

---

## Implementation Notes (MVP 2)

### Message Persistence with Room

**Completed**: January 2025

**Overview**: Implemented local message persistence using Room database to preserve conversation history across app sessions.

**Components Added**:

1. **Database Layer** (`app/src/main/java/com/nervesparks/iris/data/local/`):
   - `MessageEntity`: Room entity representing persisted messages with columns for id, content, role, timestamp, processingTimeMs, and tokenCount
   - `MessageDao`: Data Access Object providing methods for insert, query, and delete operations with both suspend functions and Flow support
   - `AppDatabase`: Room database singleton with thread-safe initialization
   - `MessageMapper`: Utility to convert between domain `Message` and database `MessageEntity`, handling Instant↔Long and enum↔String conversions

2. **Testing**:
   - Unit tests for `MessageMapper` covering conversion, null handling, round-trip preservation, and edge cases (13 tests)
   - Instrumented tests for `MessageDao` covering CRUD operations, ordering, batch inserts, and conflict resolution (11 tests)
   - Compose UI test for message history restoration verifying correct display after reload (5 test scenarios)

**Key Design Decisions**:
- Used Room 2.6.1 with KSP for annotation processing
- Stored timestamps as Long (epoch milliseconds) for database compatibility
- Implemented REPLACE conflict strategy to handle duplicate message IDs
- Used Flow for reactive updates and suspend functions for one-time queries
- Singleton database pattern with double-checked locking for thread safety

**Database Schema**:
```sql
CREATE TABLE messages (
    id TEXT PRIMARY KEY NOT NULL,
    content TEXT NOT NULL,
    role TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    processingTimeMs INTEGER,
    tokenCount INTEGER
)
```

**Integration Points**:
- Messages should be persisted via `messageDao.insertMessage()` when sent or received
- History should be restored at app startup using `messageDao.getAllMessages().first()`
- ViewModel integration pending to connect persistence with UI layer

**Testing Coverage**:
- MessageMapper: 100% (all conversion paths tested)
- MessageDao: 100% (all database operations tested)
- UI restoration: Verified message display after simulated restart

**Future Enhancements**:
- Add conversation management (multiple conversations)
- Implement message search and filtering
- Add database migrations for schema changes
- Consider conversation archival for old messages

## Implementation Notes (MVP 3)

### Error States & Empty/Loading UX

**Implementation Date**: October 2025

**Components Added**:

1. **EmptyState Component** (`EmptyState.kt`)
   - Displays welcome message: "Hello, Ask me Anything"
   - Shows 3 conversation starters as interactive cards
   - Replaces inline empty state implementation in MainChatScreen
   - Consistent styling with existing design system (dark theme, rounded corners)
   - Supports optional click callbacks for conversation starters

2. **LoadingSkeleton Component** (`LoadingSkeleton.kt`)
   - Animated shimmer placeholder for messages being loaded
   - Supports both user and assistant message alignment
   - Uses infinite repeating animation with alpha transitions
   - Matches message bubble styling for visual consistency
   - Can be used during message fetch or processing states

3. **ErrorBanner Component** (`ErrorBanner.kt`)
   - Card-based error display with warning icon
   - Shows error title and descriptive message
   - Includes "Retry" button for failed operations
   - Optional "Dismiss" button to hide errors
   - Red color scheme (0xFF3D1F1F background) for visual distinction
   - Elevated card with rounded corners for prominence

**ViewModel Changes**:

- Added `errorMessage: String?` state variable to track error state
- Added `setError(error: String)` method to set error messages
- Added `clearError()` method to dismiss errors
- Updated `send()` method to:
  - Clear errors when sending new messages
  - Set errors when message processing fails
  - Catch exceptions and display user-friendly error messages

**MainChatScreen Integration**:

- Replaced inline empty state LazyColumn with `EmptyState` component
- Added `ProcessingIndicator` display inside message LazyColumn when `viewModel.getIsSending()` is true
- Added `ErrorBanner` below message list when `viewModel.errorMessage` is not null
- Error banner provides retry functionality by calling `viewModel.send()` again
- Error can be dismissed, clearing the error state

**State Machine Implementation**:

The following state transitions are now fully supported:

```
Empty → (user types) → FirstMessage
FirstMessage → (send pressed) → Processing (shows ProcessingIndicator)
Processing → (response received) → WithHistory
Processing → (error occurs) → Error (shows ErrorBanner)
Error → (retry pressed) → Processing (re-attempts send)
Error → (dismiss pressed) → WithHistory (error cleared)
```

**Testing Coverage**:

- **EmptyStateTest**: 6 tests covering welcome message, starters display, click handling
- **LoadingSkeletonTest**: 4 tests covering visibility, alignment, animation
- **ErrorBannerTest**: 9 tests covering error display, retry/dismiss actions, icon display
- **ChatStateTransitionTest**: 10 tests covering full state machine paths
- **Total**: 29 new Compose UI tests

**Design Decisions**:

1. **Empty State**: Chose card-based design over simple centered text for better visual hierarchy and to accommodate conversation starters without cluttering the UI.

2. **Loading Skeleton**: Implemented shimmer animation rather than spinner for better indication of message shape/structure and reduced perceived wait time.

3. **Error Banner**: Used banner style instead of dialog to allow users to continue reading messages while error is displayed. Positioned below messages for visibility without blocking content.

4. **Error Handling Strategy**: Clear errors automatically on new message send to prevent stale error state. Provide both retry and dismiss options for user control.

5. **State Transitions**: Designed to be deterministic and testable, with clear entry/exit conditions for each state.

---

## Implementation Notes (MVP 4 - Slice 3)

### Edit & Resend / Retry Last Message

**Feature**: Users can now edit and resend their last message or retry it verbatim via a long-press menu.

**ViewModel Changes**:

Added three new methods to `MainViewModel`:

1. `getLastUserMessage(): String?` (private)
   - Helper to find the last user message in the conversation
   - Returns `null` if no user messages exist

2. `retryLastMessage()`
   - Reuses the last user message verbatim
   - Removes the last assistant response if present (to retry after a failed response)
   - Calls `send()` to resend the message

3. `editAndResend(editedMessage: String)`
   - Accepts an edited version of the last user message
   - Removes the last assistant response and last user message
   - Validates that `editedMessage` is not blank
   - Calls `send()` with the edited message

**UI Changes**:

Modified `MessageBottomSheet` composable:

- Added `isLastUserMessage: Boolean = false` parameter to identify the last user message
- Added "Edit & Resend" button (only visible for last user message)
  - Opens a dialog with text field pre-populated with the current message
  - Dialog includes Cancel and Send buttons
  - Send button calls `viewModel.editAndResend(editedText)`
- Added "Retry" button (only visible for last user message)
  - Immediately calls `viewModel.retryLastMessage()` without confirmation
- Both buttons are disabled when AI is generating (`viewModel.getIsSending()`)
- Updated call sites in `MainChatScreen` to pass `isLastUserMessage` parameter
  - Calculates last user message by finding `indexOfLast { it["role"] == "user" }`
  - Compares with current message index (accounting for sliced messages)

**Edit Dialog**:

- Material 3 Dialog with dark theme matching app colors
- OutlinedTextField with 150dp height and 6 maxLines
- Cancel button dismisses dialog without changes
- Send button validates, calls editAndResend, dismisses dialog and bottom sheet

**Testing Coverage**:

**Unit Tests** (`MainViewModelEditRetryTest.kt` - 9 tests):
- `retryLastMessage_withNoMessages_doesNothing`
- `retryLastMessage_withUserMessage_resendsSameMessage`
- `retryLastMessage_removesLastAssistantResponse`
- `editAndResend_withBlankMessage_doesNothing`
- `editAndResend_withValidMessage_removesLastUserAndAssistantMessages`
- `editAndResend_withOnlyUserMessage_removesLastUserMessage`
- `editAndResend_preservesEarlierMessages`
- `retryLastMessage_findsCorrectLastUserMessage`
- `editAndResend_withWhitespaceOnly_doesNothing`

**Compose UI Tests** (`MessageBottomSheetEditRetryTest.kt` - 9 tests):
- `messageBottomSheet_showsEditAndRetryButtons_forLastUserMessage`
- `messageBottomSheet_hidesEditAndRetryButtons_forNonLastUserMessage`
- `messageBottomSheet_retryButton_callsRetryLastMessage`
- `messageBottomSheet_editButton_showsEditDialog`
- `editDialog_cancelButton_closesDialog`
- `editDialog_sendButton_callsEditAndResend`
- `messageBottomSheet_alwaysShowsCopyButton`
- `messageBottomSheet_buttonsDisabled_whenAIGenerating`

**Total**: 18 new tests (9 unit + 9 UI)

**Design Decisions**:

1. **Last User Message Only**: Edit/Retry actions only appear for the last user message to keep UX simple and avoid complex conversation history manipulation. Editing earlier messages would require re-generating all subsequent responses.

2. **Retry vs Edit**: Separate actions for different use cases:
   - Retry: Quick one-click action for regenerating a response (e.g., after error or unsatisfactory answer)
   - Edit: Allows refinement of the original prompt before resending

3. **Remove Assistant Response**: Both actions remove the last assistant response if present, ensuring a clean conversation state before resending. This prevents duplicate or conflicting responses.

4. **Dialog for Edit**: Used a dialog (rather than inline editing) to provide a focused editing experience with clear commit/cancel actions, preventing accidental edits.

5. **No Confirmation for Retry**: Retry doesn't require confirmation since it's a non-destructive action that can be easily reversed with stop button or new message.

6. **Disabled During Generation**: Both actions disabled while AI is generating to prevent race conditions and ensure consistent conversation state.

**Acceptance Criteria Met**:
- ✅ Long-press menu on last user message shows "Edit & Resend" option
- ✅ "Edit & Resend" opens dialog allowing message editing
- ✅ Edited message replaces original and triggers new generation
- ✅ "Retry" reuses previous prompt verbatim
- ✅ Both actions disabled when AI is generating
- ✅ ViewModel unit tests added and passing
- ✅ Compose UI tests added for menu actions
- ✅ No destructive refactors or package renames
- ✅ Follows existing patterns from .github/copilot-instructions.md
- ✅ Documentation updated with implementation notes

---

1. **EmptyState replaces inline implementation**: The existing empty state code in MainChatScreen was replaced with a reusable component to improve maintainability and testability.

2. **Error banner positioned outside LazyColumn**: Errors are shown below the message list rather than inside it, ensuring they remain visible while scrolling.

3. **ProcessingIndicator shown inside LazyColumn**: This allows the indicator to scroll with messages and appear in the natural message flow.

4. **No destructive changes**: Existing message rendering logic remains unchanged; new components are additive.

5. **Error handling is opt-in**: Errors are only displayed when explicitly set via `setError()`, maintaining backward compatibility.

**Known Limitations**:

- LoadingSkeleton is created but not yet integrated into MainChatScreen (planned for future message loading scenarios)
- Error retry always calls `send()` with the last message; more sophisticated retry logic could be added
- Conversation starters don't auto-populate the input field (currently just fire callbacks)

**Accessibility Considerations**:

- All components use Material3 theming for consistent color contrast
- Error banner includes semantic warning icon with content description
- Text sizes match existing typography scale
- Touch targets meet 44dp minimum requirement

**Future Enhancements**:

- Add network error specific messaging (though currently on-device only)
- Implement exponential backoff for retries
- Add error categorization (transient vs permanent errors)
- Show loading skeleton during message history restoration
- Add haptic feedback for error states

---

*Specification Version: 1.0*  
*Last Updated: October 2025*  
*Implementation Target: Milestone 1*