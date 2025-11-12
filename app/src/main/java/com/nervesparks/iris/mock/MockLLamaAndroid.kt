package com.nervesparks.iris.mock

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Mock implementation of LLamaAndroid for MVP testing
 */
class MockLLamaAndroid {
    
    companion object {
        @JvmStatic
        fun instance(): MockLLamaAndroid {
            return MockLLamaAndroid()
        }
    }
    
    fun load_model(modelPath: String): Int {
        // Mock implementation - returns success
        return 0
    }
    
    fun free_model() {
        // Mock implementation - do nothing
    }
    
    fun completion_init(text: String, n_len: Int): String {
        // Mock implementation - return the input text as a simple echo
        return "Mock response: $text"
    }
    
    fun completion_loop(): String {
        // Mock implementation - return empty string to indicate completion
        return ""
    }
    
    fun kv_cache_clear() {
        // Mock implementation - do nothing
    }
    
    fun bench(pp: Int, tg: Int, pl: Int, nr: Int): String {
        // Mock implementation - return fake benchmark results
        return "Mock benchmark: pp=$pp, tg=$tg, pl=$pl, nr=$nr"
    }
    
    // Additional methods used in MainViewModel
    fun unload() {
        // Mock implementation - do nothing
    }
    
    fun getTemplate(messages: List<Any>): String {
        // Mock implementation - return a simple template
        return "Mock template for ${messages.size} messages"
    }
    
    fun tryEnqueue(message: String): Boolean {
        // Mock implementation - always return true
        return true
    }
    
    fun send(message: String): Flow<String> {
        // Mock implementation - return a flow with a mock response
        return flow {
            emit("Mock AI response to: $message")
        }
    }
    
    fun myCustomBenchmark(): Flow<String> {
        // Mock implementation - return a flow with fake benchmark results
        return flow {
            emit("Mock custom benchmark completed")
        }
    }
    
    fun load(path: String, userThreads: Int, topK: Int, topP: Float, temp: Float): Boolean {
        // Mock implementation - always return success
        return true
    }
    
    fun send_eot_str(): String {
        // Mock implementation - return end of text marker
        return "<|endoftext|>"
    }
    
    fun getIsSending(): Boolean {
        // Mock implementation - always return false
        return false
    }
    
    fun getIsMarked(): Boolean {
        // Mock implementation - always return false
        return false
    }
    
    fun getIsCompleteEOT(): Boolean {
        // Mock implementation - always return true
        return true
    }
    
    fun stopTextGeneration() {
        // Mock implementation - do nothing
    }
    
    // Queue state methods
    fun isQueued(): Boolean {
        // Mock implementation - always return false
        return false
    }
    
    fun getQueueSize(): Int {
        // Mock implementation - always return 0
        return 0
    }
    
    fun isRateLimited(): Boolean {
        // Mock implementation - always return false
        return false
    }
    
    fun isThermalThrottled(): Boolean {
        // Mock implementation - always return false
        return false
    }
    
    fun getRateLimitCooldownSeconds(): Int {
        // Mock implementation - always return 0
        return 0
    }
}