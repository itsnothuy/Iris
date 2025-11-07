# llama.cpp Integration - Current Status & Next Steps

## Summary

This document describes the current state of the llama.cpp integration for the `core-llm` module and outlines potential paths forward to complete the implementation.

## What Was Implemented

### ✅ Completed Components

1. **Git Submodule Setup**
   - Added llama.cpp as a submodule at `core-llm/src/main/cpp/llama.cpp`
   - Currently at commit 7f09a680a (tag: b6970)

2. **Native C++ Implementation**
   - `model_manager.cpp/.h` - Model lifecycle management
   - `generation_engine.cpp/.h` - Text generation with greedy sampling
   - `jni_bridge.cpp` - JNI interface layer with native method bindings
   - Updated to use current (non-deprecated) llama.cpp API

3. **Kotlin Integration**
   - Updated `LLMEngineImpl.kt` with native method declarations
   - Added `ModelLoadParams` data class for JNI parameter passing
   - Integrated with BackendRouter for backend selection
   - Added exception classes (`LLMException`, `GenerationException`, `EmbeddingException`)
   - Implemented coroutine-based text generation with Flow

4. **Build Configuration**
   - Updated `core-llm/build.gradle.kts` with NDK/CMake configuration
   - Created `CMakeLists.txt` with llama.cpp subdirectory build
   - Configured for ARM64-v8a and ARMv7a architectures

### Current Build Issue

**Problem**: The llama.cpp `ggml-cpu` backend attempts to use `-mcpu=native` compiler flag, which is incompatible with Android NDK cross-compilation.

**Error**: 
```
clang++: error: unsupported argument 'native' to option '-mcpu='
```

**Root Cause**: llama.cpp's CMake configuration in `ggml/src/ggml-cpu/CMakeLists.txt` attempts to detect CPU features and uses `-mcpu=native` when building CPU optimization variants.

## Paths Forward

### Option 1: Disable CPU Optimizations (Quick Fix)
**Pros**: Minimal changes, uses standard llama.cpp
**Cons**: Suboptimal performance

**Steps**:
1. Find the right CMake options to completely disable CPU variant builds
2. May require patching llama.cpp's CMakeLists.txt to skip ARM feature detection
3. Accept baseline ARM performance without optimizations

### Option 2: Use Older llama.cpp Version
**Pros**: Known working Android builds exist for older versions
**Cons**: Miss out on newer features and optimizations

**Steps**:
1. Research llama.cpp commits/tags that have good Android support
2. Update submodule to that commit
3. May need to adjust API usage if older

### Option 3: Apply CMake Patches
**Pros**: Get optimized build, use latest llama.cpp  
**Cons**: Maintenance burden, fragile across llama.cpp updates

**Steps**:
1. Create patch file for llama.cpp CMakeLists.txt files
2. Replace `-mcpu=native` with specific ARM architecture flags (e.g., `-mcpu=cortex-a75`)
3. Apply patches as part of build process
4. Document patching process for future llama.cpp updates

### Option 4: Use Pre-built llama.cpp Library
**Pros**: Avoids build complexity, known working configuration
**Cons**: Need to maintain pre-built binaries, less flexible

**Steps**:
1. Build llama.cpp separately for Android using appropriate toolchain
2. Package as AAR or shared libraries
3. Link against pre-built libraries in CMakeLists.txt
4. Set up CI to rebuild when updating llama.cpp

### Option 5: Alternative Build System
**Pros**: More control over build process
**Cons**: Significant setup effort

**Steps**:
1. Use Android.mk instead of CMake
2. Manually specify source files and compiler flags
3. Provides finer control over compilation flags

## Recommended Approach

**Option 3 (Apply CMake Patches)** is recommended because:
- Maintains latest llama.cpp features
- Provides optimized performance
- Relatively straightforward to implement
- Can be automated in build process

### Implementation Plan

1. **Create patch for ggml-cpu CMakeLists.txt**:
   ```cmake
   # Replace -mcpu=native detection with explicit Android flags
   if(ANDROID)
       set(ARM_MCPU_FLAG -march=armv8-a)
   else()
       # existing native detection
   endif()
   ```

2. **Apply patch during CMake configuration**:
   - Either patch submodule directly (not ideal)
   - Or use CMake's `execute_process` to apply patch before `add_subdirectory`

3. **Alternative: Fork llama.cpp temporarily**:
   - Fork llama.cpp on GitHub
   - Apply Android-specific patches
   - Use forked version as submodule
   - Sync with upstream periodically

## Testing Plan (Once Build Works)

1. **Unit Tests**:
   - Test model loading with mock model file
   - Test error handling for invalid paths
   - Test memory cleanup

2. **Integration Tests** (requires small test model):
   - Load a tiny GGUF model (~100MB)
   - Generate tokens
   - Verify embedding generation
   - Test model unloading

3. **Performance Tests**:
   - Measure tokens/second on target device
   - Monitor memory usage
   - Test thermal behavior

## Additional Notes

- Current implementation uses greedy sampling (picks highest probability token)
- Full sampling with temperature/top-k/top-p can be added later using llama_sampler API
- GPU backends (OpenCL, Vulkan) are disabled for this minimal implementation
- Architecture follows docs/architecture.md Section 5 guidelines

## References

- llama.cpp Android discussion: https://github.com/ggerganov/llama.cpp/discussions (search for Android)
- Android NDK CMake: https://developer.android.com/ndk/guides/cmake
- llama.cpp examples: `examples/simple/simple.cpp` in submodule
