package com.sunmobile.aiagent.sample

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.sunmobile.aiagent.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Offline sample. The tool returns fixed notes; this is not live web research. */
class MainActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        output = TextView(this).apply {
            text = "Offline demonstration: the backend and research notes are mocked. No network calls are made."
            textSize = 16f
            setPadding(24, 24, 24, 24)
        }
        val run = Button(this).apply {
            text = "Run sample research"
            setOnClickListener { runResearch() }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(16, 24, 16, 16)
            addView(run)
            addView(ScrollView(this@MainActivity).apply { addView(output) },
                LinearLayout.LayoutParams(-1, 0, 1f))
        })
    }

    private fun runResearch() {
        output.text = "Running offline demo..."
        scope.launch {
            val registry = ToolRegistry().register(SampleKnowledgeTool())
            val agent = AgentBuilder()
                .configuration(AgentConfiguration(
                    name = "SampleResearchAssistant",
                    instructions = "Summarize the supplied sample notes and disclose they are mock data."
                ))
                .backend(SampleMockBackend())
                .tools(registry)
                .policy(AgentPolicy.allowAll())
                .build()
            try {
                output.text = agent.execute(AgentRequest(
                    "Summarize the sample notes about renewable energy."
                )).text ?: "No text returned."
            } finally {
                agent.close()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

private class SampleKnowledgeTool : AgentTool {
    override val definition = ToolDefinition(
        name = "readSampleNotes",
        description = "Read fixed illustrative notes bundled with this sample.",
        parameters = buildJsonObject { put("type", "object") }
    )

    override suspend fun execute(
        arguments: kotlinx.serialization.json.JsonObject,
        context: ToolExecutionContext
    ) = ToolResult(
        "MOCK SAMPLE DATA: notes mention solar and wind generation, grid storage, transmission upgrades, and demand response. This is illustrative, not sourced research."
    )
}

private class SampleMockBackend : AgentBackendProvider {
    override val backendId = "sample-mock"
    override val capabilities = setOf(AgentCapability.TOOL_CALLING, AgentCapability.MULTI_TURN)

    override suspend fun createAgent(
        configuration: AgentConfiguration,
        tools: ToolRegistry,
        policy: AgentPolicy
    ): Agent = object : Agent {
        override val capabilities = this@SampleMockBackend.capabilities

        override suspend fun execute(request: AgentRequest): AgentResult {
            val tool = tools.get("readSampleNotes")
                ?: return AgentResult("No sample notes tool registered.")
            val context = ToolExecutionContext(configuration.name, request.sessionId, request.metadata)
            val decision = policy.authorize(tool.definition.name, buildJsonObject {}, context)
            if (decision !is ToolAuthorization.Allow) {
                return AgentResult("Host policy did not authorize the sample tool.")
            }
            val notes = tool.execute(buildJsonObject {}, context)
            return AgentResult(
                "Offline mock summary:\n\n${notes.content}\n\nThis demonstrates SDK composition only. Replace the mock provider and tool with real, host-authorized integrations."
            )
        }

        override fun executeStreaming(request: AgentRequest): Flow<AgentEvent> = flow {
            emit(AgentEvent.Started(request.sessionId))
            val result = execute(request)
            emit(AgentEvent.TextDelta(result.text.orEmpty()))
            emit(AgentEvent.Completed(result))
        }

        override fun close() = Unit
    }
}
