# core-models

Model Management & Registry System for iris_android

## Overview

The `core-models` module provides comprehensive model management infrastructure for the iris_android AI assistant. It handles model discovery, download, validation, storage, and device-aware recommendations.

## Features

### 📋 Model Registry
- **Model Catalog**: Centralized JSON catalog of available AI models
- **Device-Aware Recommendations**: Smart model suggestions based on device capabilities
- **Compatibility Assessment**: Automatic validation of RAM, Android version, backends, and device class
- **Performance Estimation**: Expected tokens/second, power consumption, and thermal profiles
- **Catalog Caching**: 24-hour cache for offline operation

### 💾 Model Storage
- **File Management**: Organized storage in app-specific directories
- **Metadata Persistence**: JSON metadata for quick model lookups
- **Integrity Verification**: SHA-256 checksums for downloaded models
- **Space Management**: Storage availability checks and cleanup
- **CRUD Operations**: Create, read, update, delete model files and metadata

### 📥 Model Downloader
- **HTTP Downloads**: OkHttp-based efficient downloads
- **Progress Tracking**: Real-time download progress via Kotlin Flow
- **Integrity Verification**: SHA-256 hash validation after download
- **Error Handling**: Automatic cleanup on failure
- **Concurrent Management**: Track multiple active downloads

## Architecture

```
core-models/
├── registry/
│   ├── ModelRegistry.kt          # Interface
│   └── ModelRegistryImpl.kt      # Implementation with device profiling
├── downloader/
│   ├── ModelDownloader.kt        # Interface
│   └── ModelDownloaderImpl.kt    # OkHttp-based implementation
├── storage/
│   ├── ModelStorage.kt           # Interface
│   └── ModelStorageImpl.kt       # File-based implementation
├── di/
│   └── ModelModule.kt            # Hilt dependency injection
└── ModelDescriptor.kt            # Core data models
```

## Model Catalog

The catalog (`src/main/assets/models.json`) includes:

### Language Models (LLM)
- **TinyLlama 1.1B Q4_0** - Ultra-lightweight for budget devices (639 MB)
- **Phi-3 Mini 4K Q4_K_M** - Microsoft's efficient 3.8B model (2 GB)
- **Llama 3.2 3B Q4_K_M** - Meta's latest 3B model (1.75 GB)

### Embedding Models
- **All-MiniLM-L6-v2 Q8_0** - Sentence embeddings (21 MB)

### Safety Models
- **Llama Guard 3 8B Q4_0** - Content moderation (4 GB)

## Usage

### Get Recommended Models

```kotlin
@Inject lateinit var modelRegistry: ModelRegistry

val recommendations = modelRegistry.getRecommendedModels()
recommendations.forEach { recommendation ->
    println("${recommendation.model.name}: ${recommendation.compatibilityScore}/100")
    println("  Reason: ${recommendation.recommendationReason}")
    println("  Expected: ${recommendation.estimatedPerformance.expectedTokensPerSecond} tokens/sec")
}
```

### Download a Model

```kotlin
@Inject lateinit var modelDownloader: ModelDownloader
@Inject lateinit var modelStorage: ModelStorage

val model = modelRegistry.getModelById("tinyllama-1.1b-q4_0")!!
val destinationPath = File(modelStorage.getModelsDirectory(), "${model.id}.gguf").absolutePath

modelDownloader.downloadModel(model, destinationPath)
    .collect { event ->
        when (event) {
            is DownloadEvent.Started -> {
                println("Download started: ${event.totalBytes} bytes")
            }
            is DownloadEvent.Progress -> {
                val percent = (event.bytesDownloaded * 100) / event.totalBytes
                println("Progress: $percent%")
            }
            is DownloadEvent.Verifying -> {
                println("Verifying integrity...")
            }
            is DownloadEvent.Completed -> {
                println("Download complete: ${event.filePath}")
            }
            is DownloadEvent.Failed -> {
                println("Download failed: ${event.error.message}")
            }
        }
    }
```

### Check Model Storage

```kotlin
@Inject lateinit var modelStorage: ModelStorage

// Check if model is stored
if (modelStorage.isModelStored("tinyllama-1.1b-q4_0")) {
    val path = modelStorage.getModelPath("tinyllama-1.1b-q4_0")
    println("Model stored at: $path")
}

// Get all stored models
val storedModels = modelStorage.getStoredModels()
storedModels.forEach { model ->
    println("${model.name} - ${model.fileSize / (1024 * 1024)} MB")
}

// Check available space
val availableSpace = modelStorage.getAvailableSpace()
println("Available: ${availableSpace / (1024 * 1024 * 1024)} GB")
```

### Validate Model Compatibility

```kotlin
@Inject lateinit var modelRegistry: ModelRegistry

val model = modelRegistry.getModelById("phi-3-mini-4k-q4_k_m")!!
val validation = modelRegistry.validateModel(model)

if (validation.isValid) {
    println("Model is compatible!")
} else {
    println("Incompatible: ${validation.reason}")
    validation.issues.forEach { issue ->
        println("  - $issue")
    }
}
```

## Device Compatibility

The registry assesses compatibility based on:

1. **RAM Requirements**
   - Minimum RAM threshold
   - Recommended RAM for optimal performance
   - Score penalty if below recommended

2. **Android Version**
   - Minimum API level requirement
   - Blocks incompatible versions

3. **Device Class**
   - BUDGET, MID_RANGE, HIGH_END, FLAGSHIP
   - Score adjustment for target device class

4. **Backend Support**
   - CPU_NEON (universal fallback)
   - OPENCL_ADRENO (Qualcomm GPUs)
   - VULKAN_MALI (Samsung/Google GPUs)
   - QNN_HEXAGON (Qualcomm NPU)
   - Selects optimal backend per device

## Performance Estimation

Models include performance metrics for different device classes and backends:

```json
{
  "performance": {
    "tokensPerSecond": {
      "CPU_NEON": {
        "BUDGET": 2.5,
        "MID_RANGE": 4.0,
        "HIGH_END": 6.0,
        "FLAGSHIP": 8.0
      },
      "QNN_HEXAGON": {
        "FLAGSHIP": 15.0
      }
    },
    "powerConsumption": "LOW",
    "thermalProfile": "COOL"
  }
}
```

## Dependencies

- **common** - Shared models and utilities
- **core-hw** - Hardware detection and device profiling
- **Hilt** - Dependency injection
- **Kotlin Coroutines** - Async operations
- **Gson** - JSON parsing
- **OkHttp** - HTTP client for downloads

## Testing

```bash
# Run all tests
./gradlew :core-models:test

# Run specific test class
./gradlew :core-models:testDebugUnitTest --tests "ModelStorageImplTest"

# Run with coverage
./gradlew :core-models:testDebugUnitTestCoverage
```

### Test Coverage

- ✅ **ModelStorage**: 9/9 tests passing
  - Directory management
  - Metadata persistence
  - File operations
  - Storage queries

- ⚠️ **ModelRegistry**: 2/8 tests passing
  - Basic functionality works
  - Asset loading in tests needs Robolectric configuration

## Future Enhancements

1. **Remote Catalog Updates**: Fetch latest model catalog from GitHub releases
2. **Resume Downloads**: Support HTTP range requests for resumable downloads
3. **Download Queuing**: Priority queue with concurrent download limits
4. **Model Variants**: Support different quantization levels per model
5. **Delta Updates**: Incremental model updates instead of full downloads
6. **P2P Sharing**: Share models between devices via local network
7. **Model Compression**: On-the-fly decompression during download

## License

Part of the iris_android project. See root LICENSE file.
