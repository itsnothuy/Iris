package com.nervesparks.iris.common.config

/**
 * Performance profile modes for adaptive performance
 */
enum class PerformanceProfile {
    /** Maximum performance, higher power consumption */
    PERFORMANCE,
    
    /** Balanced performance and efficiency */
    BALANCED,
    
    /** Optimized for battery life */
    BATTERY_SAVER,
    
    /** Minimal power consumption for critical battery states */
    EMERGENCY
}

/**
 * Safety level configuration
 */
enum class SafetyLevel {
    /** No safety filtering */
    NONE,
    
    /** Basic safety checks */
    LOW,
    
    /** Standard safety filtering */
    MEDIUM,
    
    /** Strict safety enforcement */
    HIGH
}

/**
 * Thermal state for device monitoring
 */
enum class ThermalState {
    /** Normal operating temperature */
    NORMAL,
    
    /** Slightly elevated temperature */
    LIGHT,
    
    /** Moderate thermal throttling needed */
    MODERATE,
    
    /** Significant thermal throttling required */
    SEVERE,
    
    /** Critical thermal state, minimal operation */
    CRITICAL
}
