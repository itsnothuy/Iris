cmake_minimum_required(VERSION 3.22.1)

project(iris_multimodal VERSION 1.0.0 LANGUAGES C CXX)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Android-specific configuration
set(CMAKE_ANDROID_STL_TYPE c++_shared)

# Compiler flags for performance and compatibility
add_compile_options(
    -fexceptions
    -frtti
    -Wall
    -Wextra
    -Werror=return-type
    -fvisibility=hidden
)

# Platform-specific optimizations
if(ANDROID_ABI STREQUAL "arm64-v8a")
    add_compile_options(-march=armv8-a -mtune=generic)
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    add_compile_options(-march=armv7-a -mfpu=neon -mfloat-abi=softfp)
endif()

# Find required Android libraries
find_library(log-lib log)
find_library(android-lib android)

# ============================================================================
# Native Multimodal Library (Main Target)
# ============================================================================

add_library(iris_multimodal SHARED
    # JNI bridges (to be implemented)
    # llava_android.cpp
    # whisper_android.cpp
    # piper_android.cpp
    # jni_utils.cpp
)

target_include_directories(iris_multimodal PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    # Submodule includes (add after submodules are initialized)
    # ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp
    # ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp
    # ${CMAKE_CURRENT_SOURCE_DIR}/piper/src
)

target_link_libraries(iris_multimodal
    ${log-lib}
    ${android-lib}
    # Native library links (add after submodules are initialized)
    # llama
    # ggml
    # clip
    # whisper
    # piper
)

target_compile_definitions(iris_multimodal PRIVATE
    # GGML configuration
    GGML_USE_CPU=1
    GGML_USE_LLAMAFILE=0
    
    # Platform detection
    __ANDROID__=1
)

# ============================================================================
# Submodule Integration (Commented out until submodules are added)
# ============================================================================

# # LLaVA/llama.cpp Integration
# if(EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)
#     message(STATUS "Found llama.cpp submodule")
#     
#     # Configure llama.cpp build
#     set(GGML_LLAMAFILE OFF CACHE BOOL "Disable llamafile for Android")
#     set(GGML_CUDA OFF CACHE BOOL "Disable CUDA for Android")
#     set(GGML_METAL OFF CACHE BOOL "Disable Metal for Android")
#     set(GGML_VULKAN OFF CACHE BOOL "Disable Vulkan initially")
#     set(GGML_OPENCL OFF CACHE BOOL "Disable OpenCL initially")
#     set(GGML_BLAS OFF CACHE BOOL "Disable BLAS")
#     set(LLAMA_BUILD_TESTS OFF CACHE BOOL "Don't build tests")
#     set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "Don't build examples")
#     
#     add_subdirectory(llama.cpp EXCLUDE_FROM_ALL)
#     
#     target_sources(iris_multimodal PRIVATE
#         llava_android.cpp
#     )
#     
#     target_link_libraries(iris_multimodal
#         llama
#         ggml
#         clip
#     )
# else()
#     message(WARNING "llama.cpp submodule not found. Vision processing will be unavailable.")
# endif()

# # Whisper.cpp Integration
# if(EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp)
#     message(STATUS "Found whisper.cpp submodule")
#     
#     # Configure whisper.cpp build
#     set(WHISPER_BUILD_TESTS OFF CACHE BOOL "Don't build tests")
#     set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "Don't build examples")
#     
#     add_subdirectory(whisper.cpp EXCLUDE_FROM_ALL)
#     
#     target_sources(iris_multimodal PRIVATE
#         whisper_android.cpp
#     )
#     
#     target_link_libraries(iris_multimodal
#         whisper
#     )
# else()
#     message(WARNING "whisper.cpp submodule not found. Speech-to-text will be unavailable.")
# endif()

# # Piper TTS Integration
# if(EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/piper)
#     message(STATUS "Found piper submodule")
#     
#     # Configure Piper build (may need ONNX Runtime)
#     add_subdirectory(piper EXCLUDE_FROM_ALL)
#     
#     target_sources(iris_multimodal PRIVATE
#         piper_android.cpp
#     )
#     
#     target_link_libraries(iris_multimodal
#         piper
#     )
# else()
#     message(WARNING "piper submodule not found. Text-to-speech will be unavailable.")
# endif()

# ============================================================================
# Installation & Packaging
# ============================================================================

# Strip symbols in release builds
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    add_custom_command(TARGET iris_multimodal POST_BUILD
        COMMAND ${CMAKE_STRIP} --strip-unneeded $<TARGET_FILE:iris_multimodal>
        COMMENT "Stripping symbols from release binary"
    )
endif()

# Print configuration summary
message(STATUS "")
message(STATUS "=== Iris Multimodal Native Build Configuration ===")
message(STATUS "Build type: ${CMAKE_BUILD_TYPE}")
message(STATUS "Android ABI: ${ANDROID_ABI}")
message(STATUS "Android API level: ${ANDROID_PLATFORM}")
message(STATUS "NDK version: ${ANDROID_NDK_REVISION}")
message(STATUS "C++ standard: ${CMAKE_CXX_STANDARD}")
message(STATUS "=================================================")
message(STATUS "")
