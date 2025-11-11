# Core Multimodal Module

## Overview

The `core-multimodal` module provides production-ready infrastructure for multimodal AI capabilities in Iris, enabling vision-language model support for image understanding, analysis, and processing, as well as comprehensive voice processing capabilities including speech-to-text and text-to-speech.

## Features

- **Device-Aware Model Registry**: Intelligent model recommendation based on device capabilities
- **Production Image Processing**: Real image preprocessing using Android Bitmap APIs
- **Vision Processing Engine**: Infrastructure for vision-language model inference
- **Voice Processing Engine**: On-device speech-to-text and text-to-speech capabilities
- **Audio Processing**: Real-time audio capture, processing, and playback
- **Voice Activity Detection**: Intelligent speech detection and endpoint detection
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

#### 4. AudioProcessor
Handles audio capture, processing, and playback using Android AudioRecord and AudioTrack APIs.

**Key Features:**
- Real-time audio recording with configurable sample rate and channels
- Audio playback with automatic format conversion
- Audio preprocessing (noise reduction, automatic gain control, echo cancellation)
- WAV file I/O for loading and saving audio
- Voice Activity Detection (VAD) for speech detection

**Audio Processing:**
- Automatic Gain Control (AGC) for consistent volume
- Simple noise reduction via energy thresholding
- PCM 16-bit to float conversion for model input
- Configurable buffer sizes for low-latency processing

#### 5. SpeechToTextEngine
Converts spoken audio to text using on-device speech recognition models.

**Key Features:**
- Model loading with device compatibility validation
- Streaming recognition with real-time partial results
- Voice Activity Detection for automatic speech endpoint detection
- Batch transcription for audio files
- Multi-language support
- Confidence scoring for transcription quality

**Recognition Modes:**
- Streaming mode: Real-time partial transcriptions as speech is detected
- Batch mode: Process complete audio files
- Auto-endpoint detection: Automatically stops when speech ends

**Current Status:** Infrastructure complete, pending native STT model integration (e.g., Whisper.cpp)

#### 6. TextToSpeechEngine
Converts text to natural-sounding speech using on-device TTS models.

**Key Features:**
- Model loading with device compatibility validation
- Text-to-audio synthesis with adjustable parameters
- Streaming synthesis for long text passages
- Direct playback with speak() method
- Voice parameter controls (rate, pitch, volume)
- Multiple voice support

**Speech Parameters:**
- Speaking rate: Control speed of speech
- Pitch: Adjust voice pitch
- Volume: Control output volume
- Voice selection: Choose from available voice models

**Current Status:** Infrastructure complete, pending native TTS model integration (e.g., Piper, Coqui TTS)

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

@Inject
lateinit var audioProcessor: AudioProcessor

@Inject
lateinit var sttEngine: SpeechToTextEngine

@Inject
lateinit var ttsEngine: TextToSpeechEngine
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

### Speech-to-Text Usage

```kotlin
// Define STT model
val sttModel = STTModelDescriptor(
    id = "whisper-tiny",
    name = "Whisper Tiny",
    description = "Lightweight STT model",
    language = "en",
    supportedLanguages = listOf("en", "es", "fr"),
    audioRequirements = AudioRequirements(
        sampleRate = 16000,
        channels = 1,
        bitDepth = 16,
        supportedFormats = listOf("PCM", "WAV")
    ),
    memoryRequirements = MemoryRequirements(
        minRAM = 2L * 1024 * 1024 * 1024, // 2GB
        recommendedRAM = 4L * 1024 * 1024 * 1024, // 4GB
        modelSize = 75L * 1024 * 1024 // 75MB
    ),
    supportedBackends = listOf(STTBackend.CPU, STTBackend.GPU),
    accuracy = 0.85f,
    fileSize = 75L * 1024 * 1024
)

// Load STT model
sttEngine.loadSTTModel(sttModel).getOrThrow()

// Start listening with streaming recognition
val config = ListeningConfig(
    streamingMode = true,
    endOfSpeechSilenceMs = 1500,
    language = "en"
)

sttEngine.startListening(config).collect { result ->
    when (result) {
        is SpeechRecognitionResult.ListeningStarted -> {
            Log.i("Voice", "Started listening: ${result.sessionId}")
        }
        is SpeechRecognitionResult.SpeechDetected -> {
            Log.i("Voice", "Speech detected")
        }
        is SpeechRecognitionResult.PartialTranscription -> {
            Log.d("Voice", "Partial: ${result.text}")
        }
        is SpeechRecognitionResult.FinalTranscription -> {
            Log.i("Voice", "Final: ${result.text} (confidence: ${result.confidence})")
        }
        is SpeechRecognitionResult.Error -> {
            Log.e("Voice", "Error: ${result.message}")
        }
        else -> {}
    }
}

// Transcribe audio file
val audioFile = File("/path/to/audio.wav")
val transcription = sttEngine.transcribeAudio(audioFile, "en").getOrThrow()
Log.i("Voice", "Transcription: ${transcription.text}")
```

### Text-to-Speech Usage

```kotlin
// Define TTS model
val ttsModel = TTSModelDescriptor(
    id = "piper-en",
    name = "Piper English",
    description = "Fast TTS model",
    supportedLanguages = listOf("en"),
    supportedVoices = listOf(
        VoiceDescriptor("en_US-amy", "Amy (US)", "en", VoiceGender.FEMALE)
    ),
    audioFormat = AudioFormat(
        sampleRate = 22050,
        channels = 1,
        bitDepth = 16,
        encoding = AudioEncoding.PCM_16BIT
    ),
    memoryRequirements = MemoryRequirements(
        minRAM = 2L * 1024 * 1024 * 1024,
        recommendedRAM = 4L * 1024 * 1024 * 1024,
        modelSize = 50L * 1024 * 1024
    ),
    supportedBackends = listOf(TTSBackend.CPU, TTSBackend.GPU),
    quality = 0.9f,
    fileSize = 50L * 1024 * 1024
)

// Load TTS model
ttsEngine.loadTTSModel(ttsModel).getOrThrow()

// Synthesize and speak text
val text = "Hello, this is a test of the text to speech engine."
val parameters = SpeechParameters(
    speakingRate = 1.0f,
    pitch = 1.0f,
    volume = 1.0f
)

ttsEngine.speak(text, parameters).getOrThrow()

// Stream speech synthesis for longer text
ttsEngine.streamSpeech(text, parameters).collect { chunk ->
    Log.d("Voice", "Audio chunk: ${chunk.samples.size} samples at ${chunk.sampleRate}Hz")
}
```

### Audio Processing Usage

```kotlin
// Start recording
val recordingFlow = audioProcessor.startRecording(
    sampleRate = 16000,
    channels = 1,
    config = AudioConfig(
        noiseReduction = true,
        automaticGainControl = true,
        echoCancellation = false
    )
)

recordingFlow.collect { data ->
    when (data) {
        is AudioData.Chunk -> {
            Log.d("Audio", "Recorded ${data.samples.size} samples")
            // Process audio samples
        }
        is AudioData.Error -> {
            Log.e("Audio", "Recording error: ${data.message}")
        }
        is AudioData.Ended -> {
            Log.i("Audio", "Recording ended")
        }
    }
}

// Load and play audio file
val audioSamples = audioProcessor.loadAudioFile(File("/path/to/audio.wav")).getOrThrow()
audioProcessor.playAudio(audioSamples, 22050).getOrThrow()

// Save audio to file
audioProcessor.saveAudioFile(
    audioData = audioSamples,
    file = File("/path/to/output.wav"),
    sampleRate = 22050,
    format = AudioFileFormat.WAV
).getOrThrow()
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

### Voice Processing Tests (To Be Implemented)
- SpeechToTextEngine: Model loading, streaming recognition, VAD, batch transcription
- TextToSpeechEngine: Model loading, speech synthesis, streaming, playback
- AudioProcessor: Recording, playback, file I/O, audio preprocessing
- Mock implementations available for integration testing

Run tests:
```bash
./gradlew :core-multimodal:test
```

## Dependencies

### Module Dependencies
- `:common` - Common types and utilities
- `:core-hw` - Device profiling
- `:core-models` - Base model types
- `:app` - EventBus for inter-module communication

### External Dependencies
- Hilt - Dependency injection
- Coroutines - Async operations
- Kotlinx Serialization - JSON parsing
- Android Bitmap APIs - Image processing
- Android AudioRecord/AudioTrack - Audio capture and playback

## Future Work

### Native Integration

#### Vision Processing
The vision processing engine is ready for integration with llama.cpp vision API:

1. **JNI Bridge**: Create native bindings to llama.cpp multimodal functions
2. **Vision Encoder**: Integrate CLIP or similar vision encoder for image embeddings
3. **Cross-Modal Attention**: Implement attention mechanism between vision and text
4. **Streaming**: Add support for streaming response generation
5. **Optimization**: Implement hardware acceleration (GPU, NPU) for vision processing

#### Voice Processing
The voice processing engines are ready for native STT and TTS model integration:

1. **STT Integration**: Integrate Whisper.cpp or similar for on-device speech recognition
2. **TTS Integration**: Integrate Piper, Coqui TTS, or similar for speech synthesis
3. **Advanced VAD**: Implement WebRTC VAD or similar for better speech detection
4. **Model Quantization**: Support Q4, Q5, Q8 quantized voice models
5. **Hardware Acceleration**: GPU/NPU acceleration for voice models
6. **SSML Support**: Parse and handle SSML for advanced speech control
7. **Multi-Speaker TTS**: Support multiple voice personalities and languages

### Additional Features

#### Vision
- OCR capabilities with dedicated models
- Document analysis for forms and invoices
- Screenshot analysis for UI understanding
- Batch image processing
- Model quantization support (Q8, Q5, Q4)

#### Voice
- Wake word detection for hands-free activation
- Speaker diarization for multi-speaker scenarios
- Real-time transcription with punctuation restoration
- Voice cloning and custom voice training
- Audio effects and post-processing
- Multi-channel audio support

## Performance Considerations

### Memory Management
- Maximum 2 models cached simultaneously
- Automatic LRU eviction of oldest model
- Bitmap recycling after processing
- ByteArray reuse for image data
- Audio buffer pooling for reduced allocations

### Image Optimization
- Aspect-ratio-preserving resizing
- Configurable JPEG quality (default 85%)
- Format-specific compression settings
- Size limit enforcement (10MB max)

### Audio Optimization
- Low-latency audio buffers (4x minimum buffer size)
- Real-time audio preprocessing with minimal CPU overhead
- Efficient PCM conversion (16-bit to float)
- Memory-efficient audio chunk processing

### Compatibility Scoring
- Device class-aware recommendations
- Memory availability checks
- Performance estimation based on device tier
- Thermal considerations for sustained inference

### Privacy & Security
- All voice processing occurs on-device
- No audio data transmitted to external servers
- Audio buffers cleared after processing
- Temporary files securely deleted

## License

Part of the Iris project. See main repository LICENSE for details.
