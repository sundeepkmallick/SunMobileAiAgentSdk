package com.sunmobile.aiagent.core

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.putJsonArray
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

    @Test fun invalidArgumentsAreRejectedBeforePolicyOrTool() = runBlocking {
        val tool = object : AgentTool {
            override val definition = ToolDefinition(
                "requiredInput", "Requires a query",
                buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") { putJsonObject("query") { put("type", "string") } }
                    putJsonArray("required") { add(JsonPrimitive("query")) }
                }
            )
            override suspend fun execute(arguments: JsonObject, context: ToolExecutionContext) = ToolResult("unexpected")
        }
        var policyCalls = 0
        val policy = AgentPolicy { _, _, _ -> policyCalls++; ToolAuthorization.Allow }
        val result = ToolExecutor(ToolRegistry().register(tool), policy)
            .execute("requiredInput", buildJsonObject {}, ToolExecutionContext("test", null))
        assertTrue(result.isError)
        assertEquals(0, policyCalls)
    }

    @Test fun allowPolicyInvokesToolAndPassesRisk() = runBlocking {
        val tool = CountingTool()
        val executor = ToolExecutor(ToolRegistry().register(tool), AgentPolicy.allowAll())
        val result = executor.execute("mutate", buildJsonObject {}, ToolExecutionContext("test", null))
        assertFalse(result.isError)
        assertEquals(1, tool.calls)
    }
}
