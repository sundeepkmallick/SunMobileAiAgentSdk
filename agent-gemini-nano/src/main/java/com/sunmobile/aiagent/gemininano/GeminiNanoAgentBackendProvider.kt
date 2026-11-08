package com.sunmobile.aiagent.gemininano

import android.content.Context
import android.os.Build
import com.google.mlkit.genai.agent.Agent
import com.google.mlkit.genai.agent.AgentEvent
import com.google.mlkit.genai.agent.AgentRunner
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Content
import com.google.mlkit.genai.prompt.Generation
import com.sunmobile.aiagent.core.Agent
import com.sunmobile.aiagent.core.AgentBackendProvider
import com.sunmobile.aiagent.core.AgentCapability
import com.sunmobile.aiagent.core.AgentConfiguration
import com.sunmobile.aiagent.core.AgentEvent as SdkAgentEvent
import com.sunmobile.aiagent.core.AgentPolicy
import com.sunmobile.aiagent.core.AgentRequest
import com.sunmobile.aiagent.core.AgentResult
import com.sunmobile.aiagent.core.ToolRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject

/**
 * ML Kit Agent API + Gemini Nano backend.
 *
 * This backend intentionally uses host-declared Android AppFunctions rather than the SDK's
 * dynamic ToolRegistry. Every AppFunction must independently authenticate and authorize its
 * caller; the SDK AgentPolicy cannot secure framework-dispatched AppFunctions.
 */
class GeminiNanoAgentBackendProvider(
    private val context: Context,
    private val minimumAndroidApi: Int = 36,
) : AgentBackendProvider {
    override val backendId: String = "gemini-nano-appfunctions"
    override val capabilities = setOf(
        AgentCapability.LOCAL_EXECUTION,
        AgentCapability.STREAMING,
        AgentCapability.APP_FUNCTIONS,
        AgentCapability.CANCELLATION,
    )

    override suspend fun createAgent(
        configuration: AgentConfiguration,
        tools: ToolRegistry,
        policy: AgentPolicy,
    ): com.sunmobile.aiagent.core.Agent {
        require(tools.size == 0) {
            "Gemini Nano AppFunctions are declared by the host app; dynamic SDK tools are not supported by this backend."
        }
        return GeminiNanoAgent(context.applicationContext, configuration, minimumAndroidApi)
    }
}

private class GeminiNanoAgent(
    private val context: Context,
    private val configuration: AgentConfiguration,
    private val minimumAndroidApi: Int,
) : com.sunmobile.aiagent.core.Agent {
    override val capabilities = setOf(
        AgentCapability.LOCAL_EXECUTION,
        AgentCapability.STREAMING,
        AgentCapability.APP_FUNCTIONS,
        AgentCapability.CANCELLATION,
    )

    suspend fun availabilityStatus(): String {
        check(Build.VERSION.SDK_INT >= minimumAndroidApi) {
            "Gemini Nano AppFunctions require Android API $minimumAndroidApi or later"
        }
        val generation = Generation.getClient()
        return try {
            generation.checkStatus().toString()
        } finally {
            generation.close()
        }
    }

    suspend fun isAvailable(): Boolean = availabilityStatus() == FeatureStatus.AVAILABLE.toString()

    override suspend fun execute(request: AgentRequest): AgentResult =
        executeInternal(request) {}

    override fun executeStreaming(request: AgentRequest): Flow<SdkAgentEvent> = flow {
        emit(SdkAgentEvent.Started(null))
        try {
            val result = executeInternal(request) { emit(it) }
            emit(SdkAgentEvent.Completed(result))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            emit(SdkAgentEvent.Failed(error))
        }
    }

    private suspend fun executeInternal(
        request: AgentRequest,
        onEvent: suspend (SdkAgentEvent) -> Unit,
    ): AgentResult {
        check(isAvailable()) { "Gemini Nano is not currently available on this device" }
        val generation = Generation.getClient()
        var runner: AgentRunner? = null
        val invokedFunctions = mutableListOf<String>()
        var finalText: String? = null
        try {
            val agent = Agent.builder()
                .setName(configuration.name)
                .setInstructions(configuration.instructions)
                .enableAppFunctions(true)
                .setMaxTurns(configuration.maxTurns)
                .build()
            runner = AgentRunner(context, agent)
            runner.runSingleTurnAsync(Content.builder().text(request.prompt).build()).collect { event ->
                when (event) {
                    is AgentEvent.FunctionCall -> {
                        invokedFunctions += event.name
                        onEvent(SdkAgentEvent.ToolStarted(event.name, buildJsonObject {}))
                    }
                    is AgentEvent.TextMessage -> {
                        if (!event.isPartial) finalText = event.text
                        if (event.text.isNotBlank()) onEvent(SdkAgentEvent.TextDelta(event.text))
                    }
                    else -> Unit
                }
            }
            val response = checkNotNull(finalText?.takeIf(String::isNotBlank)) {
                "Gemini Nano completed without a final response"
            }
            return AgentResult(
                text = response.trim(),
                toolInvocations = invokedFunctions.map {
                    com.sunmobile.aiagent.core.ToolInvocation(it, buildJsonObject {})
                },
                metadata = mapOf("execution" to "on-device", "appFunctionsInvoked" to invokedFunctions.size.toString()),
            )
        } finally {
            runner?.close()
            generation.close()
        }
    }

    override fun close() = Unit
}
