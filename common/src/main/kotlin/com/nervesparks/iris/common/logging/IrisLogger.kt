package com.nervesparks.iris.common.logging

/**
 * Log levels for Iris application
 */
enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARNING,
    ERROR
}

/**
 * Centralized logging interface for Iris
 */
object IrisLogger {
    
    private var logLevel: LogLevel = LogLevel.INFO
    private val tag = "Iris"
    
    /**
     * Set the minimum log level
     */
    fun setLogLevel(level: LogLevel) {
        logLevel = level
    }
    
    /**
     * Log verbose message
     */
    fun verbose(message: String, throwable: Throwable? = null) {
        if (logLevel.ordinal <= LogLevel.VERBOSE.ordinal) {
            android.util.Log.v(tag, message, throwable)
        }
    }
    
    /**
     * Log debug message
     */
    fun debug(message: String, throwable: Throwable? = null) {
        if (logLevel.ordinal <= LogLevel.DEBUG.ordinal) {
            android.util.Log.d(tag, message, throwable)
        }
    }
    
    /**
     * Log info message
     */
    fun info(message: String, throwable: Throwable? = null) {
        if (logLevel.ordinal <= LogLevel.INFO.ordinal) {
            android.util.Log.i(tag, message, throwable)
        }
    }
    
    /**
     * Log warning message
     */
    fun warning(message: String, throwable: Throwable? = null) {
        if (logLevel.ordinal <= LogLevel.WARNING.ordinal) {
            android.util.Log.w(tag, message, throwable)
        }
    }
    
    /**
     * Log error message
     */
    fun error(message: String, throwable: Throwable? = null) {
        if (logLevel.ordinal <= LogLevel.ERROR.ordinal) {
            android.util.Log.e(tag, message, throwable)
        }
    }
}
