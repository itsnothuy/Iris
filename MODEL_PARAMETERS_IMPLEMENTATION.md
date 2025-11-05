# Model Parameters Configuration - Implementation Summary

## Overview
This implementation adds comprehensive model parameter configuration with real-time adjustment capabilities for the Iris Android AI assistant app.

## Features Implemented

### 1. Parameter Storage (UserPreferencesRepository)
- **Temperature**: Float (default 1.0, range 0.1-2.0)
- **Top P**: Float (default 0.9, range 0.1-1.0)
- **Top K**: Int (default 40, range 1-100)
- **Context Length**: Int (default 2048, range 512-4096)

All parameters persist across app sessions using SharedPreferences.

### 2. UI Components

#### ParameterSlider Component
A reusable Compose component for parameter adjustment:
- Label with current value display
- Help text explaining the parameter
- Custom value formatters
- Integer variant (ParameterSliderInt) for discrete values

#### Settings Bottom Sheet Enhancement
Added "Model Parameters" section with:
- Three quick preset buttons (Conservative, Balanced, Creative)
- Four parameter sliders (Temperature, Top P, Top K, Context Length)
- Help text for each parameter
- "Reset to Defaults" button

### 3. Parameter Presets

**Conservative** (Focused, deterministic output):
- Temperature: 0.5
- Top P: 0.7
- Top K: 20

**Balanced** (Default settings):
- Temperature: 1.0
- Top P: 0.9
- Top K: 40

**Creative** (More random, creative output):
- Temperature: 1.5
- Top P: 0.95
- Top K: 60

### 4. Integration with Model Loading
Parameters are automatically applied when:
- Loading a model for the first time
- Switching between models
- Reloading the current model

Changes take effect on the next model load - no hot-swapping to ensure stability.

## Test Coverage

### Unit Tests (21 tests)
- **UserPreferencesRepository**: 9 new tests for parameter storage
- **MainViewModel**: 12 new tests for parameter management

### Compose UI Tests (25 tests)
- **ParameterSlider**: 13 tests for slider component behavior
- **SettingsBottomSheet**: 12 tests for settings UI integration

## Files Modified

### Production Code
1. `app/src/main/java/com/nervesparks/iris/data/UserPreferencesRespository.kt`
   - Added parameter storage constants and methods
   - Added resetParametersToDefaults() method

2. `app/src/main/java/com/nervesparks/iris/ui/components/ParameterSlider.kt` (NEW)
   - Reusable parameter slider component
   - Float and Integer variants

3. `app/src/main/java/com/nervesparks/iris/ui/MainChatScreen.kt`
   - Added Model Parameters section to SettingsBottomSheet
   - Integrated preset buttons and parameter sliders

4. `app/src/main/java/com/nervesparks/iris/MainViewModel.kt`
   - Added parameter get/set methods
   - Added preset application logic
   - Updated load() to use stored parameters
   - Added ParameterPreset enum

### Test Code
1. `app/src/test/java/com/nervesparks/iris/data/UserPreferencesRepositoryTest.kt`
   - 9 new parameter storage tests

2. `app/src/test/java/com/nervesparks/iris/MainViewModelParametersTest.kt` (NEW)
   - 12 new parameter management tests

3. `app/src/androidTest/java/com/nervesparks/iris/ui/components/ParameterSliderTest.kt` (NEW)
   - 13 new slider component tests

4. `app/src/androidTest/java/com/nervesparks/iris/ui/SettingsParametersTest.kt` (NEW)
   - 12 new settings integration tests

## Technical Notes

### Parameter Application
- Parameters are read from UserPreferencesRepository when loading a model
- Stored parameters persist across app restarts
- Changes take effect on next model load (not immediately during inference)

### Context Length
- Currently displayed in UI but fixed at 4096 in LlamaAndroid
- Slider prepared for future dynamic context length support

### Backward Compatibility
- Default values ensure existing behavior is maintained
- No breaking changes to existing APIs
- Model switching functionality unchanged

## Usage

### For Users
1. Open Settings from the chat screen
2. Scroll to "Model Parameters" section
3. Use preset buttons for quick configuration
4. Or adjust individual sliders for fine control
5. Changes apply automatically on next model load

### For Developers
```kotlin
// Get parameter values
val temperature = viewModel.getTemperature()
val topP = viewModel.getTopP()
val topK = viewModel.getTopK()

// Set parameter values
viewModel.setTemperature(1.2f)
viewModel.setTopP(0.85f)
viewModel.setTopK(50)

// Apply presets
viewModel.applyParameterPreset(ParameterPreset.CONSERVATIVE)

// Reset to defaults
viewModel.resetParametersToDefaults()
```

## Security & Privacy
- No telemetry added
- All data stored locally in SharedPreferences
- No network communication for parameter management
- Privacy posture maintained

## Future Enhancements
- Dynamic context length support (when LlamaAndroid supports it)
- Advanced parameter profiles with custom names
- Parameter validation with visual feedback
- Export/import parameter configurations
