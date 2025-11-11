# Multimodal Module Integration Example

This document shows how to integrate the core-multimodal module into the main Iris application.

## 1. Add Module Dependency

In `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":core-multimodal"))
    // ... other dependencies
}
```

## 2. Dependency Injection Setup

Create provider modules for multimodal services:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object MultimodalModule {
    
    @Provides
    @Singleton
    fun provideImageProcessor(@ApplicationContext context: Context): ImageProcessor {
        return MockImageProcessor(context)
    }
    
    @Provides
    @Singleton
    fun provideModelRegistry(@ApplicationContext context: Context): MultimodalModelRegistry {
        return MockMultimodalModelRegistry(context)
    }
    
    @Provides
    @Singleton
    fun provideVisionEngine(
        @ApplicationContext context: Context,
        imageProcessor: ImageProcessor
    ): VisionProcessingEngine {
        return MockVisionProcessingEngine(context)
    }
}
```

## 3. ViewModel Integration

Create a ViewModel for vision analysis:

```kotlin
@HiltViewModel
class VisionAnalysisViewModel @Inject constructor(
    private val visionEngine: VisionProcessingEngine,
    private val modelRegistry: MultimodalModelRegistry
) : ViewModel() {
    
    private val _analysisResult = MutableLiveData<VisionResult.AnalysisResult?>()
    val analysisResult: LiveData<VisionResult.AnalysisResult?> = _analysisResult
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    fun analyzeImage(imageUri: Uri, prompt: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Get recommended model for general Q&A
                val modelResult = modelRegistry.getRecommendedModel(VisionTask.GENERAL_QA)
                val model = modelResult.getOrThrow()
                
                // Analyze the image
                val analysisResult = visionEngine.analyzeImage(
                    imageUri = imageUri,
                    prompt = prompt,
                    model = model,
                    parameters = VisionParameters(
                        maxTokens = 512,
                        temperature = 0.7f,
                        confidence = 0.6f
                    )
                )
                
                analysisResult.fold(
                    onSuccess = { result ->
                        _analysisResult.value = result
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Analysis failed"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun analyzeScreenshot(screenshotData: ByteArray, prompt: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val modelResult = modelRegistry.getRecommendedModel(VisionTask.SCENE_ANALYSIS)
                val model = modelResult.getOrThrow()
                
                val result = visionEngine.processScreenshot(
                    screenshotData = screenshotData,
                    prompt = prompt,
                    model = model,
                    parameters = VisionParameters()
                )
                
                result.fold(
                    onSuccess = { screenshotResult ->
                        // Convert to AnalysisResult for UI consistency
                        _analysisResult.value = VisionResult.AnalysisResult(
                            text = "${screenshotResult.text}\n\nUI Elements: ${screenshotResult.uiElements.joinToString(", ")}",
                            confidence = screenshotResult.confidence,
                            processingTimeMs = screenshotResult.processingTimeMs
                        )
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Screenshot analysis failed"
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

## 4. UI Integration

Integrate with Compose UI:

```kotlin
@Composable
fun VisionAnalysisScreen(
    viewModel: VisionAnalysisViewModel = hiltViewModel()
) {
    val analysisResult by viewModel.analysisResult.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(initial = false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var userPrompt by remember { mutableStateOf("") }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Image selection
        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select Image")
        }
        
        selectedImageUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = "Selected image",
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
        }
        
        // Prompt input
        OutlinedTextField(
            value = userPrompt,
            onValueChange = { userPrompt = it },
            label = { Text("What do you want to know about this image?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        
        // Analyze button
        Button(
            onClick = {
                selectedImageUri?.let { uri ->
                    if (userPrompt.isNotBlank()) {
                        viewModel.analyzeImage(uri, userPrompt)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedImageUri != null && userPrompt.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text("Analyze Image")
            }
        }
        
        // Results section
        analysisResult?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Analysis Result",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result.text,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Confidence: ${(result.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Time: ${result.processingTimeMs}ms",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        // Error handling
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
```

## 5. Navigation Integration

Add to your navigation graph:

```kotlin
composable("vision_analysis") {
    VisionAnalysisScreen()
}
```

## 6. Permissions (if needed for camera/gallery access)

In `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.CAMERA" />
```

## 7. Testing

Create integration tests:

```kotlin
@Test
fun testVisionAnalysis() = runTest {
    val mockRegistry = MockMultimodalModelRegistry(context)
    val mockEngine = MockVisionProcessingEngine(context)
    
    val model = mockRegistry.getRecommendedModel(VisionTask.GENERAL_QA).getOrThrow()
    val result = mockEngine.analyzeImage(
        imageUri = mockImageUri,
        prompt = "What's in this image?",
        model = model,
        parameters = VisionParameters()
    ).getOrThrow()
    
    assertTrue(result.text.isNotEmpty())
    assertTrue(result.confidence > 0)
}
```

This integration example shows how to use the multimodal module in a real application with proper error handling, loading states, and user interface integration.