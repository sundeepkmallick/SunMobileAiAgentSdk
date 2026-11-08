package com.sunmobile.aiagent.core

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.*
import org.junit.Test

class ToolExecutorTest {
    private class CountingTool : AgentTool {
        var calls = 0
        override val definition = ToolDefinition(
            "mutate", "A sensitive sample action", buildJsonObject {},
            ToolRiskLevel.SENSITIVE
        )
        override suspend fun execute(arguments: kotlinx.serialization.json.JsonObject, context: ToolExecutionContext): ToolResult {
            calls++
            assertEquals(ToolRiskLevel.SENSITIVE, context.toolRiskLevel)
            return ToolResult("executed")
        }
    }

    @Test fun denyPolicyPreventsInvocation() = runBlocking {
        val tool = CountingTool()
        val executor = ToolExecutor(ToolRegistry().register(tool), AgentPolicy.denyAll())
        val result = executor.execute("mutate", buildJsonObject {}, ToolExecutionContext("test", null))
        assertTrue(result.isError)
        assertEquals(0, tool.calls)
    }

    @Test fun approvalRequestFailsClosed() = runBlocking {
        val tool = CountingTool()
        val policy = AgentPolicy { _, _, _ -> ToolAuthorization.RequireApproval("confirm with operator") }
        val executor = ToolExecutor(ToolRegistry().register(tool), policy)
        val result = executor.execute("mutate", buildJsonObject {}, ToolExecutionContext("test", null))
        assertTrue(result.isError)
        assertEquals(0, tool.calls)
    }

    @Test fun allowPolicyInvokesToolAndPassesRisk() = runBlocking {
        val tool = CountingTool()
        val executor = ToolExecutor(ToolRegistry().register(tool), AgentPolicy.allowAll())
        val result = executor.execute("mutate", buildJsonObject {}, ToolExecutionContext("test", null))
        assertFalse(result.isError)
        assertEquals(1, tool.calls)
    }
}
