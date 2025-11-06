# Issue #04: Model Management & Registry System

## 🎯 Epic: Model Infrastructure
**Priority**: P1 (High)  
**Estimate**: 6-8 days  
**Dependencies**: #01 (Core Architecture), #02 (Native llama.cpp), #03 (Hardware Detection)  
**Architecture Reference**: [docs/architecture.md](../architecture.md) - Section 11.1 APK Structure & Build Configuration

## 📋 Overview
Implement comprehensive model management system that handles GGUF model discovery, download, validation, storage, and selection with device-aware recommendations. This system provides the foundation for all AI model operations in iris_android.

## 🎯 Goals
- **Model Registry**: Centralized catalog of available models with metadata
- **Smart Downloads**: Device-aware model recommendations and efficient downloads
- **Storage Management**: Secure local storage with integrity validation
- **Performance Optimization**: Model selection based on device capabilities
- **User Experience**: Intuitive model management interface

## 📝 Detailed Tasks

### 1. Model Registry & Metadata System

#### 1.1 Model Catalog Schema
Create `core-models/src/main/assets/models.json`:

```json
{
  "version": "1.0",
  "lastUpdated": "2025-11-05T00:00:00Z",
  "models": {
    "llm": [
      {
        "id": "tinyllama-1.1b-q4_0",
        "name": "TinyLlama 1.1B Q4_0",
        "description": "Ultra-lightweight model for budget devices",
        "type": "llm",
        "parameterCount": "1.1B",
        "quantization": "Q4_0",
        "fileSize": 669868800,
        "contextSize": 2048,
        "vocabSize": 32000,
        "capabilities": ["text_generation", "chat"],
        "license": "apache-2.0",
        "architecture": "llama",
        "downloadUrl": "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.q4_0.gguf",
        "sha256": "d8c2c9d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2",
        "deviceRequirements": {
          "minRAM": 2147483648,
          "recommendedRAM": 3221225472,
          "minAndroidVersion": 29,
          "supportedBackends": ["CPU_NEON"],
          "deviceClass": ["BUDGET", "MID_RANGE", "HIGH_END", "FLAGSHIP"]
        },
        "performance": {
          "tokensPerSecond": {
            "CPU_NEON": {
              "BUDGET": 2.5,
              "MID_RANGE": 4.0,
              "HIGH_END": 6.0,
              "FLAGSHIP": 8.0
            }
          },
          "powerConsumption": "LOW",
          "thermalProfile": "COOL"
        }
      },
      {
        "id": "phi-3-mini-4k-q4_k_m",
        "name": "Phi-3 Mini 4K Q4_K_M",
        "description": "Microsoft's efficient 3.8B parameter model",
        "type": "llm",
        "parameterCount": "3.8B",
        "quantization": "Q4_K_M",
        "fileSize": 2147483648,
        "contextSize": 4096,
        "vocabSize": 32064,
        "capabilities": ["text_generation", "chat", "reasoning"],
        "license": "mit",
        "architecture": "phi3",
        "downloadUrl": "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf",
        "sha256": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2",
        "deviceRequirements": {
          "minRAM": 4294967296,
          "recommendedRAM": 6442450944,
          "minAndroidVersion": 29,
          "supportedBackends": ["CPU_NEON", "OPENCL_ADRENO", "VULKAN_MALI"],
          "deviceClass": ["MID_RANGE", "HIGH_END", "FLAGSHIP"]
        },
        "performance": {
          "tokensPerSecond": {
            "CPU_NEON": {
              "MID_RANGE": 2.0,
              "HIGH_END": 3.5,
              "FLAGSHIP": 5.0
            },
            "OPENCL_ADRENO": {
              "HIGH_END": 8.0,
              "FLAGSHIP": 12.0
            },
            "VULKAN_MALI": {
              "HIGH_END": 6.0,
              "FLAGSHIP": 10.0
            }
          },
          "powerConsumption": "MEDIUM",
          "thermalProfile": "WARM"
        }
      },
      {
        "id": "llama-3.2-3b-q4_k_m",
        "name": "Llama 3.2 3B Q4_K_M",
        "description": "Meta's latest efficient 3B parameter model",
        "type": "llm",
        "parameterCount": "3B",
        "quantization": "Q4_K_M",
        "fileSize": 1879048192,
        "contextSize": 131072,
        "vocabSize": 128256,
        "capabilities": ["text_generation", "chat", "reasoning", "code"],
        "license": "llama3.2",
        "architecture": "llama",
        "downloadUrl": "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
        "sha256": "b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3",
        "deviceRequirements": {
          "minRAM": 4294967296,
          "recommendedRAM": 6442450944,
          "minAndroidVersion": 29,
          "supportedBackends": ["CPU_NEON", "OPENCL_ADRENO", "VULKAN_MALI", "QNN_HEXAGON"],
          "deviceClass": ["MID_RANGE", "HIGH_END", "FLAGSHIP"]
        },
        "performance": {
          "tokensPerSecond": {
            "CPU_NEON": {
              "MID_RANGE": 1.8,
              "HIGH_END": 3.2,
              "FLAGSHIP": 4.5
            },
            "OPENCL_ADRENO": {
              "HIGH_END": 7.5,
              "FLAGSHIP": 11.0
            },
            "VULKAN_MALI": {
              "HIGH_END": 5.5,
              "FLAGSHIP": 9.0
            },
            "QNN_HEXAGON": {
              "FLAGSHIP": 15.0
            }
          },
          "powerConsumption": "MEDIUM",
          "thermalProfile": "WARM"
        }
      }
    ],
    "embedding": [
      {
        "id": "all-minilm-l6-v2-q8_0",
        "name": "All-MiniLM-L6-v2 Q8_0",
        "description": "Efficient sentence embedding model",
        "type": "embedding",
        "parameterCount": "22.7M",
        "quantization": "Q8_0",
        "fileSize": 22020096,
        "dimensions": 384,
        "maxSequenceLength": 256,
        "capabilities": ["text_embedding", "semantic_search"],
        "license": "apache-2.0",
        "architecture": "bert",
        "downloadUrl": "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2-gguf/resolve/main/all-MiniLM-L6-v2-Q8_0.gguf",
        "sha256": "c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4",
        "deviceRequirements": {
          "minRAM": 134217728,
          "recommendedRAM": 268435456,
          "minAndroidVersion": 29,
          "supportedBackends": ["CPU_NEON", "OPENCL_ADRENO", "VULKAN_MALI"],
          "deviceClass": ["BUDGET", "MID_RANGE", "HIGH_END", "FLAGSHIP"]
        }
      }
    ],
    "safety": [
      {
        "id": "llama-guard-3-8b-q4_0",
        "name": "Llama Guard 3 8B Q4_0",
        "description": "Advanced safety classification model",
        "type": "safety",
        "parameterCount": "8B",
        "quantization": "Q4_0",
        "fileSize": 4294967296,
        "contextSize": 8192,
        "capabilities": ["safety_classification", "content_moderation"],
        "license": "llama3",
        "architecture": "llama",
        "downloadUrl": "https://huggingface.co/bartowski/Llama-Guard-3-8B-GGUF/resolve/main/Llama-Guard-3-8B-Q4_0.gguf",
        "sha256": "d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5",
        "deviceRequirements": {
          "minRAM": 6442450944,
          "recommendedRAM": 8589934592,
          "minAndroidVersion": 29,
          "supportedBackends": ["CPU_NEON", "OPENCL_ADRENO", "VULKAN_MALI"],
          "deviceClass": ["HIGH_END", "FLAGSHIP"]
        }
      }
    ]
  },
  "categories": {
    "llm": {
      "name": "Language Models",
      "description": "Text generation and chat models",
      "icon": "chat_bubble"
    },
    "embedding": {
      "name": "Embedding Models",
      "description": "Models for text embeddings and semantic search",
      "icon": "vector_combine"
    },
    "safety": {
      "name": "Safety Models",
      "description": "Content moderation and safety classification",
      "icon": "security"
    }
  }
}
```

#### 1.2 Model Registry Implementation
Create `core-models/src/main/kotlin/ModelRegistryImpl.kt`:

```kotlin
@Singleton
class ModelRegistryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceProfileProvider: DeviceProfileProvider,
    private val networkManager: NetworkManager
) : ModelRegistry {
    
    companion object {
        private const val TAG = "ModelRegistry"
        private const val MODELS_CATALOG_FILE = "models.json"
        private const val REGISTRY_CACHE_KEY = "model_registry_cache"
        private const val CACHE_VALIDITY_HOURS = 24
    }
    
    private var cachedRegistry: ModelCatalog? = null
    private val preferences = context.getSharedPreferences("model_registry", Context.MODE_PRIVATE)
    
    override suspend fun getAvailableModels(type: ModelType?): List<ModelDescriptor> {
        val catalog = getModelCatalog()
        return when (type) {
            ModelType.LLM -> catalog.models.llm
            ModelType.EMBEDDING -> catalog.models.embedding
            ModelType.SAFETY -> catalog.models.safety
            null -> catalog.models.llm + catalog.models.embedding + catalog.models.safety
        }
    }
    
    override suspend fun getRecommendedModels(): List<ModelRecommendation> {
        val deviceProfile = deviceProfileProvider.getDeviceProfile()
        val availableModels = getAvailableModels()
        
        return availableModels.mapNotNull { model ->
            val compatibility = assessModelCompatibility(model, deviceProfile)
            if (compatibility.isCompatible) {
                ModelRecommendation(
                    model = model,
                    compatibilityScore = compatibility.score,
                    recommendationReason = compatibility.reason,
                    estimatedPerformance = estimatePerformance(model, deviceProfile),
                    category = determineRecommendationCategory(compatibility.score, deviceProfile)
                )
            } else null
        }.sortedByDescending { it.compatibilityScore }
    }
    
    override suspend fun getModelById(modelId: String): ModelDescriptor? {
        return getAvailableModels().find { it.id == modelId }
    }
    
    override suspend fun validateModel(modelDescriptor: ModelDescriptor): ModelValidationResult {
        return try {
            // Check device compatibility
            val deviceProfile = deviceProfileProvider.getDeviceProfile()
            val compatibility = assessModelCompatibility(modelDescriptor, deviceProfile)
            
            if (!compatibility.isCompatible) {
                return ModelValidationResult(
                    isValid = false,
                    reason = compatibility.reason,
                    issues = listOf(ValidationIssue.DEVICE_INCOMPATIBLE)
                )
            }
            
            // Check storage space
            val availableSpace = getAvailableStorageSpace()
            if (availableSpace < modelDescriptor.fileSize + STORAGE_BUFFER) {
                return ModelValidationResult(
                    isValid = false,
                    reason = "Insufficient storage space. Need ${formatBytes(modelDescriptor.fileSize)} but only ${formatBytes(availableSpace)} available.",
                    issues = listOf(ValidationIssue.INSUFFICIENT_STORAGE)
                )
            }
            
            // Validate download URL accessibility
            val urlAccessible = networkManager.isUrlAccessible(modelDescriptor.downloadUrl)
            if (!urlAccessible) {
                return ModelValidationResult(
                    isValid = false,
                    reason = "Download URL not accessible",
                    issues = listOf(ValidationIssue.URL_INACCESSIBLE)
                )
            }
            
            ModelValidationResult(
                isValid = true,
                reason = "Model is compatible and ready for download",
                issues = emptyList()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Model validation failed for ${modelDescriptor.id}", e)
            ModelValidationResult(
                isValid = false,
                reason = "Validation failed: ${e.message}",
                issues = listOf(ValidationIssue.VALIDATION_ERROR)
            )
        }
    }
    
    override suspend fun refreshCatalog(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Try to fetch updated catalog from remote source
            val updatedCatalog = fetchRemoteCatalog()
            
            if (updatedCatalog != null) {
                // Validate and cache the new catalog
                cachedRegistry = updatedCatalog
                cacheCatalog(updatedCatalog)
                Log.i(TAG, "Model catalog refreshed successfully")
                Result.success(Unit)
            } else {
                // Fall back to bundled catalog
                Log.w(TAG, "Failed to fetch remote catalog, using bundled version")
                cachedRegistry = loadBundledCatalog()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh model catalog", e)
            Result.failure(e)
        }
    }
    
    private suspend fun getModelCatalog(): ModelCatalog {
        // Return cached if available and fresh
        cachedRegistry?.let { cached ->
            if (isCacheValid()) {
                return cached
            }
        }
        
        // Try to load from cache
        val cachedCatalog = loadCachedCatalog()
        if (cachedCatalog != null && isCacheValid()) {
            cachedRegistry = cachedCatalog
            return cachedCatalog
        }
        
        // Refresh catalog in background and return bundled for now
        CoroutineScope(Dispatchers.IO).launch {
            refreshCatalog()
        }
        
        val bundledCatalog = loadBundledCatalog()
        cachedRegistry = bundledCatalog
        return bundledCatalog
    }
    
    private fun loadBundledCatalog(): ModelCatalog {
        return try {
            val inputStream = context.assets.open(MODELS_CATALOG_FILE)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            gson.fromJson(jsonString, ModelCatalog::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bundled model catalog", e)
            // Return empty catalog as fallback
            ModelCatalog(
                version = "1.0",
                lastUpdated = System.currentTimeMillis(),
                models = ModelCollections(
                    llm = emptyList(),
                    embedding = emptyList(),
                    safety = emptyList()
                ),
                categories = emptyMap()
            )
        }
    }
    
    private fun loadCachedCatalog(): ModelCatalog? {
        return try {
            val cachedJson = preferences.getString(REGISTRY_CACHE_KEY, null) ?: return null
            gson.fromJson(cachedJson, ModelCatalog::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load cached catalog", e)
            null
        }
    }
    
    private fun cacheCatalog(catalog: ModelCatalog) {
        try {
            val json = gson.toJson(catalog)
            preferences.edit()
                .putString(REGISTRY_CACHE_KEY, json)
                .putLong("cache_timestamp", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache catalog", e)
        }
    }
    
    private fun isCacheValid(): Boolean {
        val cacheTimestamp = preferences.getLong("cache_timestamp", 0)
        val ageHours = (System.currentTimeMillis() - cacheTimestamp) / (1000 * 60 * 60)
        return ageHours < CACHE_VALIDITY_HOURS
    }
    
    private suspend fun fetchRemoteCatalog(): ModelCatalog? {
        // TODO: Implement remote catalog fetching
        // This could fetch from GitHub releases, HuggingFace, or custom server
        return null
    }
    
    private fun assessModelCompatibility(
        model: ModelDescriptor,
        deviceProfile: DeviceProfile
    ): CompatibilityAssessment {
        val issues = mutableListOf<String>()
        var score = 100
        
        // Check RAM requirements
        if (deviceProfile.totalRAM < model.deviceRequirements.minRAM) {
            return CompatibilityAssessment(
                isCompatible = false,
                score = 0,
                reason = "Insufficient RAM: need ${formatBytes(model.deviceRequirements.minRAM)}, have ${formatBytes(deviceProfile.totalRAM)}"
            )
        }
        
        if (deviceProfile.totalRAM < model.deviceRequirements.recommendedRAM) {
            score -= 20
            issues.add("Below recommended RAM")
        }
        
        // Check Android version
        if (deviceProfile.androidVersion < model.deviceRequirements.minAndroidVersion) {
            return CompatibilityAssessment(
                isCompatible = false,
                score = 0,
                reason = "Android version too old: need API ${model.deviceRequirements.minAndroidVersion}, have ${deviceProfile.androidVersion}"
            )
        }
        
        // Check device class
        if (!model.deviceRequirements.deviceClass.contains(deviceProfile.deviceClass)) {
            score -= 30
            issues.add("Not optimized for ${deviceProfile.deviceClass} devices")
        }
        
        // Check backend support
        val supportedBackends = model.deviceRequirements.supportedBackends.intersect(
            getAvailableBackends(deviceProfile)
        )
        
        if (supportedBackends.isEmpty()) {
            return CompatibilityAssessment(
                isCompatible = false,
                score = 0,
                reason = "No compatible backends available on this device"
            )
        }
        
        // Bonus for optimal backend availability
        val optimalBackend = getOptimalBackend(deviceProfile)
        if (model.deviceRequirements.supportedBackends.contains(optimalBackend)) {
            score += 10
        }
        
        val reason = when {
            score >= 90 -> "Excellent compatibility - optimal for your device"
            score >= 70 -> "Good compatibility with minor limitations: ${issues.joinToString(", ")}"
            score >= 50 -> "Fair compatibility with some limitations: ${issues.joinToString(", ")}"
            else -> "Poor compatibility: ${issues.joinToString(", ")}"
        }
        
        return CompatibilityAssessment(
            isCompatible = true,
            score = score,
            reason = reason
        )
    }
    
    private fun estimatePerformance(
        model: ModelDescriptor,
        deviceProfile: DeviceProfile
    ): PerformanceEstimate {
        val backendType = getOptimalBackend(deviceProfile)
        val deviceClass = deviceProfile.deviceClass
        
        val tokensPerSecond = model.performance?.tokensPerSecond
            ?.get(backendType.name)
            ?.get(deviceClass.name)
            ?: 1.0
        
        val powerConsumption = model.performance?.powerConsumption ?: "UNKNOWN"
        val thermalProfile = model.performance?.thermalProfile ?: "UNKNOWN"
        
        return PerformanceEstimate(
            expectedTokensPerSecond = tokensPerSecond,
            powerConsumption = PowerConsumption.valueOf(powerConsumption),
            thermalProfile = ThermalProfile.valueOf(thermalProfile),
            backend = backendType
        )
    }
    
    private fun determineRecommendationCategory(
        score: Int,
        deviceProfile: DeviceProfile
    ): RecommendationCategory {
        return when {
            score >= 90 -> RecommendationCategory.RECOMMENDED
            score >= 70 -> RecommendationCategory.COMPATIBLE
            score >= 50 -> RecommendationCategory.EXPERIMENTAL
            else -> RecommendationCategory.NOT_RECOMMENDED
        }
    }
    
    private fun getAvailableBackends(deviceProfile: DeviceProfile): Set<BackendType> {
        val backends = mutableSetOf(BackendType.CPU_NEON)
        
        if (deviceProfile.capabilities.contains(HardwareCapability.OPENCL)) {
            backends.add(BackendType.OPENCL_ADRENO)
        }
        
        if (deviceProfile.capabilities.contains(HardwareCapability.VULKAN)) {
            backends.add(BackendType.VULKAN_MALI)
        }
        
        if (deviceProfile.capabilities.contains(HardwareCapability.QNN)) {
            backends.add(BackendType.QNN_HEXAGON)
        }
        
        return backends
    }
    
    private fun getOptimalBackend(deviceProfile: DeviceProfile): BackendType {
        return when (deviceProfile.socVendor) {
            SoCVendor.QUALCOMM -> {
                when (deviceProfile.deviceClass) {
                    DeviceClass.FLAGSHIP -> BackendType.QNN_HEXAGON
                    DeviceClass.HIGH_END -> BackendType.OPENCL_ADRENO
                    else -> BackendType.CPU_NEON
                }
            }
            SoCVendor.SAMSUNG, SoCVendor.GOOGLE -> {
                when (deviceProfile.deviceClass) {
                    DeviceClass.FLAGSHIP, DeviceClass.HIGH_END -> BackendType.VULKAN_MALI
                    else -> BackendType.CPU_NEON
                }
            }
            else -> BackendType.CPU_NEON
        }
    }
    
    private fun getAvailableStorageSpace(): Long {
        val modelsDir = getModelsDirectory()
        return modelsDir.usableSpace
    }
    
    private fun getModelsDirectory(): File {
        return File(context.getExternalFilesDir(null), "models").also { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }
    
    companion object {
        private val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
        
        private const val STORAGE_BUFFER = 500L * 1024 * 1024 // 500MB buffer
        
        private fun formatBytes(bytes: Long): String {
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var size = bytes.toDouble()
            var unitIndex = 0
            
            while (size >= 1024 && unitIndex < units.size - 1) {
                size /= 1024
                unitIndex++
            }
            
            return "%.1f %s".format(size, units[unitIndex])
        }
    }
}

// Data classes
data class ModelCatalog(
    val version: String,
    val lastUpdated: Long,
    val models: ModelCollections,
    val categories: Map<String, CategoryInfo>
)

data class ModelCollections(
    val llm: List<ModelDescriptor>,
    val embedding: List<ModelDescriptor>,
    val safety: List<ModelDescriptor>
)

data class CategoryInfo(
    val name: String,
    val description: String,
    val icon: String
)

data class ModelRecommendation(
    val model: ModelDescriptor,
    val compatibilityScore: Int,
    val recommendationReason: String,
    val estimatedPerformance: PerformanceEstimate,
    val category: RecommendationCategory
)

data class CompatibilityAssessment(
    val isCompatible: Boolean,
    val score: Int,
    val reason: String
)

data class PerformanceEstimate(
    val expectedTokensPerSecond: Double,
    val powerConsumption: PowerConsumption,
    val thermalProfile: ThermalProfile,
    val backend: BackendType
)

data class ModelValidationResult(
    val isValid: Boolean,
    val reason: String,
    val issues: List<ValidationIssue>
)

enum class RecommendationCategory {
    RECOMMENDED, COMPATIBLE, EXPERIMENTAL, NOT_RECOMMENDED
}

enum class PowerConsumption {
    LOW, MEDIUM, HIGH, UNKNOWN
}

enum class ThermalProfile {
    COOL, WARM, HOT, UNKNOWN
}

enum class ValidationIssue {
    DEVICE_INCOMPATIBLE,
    INSUFFICIENT_STORAGE,
    URL_INACCESSIBLE,
    VALIDATION_ERROR
}
```

### 2. Model Download & Storage System

#### 2.1 Model Downloader Implementation
Create `core-models/src/main/kotlin/ModelDownloaderImpl.kt`:

```kotlin
@Singleton
class ModelDownloaderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkManager: NetworkManager,
    private val eventBus: EventBus
) : ModelDownloader {
    
    companion object {
        private const val TAG = "ModelDownloader"
        private const val DOWNLOAD_CHUNK_SIZE = 8192
        private const val MAX_CONCURRENT_DOWNLOADS = 2
        private const val DOWNLOAD_TIMEOUT_MS = 30_000L
        private const val INTEGRITY_CHECK_CHUNK_SIZE = 1024 * 1024 // 1MB
    }
    
    private val activeDownloads = mutableMapOf<String, DownloadJob>()
    private val downloadSemaphore = Semaphore(MAX_CONCURRENT_DOWNLOADS)
    
    override suspend fun downloadModel(
        modelDescriptor: ModelDescriptor,
        progressCallback: ((Progress) -> Unit)?
    ): Result<DownloadResult> = withContext(Dispatchers.IO) {
        
        if (activeDownloads.containsKey(modelDescriptor.id)) {
            return@withContext Result.failure(
                DownloadException("Download already in progress for ${modelDescriptor.id}")
            )
        }
        
        try {
            downloadSemaphore.acquire()
            
            val downloadJob = DownloadJob(
                modelId = modelDescriptor.id,
                url = modelDescriptor.downloadUrl,
                expectedSize = modelDescriptor.fileSize,
                expectedHash = modelDescriptor.sha256,
                targetFile = getModelFile(modelDescriptor),
                progressCallback = progressCallback
            )
            
            activeDownloads[modelDescriptor.id] = downloadJob
            
            val result = performDownload(downloadJob)
            
            activeDownloads.remove(modelDescriptor.id)
            downloadSemaphore.release()
            
            result
            
        } catch (e: Exception) {
            activeDownloads.remove(modelDescriptor.id)
            downloadSemaphore.release()
            Result.failure(DownloadException("Download failed for ${modelDescriptor.id}", e))
        }
    }
    
    override suspend fun cancelDownload(modelId: String): Boolean {
        val downloadJob = activeDownloads[modelId] ?: return false
        
        downloadJob.cancel()
        activeDownloads.remove(modelId)
        
        // Clean up partial download
        try {
            if (downloadJob.targetFile.exists()) {
                downloadJob.targetFile.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clean up partial download", e)
        }
        
        eventBus.emit(IrisEvent.ModelDownloadCancelled(modelId))
        return true
    }
    
    override suspend fun verifyModelIntegrity(
        modelDescriptor: ModelDescriptor,
        localFile: File
    ): Result<VerificationResult> = withContext(Dispatchers.IO) {
        
        try {
            if (!localFile.exists()) {
                return@withContext Result.failure(
                    VerificationException("Model file does not exist: ${localFile.path}")
                )
            }
            
            // Check file size
            if (localFile.length() != modelDescriptor.fileSize) {
                return@withContext Result.failure(
                    VerificationException(
                        "File size mismatch: expected ${modelDescriptor.fileSize}, got ${localFile.length()}"
                    )
                )
            }
            
            // Verify SHA-256 hash
            val calculatedHash = calculateSHA256(localFile)
            val expectedHash = modelDescriptor.sha256.lowercase()
            
            if (calculatedHash != expectedHash) {
                return@withContext Result.failure(
                    VerificationException(
                        "Hash mismatch: expected $expectedHash, got $calculatedHash"
                    )
                )
            }
            
            // Verify GGUF format (basic check)
            val isValidGGUF = verifyGGUFFormat(localFile)
            if (!isValidGGUF) {
                return@withContext Result.failure(
                    VerificationException("Invalid GGUF file format")
                )
            }
            
            Result.success(
                VerificationResult(
                    isValid = true,
                    fileSize = localFile.length(),
                    calculatedHash = calculatedHash,
                    verificationTime = System.currentTimeMillis()
                )
            )
            
        } catch (e: Exception) {
            Result.failure(VerificationException("Verification failed", e))
        }
    }
    
    override suspend fun getDownloadProgress(modelId: String): DownloadProgress? {
        return activeDownloads[modelId]?.getProgress()
    }
    
    override suspend fun getActiveDownloads(): List<String> {
        return activeDownloads.keys.toList()
    }
    
    private suspend fun performDownload(downloadJob: DownloadJob): Result<DownloadResult> {
        return try {
            eventBus.emit(IrisEvent.ModelDownloadStarted(downloadJob.modelId))
            
            val startTime = System.currentTimeMillis()
            
            // Create temporary file for download
            val tempFile = File(downloadJob.targetFile.parent, "${downloadJob.targetFile.name}.tmp")
            
            // Download with progress tracking
            val downloadSuccess = downloadWithProgress(downloadJob, tempFile)
            
            if (!downloadSuccess) {
                tempFile.delete()
                return Result.failure(DownloadException("Download failed"))
            }
            
            // Verify integrity
            val calculatedHash = calculateSHA256(tempFile)
            if (calculatedHash != downloadJob.expectedHash.lowercase()) {
                tempFile.delete()
                return Result.failure(
                    DownloadException("Downloaded file hash mismatch")
                )
            }
            
            // Move to final location
            if (!tempFile.renameTo(downloadJob.targetFile)) {
                tempFile.delete()
                return Result.failure(
                    DownloadException("Failed to move downloaded file to final location")
                )
            }
            
            val downloadTime = System.currentTimeMillis() - startTime
            val downloadSpeed = (downloadJob.expectedSize.toDouble() / downloadTime) * 1000 // bytes/sec
            
            val result = DownloadResult(
                modelId = downloadJob.modelId,
                filePath = downloadJob.targetFile.absolutePath,
                fileSize = downloadJob.targetFile.length(),
                downloadTime = downloadTime,
                downloadSpeed = downloadSpeed,
                verified = true
            )
            
            eventBus.emit(IrisEvent.ModelDownloadCompleted(downloadJob.modelId, result))
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for ${downloadJob.modelId}", e)
            eventBus.emit(IrisEvent.ModelDownloadFailed(downloadJob.modelId, e.message ?: "Unknown error"))
            Result.failure(DownloadException("Download failed", e))
        }
    }
    
    private suspend fun downloadWithProgress(
        downloadJob: DownloadJob,
        targetFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(DOWNLOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(DOWNLOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build()
            
            val request = Request.Builder()
                .url(downloadJob.url)
                .addHeader("User-Agent", "iris-android/1.0")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Download request failed: ${response.code}")
                    return@withContext false
                }
                
                val responseBody = response.body ?: return@withContext false
                val contentLength = responseBody.contentLength()
                
                if (contentLength != downloadJob.expectedSize) {
                    Log.w(TAG, "Content length mismatch: expected ${downloadJob.expectedSize}, got $contentLength")
                }
                
                var downloadedBytes = 0L
                val buffer = ByteArray(DOWNLOAD_CHUNK_SIZE)
                
                responseBody.byteStream().use { inputStream ->
                    targetFile.outputStream().use { outputStream ->
                        
                        while (!downloadJob.isCancelled) {
                            val bytesRead = inputStream.read(buffer)
                            if (bytesRead == -1) break
                            
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            
                            // Update progress
                            val progress = Progress(
                                downloadedBytes = downloadedBytes,
                                totalBytes = downloadJob.expectedSize,
                                percentage = (downloadedBytes * 100.0 / downloadJob.expectedSize).toInt(),
                                speed = calculateCurrentSpeed(downloadJob, downloadedBytes)
                            )
                            
                            downloadJob.updateProgress(progress)
                            downloadJob.progressCallback?.invoke(progress)
                        }
                        
                        outputStream.flush()
                    }
                }
                
                !downloadJob.isCancelled && downloadedBytes == downloadJob.expectedSize
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            false
        }
    }
    
    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(INTEGRITY_CHECK_CHUNK_SIZE)
        
        file.inputStream().use { inputStream ->
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    private fun verifyGGUFFormat(file: File): Boolean {
        return try {
            file.inputStream().use { inputStream ->
                val header = ByteArray(8)
                val bytesRead = inputStream.read(header)
                
                if (bytesRead < 8) return false
                
                // Check GGUF magic number: "GGUF" (0x46554747)
                val magic = String(header, 0, 4, Charsets.US_ASCII)
                magic == "GGUF"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to verify GGUF format", e)
            false
        }
    }
    
    private fun getModelFile(modelDescriptor: ModelDescriptor): File {
        val modelsDir = File(context.getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        
        val filename = "${modelDescriptor.id}.gguf"
        return File(modelsDir, filename)
    }
    
    private fun calculateCurrentSpeed(downloadJob: DownloadJob, currentBytes: Long): Double {
        val elapsedTime = System.currentTimeMillis() - downloadJob.startTime
        return if (elapsedTime > 0) {
            (currentBytes.toDouble() / elapsedTime) * 1000 // bytes/sec
        } else {
            0.0
        }
    }
}

// Data classes for download management
data class DownloadJob(
    val modelId: String,
    val url: String,
    val expectedSize: Long,
    val expectedHash: String,
    val targetFile: File,
    val progressCallback: ((Progress) -> Unit)?,
    val startTime: Long = System.currentTimeMillis()
) {
    @Volatile
    var isCancelled = false
        private set
    
    private var currentProgress = Progress(0, expectedSize, 0, 0.0)
    
    fun cancel() {
        isCancelled = true
    }
    
    fun updateProgress(progress: Progress) {
        currentProgress = progress
    }
    
    fun getProgress(): DownloadProgress {
        return DownloadProgress(
            modelId = modelId,
            downloadedBytes = currentProgress.downloadedBytes,
            totalBytes = currentProgress.totalBytes,
            percentage = currentProgress.percentage,
            speed = currentProgress.speed,
            eta = calculateETA()
        )
    }
    
    private fun calculateETA(): Long {
        val remainingBytes = expectedSize - currentProgress.downloadedBytes
        return if (currentProgress.speed > 0) {
            (remainingBytes / currentProgress.speed).toLong() * 1000
        } else {
            -1L
        }
    }
}

data class Progress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val percentage: Int,
    val speed: Double // bytes/sec
)

data class DownloadProgress(
    val modelId: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val percentage: Int,
    val speed: Double,
    val eta: Long // milliseconds, -1 if unknown
)

data class DownloadResult(
    val modelId: String,
    val filePath: String,
    val fileSize: Long,
    val downloadTime: Long,
    val downloadSpeed: Double,
    val verified: Boolean
)

data class VerificationResult(
    val isValid: Boolean,
    val fileSize: Long,
    val calculatedHash: String,
    val verificationTime: Long
)

// Exception classes
class DownloadException(message: String, cause: Throwable? = null) : Exception(message, cause)
class VerificationException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

## 🧪 Testing Strategy

### Unit Tests
- [ ] **Model Registry Logic**
  - Catalog parsing and validation
  - Compatibility assessment accuracy
  - Recommendation algorithm correctness
  - Cache management functionality

### Integration Tests
- [ ] **Download System**
  - End-to-end download flow
  - Progress tracking accuracy
  - Integrity verification
  - Error handling and recovery

### UI Tests
- [ ] **Model Manager Interface**
  - Model browsing and filtering
  - Download progress display
  - Error message presentation
  - Storage management

### Performance Tests
- [ ] **Download Performance**
  - Concurrent download handling
  - Large model download stability
  - Network interruption recovery
  - Storage efficiency

## ✅ Acceptance Criteria

### Primary Criteria
- [ ] **Model Discovery**: Users can browse and discover appropriate models
- [ ] **Smart Recommendations**: Device-aware model recommendations work correctly
- [ ] **Reliable Downloads**: Model downloads complete successfully with progress tracking
- [ ] **Integrity Verification**: All downloaded models pass integrity checks
- [ ] **Storage Management**: Efficient storage usage with cleanup capabilities

### Technical Criteria
- [ ] **Catalog Performance**: Model catalog loads in <3 seconds
- [ ] **Download Speed**: Download speed matches device network capability
- [ ] **Memory Efficiency**: Download process uses <100MB peak memory
- [ ] **Error Recovery**: Robust error handling for network and storage issues

### User Experience Criteria
- [ ] **Intuitive Interface**: Clear model information and recommendations
- [ ] **Progress Feedback**: Real-time download progress with ETA
- [ ] **Storage Awareness**: Clear storage usage and space requirements
- [ ] **Offline Support**: Basic functionality works without network

## 🔗 Related Issues
- **Depends on**: #01 (Core Architecture), #02 (Native llama.cpp), #03 (Hardware Detection)
- **Enables**: #05 (Chat Engine), #10 (Safety Engine), #06 (RAG Engine)
- **Related**: #14 (UI/UX Implementation)

## 📋 Definition of Done
- [ ] Complete model registry system with device-aware recommendations
- [ ] Robust download system with progress tracking and verification
- [ ] Model storage management with integrity checking
- [ ] Comprehensive test suite covering all scenarios
- [ ] Performance benchmarks meet acceptance criteria
- [ ] User interface for model management functional
- [ ] Documentation complete with supported models and requirements
- [ ] Code review completed and approved

---

**Note**: This system provides the foundation for all model-related operations. The model registry will be regularly updated with new models as they become available and tested.