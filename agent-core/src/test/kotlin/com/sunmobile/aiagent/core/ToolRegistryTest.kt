package com.sunmobile.aiagent.core

import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.*
import org.junit.Test

class ToolRegistryTest {
    private fun tool(name: String) = object : AgentTool {
        override val definition = ToolDefinition(name, "Test tool", buildJsonObject {})
        override suspend fun execute(arguments: kotlinx.serialization.json.JsonObject, context: ToolExecutionContext) =
            ToolResult("ok")
    }

    @Test fun registerAndLookup() {
        val registry = ToolRegistry().register(tool("lookup"))
        assertEquals(1, registry.size)
        assertNotNull(registry.get("lookup"))
        assertTrue(registry.contains("lookup"))
    }

    @Test fun duplicateNamesRejected() {
        val registry = ToolRegistry().register(tool("lookup"))
        assertThrows(IllegalArgumentException::class.java) { registry.register(tool("lookup")) }
    }

    @Test fun unregisterIsObservable() {
        val registry = ToolRegistry().register(tool("lookup"))
        assertTrue(registry.unregister("lookup"))
        assertFalse(registry.unregister("lookup"))
    }
}
