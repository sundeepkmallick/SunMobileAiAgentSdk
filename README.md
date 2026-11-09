# SunMobileAiAgentSdk

A reusable Kotlin/Android agent SDK that separates backend-neutral agent infrastructure from host-application business logic.

> **Status: 0.x / experimental.** The core API, ADK/Firebase/LiteRT-LM/Gemini Nano adapters, offline sample and CI are implemented. The SDK CI currently passes core unit tests and compiles the sample plus all adapter modules. SunCommerzAssociate integration has been implemented on its dedicated PR branch; that host PR remains draft and its latest Android unit-test workflow is not green. Device validation, security review and release qualification remain outstanding.

## Modules

- `:agent-core`: backend-neutral contracts, capabilities, session facade, tool registry, JSON-schema subset validation and fail-closed host policy executor. Kotlin/JVM.
- `:agent-adk`: Google ADK runner, in-memory session service, streaming/event translation and dynamic bridge from the core registry to ADK tools.
- `:agent-firebase`: Firebase AI Logic model factory for the shared ADK adapter.
- `:agent-litertlm`: LiteRT-LM model factory for a host-installed model file.
- `:agent-gemini-nano`: ML Kit Agent API adapter using host-declared Android AppFunctions; dynamic SDK tools are intentionally unsupported.
- `:sample-app`: offline Android Research Assistant using explicitly labelled mock data; it makes no network calls.

## Quick start

Add the SDK modules you need and provide a backend/model factory, host tools and an explicit policy:

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

See [Modules](docs/MODULES.md), [Public API](docs/PUBLIC_API.md) and [Tool Development](docs/TOOL_DEVELOPMENT.md) for backend-specific setup and usage.

## SunCommerzAssociate integration

The integration is implemented in [SunCommerzAssociate PR #4](https://github.com/sundeepkmallick/SunCommerzAssociate/pull/4), which is currently a draft. The host app consumes the SDK as a Git submodule and Gradle composite build; no Maven publication is required.

- Firebase AI Logic and LiteRT-LM use the SDK's ADK runner, registry, schema validation and policy executor.
- BOPIS tools, instructions, repositories, identity/order scoping, fallback, approval and UI remain host-owned.
- The host supplies product, store, quantity and radius scope; tools are read-only and the host revalidates current inventory before approval.
- Gemini Nano intentionally remains on the host ML Kit Agent API + AppFunctions implementation. AppFunctions perform their own authorization and are not intercepted by SDK AgentPolicy.
- The host retains model download/catalogue/checksum/device admission and serializes LiteRT-LM model lifecycle.

The host PR is not yet production-ready: its latest Android CI run compiled the app but failed two unit tests in `LocalModelCompatibilityTest`. See [the migration guide](docs/SUNCOMMERZ_MIGRATION.md) and [roadmap](docs/ROADMAP.md).

## Design and safety principles

1. Core has no Android or vendor-runtime dependency.
2. Hosts own domain logic, credentials, identity, authorization, business rules and UI.
3. Tool registration is not permission; provide an explicit policy. The default policy denies tool calls.
4. Model output and tool arguments are untrusted; validate at execution and re-check authoritative state before side effects.
5. Sessions are not durable across process death.
6. Gemini Nano AppFunctions are host-declared and must independently enforce security.
7. No model downloads, remote calls, publication or order mutations happen implicitly.

## Build and test

Requires JDK 17; an Android SDK is required for Android modules and the sample.

```bash
./gradlew :agent-core:test
./gradlew :sample-app:assembleDebug :agent-adk:assembleDebug :agent-firebase:assembleDebug :agent-litertlm:assembleDebug :agent-gemini-nano:assembleDebug
```

CI run [#123](https://github.com/sundeepkmallick/SunMobileAiAgentSdk/actions/runs/42594131318) passed both `core-tests` and `sample-build`, including compilation of all four adapter modules. This does not establish device/runtime behavior, production readiness or host-app regression success.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Modules](docs/MODULES.md)
- [Public API](docs/PUBLIC_API.md)
- [Tool Development](docs/TOOL_DEVELOPMENT.md)
- [Security](docs/SECURITY.md)
- [Compatibility and Validation](docs/COMPATIBILITY.md)
- [SunCommerzAssociate Migration](docs/SUNCOMMERZ_MIGRATION.md)
- [Roadmap](docs/ROADMAP.md)
- [Release Checklist](docs/RELEASE_CHECKLIST.md)

## License

Apache License 2.0. See [LICENSE](LICENSE).
