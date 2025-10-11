# MVP Slice 1: UI Mockup

This document provides ASCII art mockups of how the implemented components should appear when rendered.

## Full Chat Interface Mockup

```
╔══════════════════════════════════════════════════════════════╗
║  Iris Chat                                    [≡] [⋮]         ║
╠══════════════════════════════════════════════════════════════╣
║                                                                ║
║  ┌────────────────────────────────────────────────┐          ║
║  │ 🤖  Welcome to Iris! I'm your on-device AI     │          ║
║  │     assistant. How can I help you today?       │          ║
║  │     10:30 AM                                    │          ║
║  │     500ms • 15 tokens                           │          ║
║  └────────────────────────────────────────────────┘          ║
║                                                                ║
║                        ┌──────────────────────────┐           ║
║                        │ What can you help me     │ 👤        ║
║                        │ with?                    │           ║
║                        │ 10:31 AM                 │           ║
║                        └──────────────────────────┘           ║
║                                                                ║
║  ┌────────────────────────────────────────────────┐          ║
║  │ 🤖  I can help you with various tasks:          │          ║
║  │     • Answer questions                          │          ║
║  │     • Generate text                             │          ║
║  │     • Provide explanations                      │          ║
║  │     • And much more!                            │          ║
║  │     10:32 AM                                    │          ║
║  │     1200ms • 42 tokens                          │          ║
║  └────────────────────────────────────────────────┘          ║
║                                                                ║
║                        ┌──────────────────────────┐           ║
║                        │ Tell me about quantum    │ 👤        ║
║                        │ computing                │           ║
║                        │ 10:33 AM                 │           ║
║                        └──────────────────────────┘           ║
║                                                                ║
║  ┌────────────────────────────────────────────────┐          ║
║  │ 🤖  Thinking • • •                              │          ║
║  └────────────────────────────────────────────────┘          ║
║                                                                ║
║                                                                ║
║                                                                ║
║                                                                ║
╠══════════════════════════════════════════════════════════════╣
║  [Type a message...]                             [🎤] [→]     ║
╚══════════════════════════════════════════════════════════════╝
```

## Individual Component Mockups

### 1. User Message (MessageBubble with MessageRole.USER)
```
                     ┌──────────────────────────────────┐
                     │  Hello, how are you today?       │ 👤
                     │  This is a user message          │
                     │  12:34 PM                         │
                     └──────────────────────────────────┘

Colors:
- Background: #171E2C (Dark blue)
- Text: #A0A0A5 (Light gray)
- Timestamp: #6C6C70
- Alignment: Right
- Icon: User icon (24dp) on right side
```

### 2. Assistant Message (MessageBubble with MessageRole.ASSISTANT)
```
┌──────────────────────────────────────┐
│ 🤖  I'm doing well, thank you!        │
│     How can I help you today?         │
│     1:23 PM                            │
│     1500ms • 25 tokens                 │
└──────────────────────────────────────┘

Colors:
- Background: Transparent
- Text: #A0A0A5 (Light gray)
- Timestamp: #6C6C70
- Metrics: #6C6C70
- Alignment: Left
- Icon: Assistant icon (24dp) on left side
```

### 3. System Message (MessageBubble with MessageRole.SYSTEM)
```
        ┌──────────────────────────────────┐
        │  Model loaded successfully        │
        │  11:45 AM                          │
        └──────────────────────────────────┘

Colors:
- Background: #2C2C2E (Dark gray)
- Text: #A0A0A5 (Light gray)
- Timestamp: #6C6C70
- Alignment: Left/Center
- Icon: None
```

### 4. Processing Indicator (ProcessingIndicator)
```
┌──────────────────────────────────────┐
│ 🤖  Thinking • • •                    │
└──────────────────────────────────────┘

Animation Frames:
Frame 1: Thinking ● ○ ○
Frame 2: Thinking ○ ● ○
Frame 3: Thinking ○ ○ ●
(repeats)

Colors:
- Background: Transparent
- Text: #A0A0A5 (Light gray)
- Dots: #A0A0A5 with alpha animation
- Icon: Assistant icon (24dp) on left side
```

## Interaction States

### MessageBubble - Long Press Action
```
┌────────────────────────────────────────────────────────┐
│                      ┌──────────────────────────┐      │
│                      │ What's the weather?      │ 👤   │
│                      │ [Long press detected]    │      │
│                      └──────────────────────────┘      │
│                                                         │
│  ┌──────────────────────────────────────────────────┐ │
│  │   📋 Message copied to clipboard                  │ │
│  └──────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

## Responsive Layouts

### Phone Portrait (Default)
```
╔════════════════════╗
║ 🤖  Message content║
║     is displayed   ║
║     at full width  ║
║     with proper    ║
║     padding        ║
╠════════════════════╣
║         [User msg] ║
║                 👤 ║
╚════════════════════╝
```

### Phone Landscape
```
╔════════════════════════════════════════╗
║  🤖  Message content has slightly      ║
║      wider bubbles for readability     ║
╠════════════════════════════════════════╣
║                    [User message]   👤 ║
╚════════════════════════════════════════╝
```

### Tablet
```
╔══════════════════════════════════════════════════════╗
║             Centered conversation area                ║
║  ┌────────────────────────────────────────────┐      ║
║  │ 🤖  Message bubbles have max width          │      ║
║  │     for optimal reading experience          │      ║
║  └────────────────────────────────────────────┘      ║
║                                                       ║
║                  ┌──────────────────────────┐        ║
║                  │ User messages also       │ 👤     ║
║                  │ respect max width        │        ║
║                  └──────────────────────────┘        ║
╚══════════════════════════════════════════════════════╝
```

## Color Palette Visual Reference

```
┌─────────────────────────────────────────────────────────┐
│  User Message Background (#171E2C)                      │
│  ███████████████████████████████████████████████████    │
│  Dark Blue - Clear distinction from assistant messages  │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  System Message Background (#2C2C2E)                    │
│  ███████████████████████████████████████████████████    │
│  Dark Gray - Subtle, non-intrusive                      │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  Text Color (#A0A0A5)                                   │
│  ███████████████████████████████████████████████████    │
│  Light Gray - Good contrast on dark backgrounds         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  Timestamp/Metrics Color (#6C6C70)                      │
│  ███████████████████████████████████████████████████    │
│  Darker Gray - Secondary information                    │
└─────────────────────────────────────────────────────────┘
```

## Typography Hierarchy

```
┌──────────────────────────────────────────────────────────┐
│  Message Content                                         │
│  MaterialTheme.typography.bodyLarge                      │
│  Primary reading text                                    │
│  ════════════════════════════════════════════════        │
│                                                           │
│  Timestamp & Metrics                                     │
│  MaterialTheme.typography.labelSmall                     │
│  Secondary information                                   │
│  ────────────────────────────────────────────────        │
└──────────────────────────────────────────────────────────┘
```

## Spacing Guide

```
Message Bubble Spacing:
┌──────────────────────────────────────────┐
│ ← 8dp →                                   │  ← Row padding
│         ┌──────────────────────────┐     │
│   4dp → │ ← 12dp content padding   │     │
│         │                           │     │
│         │  Message content here     │     │
│         │                           │     │
│         │ 12dp →                    │     │
│         └──────────────────────────┘     │
│                                           │
│         ← 4dp → Timestamp                 │
│                                           │
│         ← 4dp → Metrics                   │
└──────────────────────────────────────────┘
```

## Animation States

### Processing Indicator Animation Sequence
```
Time: 0ms      Time: 200ms    Time: 400ms    Time: 600ms
┌──────────┐  ┌──────────┐   ┌──────────┐   ┌──────────┐
│ Think ●○○│  │ Think ○●○│   │ Think ○○●│   │ Think ●○○│
└──────────┘  └──────────┘   └──────────┘   └──────────┘
     ↓              ↓              ↓              ↓
Alpha:        Alpha:         Alpha:         Alpha:
Dot 1: 1.0    Dot 1: 0.3     Dot 1: 0.3     Dot 1: 1.0
Dot 2: 0.3    Dot 2: 1.0     Dot 2: 0.3     Dot 2: 0.3
Dot 3: 0.3    Dot 3: 0.3     Dot 3: 1.0     Dot 3: 0.3
```

## Accessibility Features

```
┌──────────────────────────────────────────────────────────┐
│  Screen Reader Support                                   │
│  ────────────────────────────────────────────           │
│  • User Icon: "User Icon"                                │
│  • Assistant Icon: "AI Assistant Icon"                   │
│  • Message content: Fully readable                       │
│  • Timestamps: Announced with proper formatting          │
│  • Metrics: "1500 milliseconds, 25 tokens"               │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│  Touch Targets                                           │
│  ────────────────────────────────────────────           │
│  • Minimum 48dp hit area (including padding)             │
│  • Long-press: Accessible gesture                        │
│  • Full message area is interactive                      │
└──────────────────────────────────────────────────────────┘
```

## Edge Cases Handled

### Very Long Message
```
┌──────────────────────────────────────────┐
│ 🤖  This is a very long message that     │
│     wraps across multiple lines. The     │
│     MessageBubble component handles      │
│     text wrapping automatically and      │
│     maintains proper spacing and         │
│     alignment throughout the entire      │
│     message content area.                │
│     10:45 AM                              │
│     3500ms • 120 tokens                   │
└──────────────────────────────────────────┘
```

### Empty Message
```
┌──────────────────────────────────────────┐
│ 🤖                                        │
│     10:45 AM                              │
└──────────────────────────────────────────┘
```

### Special Characters
```
┌──────────────────────────────────────────┐
│ 🤖  Hello! 😀                             │
│     Line 1                                │
│     Line 2 (with tab→here)                │
│     Special chars: @#$%^&*               │
│     10:45 AM                              │
└──────────────────────────────────────────┘
```

## Implementation Status

✅ All components implemented  
✅ Styling matches specification  
✅ Animations configured  
✅ Colors defined  
✅ Typography set  
✅ Spacing established  
✅ Accessibility features included  
⏳ Visual validation pending (requires build)  

## Testing Verification

Once the app is built, verify these visual elements:

- [ ] User messages appear on right with blue background
- [ ] Assistant messages appear on left with transparent background
- [ ] System messages appear with gray background
- [ ] Icons are properly positioned (user right, assistant left)
- [ ] Timestamps display in correct format
- [ ] Processing metrics show for assistant messages only
- [ ] ProcessingIndicator animates smoothly
- [ ] Long-press shows clipboard toast
- [ ] Text wrapping works correctly
- [ ] Special characters render properly
- [ ] Colors match specification (#171E2C, #A0A0A5, etc.)
- [ ] Spacing matches specification (8dp, 12dp, etc.)
- [ ] Touch targets are adequate for accessibility
