package com.nervesparks.iris.core.tools.registry

import com.nervesparks.iris.core.tools.models.ExecutionType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ToolRegistryImplTest {
    
    private lateinit var registry: ToolRegistryImpl
    
    @Before
    fun setup() {
        registry = ToolRegistryImpl()
    }
    
    @Test
    fun `getAllTools returns all registered tools`() {
        val tools = registry.getAllTools()
        
        assertTrue(tools.isNotEmpty())
        assertTrue(tools.size >= 6) // At least the standard tools
    }
    
    @Test
    fun `getTool returns correct tool by name`() {
        val tool = registry.getTool("create_calendar_event")
        
        assertNotNull(tool)
        assertEquals("create_calendar_event", tool?.name)
        assertEquals(ExecutionType.INTENT_LAUNCH, tool?.executionType)
        assertTrue(tool?.parameters?.containsKey("title") == true)
    }
    
    @Test
    fun `getTool returns null for non-existent tool`() {
        val tool = registry.getTool("non_existent_tool")
        
        assertNull(tool)
    }
    
    @Test
    fun `hasTool returns true for existing tool`() {
        assertTrue(registry.hasTool("send_sms"))
    }
    
    @Test
    fun `hasTool returns false for non-existent tool`() {
        assertFalse(registry.hasTool("non_existent_tool"))
    }
    
    @Test
    fun `getToolsByCategory returns tools in category`() {
        val calendarTools = registry.getToolsByCategory("calendar")
        
        assertTrue(calendarTools.isNotEmpty())
        assertTrue(calendarTools.any { it.name == "create_calendar_event" })
    }
    
    @Test
    fun `getToolsByCategory returns empty list for non-existent category`() {
        val tools = registry.getToolsByCategory("non_existent_category")
        
        assertTrue(tools.isEmpty())
    }
    
    @Test
    fun `standard tools have required fields`() {
        val tools = registry.getAllTools()
        
        for (tool in tools) {
            assertNotNull(tool.name)
            assertNotNull(tool.description)
            assertNotNull(tool.parameters)
            assertNotNull(tool.executionType)
            assertTrue(tool.name.isNotEmpty())
            assertTrue(tool.description.isNotEmpty())
        }
    }
    
    @Test
    fun `create_calendar_event has correct parameters`() {
        val tool = registry.getTool("create_calendar_event")
        
        assertNotNull(tool)
        assertTrue(tool?.parameters?.containsKey("title") == true)
        assertTrue(tool?.parameters?.containsKey("datetime") == true)
        assertTrue(tool?.parameters?.get("title")?.required == true)
        assertTrue(tool?.parameters?.get("datetime")?.required == true)
        assertTrue(tool?.parameters?.get("duration_mins")?.required == false)
    }
    
    @Test
    fun `send_sms has correct parameters`() {
        val tool = registry.getTool("send_sms")
        
        assertNotNull(tool)
        assertTrue(tool?.parameters?.containsKey("to") == true)
        assertTrue(tool?.parameters?.containsKey("message") == true)
        assertTrue(tool?.parameters?.get("to")?.required == true)
        assertTrue(tool?.parameters?.get("message")?.required == true)
    }
    
    @Test
    fun `set_alarm has correct parameters`() {
        val tool = registry.getTool("set_alarm")
        
        assertNotNull(tool)
        assertTrue(tool?.parameters?.containsKey("hour") == true)
        assertTrue(tool?.parameters?.containsKey("minute") == true)
        assertTrue(tool?.parameters?.get("hour")?.required == true)
        assertTrue(tool?.parameters?.get("minute")?.required == true)
    }
    
    @Test
    fun `search_contacts uses DIRECT_API execution type`() {
        val tool = registry.getTool("search_contacts")
        
        assertNotNull(tool)
        assertEquals(ExecutionType.DIRECT_API, tool?.executionType)
    }
    
    @Test
    fun `web_search has no required permissions`() {
        val tool = registry.getTool("web_search")
        
        assertNotNull(tool)
        assertTrue(tool?.requiredPermissions?.isEmpty() == true)
    }
}
