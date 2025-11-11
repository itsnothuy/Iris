# Core Multimodal Module Implementation Guide

## Overview

The `core-multimodal` module provides the foundation for vision-language AI capabilities in the Iris Android application. This document describes the completed implementation and provides guidance for future development.

## Module Structure

The core-multimodal module is organized as follows:

```
core-multimodal/
├── build.gradle.kts                    # Module build configuration
└── src/main/kotlin/com/nervesparks/iris/core/multimodal/
    ├── MultimodalTypes.kt               # Core data types and enums
    ├── MultimodalInterfaces.kt          # Main service interfaces
    ├── image/
    │   └── MockImageProcessor.kt        # Image preprocessing implementation
    ├── registry/
    │   └── MockMultimodalModelRegistry.kt  # Model management implementation
    └── vision/
        └── MockVisionProcessingEngine.kt   # Vision processing implementation
```

## Core Components

### 1. Data Types (`MultimodalTypes.kt`)

The module defines comprehensive data types for multimodal AI operations:

#### Key Enums:
- **VisionTask**: Supported vision processing tasks (object detection, text recognition, image classification, scene analysis, general Q&A)
- **ModelBackend**: AI inference backends (ONNX, TensorFlow Lite, PyTorch, TensorFlow)
- **ImageFormat**: Supported image formats (JPEG, PNG, WebP, BMP)
- **DocumentType**: Document types for specialized processing
- **MultimodalCapability**: Model capabilities for feature matching

#### Core Data Classes:
- **MultimodalModelDescriptor**: Complete model specification with capabilities and requirements
- **VisionParameters**: Configuration parameters for vision processing
- **ProcessedImageData**: Preprocessed image data ready for model input
- **VisionResult**: Sealed class hierarchy for different types of vision processing results

#### Result Types:
- **VisionResult.AnalysisResult**: General image analysis with text, confidence, and timing
- **VisionResult.ScreenshotResult**: Screenshot processing with UI element detection
- **VisionResult.OCRResult**: Text extraction results with region information
- **VisionResult.DocumentResult**: Document analysis with structured data extraction
- **VisionResult.StreamResult**: Streaming analysis results for real-time processing

### 2. Service Interfaces (`MultimodalInterfaces.kt`)

#### MultimodalModelRegistry
Manages available AI models with device-aware recommendations:
- `getRecommendedModel(visionTask)`: Get optimal model for specific task
- `assessModelCompatibility(model)`: Evaluate model compatibility with device
- `getAvailableModels()`: List all available models
- `getModelById(modelId)`: Retrieve specific model by identifier

#### VisionProcessingEngine
Core vision processing capabilities:
- `analyzeImage()`: General image analysis with text prompts
- `processScreenshot()`: Screenshot analysis with UI element detection
- `extractTextFromImage()`: OCR text extraction
- `analyzeDocument()`: Specialized document processing
- `streamVisionAnalysis()`: Real-time streaming analysis

#### ImageProcessor
Image preprocessing and validation:
- `preprocessImage()`: Prepare images for model input
- `validateImage()`: Verify image compatibility

### 3. Mock Implementations

The current implementation includes mock services for initial development and testing:

#### MockMultimodalModelRegistry
- Provides hardcoded model descriptors for testing
- Returns mock compatibility assessments
- Simulates device-aware model recommendations

#### MockVisionProcessingEngine
- Generates realistic mock responses for all vision tasks
- Includes proper data structures and timing simulation
- Supports streaming analysis workflows

#### MockImageProcessor
- Handles basic image validation and preprocessing simulation
- Returns mock processed image data for testing

## Build Configuration

The module is configured as an Android Library with minimal dependencies:

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.android)
    // Testing dependencies...
}
```

### Key Configuration Details:
- **Compile SDK**: Android API 35
- **Min SDK**: Inherited from root project
- **Kotlin Coroutines**: Async processing support
- **No External AI Dependencies**: Self-contained for initial development

## Integration Points

### 1. Future AI Backend Integration
The interfaces are designed to support real AI implementations:

```kotlin
// Replace mock implementations with real services
class ONNXVisionProcessingEngine(
    private val modelRegistry: MultimodalModelRegistry,
    private val imageProcessor: ImageProcessor
) : VisionProcessingEngine {
    // Real ONNX implementation
}
```

### 2. Model Management
The registry supports dynamic model loading and device optimization:

```kotlin
// Example model configuration
val model = MultimodalModelDescriptor(
    id = "llava-1.5-7b-mobile",
    name = "LLaVA Mobile",
    baseModel = "llama-2-7b",
    visionRequirements = VisionRequirements(
        maxImageSize = ImageSize(512, 512),
        supportedFormats = listOf(ImageFormat.JPEG, ImageFormat.PNG)
    ),
    performance = ModelPerformance(
        inferenceTimeMs = 2000,
        memoryUsageMB = 2048,
        accuracy = 0.87f
    )
)
```

### 3. Application Integration
The module integrates with the main app through dependency injection:

```kotlin
// In your activity or fragment
class MainActivity {
    @Inject lateinit var visionEngine: VisionProcessingEngine
    @Inject lateinit var modelRegistry: MultimodalModelRegistry
    
    suspend fun analyzeUserImage(uri: Uri, prompt: String) {
        val model = modelRegistry.getRecommendedModel(VisionTask.GENERAL_QA).getOrThrow()
        val result = visionEngine.analyzeImage(uri, prompt, model, VisionParameters())
        // Handle result...
    }
}
```

## Development Guidelines

### 1. Error Handling
All operations return `Result<T>` for proper error handling:

```kotlin
val result = visionEngine.analyzeImage(uri, prompt, model, params)
result.fold(
    onSuccess = { analysisResult ->
        // Handle successful analysis
    },
    onFailure = { exception ->
        // Handle errors gracefully
    }
)
```

### 2. Async Processing
Use Kotlin Coroutines for all async operations:

```kotlin
viewModelScope.launch {
    val result = withContext(Dispatchers.Default) {
        visionEngine.analyzeImage(uri, prompt, model, params)
    }
    // Update UI on main thread
}
```

### 3. Streaming Analysis
For real-time applications, use the streaming API:

```kotlin
visionEngine.streamVisionAnalysis(uri, prompt, model, params)
    .collect { streamResult ->
        when (streamResult) {
            is VisionResult.StreamResult.Started -> showProgress()
            is VisionResult.StreamResult.TextChunk -> updatePartialResult(streamResult.text)
            is VisionResult.StreamResult.Completed -> showFinalResult(streamResult.finalResult)
            is VisionResult.StreamResult.Error -> handleError(streamResult.exception)
        }
    }
```

## Future Development

### 1. Real AI Implementation
- Replace mock implementations with actual AI backends (ONNX Runtime, TensorFlow Lite)
- Integrate with on-device models like LLaVA, MobileVLM, or Qwen-VL
- Add device capability detection and model optimization

### 2. Advanced Features
- Image segmentation and object detection
- Multi-modal conversation history
- Fine-tuning for specific use cases
- Federated learning capabilities

### 3. Performance Optimization
- Model quantization and compression
- Hardware acceleration (NPU, GPU)
- Caching and memory management
- Batch processing support

### 4. Testing Strategy
- Create comprehensive unit tests for all interfaces
- Add integration tests with real models
- Performance benchmarking and regression tests
- Device compatibility testing matrix

## Conclusion

The core-multimodal module provides a solid foundation for vision-language AI capabilities in Iris. The current mock implementation allows for immediate development and testing, while the clean interface design supports seamless integration of real AI backends in future iterations.

The modular architecture ensures that AI capabilities can be developed independently of the main application, with clear contracts for integration and testing.