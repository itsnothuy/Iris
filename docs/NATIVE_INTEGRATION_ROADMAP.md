# Native Integration Roadmap for Issue #8.75

## Overview
This document outlines the step-by-step implementation plan for integrating native multimodal capabilities (vision, speech-to-text, text-to-speech) into iris_android.

## Current State Assessment

### ✅ Completed Infrastructure
- **Module Architecture**: Clean separation with core-multimodal module
- **Interface Definitions**: VisionProcessingEngine, SpeechToTextEngine, TextToSpeechEngine
- **Mock Implementations**: Working placeholders for development/testing
- **Audio Processing**: AudioProcessor with real-time pipeline
- **Image Processing**: ImageProcessor with format conversion and preprocessing
- **Dependency Injection**: Hilt setup complete
- **Hardware Abstraction**: DeviceProfileProvider for capability detection

### ⚠️ Pending Integration Points

**VisionProcessingEngineImpl.kt** (core-multimodal/src/main/kotlin/.../vision/):
```kotlin
// Line 65: TODO: Integrate with native inference engine for actual model loading
// Line 158: TODO: Integrate with native inference engine for actual vision processing
// Line 184: TODO: Call native inference engine to unload model
```

**SpeechToTextEngineImpl.kt** (core-multimodal/src/main/kotlin/.../voice/):
```kotlin
// Mock transcription returns placeholder text instead of real ASR
// Needs: nativeTranscribeAudio() implementation via whisper.cpp
```

**TextToSpeechEngineImpl.kt** (core-multimodal/src/main/kotlin/.../voice/):
```kotlin
// Synthetic audio generation instead of neural TTS
// Needs: nativeSynthesizeSpeech() implementation via Piper
```

## Implementation Phases

### Phase 1: Build System & Infrastructure ✅ IN PROGRESS

#### 1.1 Gradle Configuration
- [x] Add KAPT stability configuration to gradle.properties
- [ ] Update core-multimodal/build.gradle.kts with NDK support
- [ ] Add CMake external native build configuration
- [ ] Configure ABI filters for ARM64/ARM32

#### 1.2 Directory Structure
```bash
core-multimodal/
├── src/
│   ├── main/
│   │   ├── kotlin/      # Existing Kotlin code
│   │   ├── cpp/         # Native C++ code (to be created)
│   │   │   ├── CMakeLists.txt
│   │   │   ├── llama.cpp/        # Git submodule
│   │   │   ├── whisper.cpp/      # Git submodule
│   │   │   ├── piper/            # Git submodule
│   │   │   ├── llava_android.cpp
│   │   │   ├── whisper_android.cpp
│   │   │   ├── piper_android.cpp
│   │   │   └── jni_utils.h       # Common JNI helpers
│   │   └── assets/
│   │       └── models/           # Model metadata (not actual models)
│   │           └── multimodal_models.json
│   └── test/            # Existing tests
```

#### 1.3 Git Submodules
```bash
# Commands to add submodules (requires git access):
cd core-multimodal/src/main/cpp/

# LLaVA integration (llama.cpp with vision support)
git submodule add https://github.com/ggerganov/llama.cpp.git
cd llama.cpp && git checkout tags/b3259 && cd ..

# Whisper.cpp for STT
git submodule add https://github.com/ggerganov/whisper.cpp.git
cd whisper.cpp && git checkout tags/v1.5.4 && cd ..

# Piper for TTS
git submodule add https://github.com/rhasspy/piper.git
cd piper && git checkout tags/v1.2.0 && cd ..
```

### Phase 2: Vision Integration (LLaVA)

#### 2.1 Native JNI Bridge
**File**: `core-multimodal/src/main/cpp/llava_android.cpp`

Key Functions:
- `nativeLoadVisionModel(modelPath, mmprojPath)` → Returns context pointer
- `nativeProcessImage(contextPtr, imageData, prompt)` → Returns description
- `nativeUnloadVisionModel(contextPtr)` → Frees resources

#### 2.2 Kotlin Integration
**File**: `VisionProcessingEngineImpl.kt`

Replace TODO sections with:
```kotlin
// Native method declarations
private external fun nativeLoadVisionModel(modelPath: String, mmprojPath: String): Long
private external fun nativeProcessImage(contextPtr: Long, imageData: ByteArray, prompt: String): String?
private external fun nativeUnloadVisionModel(contextPtr: Long)

// Static initializer
companion object {
    init {
        System.loadLibrary("iris_multimodal")
    }
}
```

#### 2.3 Model Management
- Add LLaVA model descriptors to multimodal_models.json
- Implement progressive download strategy
- Add model verification (SHA256 checksums)
- Implement fallback to mock mode if model unavailable

### Phase 3: Speech-to-Text Integration (Whisper.cpp)

#### 3.1 Native JNI Bridge
**File**: `core-multimodal/src/main/cpp/whisper_android.cpp`

Key Functions:
- `nativeLoadWhisperModel(modelPath)` → Returns context pointer
- `nativeTranscribeAudio(contextPtr, audioData, language)` → Returns transcript
- `nativeUnloadWhisperModel(contextPtr)` → Frees resources

#### 3.2 Kotlin Integration
**File**: `SpeechToTextEngineImpl.kt`

Replace mock transcription in `processFinalAudio()`:
```kotlin
private suspend fun processFinalAudio(audioBuffer: List<FloatArray>): TranscriptionResult {
    return withContext(Dispatchers.Default) {
        try {
            val combinedAudio = combineAudioChunks(audioBuffer)
            val transcript = nativeTranscribeAudio(
                nativeModelContext,
                combinedAudio,
                currentSTTModel?.language ?: "en"
            ) ?: "Unable to transcribe audio"
            
            TranscriptionResult(
                text = transcript,
                confidence = calculateConfidence(combinedAudio),
                language = currentSTTModel?.language ?: "en",
                processingTimeMs = System.currentTimeMillis() - startTime,
                segments = emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            TranscriptionResult(/* ... error result ... */)
        }
    }
}
```

#### 3.3 Model Options
- **whisper-tiny**: 75 MB, fast, lower accuracy
- **whisper-base**: 145 MB, balanced
- **whisper-small**: 466 MB, high accuracy
- Start with tiny, allow user upgrade

### Phase 4: Text-to-Speech Integration (Piper)

#### 4.1 Native JNI Bridge
**File**: `core-multimodal/src/main/cpp/piper_android.cpp`

Key Functions:
- `nativeLoadPiperModel(modelPath, configPath)` → Returns voice pointer
- `nativeSynthesizeSpeech(voicePtr, text)` → Returns audio samples
- `nativeUnloadPiperModel(voicePtr)` → Frees resources

#### 4.2 Kotlin Integration
**File**: `TextToSpeechEngineImpl.kt`

Replace synthetic audio generation:
```kotlin
override suspend fun synthesizeSpeech(
    text: String,
    parameters: SpeechParameters
): Result<AudioData> = withContext(Dispatchers.IO) {
    if (!isTTSModelLoaded || nativeModelContext == 0L) {
        return@withContext Result.failure(VoiceException("No TTS model loaded"))
    }
    
    try {
        val audioSamples = nativeSynthesizeSpeech(nativeModelContext, text)
            ?: return@withContext Result.failure(VoiceException("Speech synthesis failed"))
        
        val processedSamples = if (parameters.pitch != 1.0f || parameters.speakingRate != 1.0f) {
            applyVoiceEffects(audioSamples, parameters)
        } else {
            audioSamples
        }
        
        Result.success(AudioData.Chunk(processedSamples, System.currentTimeMillis()))
    } catch (e: Exception) {
        Result.failure(VoiceException("Speech synthesis failed", e))
    }
}
```

#### 4.3 Voice Options
- **jenny_low**: 18 MB, lightweight, clear
- **lessac_medium**: 35 MB, more natural
- **libritts_medium**: 45 MB, high quality

### Phase 5: Model Asset Management

#### 5.1 Download Strategy
**File**: `core-multimodal/src/main/kotlin/.../ModelDownloadManager.kt`

```kotlin
@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun downloadModel(
        model: MultimodalModelDescriptor,
        progressCallback: (Float) -> Unit
    ): Result<File> {
        // Progressive download with resume support
        // Store in: context.getExternalFilesDir("models")/{type}/{model-id}/
    }
    
    suspend fun verifyModel(modelFile: File, expectedChecksum: String): Boolean {
        // SHA256 verification
    }
    
    suspend fun getStorageForecast(models: List<MultimodalModelDescriptor>): StorageInfo {
        // Calculate required space, check availability
    }
}
```

#### 5.2 Background Loading Service
**File**: `app/src/main/kotlin/.../BackgroundModelService.kt`

```kotlin
@Service
class BackgroundModelService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val modelId = intent?.getStringExtra("model_id") ?: return START_NOT_STICKY
        
        serviceScope.launch {
            loadModelInBackground(modelId)
        }
        
        return START_STICKY
    }
    
    private suspend fun loadModelInBackground(modelId: String) {
        // Download if needed → Load into memory → Send broadcast
    }
}
```

#### 5.3 Smart Caching
**File**: `core-multimodal/src/main/kotlin/.../ModelCacheManager.kt`

```kotlin
@Singleton
class ModelCacheManager @Inject constructor(
    private val deviceProfileProvider: DeviceProfileProvider
) {
    private val lruCache = LruCache<String, CachedModel>(MAX_CACHED_MODELS)
    
    suspend fun preloadRecommendedModels() {
        // Based on device RAM, usage patterns, preload likely models
    }
    
    private fun calculatePriority(model: ModelDescriptor): Int {
        // Usage frequency + compatibility score + recency
    }
}
```

### Phase 6: Testing Strategy

#### 6.1 Unit Tests
- Mock native methods for fast tests
- Test model loading state machine
- Test error handling and fallbacks
- Test audio/image preprocessing pipelines

#### 6.2 Integration Tests
- Requires actual models on test devices
- Test full vision pipeline with sample images
- Test full voice pipeline with sample audio
- Test model switching and memory management

#### 6.3 Performance Tests
- Model loading time benchmarks (<15s target)
- Inference latency benchmarks (<100ms target)
- Memory usage tracking (<500MB per model)
- Thermal impact monitoring

### Phase 7: CI/CD Integration

#### 7.1 GitHub Actions Updates
```yaml
- name: Setup Android NDK
  uses: nttld/setup-ndk@v1
  with:
    ndk-version: r27b

- name: Initialize Submodules
  run: git submodule update --init --recursive

- name: Build Native Libraries
  run: ./gradlew :core-multimodal:assembleDebug

- name: Run Native Tests
  run: ./gradlew :core-multimodal:testDebugUnitTest
```

#### 7.2 Model Download in CI
- Use small test models (whisper-tiny, dummy vision model)
- Cache downloaded models between runs
- Skip model download on lint-only jobs

## Risk Mitigation

### Risk: Build Complexity
**Mitigation**: 
- Comprehensive CMakeLists.txt documentation
- Build troubleshooting guide in docs/
- Pre-built binary distribution for developers

### Risk: Model Size
**Mitigation**:
- Progressive download (don't bundle in APK)
- User consent before download
- Clear storage requirements display
- Model deletion option

### Risk: Native Crashes
**Mitigation**:
- Extensive error handling in JNI layer
- Crash reporting integration (Firebase Crashlytics)
- Fallback to mock mode on native failure
- User-facing error messages

### Risk: Platform Fragmentation
**Mitigation**:
- Target ARM64 primarily (95% of Android devices)
- ARM32 as secondary (legacy support)
- CPU-only mode for maximum compatibility
- Clear device compatibility messaging

## Success Criteria

- [ ] All native libraries build successfully across CI environments
- [ ] Vision processing produces accurate image descriptions (>70% relevance)
- [ ] STT transcribes speech with <10% WER (Word Error Rate)
- [ ] TTS produces natural-sounding audio (>3.0 MOS - Mean Opinion Score)
- [ ] Model loading completes in <15 seconds
- [ ] Inference latency <100ms per request
- [ ] Zero native crashes in 1000 inference cycles
- [ ] Memory usage stays within device thermal limits
- [ ] All tests pass on physical devices (Pixel 6+, Galaxy S21+)

## References
- [ADR-0002: Native Multimodal Integration Strategy](adr/0002-native-multimodal-integration-strategy.md)
- [Issue #8.75: Production Quality Consolidation](https://github.com/itsnothuy/Iris/issues/8.75)
- [docs/architecture.md - Section 8: Multimodal Processing](architecture.md)
- [llama.cpp Android Example](https://github.com/ggerganov/llama.cpp/tree/master/examples/llama.android)
- [whisper.cpp Android Example](https://github.com/ggerganov/whisper.cpp/tree/master/examples/whisper.android)
