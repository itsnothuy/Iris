# Issue #11: Memory Management & Resource Optimization

## 🎯 Epic: Advanced Memory & Resource Management
**Priority**: P1 (High)  
**Estimate**: 8-10 days  
**Dependencies**: #01 (Core Architecture), #05 (Chat Engine), #09 (Monitoring)  
**Architecture Reference**: [docs/architecture.md](../architecture.md) - Section 11 Memory & Resource Management

## 📋 Overview
Implement advanced memory management and resource optimization system to ensure efficient operation on Android devices with varying capabilities. This system provides intelligent memory allocation, garbage collection optimization, model caching strategies, and adaptive resource management based on device state.

## 🎯 Goals
- **Intelligent Memory Allocation**: Dynamic memory management for AI models and inference
- **Model Caching**: Efficient caching strategies for frequently used models
- **Resource Optimization**: CPU, GPU, and storage optimization
- **Adaptive Management**: Resource allocation based on device capabilities and thermal state
- **Memory Leak Prevention**: Robust cleanup and resource deallocation
- **Performance Scaling**: Automatic scaling based on available resources

## 📝 Detailed Tasks

### 1. Memory Management Engine

#### 1.1 Memory Manager Implementation
Create `core-memory/src/main/kotlin/MemoryManagerImpl.kt`:

```kotlin
@Singleton
class MemoryManagerImpl @Inject constructor(
    private val deviceProfileProvider: DeviceProfileProvider,
    private val performanceManager: PerformanceManager,
    private val eventBus: EventBus,
    @ApplicationContext private val context: Context
) : MemoryManager {
    
    companion object {
        private const val TAG = "MemoryManager"
        private const val MEMORY_CHECK_INTERVAL_MS = 2000L
        private const val LOW_MEMORY_THRESHOLD = 0.85f // 85% memory usage
        private const val CRITICAL_MEMORY_THRESHOLD = 0.95f // 95% memory usage
        private const val MODEL_CACHE_SIZE_RATIO = 0.3f // 30% of available memory for model cache
        private const val GC_TRIGGER_THRESHOLD = 0.8f // Trigger GC at 80% usage
        private const val MEMORY_PRESSURE_HISTORY_SIZE = 10
    }
    
    private val memoryMonitoringScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isMonitoringActive = false
    private val memoryPressureHistory = CircularBuffer<MemoryPressureLevel>(MEMORY_PRESSURE_HISTORY_SIZE)
    private val allocatedResources = mutableMapOf<String, AllocatedResource>()
    private val memoryPools = mutableMapOf<ResourceType, MemoryPool>()
    
    override suspend fun initialize(): Result<Unit> {
        return try {
            Log.i(TAG, "Initializing memory manager")
            
            // Initialize memory pools
            initializeMemoryPools()
            
            // Start memory monitoring
            startMemoryMonitoring()
            
            // Register for system callbacks
            registerSystemCallbacks()
            
            Log.i(TAG, "Memory manager initialized successfully")
            eventBus.emit(IrisEvent.MemoryManagerInitialized())
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Memory manager initialization failed", e)
            Result.failure(MemoryException("Memory manager initialization failed", e))
        }
    }
    
    override suspend fun allocateMemory(
        size: Long,
        type: ResourceType,
        priority: AllocationPriority,
        tag: String
    ): Result<MemoryAllocation> {
        return withContext(Dispatchers.Default) {
            try {
                // Check if allocation is possible
                val currentMemory = getCurrentMemoryUsage()
                val availableMemory = getTotalMemory() - currentMemory.usedMemory
                
                if (size > availableMemory) {
                    // Try to free memory
                    val freedMemory = freeMemoryForAllocation(size, priority)
                    if (freedMemory < size) {
                        return@withContext Result.failure(
                            MemoryException("Insufficient memory: need ${formatBytes(size)}, available ${formatBytes(availableMemory)}")
                        )
                    }
                }
                
                // Allocate from appropriate pool
                val pool = memoryPools[type] ?: return@withContext Result.failure(
                    MemoryException("No memory pool available for type: $type")
                )
                
                val allocation = pool.allocate(size, priority, tag)
                if (allocation.isSuccess) {
                    val memoryAllocation = allocation.getOrNull()!!
                    
                    // Track allocation
                    allocatedResources[memoryAllocation.id] = AllocatedResource(
                        id = memoryAllocation.id,
                        size = size,
                        type = type,
                        priority = priority,
                        tag = tag,
                        allocatedAt = System.currentTimeMillis(),
                        lastAccessed = System.currentTimeMillis()
                    )
                    
                    Log.d(TAG, "Memory allocated: ${formatBytes(size)} for $tag")
                    eventBus.emit(IrisEvent.MemoryAllocated(memoryAllocation.id, size, type))
                    
                    Result.success(memoryAllocation)
                } else {
                    Result.failure(allocation.exceptionOrNull() ?: MemoryException("Allocation failed"))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Memory allocation failed", e)
                Result.failure(MemoryException("Memory allocation failed", e))
            }
        }
    }
    
    override suspend fun deallocateMemory(allocationId: String): Boolean {
        return try {
            val resource = allocatedResources.remove(allocationId)
            if (resource != null) {
                val pool = memoryPools[resource.type]
                val success = pool?.deallocate(allocationId) ?: false
                
                if (success) {
                    Log.d(TAG, "Memory deallocated: ${formatBytes(resource.size)} for ${resource.tag}")
                    eventBus.emit(IrisEvent.MemoryDeallocated(allocationId, resource.size, resource.type))
                }
                
                success
            } else {
                Log.w(TAG, "Allocation not found for deallocation: $allocationId")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Memory deallocation failed", e)
            false
        }
    }
    
    override suspend fun getCurrentMemoryUsage(): MemoryUsage {
        return try {
            val runtime = Runtime.getRuntime()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val maxMemory = runtime.maxMemory()
            
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            MemoryUsage(
                totalMemory = memoryInfo.totalMem,
                availableMemory = memoryInfo.availMem,
                usedMemory = memoryInfo.totalMem - memoryInfo.availMem,
                usagePercentage = ((memoryInfo.totalMem - memoryInfo.availMem).toFloat() / memoryInfo.totalMem) * 100f,
                appMemoryUsage = usedMemory,
                appMemoryLimit = maxMemory,
                appMemoryPercentage = (usedMemory.toFloat() / maxMemory) * 100f
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get memory usage", e)
            MemoryUsage.empty()
        }
    }
    
    override suspend fun getMemoryPressureLevel(): MemoryPressureLevel {
        val memoryUsage = getCurrentMemoryUsage()
        val usageRatio = memoryUsage.usagePercentage / 100f
        
        return when {
            usageRatio >= CRITICAL_MEMORY_THRESHOLD -> MemoryPressureLevel.CRITICAL
            usageRatio >= LOW_MEMORY_THRESHOLD -> MemoryPressureLevel.HIGH
            usageRatio >= 0.7f -> MemoryPressureLevel.MEDIUM
            usageRatio >= 0.5f -> MemoryPressureLevel.LOW
            else -> MemoryPressureLevel.NORMAL
        }
    }
    
    override suspend fun optimizeMemoryUsage(): MemoryOptimizationResult {
        return try {
            val initialUsage = getCurrentMemoryUsage()
            var freedMemory = 0L
            val actions = mutableListOf<String>()
            
            // 1. Trigger garbage collection
            System.gc()
            Runtime.getRuntime().gc()
            actions.add("Triggered garbage collection")
            
            // 2. Clear unnecessary caches
            val cacheFreed = clearUnnecessaryCaches()
            freedMemory += cacheFreed
            if (cacheFreed > 0) {
                actions.add("Cleared ${formatBytes(cacheFreed)} from caches")
            }
            
            // 3. Deallocate low-priority resources
            val lowPriorityFreed = deallocateLowPriorityResources()
            freedMemory += lowPriorityFreed
            if (lowPriorityFreed > 0) {
                actions.add("Deallocated ${formatBytes(lowPriorityFreed)} low-priority resources")
            }
            
            // 4. Optimize memory pools
            val poolOptimized = optimizeMemoryPools()
            freedMemory += poolOptimized
            if (poolOptimized > 0) {
                actions.add("Optimized memory pools, freed ${formatBytes(poolOptimized)}")
            }
            
            val finalUsage = getCurrentMemoryUsage()
            val actualFreed = initialUsage.usedMemory - finalUsage.usedMemory
            
            val result = MemoryOptimizationResult(
                initialUsage = initialUsage,
                finalUsage = finalUsage,
                freedMemory = maxOf(actualFreed, 0L),
                actions = actions,
                success = actualFreed > 0
            )
            
            Log.i(TAG, "Memory optimization completed: freed ${formatBytes(actualFreed)}")
            eventBus.emit(IrisEvent.MemoryOptimized(result))
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Memory optimization failed", e)
            MemoryOptimizationResult.failed("Memory optimization failed: ${e.message}")
        }
    }
    
    override suspend fun setMemoryLimit(limit: Long, type: ResourceType): Boolean {
        return try {
            val pool = memoryPools[type]
            if (pool != null) {
                pool.setLimit(limit)
                Log.i(TAG, "Memory limit set for $type: ${formatBytes(limit)}")
                true
            } else {
                Log.w(TAG, "No memory pool found for type: $type")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set memory limit", e)
            false
        }
    }
    
    override suspend fun getMemoryStatistics(): MemoryStatistics {
        return try {
            val currentUsage = getCurrentMemoryUsage()
            val pressureLevel = getMemoryPressureLevel()
            
            val allocatedByType = allocatedResources.values.groupBy { it.type }
                .mapValues { (_, resources) -> resources.sumOf { it.size } }
            
            val poolStatistics = memoryPools.mapValues { (_, pool) -> pool.getStatistics() }
            
            MemoryStatistics(
                currentUsage = currentUsage,
                pressureLevel = pressureLevel,
                allocatedByType = allocatedByType,
                poolStatistics = poolStatistics,
                totalAllocations = allocatedResources.size,
                recentPressureHistory = memoryPressureHistory.toList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get memory statistics", e)
            MemoryStatistics.empty()
        }
    }
    
    override suspend fun enableAggressiveOptimization(enable: Boolean) {
        // Configure memory manager for aggressive optimization
        if (enable) {
            // Reduce cache sizes
            memoryPools.values.forEach { pool ->
                pool.setAggressiveMode(true)
            }
            
            // Increase GC frequency
            System.setProperty("dalvik.vm.heapgrowthlimit", "192m")
            
            Log.i(TAG, "Aggressive memory optimization enabled")
        } else {
            memoryPools.values.forEach { pool ->
                pool.setAggressiveMode(false)
            }
            
            Log.i(TAG, "Aggressive memory optimization disabled")
        }
        
        eventBus.emit(IrisEvent.AggressiveOptimizationChanged(enable))
    }
    
    // Private implementation methods
    
    private fun initializeMemoryPools() {
        val deviceProfile = deviceProfileProvider.getDeviceProfile()
        val totalMemory = getTotalMemory()
        
        // Calculate pool sizes based on device capabilities
        val modelCacheSize = (totalMemory * MODEL_CACHE_SIZE_RATIO).toLong()
        val inferenceCacheSize = (totalMemory * 0.2f).toLong() // 20% for inference cache
        val generalPoolSize = (totalMemory * 0.3f).toLong() // 30% for general allocation
        
        memoryPools[ResourceType.MODEL_CACHE] = MemoryPool(
            type = ResourceType.MODEL_CACHE,
            maxSize = modelCacheSize,
            strategy = PoolStrategy.LRU
        )
        
        memoryPools[ResourceType.INFERENCE_CACHE] = MemoryPool(
            type = ResourceType.INFERENCE_CACHE,
            maxSize = inferenceCacheSize,
            strategy = PoolStrategy.LFU
        )
        
        memoryPools[ResourceType.GENERAL] = MemoryPool(
            type = ResourceType.GENERAL,
            maxSize = generalPoolSize,
            strategy = PoolStrategy.FIFO
        )
        
        memoryPools[ResourceType.DOCUMENT_CACHE] = MemoryPool(
            type = ResourceType.DOCUMENT_CACHE,
            maxSize = (totalMemory * 0.1f).toLong(), // 10% for documents
            strategy = PoolStrategy.LRU
        )
        
        Log.i(TAG, "Memory pools initialized: ${memoryPools.size} pools")
    }
    
    private fun startMemoryMonitoring() {
        if (isMonitoringActive) return
        
        isMonitoringActive = true
        
        memoryMonitoringScope.launch {
            while (isMonitoringActive) {
                try {
                    val pressureLevel = getMemoryPressureLevel()
                    memoryPressureHistory.add(pressureLevel)
                    
                    // Handle memory pressure
                    when (pressureLevel) {
                        MemoryPressureLevel.CRITICAL -> {
                            Log.w(TAG, "Critical memory pressure detected")
                            eventBus.emit(IrisEvent.MemoryPressureCritical())
                            handleCriticalMemoryPressure()
                        }
                        
                        MemoryPressureLevel.HIGH -> {
                            Log.w(TAG, "High memory pressure detected")
                            eventBus.emit(IrisEvent.MemoryPressureHigh())
                            handleHighMemoryPressure()
                        }
                        
                        MemoryPressureLevel.MEDIUM -> {
                            if (shouldTriggerGC()) {
                                triggerGarbageCollection()
                            }
                        }
                        
                        else -> {} // Normal operation
                    }
                    
                    delay(MEMORY_CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.w(TAG, "Memory monitoring iteration failed", e)
                    delay(MEMORY_CHECK_INTERVAL_MS * 2) // Back off on error
                }
            }
        }
    }
    
    private suspend fun handleCriticalMemoryPressure() {
        // Aggressive memory freeing
        optimizeMemoryUsage()
        
        // Force garbage collection
        repeat(3) {
            System.gc()
            Runtime.getRuntime().gc()
            delay(100)
        }
        
        // Clear all non-essential caches
        clearAllCaches(essential = false)
        
        // Deallocate low and medium priority resources
        deallocateResourcesByPriority(listOf(AllocationPriority.LOW, AllocationPriority.MEDIUM))
    }
    
    private suspend fun handleHighMemoryPressure() {
        // Moderate memory freeing
        clearUnnecessaryCaches()
        deallocateLowPriorityResources()
        triggerGarbageCollection()
    }
    
    private fun shouldTriggerGC(): Boolean {
        val memoryUsage = getCurrentMemoryUsage()
        return memoryUsage.appMemoryPercentage / 100f >= GC_TRIGGER_THRESHOLD
    }
    
    private fun triggerGarbageCollection() {
        System.gc()
        Runtime.getRuntime().gc()
    }
    
    private suspend fun freeMemoryForAllocation(requiredSize: Long, priority: AllocationPriority): Long {
        var freedMemory = 0L
        
        // First try clearing caches
        freedMemory += clearUnnecessaryCaches()
        
        if (freedMemory < requiredSize) {
            // Deallocate lower priority resources
            val allowedPriorities = when (priority) {
                AllocationPriority.CRITICAL -> listOf(AllocationPriority.LOW, AllocationPriority.MEDIUM, AllocationPriority.HIGH)
                AllocationPriority.HIGH -> listOf(AllocationPriority.LOW, AllocationPriority.MEDIUM)
                AllocationPriority.MEDIUM -> listOf(AllocationPriority.LOW)
                AllocationPriority.LOW -> emptyList()
            }
            
            freedMemory += deallocateResourcesByPriority(allowedPriorities)
        }
        
        if (freedMemory < requiredSize) {
            // Trigger aggressive garbage collection
            repeat(3) {
                System.gc()
                Runtime.getRuntime().gc()
                delay(50)
            }
            
            // Measure freed memory after GC
            val afterGC = getCurrentMemoryUsage()
            // This is approximate since we don't have before/after measurements
        }
        
        return freedMemory
    }
    
    private suspend fun clearUnnecessaryCaches(): Long {
        var freedMemory = 0L
        
        memoryPools.values.forEach { pool ->
            freedMemory += pool.clearUnused()
        }
        
        return freedMemory
    }
    
    private suspend fun clearAllCaches(essential: Boolean): Long {
        var freedMemory = 0L
        
        memoryPools.values.forEach { pool ->
            freedMemory += if (essential) {
                pool.clearAll()
            } else {
                pool.clearNonEssential()
            }
        }
        
        return freedMemory
    }
    
    private suspend fun deallocateLowPriorityResources(): Long {
        return deallocateResourcesByPriority(listOf(AllocationPriority.LOW))
    }
    
    private suspend fun deallocateResourcesByPriority(priorities: List<AllocationPriority>): Long {
        var freedMemory = 0L
        
        val resourcesToDeallocate = allocatedResources.values.filter { resource ->
            resource.priority in priorities
        }.sortedBy { it.lastAccessed } // Deallocate oldest first
        
        resourcesToDeallocate.forEach { resource ->
            if (deallocateMemory(resource.id)) {
                freedMemory += resource.size
            }
        }
        
        return freedMemory
    }
    
    private suspend fun optimizeMemoryPools(): Long {
        var freedMemory = 0L
        
        memoryPools.values.forEach { pool ->
            freedMemory += pool.optimize()
        }
        
        return freedMemory
    }
    
    private fun registerSystemCallbacks() {
        // Register for system memory callbacks
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_DEVICE_STORAGE_LOW -> {
                        memoryMonitoringScope.launch {
                            Log.w(TAG, "System storage low - triggering memory optimization")
                            optimizeMemoryUsage()
                        }
                    }
                }
            }
        }, IntentFilter().apply {
            addAction(Intent.ACTION_DEVICE_STORAGE_LOW)
        })
    }
    
    private fun getTotalMemory(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem
    }
    
    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return "%.1f %s".format(size, units[unitIndex])
    }
}

// Memory management data structures
data class MemoryAllocation(
    val id: String,
    val size: Long,
    val type: ResourceType,
    val priority: AllocationPriority,
    val pointer: Long? = null // For native allocations
)

data class AllocatedResource(
    val id: String,
    val size: Long,
    val type: ResourceType,
    val priority: AllocationPriority,
    val tag: String,
    val allocatedAt: Long,
    var lastAccessed: Long
)

enum class ResourceType {
    MODEL_CACHE,
    INFERENCE_CACHE,
    DOCUMENT_CACHE,
    GENERAL,
    NATIVE_MEMORY,
    GPU_MEMORY
}

enum class AllocationPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class MemoryPressureLevel {
    NORMAL, LOW, MEDIUM, HIGH, CRITICAL
}

data class MemoryUsage(
    val totalMemory: Long,
    val availableMemory: Long,
    val usedMemory: Long,
    val usagePercentage: Float,
    val appMemoryUsage: Long,
    val appMemoryLimit: Long,
    val appMemoryPercentage: Float
) {
    companion object {
        fun empty(): MemoryUsage {
            return MemoryUsage(0, 0, 0, 0f, 0, 0, 0f)
        }
    }
}

data class MemoryOptimizationResult(
    val initialUsage: MemoryUsage,
    val finalUsage: MemoryUsage,
    val freedMemory: Long,
    val actions: List<String>,
    val success: Boolean
) {
    companion object {
        fun failed(reason: String): MemoryOptimizationResult {
            return MemoryOptimizationResult(
                initialUsage = MemoryUsage.empty(),
                finalUsage = MemoryUsage.empty(),
                freedMemory = 0L,
                actions = listOf("Failed: $reason"),
                success = false
            )
        }
    }
}

data class MemoryStatistics(
    val currentUsage: MemoryUsage,
    val pressureLevel: MemoryPressureLevel,
    val allocatedByType: Map<ResourceType, Long>,
    val poolStatistics: Map<ResourceType, PoolStatistics>,
    val totalAllocations: Int,
    val recentPressureHistory: List<MemoryPressureLevel>
) {
    companion object {
        fun empty(): MemoryStatistics {
            return MemoryStatistics(
                currentUsage = MemoryUsage.empty(),
                pressureLevel = MemoryPressureLevel.NORMAL,
                allocatedByType = emptyMap(),
                poolStatistics = emptyMap(),
                totalAllocations = 0,
                recentPressureHistory = emptyList()
            )
        }
    }
}

// Memory pool implementation
class MemoryPool(
    private val type: ResourceType,
    private var maxSize: Long,
    private val strategy: PoolStrategy
) {
    private val allocations = mutableMapOf<String, PooledAllocation>()
    private var currentSize = 0L
    private var isAggressiveMode = false
    
    suspend fun allocate(size: Long, priority: AllocationPriority, tag: String): Result<MemoryAllocation> {
        return withContext(Dispatchers.Default) {
            if (currentSize + size > maxSize) {
                // Try to free space using pool strategy
                val freed = freeSpace(size)
                if (freed < size) {
                    return@withContext Result.failure(
                        MemoryException("Pool exhausted: need ${size}, freed ${freed}")
                    )
                }
            }
            
            val allocationId = generateAllocationId()
            val allocation = PooledAllocation(
                id = allocationId,
                size = size,
                priority = priority,
                tag = tag,
                allocatedAt = System.currentTimeMillis(),
                lastAccessed = System.currentTimeMillis()
            )
            
            allocations[allocationId] = allocation
            currentSize += size
            
            Result.success(MemoryAllocation(allocationId, size, type, priority))
        }
    }
    
    fun deallocate(allocationId: String): Boolean {
        val allocation = allocations.remove(allocationId)
        return if (allocation != null) {
            currentSize -= allocation.size
            true
        } else {
            false
        }
    }
    
    fun setLimit(newLimit: Long) {
        maxSize = newLimit
        
        // If current usage exceeds new limit, free space
        if (currentSize > maxSize) {
            val toFree = currentSize - maxSize
            freeSpace(toFree)
        }
    }
    
    fun setAggressiveMode(aggressive: Boolean) {
        isAggressiveMode = aggressive
        
        if (aggressive) {
            // Reduce effective pool size
            maxSize = (maxSize * 0.7).toLong()
            
            // Trigger cleanup
            freeSpace(0)
        }
    }
    
    fun clearUnused(): Long {
        var freedMemory = 0L
        val currentTime = System.currentTimeMillis()
        val unusedThreshold = if (isAggressiveMode) 30_000L else 300_000L // 30s vs 5min
        
        val toRemove = allocations.values.filter { allocation ->
            currentTime - allocation.lastAccessed > unusedThreshold
        }
        
        toRemove.forEach { allocation ->
            if (deallocate(allocation.id)) {
                freedMemory += allocation.size
            }
        }
        
        return freedMemory
    }
    
    fun clearAll(): Long {
        val freedMemory = currentSize
        allocations.clear()
        currentSize = 0L
        return freedMemory
    }
    
    fun clearNonEssential(): Long {
        var freedMemory = 0L
        
        val nonEssential = allocations.values.filter { allocation ->
            allocation.priority == AllocationPriority.LOW ||
                allocation.priority == AllocationPriority.MEDIUM
        }
        
        nonEssential.forEach { allocation ->
            if (deallocate(allocation.id)) {
                freedMemory += allocation.size
            }
        }
        
        return freedMemory
    }
    
    fun optimize(): Long {
        // Defragmentation and optimization
        return clearUnused()
    }
    
    fun getStatistics(): PoolStatistics {
        return PoolStatistics(
            type = type,
            maxSize = maxSize,
            currentSize = currentSize,
            utilizationPercentage = (currentSize.toFloat() / maxSize) * 100f,
            allocationCount = allocations.size,
            strategy = strategy,
            isAggressiveMode = isAggressiveMode
        )
    }
    
    private fun freeSpace(requiredSize: Long): Long {
        var freedSize = 0L
        val currentTime = System.currentTimeMillis()
        
        // Apply pool strategy to determine what to evict
        val candidates = when (strategy) {
            PoolStrategy.LRU -> allocations.values.sortedBy { it.lastAccessed }
            PoolStrategy.LFU -> allocations.values.sortedBy { it.accessCount }
            PoolStrategy.FIFO -> allocations.values.sortedBy { it.allocatedAt }
        }
        
        for (allocation in candidates) {
            if (freedSize >= requiredSize) break
            
            // Skip critical allocations unless in aggressive mode
            if (allocation.priority == AllocationPriority.CRITICAL && !isAggressiveMode) {
                continue
            }
            
            if (deallocate(allocation.id)) {
                freedSize += allocation.size
            }
        }
        
        return freedSize
    }
    
    private fun generateAllocationId(): String {
        return "${type.name.lowercase()}_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

data class PooledAllocation(
    val id: String,
    val size: Long,
    val priority: AllocationPriority,
    val tag: String,
    val allocatedAt: Long,
    var lastAccessed: Long,
    var accessCount: Int = 1
)

enum class PoolStrategy {
    LRU, // Least Recently Used
    LFU, // Least Frequently Used
    FIFO // First In, First Out
}

data class PoolStatistics(
    val type: ResourceType,
    val maxSize: Long,
    val currentSize: Long,
    val utilizationPercentage: Float,
    val allocationCount: Int,
    val strategy: PoolStrategy,
    val isAggressiveMode: Boolean
)

// Circular buffer for pressure history
class CircularBuffer<T>(private val capacity: Int) {
    private val buffer = Array<Any?>(capacity) { null }
    private var writeIndex = 0
    private var size = 0
    
    fun add(item: T) {
        buffer[writeIndex] = item
        writeIndex = (writeIndex + 1) % capacity
        if (size < capacity) size++
    }
    
    fun toList(): List<T> {
        val result = mutableListOf<T>()
        val start = if (size < capacity) 0 else writeIndex
        
        for (i in 0 until size) {
            val index = (start + i) % capacity
            @Suppress("UNCHECKED_CAST")
            result.add(buffer[index] as T)
        }
        
        return result
    }
}

class MemoryException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

## 🧪 Testing Strategy

### Unit Tests
- [ ] **Memory Allocation Logic**
  - Pool allocation and deallocation
  - Memory pressure detection
  - Optimization algorithms
  - Cache eviction strategies

### Integration Tests
- [ ] **Resource Management**
  - End-to-end memory lifecycle
  - System integration
  - Performance under memory pressure
  - Multi-threaded access

### Performance Tests
- [ ] **Memory Performance**
  - Allocation/deallocation speed
  - Memory optimization effectiveness
  - Overhead measurement
  - Stress testing under low memory

### UI Tests
- [ ] **Memory Monitoring Interface**
  - Memory usage visualization
  - Optimization controls
  - Alert notifications
  - Statistics display

## ✅ Acceptance Criteria

### Primary Criteria
- [ ] **Efficient Allocation**: Fast and reliable memory allocation/deallocation
- [ ] **Pressure Handling**: Graceful handling of memory pressure situations
- [ ] **Resource Optimization**: Effective memory optimization and cleanup
- [ ] **Pool Management**: Intelligent memory pool strategies
- [ ] **Monitoring Integration**: Real-time memory monitoring and alerts

### Technical Criteria
- [ ] **Low Overhead**: Memory management adds <3% performance overhead
- [ ] **Fast Allocation**: Memory allocation completes in <10ms
- [ ] **Effective Cleanup**: Memory optimization frees >50% of reclaimable memory
- [ ] **Pressure Response**: Memory pressure handling within 2 seconds

### User Experience Criteria
- [ ] **Transparent Operation**: Memory management operates without user disruption
- [ ] **Performance Stability**: Consistent performance under varying memory conditions
- [ ] **Resource Awareness**: Appropriate resource usage based on device capabilities
- [ ] **Error Recovery**: Graceful handling of out-of-memory conditions

## 🔗 Related Issues
- **Depends on**: #01 (Core Architecture), #05 (Chat Engine), #09 (Monitoring)
- **Enables**: #12 (Performance Optimization), #14 (UI/UX Implementation)
- **Related**: #03 (Hardware Detection), #04 (Model Management)

## 📋 Definition of Done
- [ ] Complete memory management system with intelligent allocation
- [ ] Memory pressure detection and handling
- [ ] Resource optimization with multiple strategies
- [ ] Memory pool management with configurable policies
- [ ] Integration with monitoring and performance systems
- [ ] Comprehensive test suite covering all memory scenarios
- [ ] Performance benchmarks meet acceptance criteria
- [ ] Memory monitoring UI functional
- [ ] Documentation complete with optimization guidelines
- [ ] Code review completed and approved

---

**Note**: This memory management system ensures efficient resource utilization across all device classes while maintaining optimal performance for AI operations.