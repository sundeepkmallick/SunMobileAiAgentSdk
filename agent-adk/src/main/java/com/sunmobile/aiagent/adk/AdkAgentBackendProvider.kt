package com.sunmobile.aiagent.adk

import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.agents.ReadonlyContext
import com.google.adk.kt.models.Model
import com.google.adk.kt.runners.InMemoryRunner
import com.google.adk.kt.sessions.InMemorySessionService
import com.google.adk.kt.sessions.SessionKey
import com.google.adk.kt.tools.BaseTool
import com.google.adk.kt.tools.ToolContext
import com.google.adk.kt.tools.Toolset
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.FunctionDeclaration
import com.google.adk.kt.types.Part
import com.google.adk.kt.agents.RunConfig
import com.google.adk.kt.types.Schema
import com.google.adk.kt.types.Type
import com.sunmobile.aiagent.core.Agent
import com.sunmobile.aiagent.core.AgentBackendProvider
import com.sunmobile.aiagent.core.AgentCapability
import com.sunmobile.aiagent.core.AgentConfiguration
import com.sunmobile.aiagent.core.AgentEvent
import com.sunmobile.aiagent.core.AgentRequest
import com.sunmobile.aiagent.core.AgentResult
import com.sunmobile.aiagent.core.AgentPolicy
import com.sunmobile.aiagent.core.AgentTool
import com.sunmobile.aiagent.core.SessionAwareAgent
import com.sunmobile.aiagent.core.ToolExecutor
import com.sunmobile.aiagent.core.ToolExecutionContext
import com.sunmobile.aiagent.core.ToolInvocation
import com.sunmobile.aiagent.core.ToolRegistry
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/** Supplies a vendor-specific ADK Model; the shared ADK runner and tool bridge stay generic. */
fun interface AdkModelFactory {
    suspend fun createModel(): Model
}

/**
 * Google ADK runner adapter. Sessions live in an in-memory ADK session service; they are not
 * durable across process death. Host tools are exposed through an ADK Toolset and authorized
 * centrally through the core ToolExecutor before their implementation runs.
 */
class AdkAgentBackendProvider(
    private val modelFactory: AdkModelFactory,
    private val appName: String = "SunMobileAiAgentSdk",
) : AgentBackendProvider {
    override val backendId: String = "google-adk"
    override val capabilities = setOf(
        AgentCapability.TOOL_CALLING,
        AgentCapability.STREAMING,
        AgentCapability.MULTI_TURN,
        AgentCapability.SESSION_CLEAR,
        AgentCapability.CANCELLATION,
    )

    override suspend fun createAgent(
        configuration: AgentConfiguration,
        tools: ToolRegistry,
        policy: AgentPolicy,
    ): Agent {
        val model = modelFactory.createModel()
        try {
            val toolset = CoreAgentToolset(configuration.name, tools, policy)
            val adkAgent = LlmAgent(
                name = configuration.name,
                model = model,
                instruction = Instruction(configuration.instructions),
                toolsets = listOf(toolset),
            )
            val sessionService = InMemorySessionService()
            val runner = InMemoryRunner(adkAgent, appName, sessionService)
            return AdkAgent(runner, sessionService, model, appName, configuration.name)
        } catch (error: Throwable) {
            (model as? AutoCloseable)?.let { runCatching { it.close() } }
            throw error
        }
    }
}

private class AdkAgent(
    private val runner: InMemoryRunner,
    private val sessions: InMemorySessionService,
    private val model: Model,
    private val appName: String,
    private val agentName: String,
) : Agent, SessionAwareAgent {
    override val capabilities = setOf(
        AgentCapability.TOOL_CALLING,
        AgentCapability.STREAMING,
        AgentCapability.MULTI_TURN,
        AgentCapability.SESSION_CLEAR,
        AgentCapability.CANCELLATION,
    )
    private val sessionUsers = ConcurrentHashMap<String, String>()

    override suspend fun execute(request: AgentRequest): AgentResult =
        executeInternal(request) {}

    override fun executeStreaming(request: AgentRequest): Flow<AgentEvent> = flow {
        emit(AgentEvent.Started(request.sessionId))
        try {
            val result = executeInternal(request) { emit(it) }
            emit(AgentEvent.Completed(result))
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            emit(AgentEvent.Failed(error))
        }
    }

    private suspend fun executeInternal(
        request: AgentRequest,
        onEvent: suspend (AgentEvent) -> Unit,
    ): AgentResult {
        val userId = request.metadata["userId"]?.takeIf(String::isNotBlank) ?: "default"
        val sessionId = request.sessionId ?: UUID.randomUUID().toString()
        sessionUsers[sessionId] = userId
        val textParts = mutableListOf<String>()
        val invocations = mutableListOf<ToolInvocation>()

        runner.runAsync(
            userId = userId,
            sessionId = sessionId,
            invocationId = null,
            newMessage = Content(role = "user", parts = listOf(Part(text = request.prompt))),
            stateDelta = emptyMap(),
            runConfig = RunConfig(),
        ).collect { event ->
            event.content?.parts.orEmpty().forEach { part ->
                part.text?.takeIf(String::isNotBlank)?.let { text ->
                    if (event.author == agentName) {
                        textParts += text
                        onEvent(AgentEvent.TextDelta(text))
                    }
                }
                part.functionCall?.let { call ->
                    val args = call.args.orEmpty().toJsonObject()
                    invocations += ToolInvocation(call.name, args)
                    onEvent(AgentEvent.ToolStarted(call.name, args))
                }
            }
        }
        return AgentResult(
            text = textParts.lastOrNull()?.trim()?.takeIf(String::isNotBlank),
            toolInvocations = invocations,
            sessionId = sessionId,
        )
    }

    override suspend fun clearSession(sessionId: String) {
        val userId = sessionUsers[sessionId] ?: "default"
        sessions.deleteSession(SessionKey(appName, userId, sessionId))
        sessionUsers.remove(sessionId)
    }

    override fun close() {
        try {
            runner.close()
        } finally {
            (model as? AutoCloseable)?.close()
        }
    }
}

/** Dynamic ADK toolset that mirrors the current core registry. */
private class CoreAgentToolset(
    private val agentName: String,
    private val registry: ToolRegistry,
    private val policy: AgentPolicy,
) : Toolset {
    private val executor = ToolExecutor(registry, policy)

    override suspend fun getTools(readonlyContext: ReadonlyContext?): List<BaseTool> =
        registry.all().map { tool -> CoreBaseTool(agentName, tool, executor) }
}

private class CoreBaseTool(
    private val agentName: String,
    private val coreTool: AgentTool,
    private val executor: ToolExecutor,
) : BaseTool(coreTool.definition.name, coreTool.definition.description) {
    override fun declaration(): FunctionDeclaration = FunctionDeclaration(
        name = coreTool.definition.name,
        description = coreTool.definition.description,
        parameters = coreTool.definition.parameters.toAdkSchema(),
    )

    override suspend fun run(context: ToolContext, args: Map<String, Any?>): Any {
        val result = executor.execute(
            toolName = coreTool.definition.name,
            arguments = args.toJsonObject(),
            context = ToolExecutionContext(
                agentName = agentName,
                sessionId = context.invocationContext.session.key.id,
                requestMetadata = mapOf(
                    "adkUserId" to context.invocationContext.session.key.userId
                ),
            ),
        )
        if (result.isError) return mapOf("error" to result.content)
        return result.structuredContent?.toNativeValue() ?: mapOf("result" to result.content)
    }
}

private fun JsonObject.toAdkSchema(): Schema {
    val rawType = this["type"]?.jsonPrimitive?.contentOrNull
    val type = rawType?.let { Type.valueOf(it.uppercase()) }
    val properties = this["properties"]?.let { element ->
        element.jsonObject.mapValues { (_, value) -> value.jsonObject.toAdkSchema() }
    }
    val items = this["items"]?.jsonObject?.toAdkSchema()
    val required = this["required"]?.jsonArray?.map { it.jsonPrimitive.content }
    val description = this["description"]?.jsonPrimitive?.contentOrNull
    val enumValues = this["enum"]?.jsonArray?.map { it.jsonPrimitive.content }
    return Schema(
        type = type,
        properties = properties,
        items = items,
        required = required,
        description = description,
        enum = enumValues,
    )
}

private fun Map<String, Any?>.toJsonObject(): JsonObject =
    JsonObject(mapValues { (_, value) -> value.toJsonElement() })

private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Map<*, *> -> JsonObject(entries.associate { (key, value) -> key.toString() to value.toJsonElement() })
    is Iterable<*> -> JsonArray(map { it.toJsonElement() })
    else -> JsonPrimitive(toString())
}

private fun JsonElement.toNativeValue(): Any? = when (this) {
    JsonNull -> null
    is JsonObject -> mapValues { it.value.toNativeValue() }
    is JsonArray -> map { it.toNativeValue() }
    is JsonPrimitive -> booleanOrNull ?: longOrNull ?: doubleOrNull ?: contentOrNull
}
