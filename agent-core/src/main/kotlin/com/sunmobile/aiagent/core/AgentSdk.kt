package com.sunmobile.aiagent.core

import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.booleanOrNull

/** Capabilities actually supported by a selected backend/model configuration. */
enum class AgentCapability {
    TOOL_CALLING, STREAMING, MULTI_TURN, SESSION_CLEAR, STRUCTURED_OUTPUT,
    LOCAL_EXECUTION, CLOUD_EXECUTION, MULTIMODAL_INPUT, CANCELLATION, APP_FUNCTIONS
}

data class AgentConfiguration(
    val name: String,
    val instructions: String,
    val maxTurns: Int = 10,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(name.isNotBlank()) { "Agent name must not be blank" }
        require(maxTurns > 0) { "maxTurns must be greater than zero" }
    }
}

data class AgentRequest(
    val prompt: String,
    val sessionId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val requiredCapabilities: Set<AgentCapability> = emptySet(),
    val structuredOutputSchema: JsonElement? = null
) {
    init {
        require(prompt.isNotBlank()) { "Prompt must not be blank" }
        require(sessionId == null || sessionId.isNotBlank()) { "sessionId must not be blank" }
    }
}

data class ToolInvocation(
    val toolName: String,
    val arguments: JsonElement,
    val resultSummary: String? = null
)

data class AgentResult(
    val text: String?,
    val structuredOutput: JsonElement? = null,
    val toolInvocations: List<ToolInvocation> = emptyList(),
    val sessionId: String? = null,
    val finishReason: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

sealed interface AgentEvent {
    data class Started(val sessionId: String?) : AgentEvent
    data class TextDelta(val text: String) : AgentEvent
    data class ToolStarted(val name: String, val arguments: JsonElement) : AgentEvent
    data class ToolCompleted(val name: String, val resultSummary: String?) : AgentEvent
    data class Completed(val result: AgentResult) : AgentEvent
    data class Failed(val cause: Throwable) : AgentEvent
}

interface Agent : AutoCloseable {
    val capabilities: Set<AgentCapability>
    suspend fun execute(request: AgentRequest): AgentResult
    fun executeStreaming(request: AgentRequest): Flow<AgentEvent>
    override fun close()
}

interface SessionAwareAgent {
    suspend fun clearSession(sessionId: String)
}

/** Optional backend artifact implements this provider SPI. */
interface AgentBackendProvider {
    val backendId: String
    val capabilities: Set<AgentCapability>
    suspend fun createAgent(
        configuration: AgentConfiguration,
        tools: ToolRegistry = ToolRegistry(),
        policy: AgentPolicy = AgentPolicy.denyAll()
    ): Agent
}

class AgentBuilder {
    private var configuration: AgentConfiguration? = null
    private var backend: AgentBackendProvider? = null
    private var tools: ToolRegistry = ToolRegistry()
    private var policy: AgentPolicy = AgentPolicy.denyAll()

    fun configuration(value: AgentConfiguration) = apply { configuration = value }
    fun backend(value: AgentBackendProvider) = apply { backend = value }
    fun tools(value: ToolRegistry) = apply { tools = value }
    fun policy(value: AgentPolicy) = apply { policy = value }

    suspend fun build(): Agent {
        val provider = requireNotNull(backend) { "A backend provider is required" }
        val config = requireNotNull(configuration) { "Agent configuration is required" }
        val agent = provider.createAgent(config, tools, policy)
        require(agent.capabilities.containsAll(provider.capabilities)) {
            "Backend '${provider.backendId}' advertises capabilities not implemented by its Agent"
        }
        return CapabilityCheckingAgent(agent)
    }
}

/**
 * A conversation-scoped facade. It serializes calls and attaches a stable session ID.
 * This does not imply durable storage or cross-process persistence.
 */
class AgentSession(private val agent: Agent, val id: String = UUID.randomUUID().toString()) : AutoCloseable {
    private val mutex = Mutex()
    private val closed = AtomicBoolean(false)

    suspend fun execute(request: AgentRequest): AgentResult = mutex.withLock {
        check(!closed.get()) { "Agent session is closed" }
        agent.execute(request.copy(sessionId = id))
    }

    fun executeStreaming(request: AgentRequest): Flow<AgentEvent> {
        check(!closed.get()) { "Agent session is closed" }
        return flow {
            mutex.withLock {
                agent.executeStreaming(request.copy(sessionId = id)).collect { emit(it) }
            }
        }
    }

    suspend fun clear() = mutex.withLock {
        check(!closed.get()) { "Agent session is closed" }
        val aware = agent as? SessionAwareAgent
            ?: throw UnsupportedOperationException("This backend does not support clearing sessions")
        aware.clearSession(id)
    }

    override fun close() { closed.set(true) }
}

data class ToolExecutionContext(
    val agentName: String,
    val sessionId: String?,
    val requestMetadata: Map<String, String> = emptyMap(),
    val toolRiskLevel: ToolRiskLevel = ToolRiskLevel.READ_ONLY
)

enum class ToolRiskLevel { READ_ONLY, REVERSIBLE, SENSITIVE, HIGH_IMPACT }

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject,
    val riskLevel: ToolRiskLevel = ToolRiskLevel.READ_ONLY
) {
    init {
        require(name.matches(Regex("^[A-Za-z][A-Za-z0-9_]{0,63}$"))) { "Invalid tool name: $name" }
        require(description.isNotBlank()) { "Tool description must not be blank" }
    }
}

data class ToolResult(
    val content: String,
    val structuredContent: JsonElement? = null,
    val isError: Boolean = false
) {
    init { require(content.isNotBlank() || structuredContent != null) }
}

interface AgentTool {
    val definition: ToolDefinition
    suspend fun execute(arguments: JsonObject, context: ToolExecutionContext): ToolResult
}

/** Synchronized registry; duplicate tool names fail fast. Registration is not authorization. */
class ToolRegistry {
    private val lock = Any()
    private val tools = linkedMapOf<String, AgentTool>()

    fun register(tool: AgentTool): ToolRegistry = apply {
        synchronized(lock) {
            require(tool.definition.name !in tools) { "Tool '${tool.definition.name}' is already registered" }
            tools[tool.definition.name] = tool
        }
    }
    fun unregister(name: String): Boolean = synchronized(lock) { tools.remove(name) != null }
    fun get(name: String): AgentTool? = synchronized(lock) { tools[name] }
    fun all(): List<AgentTool> = synchronized(lock) { tools.values.toList() }
    fun contains(name: String): Boolean = synchronized(lock) { name in tools }
    val size: Int get() = synchronized(lock) { tools.size }
}

/** A host-controlled policy must authorize each tool invocation. */
fun interface AgentPolicy {
    suspend fun authorize(toolName: String, arguments: JsonObject, context: ToolExecutionContext): ToolAuthorization

    companion object {
        fun denyAll(): AgentPolicy = AgentPolicy { _, _, _ -> ToolAuthorization.Deny("No tool policy configured") }
        fun allowAll(): AgentPolicy = AgentPolicy { _, _, _ -> ToolAuthorization.Allow }
    }
}

sealed interface ToolAuthorization {
    data object Allow : ToolAuthorization
    data class Deny(val reason: String) : ToolAuthorization
    data class RequireApproval(val reason: String) : ToolAuthorization
}


private class CapabilityCheckingAgent(private val delegate: Agent) : Agent {
    override val capabilities: Set<AgentCapability> get() = delegate.capabilities

    override suspend fun execute(request: AgentRequest): AgentResult {
        val unsupported = request.requiredCapabilities - capabilities
        require(unsupported.isEmpty()) { "Backend does not support required capabilities: ${unsupported.joinToString()}" }
        return delegate.execute(request)
    }

    override fun executeStreaming(request: AgentRequest): Flow<AgentEvent> {
        val unsupported = request.requiredCapabilities - capabilities
        require(unsupported.isEmpty()) { "Backend does not support required capabilities: ${unsupported.joinToString()}" }
        require(AgentCapability.STREAMING in capabilities) { "Backend does not support streaming" }
        return delegate.executeStreaming(request)
    }

    override fun close() = delegate.close()
}


/**
 * Applies the host policy before invoking a registered tool. RequireApproval is
 * deliberately fail-closed: the host must obtain approval out-of-band and then
 * re-authorize; the SDK never interprets model output as consent.
 */
class ToolExecutor(
    private val registry: ToolRegistry,
    private val policy: AgentPolicy
) {
    suspend fun execute(
        toolName: String,
        arguments: JsonObject,
        context: ToolExecutionContext
    ): ToolResult {
        val tool = registry.get(toolName)
            ?: return ToolResult("Unknown tool: $toolName", isError = true)
        val scopedContext = context.copy(toolRiskLevel = tool.definition.riskLevel)
        try {
            ToolSchemaValidator.validate(tool.definition.parameters, arguments, "$")
        } catch (invalid: IllegalArgumentException) {
            return ToolResult("Invalid tool arguments: ${invalid.message}", isError = true)
        }
        return when (val authorization = policy.authorize(toolName, arguments, scopedContext)) {
            ToolAuthorization.Allow -> tool.execute(arguments, scopedContext)
            is ToolAuthorization.Deny -> ToolResult("Tool denied: ${authorization.reason}", isError = true)
            is ToolAuthorization.RequireApproval ->
                ToolResult("Tool requires host approval: ${authorization.reason}", isError = true)
        }
    }
}


/** Validates the deliberately small JSON Schema subset exposed by the portable core. */
private object ToolSchemaValidator {
    fun validate(schema: JsonObject, value: JsonObject, path: String) {
        validateValue(schema, value, path)
    }

    private fun validateValue(schema: JsonObject, value: JsonElement, path: String) {
        val type = schema["type"]?.jsonPrimitive?.contentOrNull
        when (type) {
            null -> Unit
            "object" -> {
                require(value is JsonObject) { "$path must be an object" }
                val properties = schema["properties"]?.jsonObject.orEmpty()
                val required = schema["required"]?.jsonArray.orEmpty().map { it.jsonPrimitive.content }
                required.forEach { key -> require(key in value) { "$path.$key is required" } }
                properties.forEach { (key, childSchema) ->
                    value[key]?.let { validateValue(childSchema.jsonObject, it, "$path.$key") }
                }
                if (schema["additionalProperties"]?.jsonPrimitive?.booleanOrNull == false) {
                    val unknown = value.keys - properties.keys
                    require(unknown.isEmpty()) { "$path contains unsupported properties: ${unknown.joinToString()}" }
                }
            }
            "string" -> {
                require(value is kotlinx.serialization.json.JsonPrimitive && value.isString) { "$path must be a string" }
                val string = value.content
                schema["minLength"]?.jsonPrimitive?.intOrNull?.let { require(string.length >= it) { "$path is too short" } }
                schema["maxLength"]?.jsonPrimitive?.intOrNull?.let { require(string.length <= it) { "$path is too long" } }
                schema["enum"]?.jsonArray?.let { allowed ->
                    require(allowed.any { it.jsonPrimitive.content == string }) { "$path is not an allowed value" }
                }
            }
            "integer" -> require(value.jsonPrimitive.longOrNull != null) { "$path must be an integer" }
            "number" -> require(value.jsonPrimitive.doubleOrNull != null) { "$path must be a number" }
            "boolean" -> require(value.jsonPrimitive.booleanOrNull != null) { "$path must be a boolean" }
            "array" -> {
                require(value is kotlinx.serialization.json.JsonArray) { "$path must be an array" }
                schema["items"]?.jsonObject?.let { itemSchema ->
                    value.forEachIndexed { index, item -> validateValue(itemSchema, item, "$path[$index]") }
                }
            }
            else -> throw IllegalArgumentException("Unsupported schema type '$type' at $path")
        }
        val number = value.jsonPrimitiveOrNull?.doubleOrNull
        number?.let {
            schema["minimum"]?.jsonPrimitive?.doubleOrNull?.let { min -> require(it >= min) { "$path is below minimum" } }
            schema["maximum"]?.jsonPrimitive?.doubleOrNull?.let { max -> require(it <= max) { "$path is above maximum" } }
        }
    }
}

private val JsonElement.jsonPrimitiveOrNull: kotlinx.serialization.json.JsonPrimitive?
    get() = this as? kotlinx.serialization.json.JsonPrimitive
