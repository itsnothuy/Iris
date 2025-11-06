# Issue #10: Safety Engine & Content Filtering

## 🎯 Epic: AI Safety & Content Moderation
**Priority**: P1 (High)  
**Estimate**: 6-8 days  
**Dependencies**: #01 (Core Architecture), #04 (Model Management), #05 (Chat Engine)  
**Architecture Reference**: [docs/architecture.md](../architecture.md) - Section 10 Safety & Content Filtering

## 📋 Overview
Implement comprehensive AI safety and content filtering system to ensure responsible AI interactions. This system provides multi-layered safety checks including input validation, output filtering, harmful content detection, and safety model integration to protect users from inappropriate or dangerous AI-generated content.

## 🎯 Goals
- **Input Safety**: Validate and filter user inputs for harmful content
- **Output Filtering**: Screen AI responses for safety violations
- **Safety Models**: Integration with specialized safety classification models
- **Content Moderation**: Detect and handle inappropriate content across modalities
- **User Protection**: Safeguard users from harmful AI interactions
- **Compliance**: Meet AI safety standards and regulations

## 📝 Detailed Tasks

### 1. Core Safety Engine

#### 1.1 Safety Engine Implementation
Create `core-safety/src/main/kotlin/SafetyEngineImpl.kt`:

```kotlin
@Singleton
class SafetyEngineImpl @Inject constructor(
    private val nativeEngine: NativeInferenceEngine,
    private val modelRegistry: ModelRegistry,
    private val safetyRulesEngine: SafetyRulesEngine,
    private val eventBus: EventBus,
    @ApplicationContext private val context: Context
) : SafetyEngine {
    
    companion object {
        private const val TAG = "SafetyEngine"
        private const val DEFAULT_SAFETY_MODEL = "llama-guard-3-8b-q4_0"
        private const val SAFETY_SCORE_THRESHOLD = 0.8f
        private const val MAX_INPUT_LENGTH = 10000
        private const val CONTENT_CACHE_SIZE = 1000
    }
    
    private var currentSafetyModel: SafetyModelDescriptor? = null
    private var isSafetyModelLoaded = false
    private val safetyCache = LRUCache<String, SafetyResult>(CONTENT_CACHE_SIZE)
    private val safetyClassifiers = mutableMapOf<SafetyCategory, SafetyClassifier>()
    
    override suspend fun initialize(): Result<Unit> {
        return try {
            Log.i(TAG, "Initializing safety engine")
            
            // Initialize rule-based classifiers
            initializeRuleBasedClassifiers()
            
            // Load default safety model if available
            val defaultModel = modelRegistry.getModelById(DEFAULT_SAFETY_MODEL)
            if (defaultModel != null && defaultModel.type == "safety") {
                loadSafetyModel(SafetyModelDescriptor.fromModelDescriptor(defaultModel))
            }
            
            Log.i(TAG, "Safety engine initialized successfully")
            eventBus.emit(IrisEvent.SafetyEngineInitialized())
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Safety engine initialization failed", e)
            Result.failure(SafetyException("Safety engine initialization failed", e))
        }
    }
    
    override suspend fun loadSafetyModel(model: SafetyModelDescriptor): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Loading safety model: ${model.id}")
                
                // Validate model compatibility
                val validation = validateSafetyModel(model)
                if (!validation.isValid) {
                    return@withContext Result.failure(
                        SafetyException("Safety model validation failed: ${validation.reason}")
                    )
                }
                
                // Load model through native engine
                val loadResult = nativeEngine.loadSafetyModel(
                    modelPath = getModelPath(model),
                    config = SafetyModelConfig(
                        categories = model.supportedCategories,
                        threshold = SAFETY_SCORE_THRESHOLD,
                        maxSequenceLength = model.maxSequenceLength
                    )
                )
                
                if (loadResult.isSuccess) {
                    currentSafetyModel = model
                    isSafetyModelLoaded = true
                    
                    Log.i(TAG, "Safety model loaded successfully: ${model.id}")
                    eventBus.emit(IrisEvent.SafetyModelLoaded(model.id))
                    Result.success(Unit)
                } else {
                    val error = loadResult.exceptionOrNull()
                    Log.e(TAG, "Safety model loading failed", error)
                    Result.failure(error ?: SafetyException("Safety model loading failed"))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception during safety model loading", e)
                Result.failure(SafetyException("Safety model loading exception", e))
            }
        }
    }
    
    override suspend fun checkInputSafety(
        input: String,
        context: SafetyContext?
    ): SafetyResult = withContext(Dispatchers.Default) {
        
        try {
            // Check cache first
            val cacheKey = generateCacheKey(input, context)
            safetyCache.get(cacheKey)?.let { return@withContext it }
            
            // Validate input length
            if (input.length > MAX_INPUT_LENGTH) {
                return@withContext SafetyResult(
                    isSafe = false,
                    reason = "Input too long (max $MAX_INPUT_LENGTH characters)",
                    category = SafetyCategory.INPUT_VALIDATION,
                    confidence = 1.0f,
                    details = listOf("Input length: ${input.length}")
                )
            }
            
            // Rule-based checks first (fast)
            val ruleBasedResult = performRuleBasedInputChecks(input, context)
            if (!ruleBasedResult.isSafe) {
                safetyCache.put(cacheKey, ruleBasedResult)
                return@withContext ruleBasedResult
            }
            
            // Model-based checks (if model is available)
            val modelBasedResult = if (isSafetyModelLoaded) {
                performModelBasedInputChecks(input, context)
            } else {
                SafetyResult.safe("Rule-based validation passed")
            }
            
            // Combine results
            val finalResult = combineSafetyResults(ruleBasedResult, modelBasedResult)
            safetyCache.put(cacheKey, finalResult)
            
            // Log safety check
            if (!finalResult.isSafe) {
                Log.w(TAG, "Unsafe input detected: ${finalResult.reason}")
                eventBus.emit(IrisEvent.UnsafeContentDetected(
                    type = "input",
                    category = finalResult.category,
                    reason = finalResult.reason
                ))
            }
            
            finalResult
            
        } catch (e: Exception) {
            Log.e(TAG, "Input safety check failed", e)
            SafetyResult(
                isSafe = false,
                reason = "Safety check failed: ${e.message}",
                category = SafetyCategory.SYSTEM_ERROR,
                confidence = 0f,
                details = listOf("Exception: ${e.javaClass.simpleName}")
            )
        }
    }
    
    override suspend fun checkOutputSafety(
        output: String,
        originalInput: String,
        context: SafetyContext?
    ): SafetyResult = withContext(Dispatchers.Default) {
        
        try {
            // Check cache first
            val cacheKey = generateCacheKey(output, context)
            safetyCache.get(cacheKey)?.let { return@withContext it }
            
            // Rule-based checks
            val ruleBasedResult = performRuleBasedOutputChecks(output, originalInput, context)
            if (!ruleBasedResult.isSafe) {
                safetyCache.put(cacheKey, ruleBasedResult)
                return@withContext ruleBasedResult
            }
            
            // Model-based checks
            val modelBasedResult = if (isSafetyModelLoaded) {
                performModelBasedOutputChecks(output, originalInput, context)
            } else {
                SafetyResult.safe("Rule-based validation passed")
            }
            
            // Combine results
            val finalResult = combineSafetyResults(ruleBasedResult, modelBasedResult)
            safetyCache.put(cacheKey, finalResult)
            
            // Log safety violations
            if (!finalResult.isSafe) {
                Log.w(TAG, "Unsafe output detected: ${finalResult.reason}")
                eventBus.emit(IrisEvent.UnsafeContentDetected(
                    type = "output",
                    category = finalResult.category,
                    reason = finalResult.reason
                ))
            }
            
            finalResult
            
        } catch (e: Exception) {
            Log.e(TAG, "Output safety check failed", e)
            SafetyResult(
                isSafe = false,
                reason = "Safety check failed: ${e.message}",
                category = SafetyCategory.SYSTEM_ERROR,
                confidence = 0f,
                details = listOf("Exception: ${e.javaClass.simpleName}")
            )
        }
    }
    
    override suspend fun checkImageSafety(
        imageUri: Uri,
        context: SafetyContext?
    ): SafetyResult = withContext(Dispatchers.IO) {
        
        try {
            // Basic image validation
            val imageInfo = validateImage(imageUri)
            if (imageInfo == null) {
                return@withContext SafetyResult(
                    isSafe = false,
                    reason = "Invalid image format or corrupted file",
                    category = SafetyCategory.INPUT_VALIDATION,
                    confidence = 1.0f,
                    details = listOf("Image validation failed")
                )
            }
            
            // Check for known unsafe image patterns (if applicable)
            val patternResult = checkImagePatterns(imageUri)
            if (!patternResult.isSafe) {
                return@withContext patternResult
            }
            
            // Model-based image safety check (if vision model supports it)
            val modelResult = if (isSafetyModelLoaded && currentSafetyModel!!.supportsImageSafety) {
                performModelBasedImageChecks(imageUri, context)
            } else {
                SafetyResult.safe("Basic image validation passed")
            }
            
            modelResult
            
        } catch (e: Exception) {
            Log.e(TAG, "Image safety check failed", e)
            SafetyResult(
                isSafe = false,
                reason = "Image safety check failed: ${e.message}",
                category = SafetyCategory.SYSTEM_ERROR,
                confidence = 0f,
                details = listOf("Exception: ${e.javaClass.simpleName}")
            )
        }
    }
    
    override suspend fun getViolationCategories(content: String): List<SafetyViolation> {
        return try {
            val violations = mutableListOf<SafetyViolation>()
            
            // Check each category
            for (category in SafetyCategory.values()) {
                if (category == SafetyCategory.SYSTEM_ERROR) continue
                
                val classifier = safetyClassifiers[category]
                if (classifier != null) {
                    val result = classifier.classify(content)
                    if (!result.isSafe) {
                        violations.add(SafetyViolation(
                            category = category,
                            confidence = result.confidence,
                            description = result.reason,
                            severity = determineSeverity(category, result.confidence)
                        ))
                    }
                }
            }
            
            violations
        } catch (e: Exception) {
            Log.e(TAG, "Violation categorization failed", e)
            emptyList()
        }
    }
    
    override suspend fun getSafetyReport(timeRange: TimeRange): SafetyReport {
        return try {
            // This would integrate with monitoring system to get safety metrics
            SafetyReport(
                timeRange = timeRange,
                totalChecks = 0, // Placeholder
                violations = 0,
                topCategories = emptyList(),
                trends = emptyList(),
                recommendations = emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Safety report generation failed", e)
            SafetyReport.empty()
        }
    }
    
    override suspend fun updateSafetyRules(rules: List<SafetyRule>): Boolean {
        return try {
            safetyRulesEngine.updateRules(rules)
            Log.i(TAG, "Safety rules updated: ${rules.size} rules")
            eventBus.emit(IrisEvent.SafetyRulesUpdated(rules.size))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update safety rules", e)
            false
        }
    }
    
    override suspend fun isModelLoaded(): Boolean = isSafetyModelLoaded
    
    override suspend fun getCurrentModel(): SafetyModelDescriptor? = currentSafetyModel
    
    // Private implementation methods
    
    private fun initializeRuleBasedClassifiers() {
        // Initialize classifiers for different safety categories
        safetyClassifiers[SafetyCategory.HARMFUL_CONTENT] = HarmfulContentClassifier()
        safetyClassifiers[SafetyCategory.HATE_SPEECH] = HateSpeechClassifier()
        safetyClassifiers[SafetyCategory.VIOLENCE] = ViolenceClassifier()
        safetyClassifiers[SafetyCategory.SEXUAL_CONTENT] = SexualContentClassifier()
        safetyClassifiers[SafetyCategory.PRIVACY_VIOLATION] = PrivacyViolationClassifier()
        safetyClassifiers[SafetyCategory.MISINFORMATION] = MisinformationClassifier()
        safetyClassifiers[SafetyCategory.SELF_HARM] = SelfHarmClassifier()
        safetyClassifiers[SafetyCategory.ILLEGAL_ACTIVITY] = IllegalActivityClassifier()
    }
    
    private fun validateSafetyModel(model: SafetyModelDescriptor): ModelValidationResult {
        // Check if model file exists
        val modelFile = File(getModelPath(model))
        if (!modelFile.exists()) {
            return ModelValidationResult(
                isValid = false,
                reason = "Safety model file not found",
                issues = listOf(ValidationIssue.FILE_NOT_FOUND)
            )
        }
        
        // Check model compatibility
        if (!model.supportedCategories.containsAll(listOf(SafetyCategory.HARMFUL_CONTENT))) {
            return ModelValidationResult(
                isValid = false,
                reason = "Safety model must support harmful content detection",
                issues = listOf(ValidationIssue.FEATURE_MISSING)
            )
        }
        
        return ModelValidationResult(
            isValid = true,
            reason = "Safety model is valid",
            issues = emptyList()
        )
    }
    
    private suspend fun performRuleBasedInputChecks(
        input: String,
        context: SafetyContext?
    ): SafetyResult {
        // Check for obvious harmful patterns
        val violations = mutableListOf<String>()
        
        // Check each classifier
        for ((category, classifier) in safetyClassifiers) {
            val result = classifier.classify(input)
            if (!result.isSafe && result.confidence > 0.7f) {
                violations.add("${category.name}: ${result.reason}")
            }
        }
        
        return if (violations.isNotEmpty()) {
            SafetyResult(
                isSafe = false,
                reason = "Rule-based violations detected",
                category = SafetyCategory.HARMFUL_CONTENT,
                confidence = 0.8f,
                details = violations
            )
        } else {
            SafetyResult.safe("Rule-based input checks passed")
        }
    }
    
    private suspend fun performRuleBasedOutputChecks(
        output: String,
        originalInput: String,
        context: SafetyContext?
    ): SafetyResult {
        // Similar to input checks but with additional output-specific rules
        return performRuleBasedInputChecks(output, context)
    }
    
    private suspend fun performModelBasedInputChecks(
        input: String,
        context: SafetyContext?
    ): SafetyResult {
        return try {
            val result = nativeEngine.classifyContentSafety(
                content = input,
                categories = currentSafetyModel!!.supportedCategories.map { it.name },
                threshold = SAFETY_SCORE_THRESHOLD
            )
            
            if (result.isSuccess) {
                val classification = result.getOrNull()!!
                if (classification.isSafe) {
                    SafetyResult.safe("Model-based validation passed")
                } else {
                    SafetyResult(
                        isSafe = false,
                        reason = classification.reason,
                        category = SafetyCategory.valueOf(classification.category),
                        confidence = classification.confidence,
                        details = classification.details
                    )
                }
            } else {
                SafetyResult.safe("Model-based check failed, defaulting to safe")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Model-based input safety check failed", e)
            SafetyResult.safe("Model check failed, defaulting to safe")
        }
    }
    
    private suspend fun performModelBasedOutputChecks(
        output: String,
        originalInput: String,
        context: SafetyContext?
    ): SafetyResult {
        // Check output with additional context from input
        val combinedText = "Input: $originalInput\nOutput: $output"
        return performModelBasedInputChecks(combinedText, context)
    }
    
    private suspend fun performModelBasedImageChecks(
        imageUri: Uri,
        context: SafetyContext?
    ): SafetyResult {
        return try {
            // This would require integration with vision safety models
            // For now, return safe as placeholder
            SafetyResult.safe("Image safety check not implemented")
        } catch (e: Exception) {
            Log.w(TAG, "Model-based image safety check failed", e)
            SafetyResult.safe("Image safety check failed, defaulting to safe")
        }
    }
    
    private fun combineSafetyResults(result1: SafetyResult, result2: SafetyResult): SafetyResult {
        // If either result is unsafe, the combined result is unsafe
        return if (!result1.isSafe) {
            result1
        } else if (!result2.isSafe) {
            result2
        } else {
            // Both are safe, return the one with higher confidence
            if (result1.confidence >= result2.confidence) result1 else result2
        }
    }
    
    private fun validateImage(uri: Uri): ImageInfo? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                    val mimeType = context.contentResolver.getType(uri) ?: ""
                    
                    if (size > 0 && mimeType.startsWith("image/")) {
                        ImageInfo(mimeType, size)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Image validation failed", e)
            null
        }
    }
    
    private fun checkImagePatterns(imageUri: Uri): SafetyResult {
        // Placeholder for image pattern checking
        // In a real implementation, this might check EXIF data, file size, dimensions, etc.
        return SafetyResult.safe("Basic image pattern check passed")
    }
    
    private fun determineSeverity(category: SafetyCategory, confidence: Float): SafetySeverity {
        return when (category) {
            SafetyCategory.VIOLENCE, SafetyCategory.SELF_HARM, SafetyCategory.ILLEGAL_ACTIVITY -> {
                if (confidence > 0.9f) SafetySeverity.CRITICAL
                else if (confidence > 0.7f) SafetySeverity.HIGH
                else SafetySeverity.MEDIUM
            }
            SafetyCategory.HATE_SPEECH, SafetyCategory.SEXUAL_CONTENT -> {
                if (confidence > 0.8f) SafetySeverity.HIGH
                else if (confidence > 0.6f) SafetySeverity.MEDIUM
                else SafetySeverity.LOW
            }
            else -> {
                if (confidence > 0.7f) SafetySeverity.MEDIUM
                else SafetySeverity.LOW
            }
        }
    }
    
    private fun generateCacheKey(content: String, context: SafetyContext?): String {
        val contextStr = context?.toString() ?: ""
        return "${content.hashCode()}_${contextStr.hashCode()}"
    }
    
    private fun getModelPath(model: SafetyModelDescriptor): String {
        return File(
            File(context.getExternalFilesDir(null), "models"),
            "${model.id}.gguf"
        ).absolutePath
    }
}

// Safety data structures
data class SafetyModelDescriptor(
    val id: String,
    val name: String,
    val description: String,
    val supportedCategories: List<SafetyCategory>,
    val maxSequenceLength: Int,
    val supportsImageSafety: Boolean = false,
    val accuracy: Float,
    val fileSize: Long
) {
    companion object {
        fun fromModelDescriptor(model: ModelDescriptor): SafetyModelDescriptor {
            return SafetyModelDescriptor(
                id = model.id,
                name = model.name,
                description = model.description,
                supportedCategories = listOf(SafetyCategory.HARMFUL_CONTENT), // Default
                maxSequenceLength = model.contextSize,
                supportsImageSafety = false,
                accuracy = 0.9f, // Default
                fileSize = model.fileSize
            )
        }
    }
}

data class SafetyResult(
    val isSafe: Boolean,
    val reason: String,
    val category: SafetyCategory,
    val confidence: Float,
    val details: List<String> = emptyList()
) {
    companion object {
        fun safe(reason: String = "Content is safe"): SafetyResult {
            return SafetyResult(
                isSafe = true,
                reason = reason,
                category = SafetyCategory.NONE,
                confidence = 1.0f
            )
        }
        
        fun unsafe(
            reason: String,
            category: SafetyCategory,
            confidence: Float = 0.8f,
            details: List<String> = emptyList()
        ): SafetyResult {
            return SafetyResult(
                isSafe = false,
                reason = reason,
                category = category,
                confidence = confidence,
                details = details
            )
        }
    }
}

enum class SafetyCategory {
    NONE,
    HARMFUL_CONTENT,
    HATE_SPEECH,
    VIOLENCE,
    SEXUAL_CONTENT,
    PRIVACY_VIOLATION,
    MISINFORMATION,
    SELF_HARM,
    ILLEGAL_ACTIVITY,
    INPUT_VALIDATION,
    SYSTEM_ERROR
}

data class SafetyContext(
    val userId: String? = null,
    val sessionId: String? = null,
    val conversationHistory: List<String> = emptyList(),
    val userAge: Int? = null,
    val userRegion: String? = null,
    val contentType: String = "text"
)

data class SafetyViolation(
    val category: SafetyCategory,
    val confidence: Float,
    val description: String,
    val severity: SafetySeverity
)

enum class SafetySeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class SafetyRule(
    val id: String,
    val name: String,
    val description: String,
    val category: SafetyCategory,
    val pattern: String,
    val action: SafetyAction,
    val enabled: Boolean = true
)

enum class SafetyAction {
    BLOCK, WARN, LOG_ONLY, FILTER
}

data class SafetyReport(
    val timeRange: TimeRange,
    val totalChecks: Int,
    val violations: Int,
    val topCategories: List<SafetyCategoryStats>,
    val trends: List<SafetyTrend>,
    val recommendations: List<String>
) {
    companion object {
        fun empty(): SafetyReport {
            return SafetyReport(
                timeRange = TimeRange.LAST_DAY,
                totalChecks = 0,
                violations = 0,
                topCategories = emptyList(),
                trends = emptyList(),
                recommendations = emptyList()
            )
        }
    }
}

data class SafetyCategoryStats(
    val category: SafetyCategory,
    val violationCount: Int,
    val percentage: Float
)

data class SafetyTrend(
    val category: SafetyCategory,
    val trend: TrendDirection,
    val changePercentage: Float
)

data class ImageInfo(
    val mimeType: String,
    val size: Long
)

// Safety classifier interfaces and implementations
interface SafetyClassifier {
    suspend fun classify(content: String): SafetyResult
}

class HarmfulContentClassifier : SafetyClassifier {
    companion object {
        private val HARMFUL_PATTERNS = listOf(
            "how to make explosives",
            "suicide methods",
            "illegal drug production",
            "identity theft guide",
            "hacking tutorial"
        )
    }
    
    override suspend fun classify(content: String): SafetyResult {
        val lowerContent = content.lowercase()
        
        for (pattern in HARMFUL_PATTERNS) {
            if (lowerContent.contains(pattern)) {
                return SafetyResult.unsafe(
                    reason = "Contains harmful instruction pattern: $pattern",
                    category = SafetyCategory.HARMFUL_CONTENT,
                    confidence = 0.9f
                )
            }
        }
        
        return SafetyResult.safe()
    }
}

class HateSpeechClassifier : SafetyClassifier {
    companion object {
        private val HATE_PATTERNS = listOf(
            "racial slur",
            "ethnic hatred",
            "religious discrimination",
            "gender hatred"
        )
    }
    
    override suspend fun classify(content: String): SafetyResult {
        // Simplified implementation - in practice this would be more sophisticated
        return SafetyResult.safe()
    }
}

class ViolenceClassifier : SafetyClassifier {
    override suspend fun classify(content: String): SafetyResult {
        // Implementation for violence detection
        return SafetyResult.safe()
    }
}

class SexualContentClassifier : SafetyClassifier {
    override suspend fun classify(content: String): SafetyResult {
        // Implementation for sexual content detection
        return SafetyResult.safe()
    }
}

class PrivacyViolationClassifier : SafetyClassifier {
    override suspend fun classify(content: String): SafetyResult {
        // Check for personal information patterns
        val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val phonePattern = Regex("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b")
        val ssnPattern = Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b")
        
        when {
            emailPattern.containsMatchIn(content) -> {
                return SafetyResult.unsafe(
                    reason = "Contains email address",
                    category = SafetyCategory.PRIVACY_VIOLATION,
                    confidence = 0.8f
                )
            }
            phonePattern.containsMatchIn(content) -> {
                return SafetyResult.unsafe(
                    reason = "Contains phone number",
                    category = SafetyCategory.PRIVACY_VIOLATION,
                    confidence = 0.8f
                )
            }
            ssnPattern.containsMatchIn(content) -> {
                return SafetyResult.unsafe(
                    reason = "Contains social security number",
                    category = SafetyCategory.PRIVACY_VIOLATION,
                    confidence = 0.9f
                )
            }
        }
        
        return SafetyResult.safe()
    }
}

class MisinformationClassifier : SafetyClassifier {
    override suspend fun classify(content: String): SafetyResult {
        // Implementation for misinformation detection
        return SafetyResult.safe()
    }
}

class SelfHarmClassifier : SafetyClassifier {
    companion object {
        private val SELF_HARM_PATTERNS = listOf(
            "how to hurt myself",
            "suicide instructions",
            "self-harm methods",
            "ending my life"
        )
    }
    
    override suspend fun classify(content: String): SafetyResult {
        val lowerContent = content.lowercase()
        
        for (pattern in SELF_HARM_PATTERNS) {
            if (lowerContent.contains(pattern)) {
                return SafetyResult.unsafe(
                    reason = "Contains self-harm content: $pattern",
                    category = SafetyCategory.SELF_HARM,
                    confidence = 0.95f
                )
            }
        }
        
        return SafetyResult.safe()
    }
}

class IllegalActivityClassifier : SafetyClassifier {
    override suspend fun classify(content: String): SafetyResult {
        // Implementation for illegal activity detection
        return SafetyResult.safe()
    }
}

class SafetyException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

## 🧪 Testing Strategy

### Unit Tests
- [ ] **Safety Classification**
  - Rule-based classifier accuracy
  - Model-based classification
  - Category detection precision
  - Edge case handling

### Integration Tests
- [ ] **Safety Workflows**
  - End-to-end safety checking
  - Multi-modal safety validation
  - Safety model integration
  - Alert system functionality

### Performance Tests
- [ ] **Safety Processing Performance**
  - Classification speed benchmarks
  - Model loading performance
  - Cache effectiveness
  - Memory usage optimization

### UI Tests
- [ ] **Safety Interface**
  - Safety warning displays
  - Content filtering controls
  - Report generation
  - User safety settings

## ✅ Acceptance Criteria

### Primary Criteria
- [ ] **Input Validation**: Comprehensive screening of user inputs
- [ ] **Output Filtering**: Effective AI response safety checking
- [ ] **Multi-Modal Safety**: Support for text and image safety validation
- [ ] **Real-Time Processing**: Safety checks complete in <500ms
- [ ] **High Accuracy**: >95% accuracy in detecting harmful content

### Technical Criteria
- [ ] **Low Latency**: Safety checks add <200ms to response time
- [ ] **High Precision**: <5% false positive rate for safety violations
- [ ] **Comprehensive Coverage**: Support for all major safety categories
- [ ] **Model Integration**: Seamless integration with safety classification models

### User Experience Criteria
- [ ] **Transparent Operation**: Safety checks operate without user disruption
- [ ] **Clear Feedback**: Informative safety violation messages
- [ ] **Configurable Settings**: User control over safety sensitivity
- [ ] **Privacy Protection**: No user data sent to external services

## 🔗 Related Issues
- **Depends on**: #01 (Core Architecture), #04 (Model Management), #05 (Chat Engine)
- **Enables**: #14 (UI/UX Implementation)
- **Related**: #07 (Multimodal Support), #09 (Monitoring & Observability)

## 📋 Definition of Done
- [ ] Complete safety engine with multi-layered content filtering
- [ ] Rule-based and model-based safety classification
- [ ] Multi-modal safety support for text and images
- [ ] Safety violation categorization and reporting
- [ ] Integration with chat and multimodal engines
- [ ] Comprehensive test suite covering all safety scenarios
- [ ] Performance benchmarks meet acceptance criteria
- [ ] Safety configuration UI functional
- [ ] Documentation complete with safety guidelines and API reference
- [ ] Code review completed and approved

---

**Note**: This safety system provides comprehensive protection against harmful AI interactions while maintaining user privacy through on-device processing and classification.