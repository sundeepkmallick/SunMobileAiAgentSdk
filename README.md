# SunMobileAiAgentSdk

A reusable Kotlin/Android agent SDK foundation designed to keep agent infrastructure separate from host-application business logic.

> **Status: 0.x foundation / experimental.** The core API, tool registry, offline Android sample, documentation, and CI definition are present. Firebase, LiteRT-LM, and Gemini Nano backend adapters are not shipped in this branch.

## Modules

- `:agent-core`: backend-neutral agent, capability, request/result, session, tool registry, and policy contracts. Pure Kotlin/JVM.
- `:sample-app`: offline Android Research Assistant composition example using a mock backend and canned notes. No network calls are made.

See [Architecture](docs/ARCHITECTURE.md), [Modules](docs/MODULES.md), [Public API](docs/PUBLIC_API.md), [Tool Development](docs/TOOL_DEVELOPMENT.md), and [Roadmap](docs/ROADMAP.md).

## Quick start

A backend provider must be supplied by an optional adapter module or host integration:

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
./gradlew :sample-app:assembleDebug
```

## Source application

The extraction assessment used [SunCommerzAssociate](https://github.com/sundeepkmallick/SunCommerzAssociate). BOPIS/pickup workflows, inventory/order/store tools, app-specific instructions, approvals, and AppFunctions remain in that application. See the roadmap for extraction mapping and remaining gates.

## License

Apache License 2.0. See [LICENSE](LICENSE).
