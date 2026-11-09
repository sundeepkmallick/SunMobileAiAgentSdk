# Modules

## Included modules

| Module | Purpose | Dependencies / boundary |
| --- | --- | --- |
| `:agent-core` | Backend-neutral contracts, capabilities, request/result/events, session facade, tool registry, JSON Schema subset validation and host policy executor | Kotlin/JVM, coroutines, kotlinx.serialization |
| `:agent-adk` | Google ADK runner, in-memory sessions, streaming/event translation and dynamic Toolset bridge for core tools | Google ADK Kotlin Android 1.0.0 |
| `:agent-firebase` | Firebase AI Logic model factory for the shared ADK provider | Host-initialized FirebaseApp; ADK Firebase integration |
| `:agent-litertlm` | LiteRT-LM model factory for an already-installed model file | ADK LiteRT-LM integration and LiteRT-LM 0.13.1 |
| `:agent-gemini-nano` | ML Kit Agent API backend using host-declared AppFunctions | ML Kit Agent API alpha1 / GenAI Prompt beta5; no dynamic SDK tools |
| `:sample-app` | Offline Android Research Assistant using explicitly labelled mock data | Android and `:agent-core`; no network calls |

## Backend usage

### ADK-backed Firebase

Create a `FirebaseAiModelFactory` with the model name and pass it to `AdkAgentBackendProvider`. Firebase initialization, App Check, authentication, project configuration, billing and model selection belong to the host application.

### ADK-backed LiteRT-LM

Create a `LiteRtLmModelFactory` with the installed model file, cache directory and model name, then pass it to `AdkAgentBackendProvider`. The file must already be installed and verified by the host. This module does not download weights or validate their license, provenance, checksum or device admission. The host must serialize native model lifecycle operations and close the engine before replacing/deleting its file.

### Gemini Nano

`GeminiNanoAgentBackendProvider` uses ML Kit Agent API and enables AppFunctions. It deliberately rejects a non-empty core ToolRegistry: AppFunctions are declared and implemented by the host app, not dynamically registered by this SDK. Every AppFunction must independently enforce caller identity, permission, consent and business authorization; SDK AgentPolicy does not intercept framework-dispatched AppFunctions. The provider defaults to Android API 36 minimum and checks runtime availability. This backend should not be treated as an interchangeable ADK model or as a durable multi-turn session.

## Host application responsibilities

The SDK does not absorb application-specific behavior. In SunCommerzAssociate, BOPIS instructions, inventory/order/store repositories and use cases, tool implementations, product/store/quantity/radius scoping, fallback selection, approval, pickup response parsing and UI remain in the host. LiteRT-LM model download/catalogue/checksum/device admission remain host-owned.

See [SunCommerzAssociate Migration](SUNCOMMERZ_MIGRATION.md) for the integration example and rollout notes.

## Validation status

SDK CI run [#123](https://github.com/sundeepkmallick/SunMobileAiAgentSdk/actions/runs/42594131318) passed core unit tests and compiled the sample and all adapter modules. This is not physical-device or production validation. The host integration PR #4 remains draft and its latest Android CI run failed two `LocalModelCompatibilityTest` cases. See [Compatibility](COMPATIBILITY.md) for the evidence and outstanding gates.
