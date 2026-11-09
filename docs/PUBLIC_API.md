# Public API (0.1.0-rc.1 review candidate)

The public API is experimental in this 0.1.0-rc.1 review candidate. SDK CI verifies compilation and core unit tests, but device/runtime validation, independent binary/API compatibility review and production qualification are not complete. Do not treat this candidate as a stable compatibility promise.

## Create and execute an agent

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

Provide a backend provider, a ToolRegistry and an explicit AgentPolicy. The default policy denies tool calls. Keep host-owned authorization and business validation in the tool implementation and authoritative service; schema validation is not authorization.

## Tools and sessions

Implement `AgentTool` with a stable `ToolDefinition` and JSON object parameter schema. Tool names must be unique in a registry. Validate arguments again at execution time and return only the minimum data needed.

`AgentSession` attaches a stable identifier and serializes calls. It is not durable storage. Clearing a session requires a backend implementing `SessionAwareAgent`; ADK sessions in this adapter are in-memory and are lost with process lifetime.

## Backend-specific constraints

- Firebase and LiteRT-LM use `AdkAgentBackendProvider` with their respective model factories and the shared dynamic core-tool bridge.
- LiteRT-LM accepts a host-installed model file only; model download, integrity, licensing and device admission remain host responsibilities.
- Gemini Nano uses host-declared AppFunctions and rejects dynamic SDK tools. Each AppFunction must independently authorize and validate its invocation; AgentPolicy does not secure framework-dispatched AppFunctions.

## Compatibility policy

Keep the first release line at 0.x. Do not promise binary compatibility until supported vendor versions, Android API levels, runtime/device behavior and API compatibility reports have been reviewed. See [Compatibility](COMPATIBILITY.md) and [Release Checklist](RELEASE_CHECKLIST.md).
