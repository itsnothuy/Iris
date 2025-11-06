package com.nervesparks.iris.core.safety

import com.nervesparks.iris.common.config.SafetyLevel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SafetyEngineImpl
 */
class SafetyEngineImplTest {
    
    private lateinit var safetyEngine: SafetyEngineImpl
    
    @Before
    fun setup() {
        safetyEngine = SafetyEngineImpl()
    }
    
    @Test
    fun `checkInput allows safe content at MEDIUM level`() = runTest {
        safetyEngine.updateSafetyLevel(SafetyLevel.MEDIUM)
        
        val result = safetyEngine.checkInput("Hello, how are you?")
        
        assertTrue(result.isAllowed)
    }
    
    @Test
    fun `checkInput blocks harmful content at HIGH level`() = runTest {
        safetyEngine.updateSafetyLevel(SafetyLevel.HIGH)
        
        val result = safetyEngine.checkInput("harmful content")
        
        assertFalse(result.isAllowed)
    }
    
    @Test
    fun `checkInput allows all content at NONE level`() = runTest {
        safetyEngine.updateSafetyLevel(SafetyLevel.NONE)
        
        val result = safetyEngine.checkInput("anything goes")
        
        assertTrue(result.isAllowed)
    }
    
    @Test
    fun `updateSafetyLevel changes current level`() {
        safetyEngine.updateSafetyLevel(SafetyLevel.HIGH)
        
        assertEquals(SafetyLevel.HIGH, safetyEngine.getSafetyLevel())
    }
    
    @Test
    fun `checkOutput allows content by default`() = runTest {
        val result = safetyEngine.checkOutput("Generated response")
        
        assertTrue(result.isAllowed)
    }
}
