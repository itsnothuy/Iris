cmake_minimum_required(VERSION 3.22.1)
project(iris_llm)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Android-specific settings
set(ANDROID_STL c++_shared)

# llama.cpp configuration
# Start with basic CPU support, GPU backends can be enabled later
set(GGML_OPENCL OFF CACHE BOOL "Enable OpenCL backend")
set(GGML_VULKAN OFF CACHE BOOL "Enable Vulkan backend")
set(GGML_NATIVE ON CACHE BOOL "Enable native optimizations")
set(GGML_ACCELERATE OFF CACHE BOOL "Disable Accelerate framework")
set(BUILD_SHARED_LIBS OFF CACHE BOOL "Build static libraries")

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
