# Native Multimodal Integration - C++ Code

## Overview
This directory contains the native C++ code for integrating multimodal AI capabilities (vision, speech-to-text, text-to-speech) into iris_android.

## Current Status: 🚧 Infrastructure Prepared

### ✅ Completed
- CMakeLists.txt configuration for Android NDK
- JNI utility headers for safe Java-C++ interop
- Build system integration points defined
- KAPT configuration for stable compilation

### ⚠️ Pending (Requires Network Access & Native Development)
- Git submodules for native libraries (llama.cpp, whisper.cpp, piper)
- JNI bridge implementations (llava_android.cpp, whisper_android.cpp, piper_android.cpp)
- Model download and asset management
- Full native compilation and testing

## Directory Structure

```
cpp/
├── CMakeLists.txt           # ✅ Main CMake configuration
├── jni_utils.h              # ✅ JNI helper utilities
├── README.md                # ✅ This file
│
├── llama.cpp/               # ⚠️ TO ADD: Git submodule for LLaVA vision
├── whisper.cpp/             # ⚠️ TO ADD: Git submodule for STT
├── piper/                   # ⚠️ TO ADD: Git submodule for TTS
│
├── llava_android.cpp        # ⚠️ TO CREATE: LLaVA JNI bridge
├── whisper_android.cpp      # ⚠️ TO CREATE: Whisper.cpp JNI bridge
├── piper_android.cpp        # ⚠️ TO CREATE: Piper JNI bridge
└── jni_utils.cpp            # ⚠️ TO CREATE: JNI utilities implementation
```

## Next Steps for Implementation

### Step 1: Add Git Submodules
```bash
cd core-multimodal/src/main/cpp/

# Add llama.cpp with vision support
git submodule add https://github.com/ggerganov/llama.cpp.git
cd llama.cpp && git checkout tags/b3259 && cd ..

# Add whisper.cpp for STT
git submodule add https://github.com/ggerganov/whisper.cpp.git
cd whisper.cpp && git checkout tags/v1.5.4 && cd ..

# Add Piper for TTS
git submodule add https://github.com/rhasspy/piper.git
cd piper && git checkout tags/v1.2.0 && cd ..
```

### Step 2: Create JNI Bridge Files

#### llava_android.cpp
Implements:
- `Java_..._nativeLoadVisionModel()` - Load LLaVA model + CLIP encoder
- `Java_..._nativeProcessImage()` - Run vision-language inference
- `Java_..._nativeUnloadVisionModel()` - Free native memory

Template:
```cpp
#include <jni.h>
#include "llama.h"
#include "llava.h"
#include "clip.h"
#include "jni_utils.h"

struct vision_context {
    llama_model* model;
    clip_ctx* clip;
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_nervesparks_iris_core_multimodal_vision_VisionProcessingEngineImpl_nativeLoadVisionModel(
    JNIEnv* env, jobject thiz, jstring model_path, jstring mmproj_path) {
    // Implementation here
}
```

#### whisper_android.cpp
Implements:
- `Java_..._nativeLoadWhisperModel()` - Load Whisper ASR model
- `Java_..._nativeTranscribeAudio()` - Transcribe audio samples
- `Java_..._nativeUnloadWhisperModel()` - Free native memory

#### piper_android.cpp
Implements:
- `Java_..._nativeLoadPiperModel()` - Load Piper voice model
- `Java_..._nativeSynthesizeSpeech()` - Generate audio from text
- `Java_..._nativeUnloadPiperModel()` - Free native memory

### Step 3: Update CMakeLists.txt
Uncomment the submodule integration sections once files are present:
```cmake
# LLaVA/llama.cpp Integration
if(EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)
    # ... configuration
    add_subdirectory(llama.cpp EXCLUDE_FROM_ALL)
    target_sources(iris_multimodal PRIVATE llava_android.cpp)
    target_link_libraries(iris_multimodal llama ggml clip)
endif()
```

### Step 4: Update Kotlin Code
Add native method declarations and companion object initializer in:
- `VisionProcessingEngineImpl.kt`
- `SpeechToTextEngineImpl.kt`
- `TextToSpeechEngineImpl.kt`

Example:
```kotlin
class VisionProcessingEngineImpl @Inject constructor(...) : VisionProcessingEngine {
    
    companion object {
        init {
            System.loadLibrary("iris_multimodal")
        }
    }
    
    private external fun nativeLoadVisionModel(modelPath: String, mmprojPath: String): Long
    private external fun nativeProcessImage(contextPtr: Long, imageData: ByteArray, prompt: String): String?
    private external fun nativeUnloadVisionModel(contextPtr: Long)
    
    // ... implementation
}
```

### Step 5: Update build.gradle.kts
Add NDK and CMake configuration to `core-multimodal/build.gradle.kts`:

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

### Step 6: Test Native Build
```bash
./gradlew :core-multimodal:assembleDebug
```

This will compile the native libraries and package them into the APK.

## JNI Method Naming Convention

JNI method names follow the pattern:
```
Java_<package_name_with_underscores>_<class_name>_<method_name>
```

Example:
```cpp
// Kotlin: com.nervesparks.iris.core.multimodal.vision.VisionProcessingEngineImpl.nativeLoadVisionModel()
// JNI:
Java_com_nervesparks_iris_core_multimodal_vision_VisionProcessingEngineImpl_nativeLoadVisionModel
```

## Memory Management

### Context Lifetime
Native contexts are represented as `jlong` (pointer as 64-bit int):
1. **Load**: Allocate native structure, return pointer as `jlong`
2. **Use**: Cast `jlong` back to pointer for operations
3. **Unload**: Cast `jlong` to pointer, free memory, return

### Error Handling
Use JNI utility functions to throw Java exceptions:
```cpp
if (error) {
    iris::jni::throw_exception(env, 
        iris::jni::exceptions::RUNTIME, 
        "Model loading failed");
    return 0;
}
```

## Performance Considerations

### Threading
- Native inference can block for 50-500ms
- Called from Kotlin coroutines on `Dispatchers.IO` or `Dispatchers.Default`
- Don't hold JNI references across thread boundaries

### Memory
- Model sizes: 75MB (whisper-tiny) to 4GB (llava-7b)
- Use memory-mapped files where possible
- Implement LRU cache for loaded models
- Monitor memory pressure and unload models proactively

### CPU/GPU Backends
- Start with CPU-only (NEON on ARM)
- Add Vulkan compute for GPU acceleration (Phase 2)
- Add QNN/NPU support for Snapdragon (Phase 3)

## Debugging

### Enable Verbose Logging
```cpp
LOGD("Model path: %s", model_path);
LOGI("Vision model loaded successfully");
LOGW("Using fallback CPU backend");
LOGE("Failed to load model: %s", error_msg);
```

### Check JNI Errors
```bash
adb logcat | grep -E "(IrisMultimodal|JNI)"
```

### Native Crash Analysis
Use `ndk-stack` to symbolicate native crashes:
```bash
adb logcat | ndk-stack -sym app/build/intermediates/cmake/debug/obj/arm64-v8a/
```

## References
- [ADR-0002: Native Multimodal Integration Strategy](../../adr/0002-native-multimodal-integration-strategy.md)
- [Native Integration Roadmap](../../NATIVE_INTEGRATION_ROADMAP.md)
- [Android NDK Documentation](https://developer.android.com/ndk)
- [JNI Tips and Best Practices](https://developer.android.com/training/articles/perf-jni)
- [llama.cpp Android Example](https://github.com/ggerganov/llama.cpp/tree/master/examples/llama.android)
- [whisper.cpp Android Example](https://github.com/ggerganov/whisper.cpp/tree/master/examples/whisper.android)

## Support
For questions or issues with native integration, see:
- [GitHub Issues](https://github.com/itsnothuy/Iris/issues)
- [Architecture Documentation](../../architecture.md)
- [Development Guide](../../../DEVELOPMENT.md)
