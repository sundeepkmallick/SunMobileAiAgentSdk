# SunMobileAiAgentSdk

A reusable Kotlin/Android agent SDK foundation designed to keep agent infrastructure separate from host-application business logic.

> **Status: 0.x foundation / experimental.** The core API, ADK/Firebase/LiteRT-LM/Gemini Nano adapter modules, offline Android sample, documentation, and CI workflows are present. Backend compilation and device-level validation are pending.

## Modules

- `:agent-core`: backend-neutral agent, capability, request/result, session, tool registry, and policy contracts. Pure Kotlin/JVM.
- `:agent-adk`: ADK runner, in-memory session and dynamic core-tool bridge.
- `:agent-firebase`: Firebase AI Logic model factory.
- `:agent-litertlm`: LiteRT-LM model factory for host-installed model files.
- `:agent-gemini-nano`: ML Kit Agent API adapter using host-declared AppFunctions (no dynamic SDK tools).
- `:sample-app`: offline Android Research Assistant composition example using a mock backend and canned notes. No network calls are made.

See [Architecture](docs/ARCHITECTURE.md), [Modules](docs/MODULES.md), [Public API](docs/PUBLIC_API.md), [Tool Development](docs/TOOL_DEVELOPMENT.md), and [Roadmap](docs/ROADMAP.md).

## Quick start

Choose an optional adapter module and provide its model/runtime configuration:

```kotlin
val agent = AgentBuilder()
    .configuration(AgentConfiguration("ResearchAssistant", "Use the registered tools."))
    .backend(myBackendProvider)
    .tools(myToolRegistry)
    .policy(myApplicationPolicy)
    .build()

try {
    val result = agent.execute(AgentRequest("Summarize the documents"))
    println(result.text)
} finally {
    agent.close()
}
```

See [Backend Modules](docs/MODULES.md) for Firebase, LiteRT-LM and Gemini Nano setup notes and [SunCommerzAssociate Migration](docs/SUNCOMMERZ_MIGRATION.md) for the host-app rollout plan. Gemini Nano AppFunctions are implemented and authorized by the host app. The LiteRT-LM adapter accepts a preinstalled model file and does not download model weights.

## Design principles

1. Core does not depend on Android or a model vendor.
2. Host applications own domain tools, credentials, authorization, business rules, and UI.
3. Tool registration is not permission. The host supplies an explicit policy.
4. Backends advertise only capabilities they actually support.
5. Model output is untrusted input; validate tool arguments and consequential actions.
6. Sessions do not imply persistence across process restarts.
7. No model downloads, remote calls, or publication occur implicitly.

## Build

Requires JDK 17 and an Android SDK for the sample.

```bash
./gradlew :agent-core:test
./gradlew :sample-app:assembleDebug :agent-adk:assembleDebug :agent-firebase:assembleDebug :agent-litertlm:assembleDebug :agent-gemini-nano:assembleDebug
```

## Source application

The extraction assessment used [SunCommerzAssociate](https://github.com/sundeepkmallick/SunCommerzAssociate). BOPIS/pickup workflows, inventory/order/store tools, app-specific instructions, approvals, and AppFunctions remain in that application. See the roadmap for extraction mapping and remaining gates. The source application has not yet been migrated.

## License

Apache License 2.0. See [LICENSE](LICENSE).
