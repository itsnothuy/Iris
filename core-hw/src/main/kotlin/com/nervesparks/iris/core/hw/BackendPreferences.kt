package com.nervesparks.iris.core.hw

import android.content.Context
import android.content.SharedPreferences
import com.nervesparks.iris.common.models.BackendType
import com.nervesparks.iris.common.models.BenchmarkResults
import com.nervesparks.iris.common.models.ComputeTask
import com.nervesparks.iris.common.models.DeviceProfile
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for caching backend selections and benchmark results
 */
interface BackendPreferences {
    fun getCachedBackend(task: ComputeTask, deviceProfile: DeviceProfile): BackendType?
    fun cacheBackendSelection(task: ComputeTask, deviceProfile: DeviceProfile, backend: BackendType)
    fun cacheBenchmarkResults(results: BenchmarkResults)
    fun getCachedBenchmarkResults(): BenchmarkResults?
    fun isGPUAllowedInSevereThermal(): Boolean
}

/**
 * Implementation of BackendPreferences using SharedPreferences
 */
@Singleton
class BackendPreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BackendPreferences {
    
    private val preferences: SharedPreferences = 
        context.getSharedPreferences("backend_cache", Context.MODE_PRIVATE)
    
    private val gson = Gson()
    
    override fun getCachedBackend(task: ComputeTask, deviceProfile: DeviceProfile): BackendType? {
        val key = "${task.name}_${deviceProfile.socVendor}_${deviceProfile.deviceClass}"
        val backendName = preferences.getString(key, null) ?: return null
        return try {
            BackendType.valueOf(backendName)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
    
    override fun cacheBackendSelection(
        task: ComputeTask,
        deviceProfile: DeviceProfile,
        backend: BackendType
    ) {
        val key = "${task.name}_${deviceProfile.socVendor}_${deviceProfile.deviceClass}"
        preferences.edit().putString(key, backend.name).apply()
    }
    
    override fun cacheBenchmarkResults(results: BenchmarkResults) {
        // Store benchmark results as JSON
        val json = gson.toJson(results)
        preferences.edit()
            .putString("benchmark_results", json)
            .putLong("benchmark_timestamp", results.timestamp)
            .apply()
    }
    
    override fun getCachedBenchmarkResults(): BenchmarkResults? {
        val json = preferences.getString("benchmark_results", null) ?: return null
        return try {
            gson.fromJson(json, BenchmarkResults::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun isGPUAllowedInSevereThermal(): Boolean {
        return preferences.getBoolean("gpu_severe_thermal", false)
    }
}
