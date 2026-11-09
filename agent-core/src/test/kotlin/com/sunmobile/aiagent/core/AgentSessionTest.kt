package com.sunmobile.aiagent.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentSessionTest {
    @Test fun builderPreservesSessionClearDelegation() = runBlocking {
        var cleared = false
        val backendAgent = object : Agent, SessionAwareAgent {
            override val capabilities = setOf(AgentCapability.SESSION_CLEAR)
            override suspend fun execute(request: AgentRequest) = AgentResult("ok")
            override fun executeStreaming(request: AgentRequest): Flow<AgentEvent> = emptyFlow()
            override suspend fun clearSession(sessionId: String) { cleared = true }
            override fun close() = Unit
        }
        val provider = object : AgentBackendProvider {
            override val backendId = "test"
            override val capabilities = setOf(AgentCapability.SESSION_CLEAR)
            override suspend fun createAgent(
                configuration: AgentConfiguration,
                tools: ToolRegistry,
                policy: AgentPolicy
            ): Agent = backendAgent
        }
        val agent = AgentBuilder()
            .configuration(AgentConfiguration("test", "test"))
            .backend(provider)
            .build()
        AgentSession(agent).clear()
        assertTrue(cleared)
        agent.close()
    }
}
