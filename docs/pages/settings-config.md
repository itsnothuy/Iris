# Settings & Configuration Specification

## Business Goals

Provide comprehensive configuration options that allow users to customize their AI experience while maintaining sensible defaults. Enable both casual users to get started quickly and power users to fine-tune performance for their specific needs and device capabilities.

**Primary Objectives**:
- Offer intuitive configuration options for model behavior and performance
- Provide clear explanations for technical parameters
- Enable device-specific optimizations
- Maintain user preferences across app sessions

## User Stories & Acceptance Tests

### Epic: Model Parameters

**US-016: Basic Parameter Control**
- *As a user, I want to adjust how creative or focused the AI responses are*
- **AC1**: Temperature slider with real-time preview of effect (0.1-2.0 range)
- **AC2**: Top-p slider for nucleus sampling control (0.1-1.0 range)
- **AC3**: Preset configurations for common use cases (Creative, Balanced, Precise)
- **AC4**: Reset to defaults button with confirmation
- **AC5**: Parameter changes apply to new conversations immediately

**US-017: Advanced Parameters**
- *As a power user, I want fine-grained control over model behavior*
- **AC1**: Context length adjustment within model limits
- **AC2**: Repetition penalty configuration to reduce repetitive outputs
- **AC3**: Top-k sampling parameter for response diversity
- **AC4**: Batch size configuration for performance tuning
- **AC5**: Advanced parameters hidden by default, accessible via toggle

**US-018: Performance Tuning**
- *As a user, I want to optimize performance for my device*
- **AC1**: Thread count adjustment based on device CPU cores
- **AC2**: Memory allocation preferences (conservative/balanced/aggressive)
- **AC3**: GPU acceleration toggle where supported
- **AC4**: Thermal throttling sensitivity adjustment
- **AC5**: Performance mode presets (Battery Saver, Balanced, Performance)

### Epic: User Interface Preferences

**US-019: Appearance Customization**
- *As a user, I want to customize the app appearance to my preferences*
- **AC1**: Dark/light/system theme selection
- **AC2**: Font size adjustment for conversation display
- **AC3**: Message bubble style options
- **AC4**: Color scheme customization for accessibility
- **AC5**: Animation preferences (reduced motion support)

**US-020: Chat Behavior**
- *As a user, I want to control how conversations behave*
- **AC1**: Auto-scroll to latest message toggle
- **AC2**: Message timestamps display options
- **AC3**: Conversation auto-save frequency settings
- **AC4**: Maximum conversation length before archiving
- **AC5**: New conversation behavior (clear vs. start fresh)

**US-021: Input Preferences**
- *As a user, I want to customize input methods and behavior*
- **AC1**: Voice input language selection
- **AC2**: Send button behavior (always visible vs. auto-hide)
- **AC3**: Enter key behavior (send vs. new line)
- **AC4**: Auto-correction and suggestions toggle
- **AC5**: Clipboard access preferences

### Epic: Privacy & Security

**US-022: Data Management**
- *As a user, I want control over my conversation data*
- **AC1**: Conversation history retention period settings
- **AC2**: Clear all conversations with confirmation
- **AC3**: Export conversations to text/markdown files
- **AC4**: Automatic cleanup preferences for old conversations
- **AC5**: Storage encryption toggle with security implications explained

**US-023: Privacy Controls**
- *As a user, I want to understand and control privacy settings*
- **AC1**: Data collection explanation (none for this app)
- **AC2**: Local processing confirmation and benefits
- **AC3**: Network permissions and usage transparency
- **AC4**: Crash reporting preferences (local only)
- **AC5**: Analytics opt-out (not applicable but clearly stated)

### Epic: System Integration

**US-024: Accessibility Settings**
- *As a user with accessibility needs, I want appropriate accommodations*
- **AC1**: Screen reader compatibility settings
- **AC2**: High contrast mode for visual impairments
- **AC3**: Large text support beyond system settings
- **AC4**: Voice control sensitivity adjustments
- **AC5**: Haptic feedback preferences

**US-025: Notifications & Alerts**
- *As a user, I want control over system notifications*
- **AC1**: Model download completion notifications
- **AC2**: Low storage warning preferences
- **AC3**: Performance alert settings
- **AC4**: Background processing notifications
- **AC5**: Quiet hours for non-critical notifications

## UI States & Navigation

### Settings Navigation Structure

```mermaid
graph TD
    A[Settings Home] --> B[Model Parameters]
    A --> C[Appearance]
    A --> D[Chat Behavior]
    A --> E[Privacy & Data]
    A --> F[Performance]
    A --> G[Accessibility]
    A --> H[About]
    
    B --> B1[Basic Parameters]
    B --> B2[Advanced Parameters]
    B --> B3[Performance Tuning]
    
    C --> C1[Theme Settings]
    C --> C2[Font & Display]
    C --> C3[Animations]
    
    D --> D1[Input Preferences]
    D --> D2[Conversation Settings]
    
    E --> E1[Data Management]
    E --> E2[Privacy Controls]
    
    F --> F1[CPU Settings]
    F --> F2[Memory Management]
    F --> F3[Thermal Controls]
```

### Settings Screen Layouts

**Settings Home**:
- Categorized list of setting groups
- Quick toggles for frequently used settings
- Search functionality for finding specific options
- Current active model display with quick switch

**Parameter Configuration**:
- Slider controls with real-time preview
- Preset buttons for common configurations
- Description text explaining each parameter's effect
- Reset to defaults option

**Data Management**:
- Storage usage visualization
- Conversation count and total storage
- Export and cleanup options
- Privacy policy and data handling explanation

### State Management

```kotlin
data class SettingsUiState(
    val modelParameters: ModelParameters,
    val uiPreferences: UiPreferences,
    val privacySettings: PrivacySettings,
    val performanceConfig: PerformanceConfig,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ModelParameters(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,
    val contextLength: Int = 2048,
    val batchSize: Int = 8
)

data class UiPreferences(
    val theme: Theme = Theme.SYSTEM,
    val fontSize: FontSize = FontSize.MEDIUM,
    val autoScroll: Boolean = true,
    val showTimestamps: Boolean = true,
    val animationsEnabled: Boolean = true
)
```

## Data Flow & Boundaries

### Settings Persistence Flow

```mermaid
sequenceDiagram
    participant UI as Settings Screen
    participant VM as SettingsViewModel
    participant Repo as SettingsRepository
    participant DS as DataStore
    participant Config as ConfigManager
    
    UI->>VM: updateSetting(key, value)
    VM->>Repo: saveSetting(key, value)
    Repo->>DS: writePreference(key, value)
    DS->>Repo: confirmSaved()
    Repo->>Config: notifyConfigChange(key, value)
    Config-->>VM: configUpdated()
    VM-->>UI: settingUpdated(newState)
```

### Parameter Application Flow

```mermaid
sequenceDiagram
    participant Settings as Settings Screen
    participant VM as SettingsViewModel
    participant ChatVM as ChatViewModel
    participant Engine as LLaMA Engine
    
    Settings->>VM: updateTemperature(0.8)
    VM->>ChatVM: applyNewParameters(params)
    ChatVM->>Engine: updateConfig(temperature=0.8)
    Engine->>ChatVM: configApplied()
    ChatVM-->>Settings: parametersApplied()
```

### Repository Pattern

```kotlin
interface SettingsRepository {
    suspend fun getModelParameters(): ModelParameters
    suspend fun saveModelParameters(params: ModelParameters)
    suspend fun getUiPreferences(): UiPreferences
    suspend fun saveUiPreferences(prefs: UiPreferences)
    suspend fun exportConversations(): Uri
    suspend fun clearAllData()
    fun observeSettingsChanges(): Flow<SettingsChange>
}

sealed class SettingsChange {
    data class ModelParameterChange(val params: ModelParameters) : SettingsChange()
    data class UiPreferenceChange(val prefs: UiPreferences) : SettingsChange()
    data class PerformanceConfigChange(val config: PerformanceConfig) : SettingsChange()
}
```

## Non-Functional Requirements

### Performance

- **Setting Changes**: Apply immediately without app restart
- **Parameter Validation**: Real-time validation with helpful error messages
- **Search Functionality**: Fast filtering across all settings
- **Persistence**: Changes saved immediately to prevent data loss

### Usability

- **Discoverability**: Settings organized logically with clear categorization
- **Explanations**: Technical parameters explained in user-friendly language
- **Defaults**: Sensible defaults that work well for most users
- **Recovery**: Easy reset options for misconfigured settings

### Compatibility

- **Device Adaptation**: Parameter limits adjusted based on device capabilities
- **Android Versions**: Graceful degradation on older Android versions
- **Screen Sizes**: Responsive layout for phones and tablets
- **Accessibility**: Full compatibility with assistive technologies

### Data Integrity

- **Validation**: All parameter values validated before application
- **Backup**: Settings backed up to prevent loss
- **Migration**: Smooth migration between app versions
- **Consistency**: Settings synchronized across app components

## Test Plan

### Unit Tests

**Settings Repository Tests**:
```kotlin
@Test
fun modelParameters_validation_rejects_invalid_values() {
    // Test parameter validation logic
}

@Test
fun settings_persistence_maintains_data_integrity() {
    // Test data storage and retrieval
}

@Test
fun settings_changes_propagate_to_dependent_components() {
    // Test change notification system
}
```

**ViewModel Tests**:
```kotlin
@Test
fun parameter_changes_update_ui_state_correctly() {
    // Test state management
}

@Test
fun invalid_settings_show_appropriate_error_messages() {
    // Test error handling
}

@Test
fun reset_to_defaults_restores_original_configuration() {
    // Test reset functionality
}
```

### UI Tests

**Settings Navigation Tests**:
```kotlin
@Test
fun settings_categories_navigate_correctly() {
    // Test navigation between setting screens
}

@Test
fun search_functionality_finds_relevant_settings() {
    // Test settings search
}
```

**Parameter Adjustment Tests**:
```kotlin
@Test
fun sliders_update_values_with_proper_precision() {
    // Test slider controls
}

@Test
fun preset_buttons_apply_correct_configurations() {
    // Test preset functionality
}

@Test
fun parameter_changes_show_immediate_feedback() {
    // Test real-time updates
}
```

### Integration Tests

**Settings Application**:
- Parameter changes applied to inference engine
- UI preferences reflected across app screens
- Performance settings impact model behavior
- Privacy settings control data handling

**Data Management**:
- Conversation export functionality
- Data cleanup operations
- Settings backup and restore
- Migration between app versions

### Accessibility Tests

**Screen Reader Support**:
- All settings announced clearly
- Slider values communicated effectively
- Navigation flow logical for assistive technology

**Visual Accessibility**:
- High contrast mode effectiveness
- Large text scaling support
- Color contrast compliance

## Telemetry **NOT** Collected

Consistent with privacy-first approach:

❌ **Setting Preferences**: No data on which settings users choose  
❌ **Parameter Values**: No information about model parameter configurations  
❌ **Usage Patterns**: No tracking of settings access frequency  
❌ **Device Performance**: No hardware capability or performance data  
❌ **Error Events**: No crash or error reporting with personal context  
❌ **Feature Usage**: No analytics on which features are used  

**Local Configuration Only**: All settings remain on-device for user privacy.

## Merge Checklist

### Development Complete
- [ ] All settings categories implemented with full functionality
- [ ] Parameter validation working for all model settings
- [ ] UI preferences apply correctly across app
- [ ] Data export and cleanup features operational
- [ ] Performance settings integrate with inference engine

### Code Quality
- [ ] Settings repository properly abstracts data storage
- [ ] Parameter validation comprehensive and user-friendly
- [ ] Change notification system reliable
- [ ] Error handling provides actionable feedback
- [ ] Settings migration strategy implemented

### UI/UX Standards
- [ ] Settings organized logically with clear navigation
- [ ] All technical parameters explained clearly
- [ ] Slider controls responsive and precise
- [ ] Search functionality fast and accurate
- [ ] Reset options clearly labeled and confirmed

### Testing & Validation
- [ ] Unit tests cover settings logic (≥80% coverage)
- [ ] UI tests validate all interactive elements
- [ ] Integration tests verify settings application
- [ ] Accessibility testing with assistive technologies
- [ ] Performance testing for settings changes

### Security & Privacy
- [ ] No sensitive data transmitted externally
- [ ] Local storage properly secured
- [ ] Data export/import secure and validated
- [ ] Privacy implications clearly communicated

### Performance
- [ ] Settings changes apply without delays
- [ ] Large settings lists perform smoothly
- [ ] Search functionality responsive
- [ ] Memory usage minimal for settings storage

---

*Specification Version: 1.0*  
*Last Updated: October 2025*  
*Implementation Target: Milestone 1*

## Implementation Notes (MVP 5)

**Model Switch Handoff Implementation** - *Completed: October 2025*

### Overview
Implemented model switching functionality with UI state management to allow users to switch between downloaded AI models while ensuring in-flight requests complete on the old model before switching.

### Changes Made

#### 1. ViewModel State Management (`MainViewModel.kt`)
- Added `isSwitchingModel` state to track model switching in progress
- Added `_pendingModelSwitch` to defer model switches when a request is in-flight
- Implemented `switchModel()` method that:
  - Checks if a request is currently being processed (`getIsSending()`)
  - If sending, defers the switch until the request completes
  - If not sending, switches immediately
- Implemented `performModelSwitch()` to execute the actual model switch operation
- Added `checkAndExecutePendingModelSwitch()` called after send operations complete
- Updates default model preference after successful switch

#### 2. Settings UI (`MainChatScreen.kt`)
Added model selection section to `SettingsBottomSheet`:
- **Model dropdown**: Shows all downloaded models with current model highlighted
- **Active indicator**: Displays currently active model with checkmark
- **Switching feedback**: Shows "Switching model..." message during switch
- **Filtering**: Only shows models that exist in the device storage
- **Color coding**: Current model highlighted in purple, others in white

#### 3. Chat Header Chip (`MainChatScreen.kt`)
Added active model indicator at top of chat screen:
- **Model name display**: Shows truncated model name (30 chars + "...")
- **Status indicator**: Color-coded dot (purple=active, orange=switching)
- **Responsive design**: Chip adapts to model name length
- **Minimal visual footprint**: Small, unobtrusive design

#### 4. Unit Tests (`MainViewModelModelSwitchTest.kt`)
Created comprehensive test suite covering:
- Immediate switching when no request in-flight
- Deferred switching when request is in-flight
- Default model preference updates
- Multiple sequential model switches
- Initial state validation
- Thread count application

### Behavior Details

**In-Flight Request Handling**:
1. User initiates model switch while AI is generating a response
2. Switch is deferred and `isSwitchingModel` state set to `true`
3. Current request completes on the old model
4. After completion, `checkAndExecutePendingModelSwitch()` executes the pending switch
5. New model is loaded and subsequent requests use the new model

**UI Feedback**:
- Settings dropdown shows current model with checkmark and highlight
- Chat header chip shows orange dot during switch, purple when active
- "Switching model..." text appears in settings during switch operation

### Testing Coverage
- ✅ Unit tests for model switching logic (8 test cases)
- ✅ State management validation
- ✅ In-flight request handling
- ✅ Default preference persistence

### Design Decisions

1. **Deferred switching**: Ensures conversation context and current response integrity by completing in-flight requests before switching
2. **Visual feedback**: Clear indicators in both settings and chat header to show active model and switching state
3. **Minimal UI changes**: Chip design is small and non-intrusive, following existing app design patterns
4. **Filter downloaded only**: Only shows models that exist on device to prevent user confusion

### Future Enhancements
- Add model switching progress bar with percentage
- Show model metadata (size, parameters) in dropdown
- Add model performance metrics in settings
- Implement model preloading for faster switches