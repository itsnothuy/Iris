# Core Multimodal Module

## Overview

The `core-multimodal` module provides production-ready infrastructure for multimodal AI capabilities in Iris, enabling vision-language model support for image understanding, analysis, and processing.

## Features

- **Device-Aware Model Registry**: Intelligent model recommendation based on device capabilities
- **Production Image Processing**: Real image preprocessing using Android Bitmap APIs
- **Vision Processing Engine**: Infrastructure for vision-language model inference
- **Comprehensive Type System**: Complete type definitions for multimodal operations
- **Dependency Injection**: Hilt-based dependency injection for all components

## Architecture

### Components

#### 1. MultimodalModelRegistry
Manages available multimodal models and provides device-aware recommendations.

**Key Features:**
- Loads model catalog from JSON asset
- Assesses device compatibility with sophisticated scoring algorithm
- Recommends optimal models for specific vision tasks
- Caches compatibility assessments for performance

**Compatibility Scoring:**
- Memory compatibility (40% weight)
- Performance expectations (30% weight)
- Feature support (20% weight)
- Device class bonus (10% weight)

#### 2. ImageProcessor
Handles image preprocessing for model inference.

**Key Features:**
- Validates images (format, size, accessibility)
- Resizes images while maintaining aspect ratio
- Converts between image formats (JPEG, PNG, WebP, BMP)
- Optimizes image quality and compression

**Supported Formats:**
- JPEG (lossy compression, 85% quality)
- PNG (lossless compression)
- WebP (modern format with lossless option)
- BMP (converted to PNG internally)

#### 3. VisionProcessingEngine
Manages vision model loading and image inference.

**Current Status:** Infrastructure complete, pending native integration

**Key Features:**
- Model loading and caching (max 2 models)
- Image validation and preprocessing pipeline
- Model lifecycle management
- Ready for llama.cpp vision API integration

**Native Integration Requirements:**
- JNI bridge to llama.cpp vision API
- CLIP/vision encoder embedding extraction
- Cross-modal attention between vision and language tokens
- Streaming response generation

## Usage

### Dependency Injection

All components are provided via Hilt:

```kotlin
@Inject
lateinit var modelRegistry: MultimodalModelRegistry

@Inject
lateinit var imageProcessor: ImageProcessor

@Inject
lateinit var visionEngine: VisionProcessingEngine
```

### Get Recommended Model

```kotlin
val model = modelRegistry.getRecommendedModel(VisionTask.GENERAL_QA)
    .getOrThrow()

Log.d("Multimodal", "Using model: ${model.name}")
```

### Assess Model Compatibility

```kotlin
val assessment = modelRegistry.assessModelCompatibility(model)
    .getOrThrow()

if (assessment.isSupported) {
    Log.i("Multimodal", "Compatibility score: ${assessment.compatibilityScore}")
} else {
    Log.w("Multimodal", "Incompatible: ${assessment.reasonsForIncompatibility}")
}
```

### Validate and Preprocess Image

```kotlin
val isValid = imageProcessor.validateImage(imageUri)
    .getOrDefault(false)

if (isValid) {
    val processedImage = imageProcessor.preprocessImage(
        uri = imageUri,
        targetSize = 512,
        format = ImageFormat.JPEG
    ).getOrThrow()
    
    Log.d("Multimodal", "Preprocessed: ${processedImage.width}x${processedImage.height}")
}
```

### Load Model and Process Image

```kotlin
// Load model
visionEngine.loadVisionModel(model).getOrThrow()

// Process image with prompt
val response = visionEngine.processImageWithPrompt(
    imageUri = imageUri,
    prompt = "What objects are in this image?"
).getOrThrow()

Log.i("Multimodal", "Response: $response")
```

## Model Catalog

Models are defined in `assets/multimodal_models.json`:

```json
{
  "version": "1.0.0",
  "models": [
    {
      "id": "llava-1.5-7b-q4",
      "name": "LLaVA 1.5 7B (Q4)",
      "baseModel": "vicuna-7b-v1.5",
      "visionRequirements": {
        "maxImageSize": {"width": 512, "height": 512},
        "supportedFormats": ["JPEG", "PNG"],
        "minConfidence": 0.6
      },
      "performance": {
        "inferenceTimeMs": 800,
        "memoryUsageMB": 4096,
        "accuracy": 0.82
      },
      "capabilities": [
        "VISUAL_QUESTION_ANSWERING",
        "IMAGE_CLASSIFICATION",
        "SCENE_ANALYSIS"
      ]
    }
  ]
}
```

## Supported Models

### LLaVA 1.5 7B (Q4)
- **Base Model**: Vicuna 7B v1.5
- **Image Size**: 512x512
- **Memory**: ~4GB
- **Capabilities**: VQA, image classification, scene analysis

### Qwen-VL-Chat (Q4)
- **Base Model**: Qwen 7B Chat
- **Image Size**: 448x448
- **Memory**: ~3.8GB
- **Capabilities**: VQA, text recognition, document analysis

## Testing

Comprehensive unit tests cover:

### MultimodalModelRegistryImplTest
- Model catalog loading
- Model lookup by ID
- Compatibility assessment
- Device-specific recommendations
- Memory constraint handling

### ImageProcessorImplTest
- Image validation (format, size, accessibility)
- MIME type verification
- Size limit enforcement
- Error handling

### VisionProcessingEngineImplTest
- Model loading and unloading
- Model state management
- Image validation pipeline
- Preprocessing integration
- Error handling

Run tests:
```bash
./gradlew :core-multimodal:test
```

## Dependencies

### Module Dependencies
- `:common` - Common types and utilities
- `:core-hw` - Device profiling
- `:core-models` - Base model types

### External Dependencies
- Hilt - Dependency injection
- Coroutines - Async operations
- Kotlinx Serialization - JSON parsing
- Android Bitmap APIs - Image processing

## Future Work

### Native Integration
The vision processing engine is ready for integration with llama.cpp vision API:

1. **JNI Bridge**: Create native bindings to llama.cpp multimodal functions
2. **Vision Encoder**: Integrate CLIP or similar vision encoder for image embeddings
3. **Cross-Modal Attention**: Implement attention mechanism between vision and text
4. **Streaming**: Add support for streaming response generation
5. **Optimization**: Implement hardware acceleration (GPU, NPU) for vision processing

### Additional Features
- OCR capabilities with dedicated models
- Document analysis for forms and invoices
- Screenshot analysis for UI understanding
- Batch image processing
- Model quantization support (Q8, Q5, Q4)

## Performance Considerations

### Memory Management
- Maximum 2 models cached simultaneously
- Automatic LRU eviction of oldest model
- Bitmap recycling after processing
- ByteArray reuse for image data

### Image Optimization
- Aspect-ratio-preserving resizing
- Configurable JPEG quality (default 85%)
- Format-specific compression settings
- Size limit enforcement (10MB max)

### Compatibility Scoring
- Device class-aware recommendations
- Memory availability checks
- Performance estimation based on device tier
- Thermal considerations for sustained inference

## License

Part of the Iris project. See main repository LICENSE for details.
