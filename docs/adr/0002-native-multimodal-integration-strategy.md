# ADR-0002: Native Multimodal Integration Strategy

## Status
Proposed

## Context
The iris_android MVP currently has placeholder implementations for vision and voice processing (VisionProcessingEngineImpl, SpeechToTextEngineImpl, TextToSpeechEngineImpl) with TODO markers at critical integration points. To achieve production quality, we need to integrate native libraries for:

1. **Vision Processing**: LLaVA (llama.cpp vision extension) for image understanding
2. **Speech-to-Text**: Whisper.cpp for on-device transcription
3. **Text-to-Speech**: Piper TTS for natural speech synthesis

These integrations require:
- C++ JNI bridges
- Native library compilation via CMake/NDK
- Large model file management (1-4GB per model)
- Cross-architecture support (ARM64, ARM32)

## Decision
We will implement native multimodal integration using a **phased, incremental approach**:

### Phase 1: Infrastructure Preparation (Current)
- Update gradle.properties for KAPT stability
- Create CMakeLists.txt infrastructure in core-multimodal
- Add git submodules for native libraries (non-recursive initially)
- Define JNI method signatures without implementation
- Document model asset management strategy

### Phase 2: Vision Integration
- Add llama.cpp with llava support as submodule
- Implement llava_android.cpp JNI bridge
- Replace VisionProcessingEngineImpl TODOs (lines 65, 158, 184)
- Add model descriptor for LLaVA-1.5-7B-Q4_0
- Implement progressive model loading

### Phase 3: Voice Integration
- Add whisper.cpp as submodule
- Implement whisper_android.cpp JNI bridge
- Replace SpeechToTextEngineImpl mock transcription
- Add piper as submodule
- Implement piper_android.cpp JNI bridge
- Replace TextToSpeechEngineImpl synthetic audio

### Phase 4: Production Polish
- Background model download service
- Smart LRU caching for loaded models
- Offline-first fallback system
- CI/CD integration with native builds

## Architecture Decisions

### JNI Bridge Pattern
```kotlin
// Kotlin side - native method declarations
private external fun nativeLoadVisionModel(modelPath: String, mmproj: String): Long
private external fun nativeProcessImage(ctx: Long, imageData: ByteArray, prompt: String): String?
private external fun nativeUnloadVisionModel(ctx: Long)
```

```cpp
// C++ side - implementation
extern "C" JNIEXPORT jlong JNICALL
Java_com_nervesparks_iris_core_multimodal_vision_VisionProcessingEngineImpl_nativeLoadVisionModel(
    JNIEnv* env, jobject thiz, jstring model_path, jstring mmproj_path) {
    // Load llama.cpp model + CLIP vision encoder
    // Return context pointer as jlong
}
```

### Model Storage Strategy
- **Small models (<500MB)**: Bundle in APK assets for offline-first
- **Medium models (500MB-2GB)**: On-demand download with caching
- **Large models (>2GB)**: Optional download, user-initiated

Models stored in: `/data/data/com.nervesparks.iris/files/models/{type}/{model-id}/`

### Native Library Selection Rationale

| Library | Purpose | Rationale |
|---------|---------|-----------|
| llama.cpp (llava) | Vision-language | Proven mobile inference, GGUF quantization, active development |
| whisper.cpp | Speech-to-text | Official Whisper port, optimized for mobile, multi-language |
| piper | Text-to-speech | Lightweight, high-quality, ONNX models, offline-capable |

## Build System Requirements

### KAPT Configuration (gradle.properties)
```properties
# KAPT stability improvements
kapt.use.worker.api=true
kapt.incremental.apt=true
kapt.include.compile.classpath=false
```

### Native Build Configuration (core-multimodal/build.gradle.kts)
```kotlin
android {
    // ... existing config
    
    ndkVersion = "27.0.12077973"
    
    defaultConfig {
        // ... existing config
        
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-fexceptions", "-frtti")
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DGGML_USE_CPU=ON",
                    "-DGGML_USE_LLAMAFILE=OFF"
                )
            }
        }
        
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }
    
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}
```

## Consequences

### Positive
- **True on-device AI**: No network required for vision/voice
- **Privacy**: All processing stays local
- **Performance**: Native code runs 3-5x faster than Java/Kotlin
- **Capability**: Unlocks multimodal AI assistant features
- **Consistency**: Same native libraries used in desktop llama.cpp ecosystem

### Negative
- **Build complexity**: CMake + NDK + submodules increases build time
- **APK size**: Native libraries add 30-50MB per architecture
- **Model size**: User must download 1-4GB models for full functionality
- **Maintenance**: Must track upstream llama.cpp/whisper.cpp/piper changes
- **Testing**: Requires physical devices or emulators with native support

### Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Build failures across environments | High | Comprehensive CI matrix (Ubuntu/macOS/Windows) |
| Model download bandwidth | Medium | Progressive loading, CDN distribution, delta updates |
| Native crash bugs | High | Extensive error handling, crash reporting, safe fallbacks |
| Upstream API changes | Medium | Pin to specific commits/tags, test before upgrading |
| Thermal throttling | Medium | Adaptive performance management (existing in core-hw) |

## Implementation Notes

### Critical TODOs to Replace

**VisionProcessingEngineImpl.kt**:
- Line 65: `loadModel()` - Load llama.cpp vision model
- Line 158: `processImage()` - Run inference with CLIP + LLaMA
- Line 184: `unloadModelInternal()` - Free native memory

**SpeechToTextEngineImpl.kt**:
- `processFinalAudio()` - Call whisper_full() for transcription

**TextToSpeechEngineImpl.kt**:
- `synthesizeSpeech()` - Call piper textToAudio() for synthesis

### Model File Structure
```
app/src/main/assets/models/
├── vision/
│   ├── llava-v1.5-7b-q4_0.gguf          # 4.0 GB
│   └── llava-v1.5-mmproj-f16.gguf       # 550 MB
├── voice/
│   ├── whisper-tiny-en.bin              # 75 MB
│   ├── whisper-base-en.bin              # 145 MB
│   └── piper/
│       ├── jenny_low.onnx               # 18 MB
│       └── jenny_low.onnx.json          # 2 KB
└── multimodal_models.json               # Model catalog
```

## References
- [llama.cpp GitHub](https://github.com/ggerganov/llama.cpp)
- [whisper.cpp GitHub](https://github.com/ggerganov/whisper.cpp)
- [Piper TTS GitHub](https://github.com/rhasspy/piper)
- [Android NDK CMake Guide](https://developer.android.com/ndk/guides/cmake)
- [Issue #08: Voice Processing](../issues/08-voice-processing.md)
- [Issue #07.5: Multimodal Production Implementation](../issues/07.5-multimodal-production-implementation.md)

## Related ADRs
- ADR-0001: Use Copilot Pro with Claude Sonnet 4
- ADR-0003: (Future) Model Download and Caching Strategy
- ADR-0004: (Future) Native Memory Management Strategy
