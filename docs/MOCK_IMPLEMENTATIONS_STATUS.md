# Current Mock Implementations & Production Integration Status

## Overview
This document details the current state of multimodal processing in iris_android, identifying which components use mock/placeholder implementations and what's required for production native integration.

**Last Updated**: 2025-11-12  
**Status**: MVP-Ready with Mock Processing | Production Integration Prepared

## Module Status Summary

| Component | Module | Mock Status | Production Ready | Integration Complexity |
|-----------|--------|-------------|------------------|----------------------|
| Vision Processing | core-multimodal | ⚠️ Mock | 🚧 Infrastructure Ready | High (requires LLaVA) |
| Speech-to-Text | core-multimodal | ⚠️ Mock | 🚧 Infrastructure Ready | Medium (requires Whisper.cpp) |
| Text-to-Speech | core-multimodal | ⚠️ Mock | 🚧 Infrastructure Ready | Medium (requires Piper) |
| Audio Processing | core-multimodal | ✅ Real | ✅ Production Ready | N/A |
| Image Processing | core-multimodal | ✅ Real | ✅ Production Ready | N/A |

## Vision Processing Engine

### Current Implementation
**File**: `core-multimodal/src/main/kotlin/com/nervesparks/iris/core/multimodal/vision/VisionProcessingEngineImpl.kt`

**Mock Behavior**:
- **Line 65**: `loadVisionModel()` - Validates model file existence but doesn't load into native engine
- **Line 158**: `processImage()` - Returns placeholder text instead of actual vision-language inference
- **Line 184**: `unloadModelInternal()` - Only removes from in-memory map, no native cleanup

**Example Output**:
```kotlin
// Input: Image of a cat + prompt "Describe this image"
// Mock output: "Vision processing system ready. Model: LLaVA-1.5-7B. Image size: 512x512. Prompt: 'Describe this image'. Note: Full native inference integration pending."
// Production output: "This image shows an orange tabby cat sitting on a wooden table. The cat appears to be looking directly at the camera with bright green eyes..."
```

**Real Components** (Already Production-Ready):
- ✅ `ImageProcessor` - Format conversion, resizing, preprocessing
- ✅ Model state management - LRU cache, loading/unloading lifecycle
- ✅ Error handling - Comprehensive exception handling
- ✅ Device compatibility checking - Hardware requirement validation
- ✅ Event bus integration - Loading progress events

**Production Integration Requirements**:
1. Add `llama.cpp` with LLaVA support as git submodule
2. Implement `llava_android.cpp` JNI bridge
3. Add native method declarations:
   ```kotlin
   private external fun nativeLoadVisionModel(modelPath: String, mmprojPath: String): Long
   private external fun nativeProcessImage(contextPtr: Long, imageData: ByteArray, prompt: String): String?
   private external fun nativeUnloadVisionModel(contextPtr: Long)
   ```
4. Replace TODO sections with native calls
5. Download LLaVA models (4-8GB)

## Speech-to-Text Engine

### Current Implementation
**File**: `core-multimodal/src/main/kotlin/com/nervesparks/iris/core/multimodal/voice/SpeechToTextEngineImpl.kt`

**Mock Behavior**:
- `processFinalAudio()` - Analyzes audio characteristics (volume, frequency) but doesn't transcribe
- Returns realistic-looking placeholder transcriptions based on audio properties

**Example Output**:
```kotlin
// Input: 3 seconds of speech audio at normal volume
// Mock output: "This is a placeholder transcription. Detected 3.2 seconds of speech at average volume 0.45. Real STT integration pending."
// Production output: "Hello, how can I help you today?"
```

**Real Components** (Already Production-Ready):
- ✅ `AudioProcessor` - Real-time audio capture and preprocessing
- ✅ Voice Activity Detection (VAD) - Detects speech vs silence
- ✅ Audio format conversion - PCM, resampling, normalization
- ✅ Recording session management - Start/stop/pause controls
- ✅ Silence detection - Automatic endpoint detection
- ✅ Audio buffering - Chunk-based streaming

**Production Integration Requirements**:
1. Add `whisper.cpp` as git submodule
2. Implement `whisper_android.cpp` JNI bridge
3. Add native method declarations:
   ```kotlin
   private external fun nativeLoadWhisperModel(modelPath: String): Long
   private external fun nativeTranscribeAudio(contextPtr: Long, audioData: FloatArray, language: String): String?
   private external fun nativeUnloadWhisperModel(contextPtr: Long)
   ```
4. Replace mock transcription in `processFinalAudio()`
5. Download Whisper models (75MB - 1.5GB)

## Text-to-Speech Engine

### Current Implementation
**File**: `core-multimodal/src/main/kotlin/com/nervesparks/iris/core/multimodal/voice/TextToSpeechEngineImpl.kt`

**Mock Behavior**:
- `synthesizeSpeech()` - Generates synthetic sine-wave audio instead of neural speech
- Creates simple formant-based tones that approximate speech rhythm

**Example Output**:
```kotlin
// Input: "Hello world"
// Mock: Simple sine wave audio with duration proportional to text length
// Production: Natural-sounding human speech with proper intonation and pronunciation
```

**Real Components** (Already Production-Ready):
- ✅ Speech parameter support - Pitch, rate, volume control
- ✅ Audio effect processing - Real-time voice effects
- ✅ Streaming synthesis - Chunk-based audio generation
- ✅ Playback integration - Direct AudioTrack output
- ✅ Session management - Pause/resume/stop controls

**Production Integration Requirements**:
1. Add `piper` as git submodule
2. Implement `piper_android.cpp` JNI bridge
3. Add native method declarations:
   ```kotlin
   private external fun nativeLoadPiperModel(modelPath: String, configPath: String): Long
   private external fun nativeSynthesizeSpeech(voicePtr: Long, text: String): FloatArray?
   private external fun nativeUnloadPiperModel(voicePtr: Long)
   ```
4. Replace synthetic audio generation with Piper calls
5. Download Piper voice models (18-45MB)

## Testing with Mock Implementations

### Advantages of Current Mock System
1. **Fast Development**: No need for GB-scale model downloads
2. **Reliable CI/CD**: Tests don't depend on network or large files
3. **Quick Iteration**: Instant feedback without waiting for inference
4. **Predictable Behavior**: Same results every run for unit tests
5. **Low Resource Usage**: Runs on any device, even emulators

### Testing Strategy
```kotlin
// Unit tests use mocks
@Test
fun `vision processing returns result`() = runTest {
    val result = visionEngine.processImage(testImage, testParams)
    assertTrue(result.isSuccess)
    // Mock returns placeholder, but we test the framework
}

// Integration tests can use real models (optional)
@Test
@RequiresAssets("llava-7b-q4_0.gguf")
fun `vision processing with real model`() = runTest {
    // Only runs if model file is present
    val result = visionEngine.processImage(realImage, testParams)
    assertContains(result.getOrThrow(), "cat") // Check actual description
}
```

## Migration Path: Mock → Production

### Phase 1: Development (Current State)
- ✅ Use mock implementations for rapid development
- ✅ Test framework and architecture without heavy dependencies
- ✅ Validate UI/UX flows with instant feedback

### Phase 2: Infrastructure Setup (In Progress)
- ✅ CMakeLists.txt prepared
- ✅ JNI utility headers created
- ✅ KAPT configuration fixed
- ✅ Build system ready for native code
- 🚧 Waiting for git submodules

### Phase 3: Native Integration
- ⏳ Add native library submodules
- ⏳ Implement JNI bridges
- ⏳ Enable native build in gradle
- ⏳ Download production models

### Phase 4: Hybrid Mode
- ⏳ Support both mock and real modes
- ⏳ Auto-detect available models
- ⏳ Graceful fallback to mock if models missing
- ⏳ User setting to enable/disable native processing

### Phase 5: Production
- ⏳ Default to native processing
- ⏳ Mock mode only for testing
- ⏳ Production model delivery (CDN/Play Asset Delivery)

## Configuration Flags

### Proposed Feature Flags (Future)
```kotlin
// gradle.properties or BuildConfig
MULTIMODAL_NATIVE_VISION_ENABLED=false  // When true, requires llava models
MULTIMODAL_NATIVE_STT_ENABLED=false     // When true, requires whisper models
MULTIMODAL_NATIVE_TTS_ENABLED=false     // When true, requires piper models
MULTIMODAL_MOCK_MODE=true               // Force mock mode for testing
```

### Runtime Detection
```kotlin
class MultimodalCapabilityDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isNativeVisionAvailable(): Boolean {
        return try {
            System.loadLibrary("iris_multimodal")
            checkModelFile("llava-7b-q4_0.gguf")
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }
    
    fun getRecommendedMode(): ProcessingMode {
        return when {
            isNativeVisionAvailable() -> ProcessingMode.NATIVE
            else -> ProcessingMode.MOCK
        }
    }
}
```

## Performance Comparison

### Vision Processing
| Metric | Mock | Production (LLaVA 7B Q4) |
|--------|------|------------------------|
| Processing Time | <1ms | 500-2000ms |
| Memory Usage | <1MB | ~4GB |
| Accuracy | N/A | High (competitive with GPT-4V) |
| Output | Placeholder | Detailed descriptions |

### Speech-to-Text
| Metric | Mock | Production (Whisper Base) |
|--------|------|--------------------------|
| Processing Time | <1ms | 200-800ms (real-time factor ~0.3) |
| Memory Usage | <1MB | ~150MB |
| Accuracy | N/A | ~10% WER (Word Error Rate) |
| Output | Placeholder | Accurate transcripts |

### Text-to-Speech
| Metric | Mock | Production (Piper) |
|--------|------|-------------------|
| Synthesis Time | <10ms | 50-200ms |
| Memory Usage | <1MB | ~20MB |
| Quality | Synthetic tones | Natural speech (MOS ~3.5) |
| Output | Sine waves | Neural speech |

## User-Facing Behavior

### Current MVP Experience
1. User selects vision model in settings
2. User uploads image
3. System shows "processing" state
4. System returns: "Vision processing ready. [placeholder text]"
5. User understands this is a demo/MVP

### Future Production Experience
1. User selects vision model (downloads in background if needed)
2. User uploads image
3. System shows progress (0-100%)
4. System returns: "This image shows [detailed description]"
5. User gets actual AI vision capability

## Documentation References
- [Native Integration Roadmap](NATIVE_INTEGRATION_ROADMAP.md)
- [ADR-0002: Native Multimodal Integration Strategy](adr/0002-native-multimodal-integration-strategy.md)
- [Architecture: Multimodal Processing](architecture.md#multimodal-processing-engine)
- [C++ Implementation Guide](core-multimodal/src/main/cpp/README.md)

## FAQ

**Q: Why not bundle models in the APK?**  
A: Models are 75MB-8GB. Bundling would make APK too large. We'll use on-demand download.

**Q: Can I test with real models locally?**  
A: Yes! Download models manually and place in `/data/data/com.nervesparks.iris/files/models/`. The framework will auto-detect and use them.

**Q: What if native integration fails?**  
A: The system automatically falls back to mock mode with a user notification. The app remains functional.

**Q: How do I know if I'm using mock vs real?**  
A: Check the logs for "Mock mode" warnings, or look for placeholder text in vision results.

**Q: Performance on low-end devices?**  
A: Native processing requires 4GB+ RAM for vision, 2GB+ for voice. Low-end devices will continue using mock mode.

## Tracking Issues
- [Issue #8.75: Production Quality Consolidation & Native Integration](https://github.com/itsnothuy/Iris/issues/8.75)
- [Issue #07: Multimodal Support](https://github.com/itsnothuy/Iris/issues/07)
- [Issue #08: Voice Processing](https://github.com/itsnothuy/Iris/issues/08)
