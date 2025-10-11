# MVP Slice 1: Component Visual Specifications

## MessageBubble Component Visual Design

### User Message (MessageRole.USER)
```
┌─────────────────────────────────────────┐
│                      [Hello, how are    │
│                       you today?]  👤   │
│                         12:34 PM        │
└─────────────────────────────────────────┘

Styling:
- Background: #171E2C (dark blue)
- Text Color: #A0A0A5 (light gray)
- Alignment: Right
- Border Radius: 12dp
- Padding: 12dp
- User Icon (👤): 24dp, positioned right
```

### Assistant Message (MessageRole.ASSISTANT)
```
┌─────────────────────────────────────────┐
│ 🤖  [I'm doing well, thank you!         │
│     How can I help you today?]          │
│     1:23 PM                              │
│     1500ms • 25 tokens                   │
└─────────────────────────────────────────┘

Styling:
- Background: Transparent
- Text Color: #A0A0A5 (light gray)
- Alignment: Left
- Border Radius: 12dp
- Padding: 12dp
- Assistant Icon (🤖): 24dp, positioned left
- Metrics shown: processing time and token count
```

### System Message (MessageRole.SYSTEM)
```
┌─────────────────────────────────────────┐
│     [Model loaded successfully]         │
│     11:45 AM                             │
└─────────────────────────────────────────┘

Styling:
- Background: #2C2C2E (dark gray)
- Text Color: #A0A0A5 (light gray)
- Alignment: Centered/Left
- Border Radius: 12dp
- Padding: 12dp
- No icons displayed
```

## ProcessingIndicator Component Visual Design

### Active Processing State
```
┌─────────────────────────────────────────┐
│ 🤖  Thinking • • •                      │
└─────────────────────────────────────────┘

Styling:
- Background: Transparent
- Text: "Thinking" in #A0A0A5
- Dots: Animated with alpha transitions (0.3 to 1.0)
- Animation: Each dot animates with 200ms delay
- Dot Size: 6dp circles
- Dot Color: #A0A0A5
- Assistant Icon (🤖): 24dp
```

### Animation Sequence
```
Frame 1: Thinking ● ○ ○
Frame 2: Thinking ○ ● ○
Frame 3: Thinking ○ ○ ●
Frame 4: Thinking ● ○ ○
(repeats)
```

## Color Palette Reference

```
User Message Background:    #171E2C (Dark Blue)
System Message Background:  #2C2C2E (Dark Gray)
Assistant Background:       Transparent
Text Color:                 #A0A0A5 (Light Gray)
Timestamp/Metrics Color:    #6C6C70 (Darker Gray)
```

## Interaction States

### MessageBubble - Long Press
1. User performs long press on message
2. Message content is copied to clipboard
3. Toast appears: "Message copied to clipboard"
4. Optional onLongClick callback is triggered

### MessageBubble - Normal Tap
1. Currently no action (onClick is empty)
2. Future: Could trigger message selection or detail view

## Spacing and Layout

### MessageBubble Spacing
```
Row Container:
  - Horizontal padding: 8dp
  - Vertical padding: 4dp

Message Box:
  - Internal padding: 12dp
  - Border radius: 12dp
  - Gap between icon and bubble: 4dp

Timestamp:
  - Top margin: 4dp
  - Horizontal padding: 4dp
  - Font: labelSmall

Metrics (assistant only):
  - Top margin: 2dp
  - Horizontal padding: 4dp
  - Font: labelSmall
```

### ProcessingIndicator Spacing
```
Row Container:
  - Horizontal padding: 8dp
  - Vertical padding: 4dp

Processing Box:
  - Internal padding: 12dp
  - Border radius: 12dp
  - Gap between icon and bubble: 4dp

Dots:
  - Size: 6dp circles
  - Spacing: 4dp between dots
```

## Typography

Using Material Design 3 typography:

- **Message Content**: MaterialTheme.typography.bodyLarge
- **Timestamp**: MaterialTheme.typography.labelSmall
- **Metrics**: MaterialTheme.typography.labelSmall
- **"Thinking" Text**: MaterialTheme.typography.bodyLarge

## Accessibility

### Content Descriptions
- User Icon: "User Icon"
- Assistant Icon: "AI Assistant Icon"
- Message bubbles: Content is readable by screen readers

### Touch Targets
- Icons: 24dp (meets minimum 48dp hit target with padding)
- Message bubbles: Full message area is tappable
- Long press: Accessible via long press gesture

### Color Contrast
- Text (#A0A0A5) on Dark Background: Meets WCAG AA
- Timestamp (#6C6C70) on Dark Background: May need adjustment for AAA

## Component Integration Example

### Full Conversation View
```
┌─────────────────────────────────────────┐
│ 🤖  [Welcome to Iris! How can I         │
│     assist you today?]                   │
│     10:30 AM                             │
│     500ms • 15 tokens                    │
├─────────────────────────────────────────┤
│                      [What's the        │
│                       weather like?] 👤  │
│                         10:31 AM         │
├─────────────────────────────────────────┤
│ 🤖  Thinking • • •                      │
└─────────────────────────────────────────┘
```

## Future Enhancements (Not in MVP)

- [ ] Message selection mode
- [ ] Reply/quote functionality
- [ ] Code syntax highlighting for assistant responses
- [ ] Markdown rendering
- [ ] Image/attachment support
- [ ] Voice message playback
- [ ] Message editing
- [ ] Message deletion
- [ ] Regenerate response action

## Implementation Status

✅ **Completed:**
- Message data class with all properties
- MessageBubble component with all styling
- ProcessingIndicator with animations
- Copy to clipboard functionality
- Role-based styling
- Timestamp display
- Processing metrics display
- Unit tests (12 tests for Message)
- UI tests (16 tests total for components)

⏳ **Pending:**
- Build verification (blocked by network)
- Screenshot generation (blocked by build)
- Integration into MainChatScreen
- ViewModel migration to use Message class

## Notes

- All measurements are in dp (density-independent pixels)
- Colors are specified in hex format
- Icons referenced (R.drawable.logo, R.drawable.user_icon) must exist in resources
- Requires Android API 26+ for java.time.Instant (project minSdk is 28 ✓)
