# Model Parameters UI - Visual Specification

## Settings Bottom Sheet - Model Parameters Section

```
┌─────────────────────────────────────────────────────┐
│                                                     │
│  Settings                                           │
│  ═══════════════════════════════════════════════   │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │  Model Selection                            │   │
│  │  Select which AI model to use               │   │
│  │                                              │   │
│  │  Active: Llama-3.2-1B-Instruct-Q6_K_L.gguf  │   │
│  │                                              │   │
│  │  [Select Model ▼]                           │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │  Select thread for process, 0 for default   │   │
│  │                                              │   │
│  │  4                                           │   │
│  │  ●────────────────────                      │   │
│  │  0                                       8   │   │
│  │                                              │   │
│  │  After changing thread please Save!          │   │
│  │  [        Save        ]                     │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │  Model Parameters                           │   │
│  │  Adjust model inference parameters.         │   │
│  │  Changes apply on next model load.          │   │
│  │                                              │   │
│  │  Quick Presets                              │   │
│  │  [Conservative] [Balanced] [Creative]       │   │
│  │                                              │   │
│  │  Temperature                         1.00   │   │
│  │  Controls randomness. Lower values make     │   │
│  │  output more focused and deterministic.     │   │
│  │  ●─────────────────────────                 │   │
│  │  0.1                                    2.0  │   │
│  │                                              │   │
│  │  Top P                               0.90   │   │
│  │  Nucleus sampling. Considers tokens with    │   │
│  │  cumulative probability up to this value.   │   │
│  │  ●──────────────────────────────            │   │
│  │  0.1                                    1.0  │   │
│  │                                              │   │
│  │  Top K                                  40   │   │
│  │  Limits sampling to the top K most likely   │   │
│  │  tokens. Lower values reduce randomness.    │   │
│  │  ●────────────────                          │   │
│  │  1                                      100  │   │
│  │                                              │   │
│  │  Context Length                       2048   │   │
│  │  Maximum conversation context length.       │   │
│  │  Higher values use more memory.             │   │
│  │  ●───────────────────                       │   │
│  │  512                                   4096  │   │
│  │                                              │   │
│  │  [       Reset to Defaults       ]          │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
└─────────────────────────────────────────────────────┘
```

## Parameter Descriptions

### Temperature (0.1 - 2.0)
- **What it does**: Controls randomness in token selection
- **Low values (0.1-0.5)**: More focused, deterministic output
- **Medium values (0.8-1.2)**: Balanced creativity and coherence
- **High values (1.5-2.0)**: More creative, diverse output

### Top P (0.1 - 1.0)
- **What it does**: Nucleus sampling threshold
- **Low values (0.1-0.5)**: Only high-probability tokens considered
- **Medium values (0.7-0.9)**: Balanced token selection
- **High values (0.95-1.0)**: Wide range of tokens considered

### Top K (1 - 100)
- **What it does**: Limits sampling to K most likely tokens
- **Low values (1-20)**: Very focused selection
- **Medium values (30-50)**: Balanced selection
- **High values (60-100)**: Wide token selection

### Context Length (512 - 4096)
- **What it does**: Maximum tokens in conversation context
- **Note**: Currently fixed at 4096 in the underlying engine
- **Future**: Will support dynamic context windows

## Preset Configurations

### Conservative Preset
```
Temperature:  0.5
Top P:        0.7
Top K:        20
Use case:     Factual responses, coding, math
```

### Balanced Preset (Default)
```
Temperature:  1.0
Top P:        0.9
Top K:        40
Use case:     General conversation, Q&A
```

### Creative Preset
```
Temperature:  1.5
Top P:        0.95
Top K:        60
Use case:     Creative writing, brainstorming
```

## Interaction Flow

1. **Opening Settings**
   - User taps settings icon in chat screen
   - Bottom sheet slides up from bottom
   - Scrollable content shows all settings sections

2. **Adjusting Parameters**
   - User scrolls to "Model Parameters" section
   - Drags slider thumb to adjust value
   - Value updates in real-time (displayed next to label)
   - Help text explains each parameter

3. **Applying Presets**
   - User taps one of three preset buttons
   - All parameter sliders animate to preset values
   - Values persist to storage immediately

4. **Resetting to Defaults**
   - User taps "Reset to Defaults" button
   - All parameters reset to default values
   - Sliders animate to default positions

5. **Parameter Application**
   - Changes are saved immediately to SharedPreferences
   - Next model load uses updated parameters
   - Visual indicator shows active model at top of chat

## Visual States

### Normal State
- Sliders enabled and interactive
- Preset buttons clickable
- Purple accent color (#6200EE)

### Disabled State (When Sending)
- Could disable sliders during active generation
- Currently allows changes anytime
- Changes take effect on next load

### Active Parameter Indicators
- Purple slider thumb
- Purple active track
- Gray inactive track
- White text on dark background

## Accessibility

- Large touch targets for sliders
- Clear labels and value displays
- Help text for screen readers
- Sufficient color contrast
- Keyboard navigation support (future)

## Responsive Behavior

- Bottom sheet scrollable for small screens
- Parameter section expands to full width
- Preset buttons wrap on narrow screens
- Slider tracks scale with screen width
