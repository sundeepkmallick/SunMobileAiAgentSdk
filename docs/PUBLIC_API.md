# Public API (0.x)

The API is experimental until backend adapters and compatibility tests are available.

```kotlin
val agent = AgentBuilder()
    .configuration(AgentConfiguration("ResearchAssistant", "Use registered tools."))
    .backend(myBackendProvider)
    .tools(myToolRegistry)
    .policy(myApplicationPolicy)
    .build()

try {
    val result = agent.execute(AgentRequest("Summarize the supplied documents"))
    println(result.text)
} finally {
    agent.close()
}
```

Implement AgentTool with a stable ToolDefinition and JSON object parameter schema. The host owns execution and authorization. Names are unique in a registry. Backends validate arguments and enforce policy decisions.

AgentSession attaches a stable identifier and serializes calls. It is not durable storage. clear() requires a backend implementing SessionAwareAgent.

Keep the first release line at 0.x. Do not promise binary compatibility before Firebase, LiteRT-LM, and Gemini Nano adapters have been tested against supported vendor versions and Android API levels.
