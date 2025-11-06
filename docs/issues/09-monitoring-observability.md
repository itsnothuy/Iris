# Issue #09: Monitoring & Observability Engine

## 🎯 Epic: System Monitoring & Analytics
**Priority**: P2 (Medium)  
**Estimate**: 8-10 days  
**Dependencies**: #01 (Core Architecture), #03 (Hardware Detection), #05 (Chat Engine)  
**Architecture Reference**: [docs/architecture.md](../architecture.md) - Section 9 Monitoring & Analytics

## 📋 Overview
Implement comprehensive monitoring and observability capabilities for performance tracking, system health monitoring, usage analytics, and debugging support. This system provides insights into AI model performance, device resource utilization, and user interaction patterns while maintaining privacy.

## 🎯 Goals
- **Performance Monitoring**: Real-time tracking of inference speed, memory usage, and thermal state
- **System Health**: Device capability monitoring and resource utilization analytics
- **Usage Analytics**: Privacy-preserving user interaction and model usage statistics
- **Error Tracking**: Comprehensive error logging and diagnostic information
- **Debug Support**: Development tools for performance optimization and troubleshooting
- **Privacy-First**: All telemetry remains on-device with optional cloud sync

## 📝 Detailed Tasks

### 1. Performance Monitoring System

#### 1.1 Performance Metrics Engine
Create `core-monitoring/src/main/kotlin/PerformanceMonitorImpl.kt`:

```kotlin
@Singleton
class PerformanceMonitorImpl @Inject constructor(
    private val deviceProfileProvider: DeviceProfileProvider,
    private val metricsDatabase: MetricsDatabase,
    private val eventBus: EventBus,
    @ApplicationContext private val context: Context
) : PerformanceMonitor {
    
    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val METRICS_COLLECTION_INTERVAL_MS = 1000L
        private const val THERMAL_CHECK_INTERVAL_MS = 5000L
        private const val MEMORY_CHECK_INTERVAL_MS = 2000L
        private const val METRIC_RETENTION_DAYS = 30
        private const val MAX_METRICS_PER_SESSION = 10000
    }
    
    private val monitoringScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val activeMonitoringJobs = mutableMapOf<String, Job>()
    private var isGlobalMonitoringActive = false
    private var currentSession: MonitoringSession? = null
    
    override suspend fun startMonitoring(config: MonitoringConfig): String {
        val sessionId = generateSessionId()
        
        try {
            currentSession = MonitoringSession(
                sessionId = sessionId,
                startTime = System.currentTimeMillis(),
                config = config,
                metrics = mutableListOf()
            )
            
            // Start global system monitoring
            if (!isGlobalMonitoringActive) {
                startGlobalMonitoring()
            }
            
            // Start session-specific monitoring
            startSessionMonitoring(sessionId, config)
            
            Log.i(TAG, "Performance monitoring started: $sessionId")
            eventBus.emit(IrisEvent.MonitoringStarted(sessionId, config))
            
            return sessionId
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start monitoring", e)
            throw MonitoringException("Monitoring startup failed", e)
        }
    }
    
    override suspend fun stopMonitoring(sessionId: String): MonitoringReport? {
        return try {
            val session = currentSession?.takeIf { it.sessionId == sessionId }
                ?: return null
            
            // Stop session monitoring
            activeMonitoringJobs[sessionId]?.cancel()
            activeMonitoringJobs.remove(sessionId)
            
            // Generate final report
            val report = generateMonitoringReport(session)
            
            // Save session data
            saveSessionData(session, report)
            
            // Clean current session if it matches
            if (currentSession?.sessionId == sessionId) {
                currentSession = null
            }
            
            Log.i(TAG, "Performance monitoring stopped: $sessionId")
            eventBus.emit(IrisEvent.MonitoringStopped(sessionId, report))
            
            report
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop monitoring", e)
            null
        }
    }
    
    override suspend fun recordMetric(metric: PerformanceMetric) {
        try {
            // Add to current session if active
            currentSession?.metrics?.add(metric)
            
            // Store in database
            metricsDatabase.metricsDao().insertMetric(metric.toEntity())
            
            // Emit real-time metric event
            eventBus.emit(IrisEvent.MetricRecorded(metric))
            
            // Check for performance alerts
            checkPerformanceAlerts(metric)
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to record metric: ${metric.name}", e)
        }
    }
    
    override suspend fun getSystemMetrics(): SystemMetrics {
        return try {
            val deviceProfile = deviceProfileProvider.getDeviceProfile()
            val performanceState = deviceProfileProvider.getCurrentPerformanceState()
            
            SystemMetrics(
                timestamp = System.currentTimeMillis(),
                cpuUsage = getCPUUsage(),
                memoryUsage = getMemoryUsage(),
                thermalState = performanceState.thermalState,
                batteryLevel = getBatteryLevel(),
                batteryTemperature = getBatteryTemperature(),
                availableStorage = getAvailableStorage(),
                networkState = getNetworkState(),
                gpuUsage = getGPUUsage(),
                deviceClass = deviceProfile.deviceClass
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get system metrics", e)
            SystemMetrics.empty()
        }
    }
    
    override suspend fun getInferenceMetrics(modelId: String): List<InferenceMetric> {
        return try {
            metricsDatabase.metricsDao()
                .getInferenceMetricsByModel(modelId)
                .map { it.toDomain() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get inference metrics for model: $modelId", e)
            emptyList()
        }
    }
    
    override suspend fun getPerformanceTrends(
        metricType: MetricType,
        timeRange: TimeRange
    ): List<PerformanceTrend> {
        return try {
            val startTime = System.currentTimeMillis() - timeRange.durationMs
            val endTime = System.currentTimeMillis()
            
            val metrics = metricsDatabase.metricsDao()
                .getMetricsByTypeAndTimeRange(metricType.name, startTime, endTime)
            
            calculatePerformanceTrends(metrics, timeRange.granularity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get performance trends", e)
            emptyList()
        }
    }
    
    override suspend fun generatePerformanceReport(
        timeRange: TimeRange,
        includeDetails: Boolean
    ): PerformanceReport {
        return try {
            val startTime = System.currentTimeMillis() - timeRange.durationMs
            val endTime = System.currentTimeMillis()
            
            val allMetrics = metricsDatabase.metricsDao()
                .getMetricsInTimeRange(startTime, endTime)
            
            PerformanceReport(
                generatedAt = System.currentTimeMillis(),
                timeRange = timeRange,
                summary = generatePerformanceSummary(allMetrics),
                trends = if (includeDetails) calculateAllTrends(allMetrics) else emptyList(),
                alerts = getPerformanceAlerts(startTime, endTime),
                recommendations = generateRecommendations(allMetrics)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate performance report", e)
            PerformanceReport.empty()
        }
    }
    
    override suspend fun cleanupOldMetrics(): Int {
        return try {
            val cutoffTime = System.currentTimeMillis() - (METRIC_RETENTION_DAYS * 24 * 60 * 60 * 1000L)
            val deletedCount = metricsDatabase.metricsDao().deleteMetricsOlderThan(cutoffTime)
            
            Log.i(TAG, "Cleaned up $deletedCount old metrics")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old metrics", e)
            0
        }
    }
    
    // Private monitoring methods
    
    private fun startGlobalMonitoring() {
        if (isGlobalMonitoringActive) return
        
        isGlobalMonitoringActive = true
        
        // Start system metrics collection
        monitoringScope.launch {
            while (isGlobalMonitoringActive) {
                try {
                    val systemMetrics = getSystemMetrics()
                    recordMetric(PerformanceMetric.fromSystemMetrics(systemMetrics))
                    
                    delay(METRICS_COLLECTION_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.w(TAG, "System metrics collection failed", e)
                    delay(METRICS_COLLECTION_INTERVAL_MS * 2) // Back off on error
                }
            }
        }
        
        // Start thermal monitoring
        monitoringScope.launch {
            while (isGlobalMonitoringActive) {
                try {
                    val thermalState = deviceProfileProvider.getCurrentPerformanceState().thermalState
                    
                    if (thermalState != ThermalState.NORMAL) {
                        recordMetric(PerformanceMetric(
                            name = "thermal_state",
                            value = thermalState.ordinal.toDouble(),
                            unit = "state",
                            timestamp = System.currentTimeMillis(),
                            type = MetricType.THERMAL,
                            tags = mapOf("state" to thermalState.name)
                        ))
                    }
                    
                    delay(THERMAL_CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.w(TAG, "Thermal monitoring failed", e)
                    delay(THERMAL_CHECK_INTERVAL_MS * 2)
                }
            }
        }
        
        // Start memory monitoring
        monitoringScope.launch {
            while (isGlobalMonitoringActive) {
                try {
                    val memoryInfo = getDetailedMemoryUsage()
                    
                    recordMetric(PerformanceMetric(
                        name = "memory_usage_detailed",
                        value = memoryInfo.usedMemory.toDouble(),
                        unit = "bytes",
                        timestamp = System.currentTimeMillis(),
                        type = MetricType.MEMORY,
                        tags = mapOf(
                            "total" to memoryInfo.totalMemory.toString(),
                            "available" to memoryInfo.availableMemory.toString(),
                            "percentage" to memoryInfo.usagePercentage.toString()
                        )
                    ))
                    
                    delay(MEMORY_CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.w(TAG, "Memory monitoring failed", e)
                    delay(MEMORY_CHECK_INTERVAL_MS * 2)
                }
            }
        }
    }
    
    private fun startSessionMonitoring(sessionId: String, config: MonitoringConfig) {
        val monitoringJob = monitoringScope.launch {
            while (currentSession?.sessionId == sessionId) {
                try {
                    // Collect session-specific metrics based on config
                    if (config.trackInference) {
                        collectInferenceMetrics(sessionId)
                    }
                    
                    if (config.trackUserInteraction) {
                        collectInteractionMetrics(sessionId)
                    }
                    
                    if (config.trackResourceUsage) {
                        collectResourceMetrics(sessionId)
                    }
                    
                    delay(config.collectionIntervalMs)
                } catch (e: Exception) {
                    Log.w(TAG, "Session monitoring failed for $sessionId", e)
                    delay(config.collectionIntervalMs * 2)
                }
            }
        }
        
        activeMonitoringJobs[sessionId] = monitoringJob
    }
    
    private suspend fun collectInferenceMetrics(sessionId: String) {
        // This would collect metrics from active inference sessions
        // Implementation depends on integration with inference engine
    }
    
    private suspend fun collectInteractionMetrics(sessionId: String) {
        // This would collect user interaction metrics
        // Implementation depends on UI integration
    }
    
    private suspend fun collectResourceMetrics(sessionId: String) {
        val session = currentSession ?: return
        
        val resourceMetric = PerformanceMetric(
            name = "session_resource_usage",
            value = getSessionResourceUsage(session),
            unit = "bytes",
            timestamp = System.currentTimeMillis(),
            type = MetricType.RESOURCE,
            tags = mapOf(
                "session_id" to sessionId,
                "duration" to (System.currentTimeMillis() - session.startTime).toString()
            )
        )
        
        recordMetric(resourceMetric)
    }
    
    private fun generateMonitoringReport(session: MonitoringSession): MonitoringReport {
        val duration = System.currentTimeMillis() - session.startTime
        val metrics = session.metrics.toList()
        
        return MonitoringReport(
            sessionId = session.sessionId,
            duration = duration,
            metricsCount = metrics.size,
            averageInferenceTime = calculateAverageInferenceTime(metrics),
            peakMemoryUsage = calculatePeakMemoryUsage(metrics),
            thermalEvents = countThermalEvents(metrics),
            errorCount = countErrors(metrics),
            summary = generateSessionSummary(metrics),
            recommendations = generateSessionRecommendations(metrics)
        )
    }
    
    private suspend fun saveSessionData(session: MonitoringSession, report: MonitoringReport) {
        try {
            // Save session metadata
            metricsDatabase.sessionDao().insertSession(
                MonitoringSessionEntity(
                    sessionId = session.sessionId,
                    startTime = session.startTime,
                    duration = report.duration,
                    metricsCount = report.metricsCount,
                    config = gson.toJson(session.config)
                )
            )
            
            // Metrics are already saved individually during collection
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session data", e)
        }
    }
    
    private fun checkPerformanceAlerts(metric: PerformanceMetric) {
        when (metric.type) {
            MetricType.MEMORY -> {
                if (metric.value > 0.9 * getTotalMemory()) { // 90% memory usage
                    eventBus.emit(IrisEvent.PerformanceAlert(
                        AlertType.HIGH_MEMORY_USAGE,
                        "Memory usage is high: ${formatBytes(metric.value.toLong())}"
                    ))
                }
            }
            
            MetricType.THERMAL -> {
                if (metric.value >= ThermalState.HOT.ordinal) {
                    eventBus.emit(IrisEvent.PerformanceAlert(
                        AlertType.THERMAL_THROTTLING,
                        "Device is overheating: ${metric.tags["state"]}"
                    ))
                }
            }
            
            MetricType.INFERENCE -> {
                if (metric.value > 10000) { // 10 second inference time
                    eventBus.emit(IrisEvent.PerformanceAlert(
                        AlertType.SLOW_INFERENCE,
                        "Inference is slow: ${metric.value}ms"
                    ))
                }
            }
            
            else -> {} // No alerts for other metric types
        }
    }
    
    // System metrics collection methods
    
    private fun getCPUUsage(): Float {
        return try {
            val statFile = File("/proc/stat")
            if (statFile.exists()) {
                val lines = statFile.readLines()
                val cpuLine = lines.firstOrNull { it.startsWith("cpu ") }
                cpuLine?.let { parseLinuxCPUUsage(it) } ?: 0f
            } else {
                0f
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get CPU usage", e)
            0f
        }
    }
    
    private fun parseLinuxCPUUsage(cpuLine: String): Float {
        // Parse /proc/stat CPU line: cpu user nice system idle iowait irq softirq steal guest guest_nice
        val values = cpuLine.split("\\s+".toRegex()).drop(1).map { it.toLongOrNull() ?: 0L }
        
        if (values.size >= 4) {
            val idle = values[3]
            val total = values.sum()
            return if (total > 0) {
                ((total - idle).toFloat() / total) * 100f
            } else 0f
        }
        
        return 0f
    }
    
    private fun getMemoryUsage(): MemoryUsage {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return MemoryUsage(
            totalMemory = memoryInfo.totalMem,
            availableMemory = memoryInfo.availMem,
            usedMemory = memoryInfo.totalMem - memoryInfo.availMem,
            usagePercentage = ((memoryInfo.totalMem - memoryInfo.availMem).toFloat() / memoryInfo.totalMem) * 100f
        )
    }
    
    private fun getDetailedMemoryUsage(): DetailedMemoryUsage {
        val runtime = Runtime.getRuntime()
        val appMemory = runtime.totalMemory() - runtime.freeMemory()
        val systemMemory = getMemoryUsage()
        
        return DetailedMemoryUsage(
            totalMemory = systemMemory.totalMemory,
            availableMemory = systemMemory.availableMemory,
            usedMemory = systemMemory.usedMemory,
            usagePercentage = systemMemory.usagePercentage,
            appMemoryUsage = appMemory,
            appMemoryPercentage = (appMemory.toFloat() / systemMemory.totalMemory) * 100f
        )
    }
    
    private fun getBatteryLevel(): Float {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toFloat()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get battery level", e)
            0f
        }
    }
    
    private fun getBatteryTemperature(): Float {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            temperature / 10f // Convert from tenths of degrees to degrees Celsius
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get battery temperature", e)
            0f
        }
    }
    
    private fun getAvailableStorage(): Long {
        return try {
            val storageDir = context.getExternalFilesDir(null) ?: context.filesDir
            storageDir.usableSpace
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get available storage", e)
            0L
        }
    }
    
    private fun getNetworkState(): NetworkState {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetworkInfo
            
            when {
                activeNetwork?.isConnected != true -> NetworkState.DISCONNECTED
                activeNetwork.type == ConnectivityManager.TYPE_WIFI -> NetworkState.WIFI
                activeNetwork.type == ConnectivityManager.TYPE_MOBILE -> NetworkState.MOBILE
                else -> NetworkState.OTHER
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get network state", e)
            NetworkState.UNKNOWN
        }
    }
    
    private fun getGPUUsage(): Float {
        // GPU usage monitoring is device-specific and may require root access
        // This is a placeholder implementation
        return 0f
    }
    
    private fun getTotalMemory(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem
    }
    
    private fun getSessionResourceUsage(session: MonitoringSession): Double {
        // Calculate current session resource usage
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory().toDouble()
    }
    
    // Analysis methods
    
    private fun calculatePerformanceTrends(
        metrics: List<PerformanceMetricEntity>,
        granularity: TrendGranularity
    ): List<PerformanceTrend> {
        // Group metrics by time buckets based on granularity
        // Calculate trends (improving, degrading, stable)
        // Return trend analysis
        return emptyList() // Placeholder
    }
    
    private fun calculateAllTrends(metrics: List<PerformanceMetricEntity>): List<PerformanceTrend> {
        // Calculate trends for all metric types
        return emptyList() // Placeholder
    }
    
    private fun generatePerformanceSummary(metrics: List<PerformanceMetricEntity>): PerformanceSummary {
        return PerformanceSummary(
            totalMetrics = metrics.size,
            averageInferenceTime = calculateAverageInferenceTimeFromEntities(metrics),
            peakMemoryUsage = calculatePeakMemoryUsageFromEntities(metrics),
            thermalEvents = countThermalEventsFromEntities(metrics),
            errorCount = countErrorsFromEntities(metrics)
        )
    }
    
    private fun getPerformanceAlerts(startTime: Long, endTime: Long): List<PerformanceAlert> {
        // Query alerts database for the time range
        return emptyList() // Placeholder
    }
    
    private fun generateRecommendations(metrics: List<PerformanceMetricEntity>): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Analyze metrics and generate recommendations
        val avgMemoryUsage = metrics.filter { it.type == MetricType.MEMORY.name }
            .map { it.value }.average()
        
        if (avgMemoryUsage > 0.8 * getTotalMemory()) {
            recommendations.add("Consider using smaller models or reducing batch sizes to lower memory usage")
        }
        
        val thermalEvents = metrics.count { it.type == MetricType.THERMAL.name && it.value >= ThermalState.WARM.ordinal }
        if (thermalEvents > 10) {
            recommendations.add("Device is frequently warm. Consider reducing inference frequency or model complexity")
        }
        
        return recommendations
    }
    
    // Helper calculation methods
    
    private fun calculateAverageInferenceTime(metrics: List<PerformanceMetric>): Double {
        val inferenceTimes = metrics.filter { it.type == MetricType.INFERENCE }
            .map { it.value }
        
        return if (inferenceTimes.isNotEmpty()) {
            inferenceTimes.average()
        } else 0.0
    }
    
    private fun calculatePeakMemoryUsage(metrics: List<PerformanceMetric>): Long {
        return metrics.filter { it.type == MetricType.MEMORY }
            .maxOfOrNull { it.value.toLong() } ?: 0L
    }
    
    private fun countThermalEvents(metrics: List<PerformanceMetric>): Int {
        return metrics.count { it.type == MetricType.THERMAL && it.value >= ThermalState.WARM.ordinal }
    }
    
    private fun countErrors(metrics: List<PerformanceMetric>): Int {
        return metrics.count { it.type == MetricType.ERROR }
    }
    
    private fun generateSessionSummary(metrics: List<PerformanceMetric>): String {
        val summary = StringBuilder()
        summary.append("Session completed with ${metrics.size} metrics collected. ")
        
        val avgInference = calculateAverageInferenceTime(metrics)
        if (avgInference > 0) {
            summary.append("Average inference time: ${avgInference.toInt()}ms. ")
        }
        
        val peakMemory = calculatePeakMemoryUsage(metrics)
        if (peakMemory > 0) {
            summary.append("Peak memory usage: ${formatBytes(peakMemory)}. ")
        }
        
        val errors = countErrors(metrics)
        if (errors > 0) {
            summary.append("$errors errors encountered. ")
        }
        
        return summary.toString().trim()
    }
    
    private fun generateSessionRecommendations(metrics: List<PerformanceMetric>): List<String> {
        return generateRecommendations(metrics.map { it.toEntity() })
    }
    
    // Entity conversion helpers
    
    private fun calculateAverageInferenceTimeFromEntities(metrics: List<PerformanceMetricEntity>): Double {
        return calculateAverageInferenceTime(metrics.map { it.toDomain() })
    }
    
    private fun calculatePeakMemoryUsageFromEntities(metrics: List<PerformanceMetricEntity>): Long {
        return calculatePeakMemoryUsage(metrics.map { it.toDomain() })
    }
    
    private fun countThermalEventsFromEntities(metrics: List<PerformanceMetricEntity>): Int {
        return countThermalEvents(metrics.map { it.toDomain() })
    }
    
    private fun countErrorsFromEntities(metrics: List<PerformanceMetricEntity>): Int {
        return countErrors(metrics.map { it.toDomain() })
    }
    
    private fun generateSessionId(): String {
        return "monitor_${System.currentTimeMillis()}_${(10000..99999).random()}"
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
    
    companion object {
        private val gson = GsonBuilder().create()
    }
}

// Monitoring data structures
data class MonitoringConfig(
    val trackInference: Boolean = true,
    val trackUserInteraction: Boolean = true,
    val trackResourceUsage: Boolean = true,
    val trackErrors: Boolean = true,
    val collectionIntervalMs: Long = METRICS_COLLECTION_INTERVAL_MS,
    val enableAlerts: Boolean = true,
    val customTags: Map<String, String> = emptyMap()
)

data class MonitoringSession(
    val sessionId: String,
    val startTime: Long,
    val config: MonitoringConfig,
    val metrics: MutableList<PerformanceMetric>
)

data class PerformanceMetric(
    val name: String,
    val value: Double,
    val unit: String,
    val timestamp: Long,
    val type: MetricType,
    val tags: Map<String, String> = emptyMap()
) {
    fun toEntity(): PerformanceMetricEntity {
        return PerformanceMetricEntity(
            id = 0, // Auto-generated
            name = name,
            value = value,
            unit = unit,
            timestamp = timestamp,
            type = type.name,
            tags = gson.toJson(tags)
        )
    }
    
    companion object {
        fun fromSystemMetrics(systemMetrics: SystemMetrics): PerformanceMetric {
            return PerformanceMetric(
                name = "system_overview",
                value = systemMetrics.memoryUsage.usagePercentage.toDouble(),
                unit = "percentage",
                timestamp = systemMetrics.timestamp,
                type = MetricType.SYSTEM,
                tags = mapOf(
                    "cpu_usage" to systemMetrics.cpuUsage.toString(),
                    "thermal_state" to systemMetrics.thermalState.name,
                    "battery_level" to systemMetrics.batteryLevel.toString(),
                    "device_class" to systemMetrics.deviceClass.name
                )
            )
        }
    }
}

enum class MetricType {
    INFERENCE, MEMORY, THERMAL, CPU, BATTERY, NETWORK, STORAGE, ERROR, USER_INTERACTION, RESOURCE, SYSTEM
}

data class SystemMetrics(
    val timestamp: Long,
    val cpuUsage: Float,
    val memoryUsage: MemoryUsage,
    val thermalState: ThermalState,
    val batteryLevel: Float,
    val batteryTemperature: Float,
    val availableStorage: Long,
    val networkState: NetworkState,
    val gpuUsage: Float,
    val deviceClass: DeviceClass
) {
    companion object {
        fun empty(): SystemMetrics {
            return SystemMetrics(
                timestamp = System.currentTimeMillis(),
                cpuUsage = 0f,
                memoryUsage = MemoryUsage(0, 0, 0, 0f),
                thermalState = ThermalState.NORMAL,
                batteryLevel = 0f,
                batteryTemperature = 0f,
                availableStorage = 0L,
                networkState = NetworkState.UNKNOWN,
                gpuUsage = 0f,
                deviceClass = DeviceClass.BUDGET
            )
        }
    }
}

data class MemoryUsage(
    val totalMemory: Long,
    val availableMemory: Long,
    val usedMemory: Long,
    val usagePercentage: Float
)

data class DetailedMemoryUsage(
    val totalMemory: Long,
    val availableMemory: Long,
    val usedMemory: Long,
    val usagePercentage: Float,
    val appMemoryUsage: Long,
    val appMemoryPercentage: Float
)

enum class NetworkState {
    WIFI, MOBILE, DISCONNECTED, OTHER, UNKNOWN
}

data class InferenceMetric(
    val modelId: String,
    val inferenceTime: Long,
    val tokenCount: Int,
    val tokensPerSecond: Double,
    val memoryUsage: Long,
    val timestamp: Long,
    val success: Boolean,
    val errorMessage: String?
)

data class TimeRange(
    val durationMs: Long,
    val granularity: TrendGranularity
) {
    companion object {
        val LAST_HOUR = TimeRange(60 * 60 * 1000L, TrendGranularity.MINUTE)
        val LAST_DAY = TimeRange(24 * 60 * 60 * 1000L, TrendGranularity.HOUR)
        val LAST_WEEK = TimeRange(7 * 24 * 60 * 60 * 1000L, TrendGranularity.DAY)
        val LAST_MONTH = TimeRange(30 * 24 * 60 * 60 * 1000L, TrendGranularity.DAY)
    }
}

enum class TrendGranularity {
    MINUTE, HOUR, DAY
}

data class PerformanceTrend(
    val metricType: MetricType,
    val timeRange: TimeRange,
    val trend: TrendDirection,
    val changePercentage: Float,
    val dataPoints: List<TrendDataPoint>
)

enum class TrendDirection {
    IMPROVING, DEGRADING, STABLE
}

data class TrendDataPoint(
    val timestamp: Long,
    val value: Double
)

data class PerformanceReport(
    val generatedAt: Long,
    val timeRange: TimeRange,
    val summary: PerformanceSummary,
    val trends: List<PerformanceTrend>,
    val alerts: List<PerformanceAlert>,
    val recommendations: List<String>
) {
    companion object {
        fun empty(): PerformanceReport {
            return PerformanceReport(
                generatedAt = System.currentTimeMillis(),
                timeRange = TimeRange.LAST_HOUR,
                summary = PerformanceSummary(0, 0.0, 0, 0, 0),
                trends = emptyList(),
                alerts = emptyList(),
                recommendations = emptyList()
            )
        }
    }
}

data class PerformanceSummary(
    val totalMetrics: Int,
    val averageInferenceTime: Double,
    val peakMemoryUsage: Long,
    val thermalEvents: Int,
    val errorCount: Int
)

data class PerformanceAlert(
    val type: AlertType,
    val message: String,
    val severity: AlertSeverity,
    val timestamp: Long,
    val resolved: Boolean = false
)

enum class AlertType {
    HIGH_MEMORY_USAGE,
    THERMAL_THROTTLING,
    SLOW_INFERENCE,
    LOW_BATTERY,
    STORAGE_FULL,
    NETWORK_ISSUES,
    MODEL_ERROR
}

enum class AlertSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class MonitoringReport(
    val sessionId: String,
    val duration: Long,
    val metricsCount: Int,
    val averageInferenceTime: Double,
    val peakMemoryUsage: Long,
    val thermalEvents: Int,
    val errorCount: Int,
    val summary: String,
    val recommendations: List<String>
)

class MonitoringException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

## 🧪 Testing Strategy

### Unit Tests
- [ ] **Monitoring Engine Logic**
  - Metrics collection accuracy
  - Performance calculation correctness
  - Alert threshold validation
  - Trend analysis algorithms

### Integration Tests
- [ ] **System Monitoring**
  - End-to-end monitoring flow
  - Database integration
  - Performance impact assessment
  - Alert system functionality

### Performance Tests
- [ ] **Monitoring Overhead**
  - Collection performance impact
  - Memory usage of monitoring
  - Storage efficiency
  - Real-time monitoring latency

### UI Tests
- [ ] **Monitoring Dashboard**
  - Metrics visualization
  - Report generation interface
  - Alert notifications
  - Performance trends display

## ✅ Acceptance Criteria

### Primary Criteria
- [ ] **Real-time Monitoring**: Continuous performance tracking with minimal overhead
- [ ] **Comprehensive Metrics**: CPU, memory, thermal, inference, and user interaction tracking
- [ ] **Alert System**: Proactive notifications for performance issues
- [ ] **Historical Analysis**: Trend analysis and performance reports
- [ ] **Privacy Compliance**: All telemetry remains on-device

### Technical Criteria
- [ ] **Low Overhead**: Monitoring adds <5% performance overhead
- [ ] **Storage Efficiency**: Metrics storage optimized for long-term retention
- [ ] **Real-time Alerts**: Performance alerts within 5 seconds of threshold breach
- [ ] **Data Retention**: 30-day metric retention with configurable cleanup

### User Experience Criteria
- [ ] **Performance Dashboard**: Clear visualization of system performance
- [ ] **Actionable Insights**: Meaningful recommendations for optimization
- [ ] **Minimal Disruption**: Monitoring operates transparently
- [ ] **Export Capabilities**: Performance reports can be exported

## 🔗 Related Issues
- **Depends on**: #01 (Core Architecture), #03 (Hardware Detection), #05 (Chat Engine)
- **Enables**: #12 (Performance Optimization), #14 (UI/UX Implementation)
- **Related**: #10 (Safety Engine), #11 (Memory Management)

## 📋 Definition of Done
- [ ] Complete performance monitoring system with real-time metrics
- [ ] System health monitoring with device resource tracking
- [ ] Usage analytics with privacy-preserving telemetry
- [ ] Alert system with configurable thresholds
- [ ] Performance reporting with trend analysis
- [ ] Comprehensive test suite covering all monitoring scenarios
- [ ] Performance benchmarks meet acceptance criteria
- [ ] Monitoring dashboard UI functional
- [ ] Documentation complete with metrics reference and usage
- [ ] Code review completed and approved

---

**Note**: This monitoring system provides comprehensive observability while maintaining privacy through on-device data processing and optional cloud synchronization.