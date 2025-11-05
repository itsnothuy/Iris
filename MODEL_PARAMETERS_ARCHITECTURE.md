# Model Parameters Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        UI Layer (Compose)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  MainChatScreen.kt                                              │
│  ├─ SettingsBottomSheet                                         │
│  │  ├─ Model Selection Section                                 │
│  │  ├─ Thread Selection Section                                │
│  │  └─ Model Parameters Section ✨ NEW                         │
│  │     ├─ Quick Presets Row                                    │
│  │     │  ├─ [Conservative] Button                             │
│  │     │  ├─ [Balanced] Button                                 │
│  │     │  └─ [Creative] Button                                 │
│  │     ├─ ParameterSlider (Temperature) ✨ NEW                 │
│  │     ├─ ParameterSlider (Top P) ✨ NEW                       │
│  │     ├─ ParameterSliderInt (Top K) ✨ NEW                    │
│  │     ├─ ParameterSliderInt (Context) ✨ NEW                  │
│  │     └─ [Reset to Defaults] Button                           │
│  │                                                              │
│  └─ Components:                                                 │
│     └─ ParameterSlider.kt ✨ NEW                                │
│        ├─ Label + Value Display                                │
│        ├─ Help Text (optional)                                 │
│        ├─ Slider with Range                                    │
│        └─ Value Formatter                                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                            ▲ │
                            │ │ UI Events / State Updates
                            │ ▼
┌─────────────────────────────────────────────────────────────────┐
│                    ViewModel Layer (Logic)                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  MainViewModel.kt                                               │
│  ├─ Parameter Management ✨ NEW                                 │
│  │  ├─ getTemperature() / setTemperature()                     │
│  │  ├─ getTopP() / setTopP()                                   │
│  │  ├─ getTopK() / setTopK()                                   │
│  │  ├─ getContextLength() / setContextLength()                 │
│  │  ├─ applyParameterPreset(preset)                            │
│  │  └─ resetParametersToDefaults()                             │
│  │                                                              │
│  ├─ Model Loading                                              │
│  │  └─ load(path, threads) ✨ UPDATED                          │
│  │     └─ Reads parameters from repository                     │
│  │        └─ Passes to LlamaAndroid.load()                     │
│  │                                                              │
│  └─ Enum ✨ NEW                                                 │
│     └─ ParameterPreset                                         │
│        ├─ CONSERVATIVE                                          │
│        ├─ BALANCED                                              │
│        └─ CREATIVE                                              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                            ▲ │
                            │ │ Read / Write Operations
                            │ ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Data Layer (Persistence)                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  UserPreferencesRepository.kt                                   │
│  ├─ Constants ✨ NEW                                            │
│  │  ├─ DEFAULT_TEMPERATURE = 1.0f                              │
│  │  ├─ DEFAULT_TOP_P = 0.9f                                    │
│  │  ├─ DEFAULT_TOP_K = 40                                      │
│  │  └─ DEFAULT_CONTEXT_LENGTH = 2048                           │
│  │                                                              │
│  ├─ Getter Methods ✨ NEW                                       │
│  │  ├─ getTemperature(): Float                                 │
│  │  ├─ getTopP(): Float                                        │
│  │  ├─ getTopK(): Int                                          │
│  │  └─ getContextLength(): Int                                 │
│  │                                                              │
│  ├─ Setter Methods ✨ NEW                                       │
│  │  ├─ setTemperature(value)                                   │
│  │  ├─ setTopP(value)                                          │
│  │  ├─ setTopK(value)                                          │
│  │  └─ setContextLength(value)                                 │
│  │                                                              │
│  └─ Reset Method ✨ NEW                                         │
│     └─ resetParametersToDefaults()                             │
│                                                                 │
│  └─ SharedPreferences (Android Storage)                        │
│     ├─ KEY_TEMPERATURE → Float                                 │
│     ├─ KEY_TOP_P → Float                                       │
│     ├─ KEY_TOP_K → Int                                         │
│     └─ KEY_CONTEXT_LENGTH → Int                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                            ▲
                            │ Parameters applied during load
                            │
┌─────────────────────────────────────────────────────────────────┐
│                 Model Inference Layer (Native)                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  LLamaAndroid.kt                                                │
│  └─ load(path, threads, topK, topP, temp)                      │
│     └─ Creates sampler with parameters                         │
│        └─ new_sampler(topK, topP, temp)                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow Diagrams

### Setting a Parameter

```
User Action                ViewModel                  Repository
    │                          │                          │
    │ Drags Slider             │                          │
    ├─────────────────────────>│                          │
    │                          │ setTemperature(1.2f)     │
    │                          ├─────────────────────────>│
    │                          │                          │ Write to
    │                          │                          │ SharedPrefs
    │                          │                          ├─────────┐
    │                          │                          │         │
    │                          │                    OK    │<────────┘
    │                          │<─────────────────────────┤
    │ UI Updates (1.2 shown)   │                          │
    │<─────────────────────────┤                          │
    │                          │                          │
```

### Loading Model with Parameters

```
User Loads Model           ViewModel                  Repository            LlamaAndroid
    │                          │                          │                      │
    │ Switch Model             │                          │                      │
    ├─────────────────────────>│                          │                      │
    │                          │ getTemperature()         │                      │
    │                          ├─────────────────────────>│                      │
    │                          │                          │ Read from            │
    │                          │                          │ SharedPrefs          │
    │                          │           1.2f           │                      │
    │                          │<─────────────────────────┤                      │
    │                          │                          │                      │
    │                          │ (Repeat for topP, topK)  │                      │
    │                          │                          │                      │
    │                          │ load(path, threads,      │                      │
    │                          │      topK=40, topP=0.9,  │                      │
    │                          │      temp=1.2)           │                      │
    │                          ├──────────────────────────┼─────────────────────>│
    │                          │                          │                      │
    │                          │                          │          Model Loaded │
    │                          │                          │          with params  │
    │                          │<─────────────────────────┴──────────────────────┤
    │ Model Ready              │                                                  │
    │<─────────────────────────┤                                                  │
    │                          │                                                  │
```

### Applying a Preset

```
User Action                ViewModel                  Repository
    │                          │                          │
    │ Click "Creative"         │                          │
    ├─────────────────────────>│                          │
    │                          │ applyParameterPreset     │
    │                          │   (CREATIVE)             │
    │                          ├─────┐                    │
    │                          │     │                    │
    │                          │<────┘                    │
    │                          │ setTemperature(1.5)      │
    │                          ├─────────────────────────>│
    │                          │ setTopP(0.95)            │
    │                          ├─────────────────────────>│
    │                          │ setTopK(60)              │
    │                          ├─────────────────────────>│
    │                          │                          │
    │ All sliders animate      │            OK            │
    │ to new values            │<─────────────────────────┤
    │<─────────────────────────┤                          │
    │                          │                          │
```

## Component Hierarchy

```
SettingsBottomSheet
└── LazyColumn
    ├── Model Selection Item
    ├── Thread Selection Item
    └── Model Parameters Item ✨ NEW
        ├── Column
        │   ├── Title Text
        │   ├── Description Text
        │   ├── Quick Presets Label
        │   ├── Row (Preset Buttons)
        │   │   ├── Button "Conservative"
        │   │   ├── Button "Balanced"
        │   │   └── Button "Creative"
        │   ├── ParameterSlider (Temperature)
        │   │   ├── Row (Label + Value)
        │   │   ├── Text (Help)
        │   │   └── Slider
        │   ├── ParameterSlider (Top P)
        │   ├── ParameterSliderInt (Top K)
        │   ├── ParameterSliderInt (Context)
        │   └── TextButton "Reset to Defaults"
        └── Box (Container)
```

## State Management

```
Compose State (Local)
├─ temperature: Float      ← remember { viewModel.getTemperature() }
├─ topP: Float            ← remember { viewModel.getTopP() }
├─ topK: Int              ← remember { viewModel.getTopK() }
└─ contextLength: Int     ← remember { viewModel.getContextLength() }
    │
    │ When slider changes
    ├─ Update local state (immediate UI feedback)
    └─ Call viewModel.setX() (persist to storage)

ViewModel (No State)
└─ Delegates to UserPreferencesRepository

Repository (Persistent State)
└─ SharedPreferences
   ├─ "model_temperature" = 1.0
   ├─ "model_top_p" = 0.9
   ├─ "model_top_k" = 40
   └─ "model_context_length" = 2048
```

## Legend
- ✨ NEW - Newly added in this PR
- ✨ UPDATED - Modified in this PR
- (No marker) - Existing code, unchanged
