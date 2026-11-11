package com.sunmobile.aiagent.mlkit

import android.content.Context
import android.os.Build
import com.google.mlkit.genai.agent.Agent as MlKitAgent
import com.google.mlkit.genai.agent.AgentEvent as MlKitAgentEvent
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

/**
 * First-class Gemini Nano provider backed by ML Kit GenAI Agent and Prompt APIs.
 *
 * Host AppFunctions are supported. Dynamic [ToolRegistry] tools are intentionally rejected:
 * ML Kit dispatches AppFunctions through Android's AppFunctions framework, outside the SDK's
 * [AgentPolicy] enforcement boundary. Every host AppFunction must independently validate
 * caller identity, authorization, arguments and current business state.
 */
class GeminiNanoAgentBackendProvider(
    context: Context,
    val configuration: GeminiNanoConfiguration,
) : AgentBackendProvider {
    private val applicationContext = context.applicationContext

    override val backendId: String = "gemini-nano-mlkit"
    override val capabilities: Set<AgentCapability> = buildSet {
        add(AgentCapability.LOCAL_EXECUTION)
        add(AgentCapability.STREAMING)
        add(AgentCapability.CANCELLATION)
        if (configuration.enableAppFunctions) add(AgentCapability.APP_FUNCTIONS)
    }

    /** Checks device/model readiness without initiating model download. */
    suspend fun checkAvailability(): GeminiNanoAvailability {
        if (Build.VERSION.SDK_INT < configuration.minimumAndroidApi) {
            return GeminiNanoAvailability.UnsupportedAndroidVersion(
                requiredApi = configuration.minimumAndroidApi,
                actualApi = Build.VERSION.SDK_INT,
            )
        }
        val generation = try {
            Generation.getClient()
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            return GeminiNanoAvailability.StatusCheckFailed(error)
        }
        return try {
            val status = generation.checkStatus()
            if (status == FeatureStatus.AVAILABLE) {
                GeminiNanoAvailability.Available
            } else {
                GeminiNanoAvailability.ModelNotReady(status.toString())
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            GeminiNanoAvailability.StatusCheckFailed(error)
        } finally {
            generation.close()
        }
    }

    override suspend fun createAgent(
        configuration: AgentConfiguration,
        tools: ToolRegistry,
        policy: AgentPolicy,
    ): Agent {
        require(tools.size == 0) {
            "Gemini Nano AppFunctions are host-declared; dynamic SDK ToolRegistry tools are not supported."
        }
        return MlKitGeminiNanoAgent(
            context = applicationContext,
            sdkConfiguration = configuration,
            mlKitConfiguration = this.configuration,
            availability = ::checkAvailability,
        )
    }
}

private class MlKitGeminiNanoAgent(
    private val context: Context,
    private val sdkConfiguration: AgentConfiguration,
    private val mlKitConfiguration: GeminiNanoConfiguration,
    private val availability: suspend () -> GeminiNanoAvailability,
) : Agent {
    override val capabilities: Set<AgentCapability> = buildSet {
        add(AgentCapability.LOCAL_EXECUTION)
        add(AgentCapability.STREAMING)
        add(AgentCapability.CANCELLATION)
        if (mlKitConfiguration.enableAppFunctions) add(AgentCapability.APP_FUNCTIONS)
    }

    override suspend fun execute(request: AgentRequest): AgentResult =
        executeInternal(request) {}

    override fun executeStreaming(request: AgentRequest): Flow<SdkAgentEvent> = flow {
        emit(SdkAgentEvent.Started(request.sessionId))
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
        when (val readiness = availability()) {
            GeminiNanoAvailability.Available -> Unit
            is GeminiNanoAvailability.UnsupportedAndroidVersion -> throw GeminiNanoException(
                GeminiNanoErrorCode.UNSUPPORTED_DEVICE,
                "Gemini Nano requires Android API ${readiness.requiredApi}; device API is ${readiness.actualApi}.",
            )
            is GeminiNanoAvailability.ModelNotReady -> throw GeminiNanoException(
                GeminiNanoErrorCode.MODEL_NOT_READY,
                "Gemini Nano is not ready (ML Kit feature status: ${readiness.featureStatus}).",
            )
            is GeminiNanoAvailability.StatusCheckFailed -> throw GeminiNanoException(
                GeminiNanoErrorCode.INITIALIZATION_FAILED,
                "Unable to check Gemini Nano model readiness.",
                readiness.cause,
            )
        }

        var runner: AgentRunner? = null
        val invokedFunctions = mutableListOf<String>()
        var finalText: String? = null
        try {
            val mlKitAgent = try {
                MlKitAgent.builder()
                    .setName(mlKitConfiguration.agentName.ifBlank { sdkConfiguration.name })
                    .setInstructions(
                        if (mlKitConfiguration.instructions.isNotBlank()) {
                            mlKitConfiguration.instructions
                        } else {
                            sdkConfiguration.instructions
                        }
                    )
                    .enableAppFunctions(mlKitConfiguration.enableAppFunctions)
                    .setMaxTurns(mlKitConfiguration.maxTurns)
                    .build()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                throw GeminiNanoException(
                    GeminiNanoErrorCode.INITIALIZATION_FAILED,
                    "Failed to initialize the ML Kit Gemini Nano agent.",
                    error,
                )
            }
            runner = try {
                AgentRunner(context, mlKitAgent)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                throw GeminiNanoException(
                    GeminiNanoErrorCode.INITIALIZATION_FAILED,
                    "Failed to initialize the ML Kit AgentRunner.",
                    error,
                )
            }

            try {
                runner.runSingleTurnAsync(Content.builder().text(request.prompt).build()).collect { event ->
                    when (event) {
                        is MlKitAgentEvent.FunctionCall -> {
                            invokedFunctions += event.name
                            onEvent(SdkAgentEvent.ToolStarted(event.name, kotlinx.serialization.json.buildJsonObject {}))
                        }
                        is MlKitAgentEvent.TextMessage -> {
                            if (!event.isPartial) finalText = event.text
                            if (event.text.isNotBlank()) onEvent(SdkAgentEvent.TextDelta(event.text))
                        }
                        else -> Unit
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                throw GeminiNanoException(
                    GeminiNanoErrorCode.INFERENCE_FAILED,
                    "Gemini Nano inference failed.",
                    error,
                )
            }

            val response = finalText?.takeIf(String::isNotBlank)
                ?: throw GeminiNanoException(
                    GeminiNanoErrorCode.INFERENCE_FAILED,
                    "Gemini Nano completed without a final text response.",
                )
            return AgentResult(
                text = response.trim(),
                toolInvocations = invokedFunctions.map {
                    com.sunmobile.aiagent.core.ToolInvocation(
                        toolName = it,
                        arguments = kotlinx.serialization.json.buildJsonObject {},
                    )
                },
                sessionId = request.sessionId,
                metadata = mapOf(
                    "backend" to "gemini-nano-mlkit",
                    "execution" to "on-device",
                    "appFunctionsInvoked" to invokedFunctions.size.toString(),
                ),
            )
        } finally {
            runner?.close()
        }
    }

    override fun close() = Unit
}
