# SunMobileAiAgentSdk

A reusable Kotlin/Android agent SDK that separates backend-neutral agent infrastructure from host-application business logic.

> **Status: 0.1.0-rc.2 review candidate (not a stable release).** The core API, ADK/Firebase/LiteRT-LM/Gemini Nano adapters, offline sample and CI are implemented. SDK CI on `main` passed core unit tests and compiles the sample plus all adapter modules ([run #137](https://github.com/sundeepkmallick/SunMobileAiAgentSdk/actions/runs/42604253210)). SunCommerzAssociate integration CI passed after its unit-test fix ([host run #37](https://github.com/sundeepkmallick/SunCommerzAssociate/actions/runs/42601525623)); its PR remains draft. Physical-device/runtime validation, independent security and privacy reviews, API/binary compatibility review, model provenance/license verification and stable-release qualification remain outstanding.

## Modules

- `:agent-core`: backend-neutral contracts, capabilities, session facade, tool registry, JSON-schema subset validation and fail-closed host policy executor. Kotlin/JVM.
- `:agent-adk`: Google ADK runner, in-memory session service, streaming/event translation and dynamic bridge from the core registry to ADK tools.
- `:agent-firebase`: Firebase AI Logic model factory for the shared ADK adapter.
- `:agent-litertlm`: LiteRT-LM model factory for a host-installed model file.
- `:agent-mlkit`: first-class ML Kit GenAI Agent + Prompt backend using host-declared Android AppFunctions; dynamic SDK tools are intentionally unsupported.
- `:agent-gemini-nano`: legacy ML Kit adapter retained for compatibility; new integrations should use `:agent-mlkit`.
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

The integration is implemented in [SunCommerzAssociate PR #4](https://github.com/sundeepkmallick/SunCommerzAssociate/pull/4), which remains a draft. Its latest observed Android CI run passed app compilation and unit tests ([run #37](https://github.com/sundeepkmallick/SunCommerzAssociate/actions/runs/42601525623)). The host app consumes the SDK as a Git submodule plus Gradle composite build; no Maven publication is required.

- Firebase AI Logic and LiteRT-LM use the SDK's ADK runner, registry, schema validation and policy executor.
- BOPIS tools, instructions, repositories, identity/order scoping, fallback, approval and UI remain host-owned.
- The host supplies product, store, quantity and radius scope; tools are read-only and the host revalidates current inventory before approval.
- Gemini Nano intentionally remains on the host ML Kit Agent API + AppFunctions implementation. AppFunctions perform their own authorization and are not intercepted by SDK AgentPolicy.
- The host retains model download/catalogue/checksum/device admission and serializes LiteRT-LM model lifecycle.

A green host CI run proves compile/unit-test success only. Device validation, Firebase credential/App Check runtime tests, Gemini Nano/AppFunctions behavior and production rollout review remain outstanding. See the [migration guide](docs/SUNCOMMERZ_MIGRATION.md) and [roadmap](docs/ROADMAP.md).

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
./gradlew :sample-app:assembleDebug :agent-adk:assembleDebug :agent-firebase:assembleDebug :agent-litertlm:assembleDebug :agent-mlkit:assembleDebug :agent-gemini-nano:assembleDebug
```

The latest successful SDK workflow on `main` is [CI run #137](https://github.com/sundeepkmallick/SunMobileAiAgentSdk/actions/runs/42604253210). It passed core unit tests and compiled the sample plus all adapter modules. The candidate branch has its own PR CI gate; see the PR checks before approving it. This evidence does not establish device/runtime behavior, production readiness or full host-app regression success.

## Maven publication candidate

All five SDK modules have a local-only Maven publication setup for `0.1.0-rc.1` under group `io.github.sundeepkmallick`. The namespace is verified in Sonatype Central. The current build still publishes to `mavenLocal()` only; remote publishing configuration and credentials are not yet enabled, and no stable `0.1.0` tag is configured. See [Maven publication guide](docs/MAVEN_PUBLICATION.md) for artifact coordinates, signing configuration, local verification and an independent consumer fixture.

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
- [Maven Publication](docs/MAVEN_PUBLICATION.md)

## License

Apache License 2.0. See [LICENSE](LICENSE).
