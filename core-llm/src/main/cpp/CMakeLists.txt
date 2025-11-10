cmake_minimum_required(VERSION 3.22.1)
project(iris_llm)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Android-specific settings
set(ANDROID_STL c++_shared)

# llama.cpp configuration
# Minimal configuration for Android
set(GGML_OPENCL OFF CACHE BOOL "Enable OpenCL backend")
set(GGML_VULKAN OFF CACHE BOOL "Enable Vulkan backend")
set(GGML_NATIVE OFF CACHE BOOL "Disable native optimizations")
set(GGML_ACCELERATE OFF CACHE BOOL "Disable Accelerate framework")
set(GGML_CPU_HBM OFF CACHE BOOL "Disable HBM support which uses -mcpu=native")
set(GGML_CPU_ALL_VARIANTS OFF CACHE BOOL "Disable all CPU variants to avoid -mcpu=native")
set(GGML_OPENMP OFF CACHE BOOL "Disable OpenMP")
set(BUILD_SHARED_LIBS OFF CACHE BOOL "Build static libraries")
# Disable llamafile SGEMM optimizations for Android
# The llamafile sgemm.cpp uses ARM NEON FP16 intrinsics which require
# ARM_FEATURE_FP16_VECTOR_ARITHMETIC, not available on all Android ARM devices
set(GGML_LLAMAFILE OFF CACHE BOOL "Disable llamafile for Android ARM compatibility")

# Add llama.cpp subdirectory
add_subdirectory(llama.cpp)

# Include directories
include_directories(
    llama.cpp/
    llama.cpp/include/
    llama.cpp/ggml/include/
)

# JNI bridge source files
set(JNI_SOURCES
    jni_bridge.cpp
    model_manager.cpp
    generation_engine.cpp
)

# Create shared library
add_library(iris_llm SHARED ${JNI_SOURCES})

# Link libraries
target_link_libraries(iris_llm
    llama
    android
    log
)

# Compiler flags for optimization
target_compile_options(iris_llm PRIVATE
    -O3
    -DNDEBUG
    -ffast-math
)
