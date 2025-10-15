package android.llama.cpp

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for rate-limit and thermal throttle policy in LLamaAndroid.
 * Tests the degradation behavior when inference is rate-limited or device is hot.
 */
@RunWith(RobolectricTestRunner::class)
class LLamaAndroidRateLimitThermalTest {
    
    private lateinit var llamaAndroid: LLamaAndroid
    
    @Before
    public fun setup() {
        llamaAndroid = LLamaAndroid.instance()
    }
    
    @Test
    public fun rateLimitState_initiallyFalse() {
        // Given: Fresh instance
        // When: Check rate limit state
        val isRateLimited = llamaAndroid.isRateLimited()
        
        // Then: Should be false initially
        assertFalse("Rate limit should be false initially", isRateLimited)
    }
    
    @Test
    public fun thermalState_initiallyFalse() {
        // Given: Fresh instance
        // When: Check thermal state
        val isThermalThrottled = llamaAndroid.isThermalThrottled()
        
        // Then: Should be false initially
        assertFalse("Thermal throttle should be false initially", isThermalThrottled)
    }
    
    @Test
    fun thermalState_canBeSetToTrue() {
        // Given: Fresh instance
        // When: Set thermal state to true
        llamaAndroid.setThermalState(true)
        
        // Then: Should be true
        assertTrue("Thermal throttle should be true after setting", llamaAndroid.isThermalThrottled())
    }
    
    @Test
    fun thermalState_canBeSetToFalse() {
        // Given: Thermal state is true
        llamaAndroid.setThermalState(true)
        
        // When: Set thermal state to false
        llamaAndroid.setThermalState(false)
        
        // Then: Should be false
        assertFalse("Thermal throttle should be false after resetting", llamaAndroid.isThermalThrottled())
    }
    
    @Test
    fun rateLimitState_remainsFalse_whenBelowThreshold() = runBlocking {
        // Given: Fresh instance
        // When: Enqueue a few messages (below threshold of 10)
        repeat(3) {
            llamaAndroid.tryEnqueue("test message $it")
        }
        
        // Then: Rate limit should still be false
        assertFalse("Rate limit should remain false below threshold", llamaAndroid.isRateLimited())
    }
    
    @Test
    fun rateLimitState_becomesTrue_whenExceedsThreshold() = runBlocking {
        // Given: Fresh instance
        // When: Enqueue many messages (exceeds threshold of 10)
        repeat(12) {
            llamaAndroid.tryEnqueue("test message $it")
        }
        
        // Then: Rate limit should be true
        assertTrue("Rate limit should be true after exceeding threshold", llamaAndroid.isRateLimited())
    }
    
    @Test
    fun rateLimitAndThermal_canBothBeTrue() {
        // Given: Rate limit exceeded and thermal throttle set
        runBlocking {
            repeat(12) {
                llamaAndroid.tryEnqueue("test message $it")
            }
        }
        llamaAndroid.setThermalState(true)
        
        // Then: Both should be true
        assertTrue("Rate limit should be true", llamaAndroid.isRateLimited())
        assertTrue("Thermal throttle should be true", llamaAndroid.isThermalThrottled())
    }
    
    @Test
    fun thermalState_independentOfRateLimit() {
        // Given: Rate limit not exceeded
        // When: Set thermal state to true
        llamaAndroid.setThermalState(true)
        
        // Then: Thermal is true, rate limit is false
        assertTrue("Thermal throttle should be true", llamaAndroid.isThermalThrottled())
        assertFalse("Rate limit should remain false", llamaAndroid.isRateLimited())
    }
    
    @Test
    fun rateLimitState_independentOfThermal() = runBlocking {
        // Given: Thermal state is false
        llamaAndroid.setThermalState(false)
        
        // When: Exceed rate limit
        repeat(12) {
            llamaAndroid.tryEnqueue("test message $it")
        }
        
        // Then: Rate limit is true, thermal is false
        assertTrue("Rate limit should be true", llamaAndroid.isRateLimited())
        assertFalse("Thermal throttle should remain false", llamaAndroid.isThermalThrottled())
    }
}
