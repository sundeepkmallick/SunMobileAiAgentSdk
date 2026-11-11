# Modules

## Included modules

| Module | Purpose | Dependencies / boundary |
| --- | --- | --- |
| `:agent-core` | Backend-neutral contracts, capabilities, request/result/events, session facade, tool registry, JSON Schema subset validation and host policy executor | Kotlin/JVM, coroutines, kotlinx.serialization |
| `:agent-adk` | Google ADK runner, in-memory sessions, streaming/event translation and dynamic Toolset bridge for core tools | Google ADK Kotlin Android 1.0.0 |
| `:agent-firebase` | Firebase AI Logic model factory for the shared ADK provider | Host-initialized FirebaseApp; ADK Firebase integration |
| `:agent-litertlm` | LiteRT-LM model factory for an already-installed model file | ADK LiteRT-LM integration and LiteRT-LM 0.13.1 |
| `:agent-mlkit` | First-class ML Kit GenAI Agent + Prompt backend using host-declared AppFunctions | ML Kit Agent API alpha1 / GenAI Prompt beta5; no dynamic SDK tools |
| `:agent-gemini-nano` | Legacy adapter retained for compatibility | Same ML Kit APIs; new consumers should use `:agent-mlkit` |
| `:sample-app` | Offline Android Research Assistant using explicitly labelled mock data | Android and `:agent-core`; no network calls |

## Backend usage

### ADK-backed Firebase

Create a `FirebaseAiModelFactory` with the model name and pass it to `AdkAgentBackendProvider`. Firebase initialization, App Check, authentication, project configuration, billing and model selection belong to the host application.

### ADK-backed LiteRT-LM

Create a `LiteRtLmModelFactory` with the installed model file, cache directory and model name, then pass it to `AdkAgentBackendProvider`. The file must already be installed and verified by the host. This module does not download weights or validate their license, provenance, checksum or device admission. The host must serialize native model lifecycle operations and close the engine before replacing/deleting its file.

### Gemini Nano

`GeminiNanoAgentBackendProvider` in `agent-mlkit` uses ML Kit Agent API and enables AppFunctions. It deliberately rejects a non-empty core ToolRegistry: AppFunctions are declared and implemented by the host app, not dynamically registered by this SDK. Every AppFunction must independently enforce caller identity, permission, consent and business authorization; SDK AgentPolicy does not intercept framework-dispatched AppFunctions. The provider defaults to Android API 36 minimum and checks runtime availability. This backend should not be treated as an interchangeable ADK model or as a durable multi-turn session.

## Host application responsibilities

The SDK does not absorb application-specific behavior. In SunCommerzAssociate, BOPIS instructions, inventory/order/store repositories and use cases, tool implementations, product/store/quantity/radius scoping, fallback selection, approval, pickup response parsing and UI remain in the host. LiteRT-LM model download/catalogue/checksum/device admission remain host-owned.

See [SunCommerzAssociate Migration](SUNCOMMERZ_MIGRATION.md) for the integration example and rollout notes.

## Validation status

On candidate commit `dda83079008701987480abfc819e09c264fa2649`, SDK CI run [#185](https://github.com/sundeepkmallick/SunMobileAiAgentSdk/actions/runs/42823069913) passed and Maven publication verification run [#34](https://github.com/sundeepkmallick/SunMobileAiAgentSdk/actions/runs/42823069812) passed. These checks do not prove physical-device Gemini Nano behavior. SunCommerzAssociate PR #4 remains draft; its user-reported local composite-build app assemble and unit-test invocation completed successfully. See [Compatibility](COMPATIBILITY.md) and [Release Checklist](RELEASE_CHECKLIST.md) for remaining gates.
