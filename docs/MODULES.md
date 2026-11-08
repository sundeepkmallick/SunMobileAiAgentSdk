# Modules

## Included

| Module | Purpose | Dependencies |
| --- | --- | --- |
| :agent-core | Backend-neutral agent/provider contracts, capability checks, sessions, JSON-schema subset validation, tool registry and host policy executor | Kotlin/JVM, coroutines, kotlinx.serialization |
| :agent-adk | Google ADK runner adapter and dynamic Toolset bridge for core tools | ADK Kotlin Android 1.0.0 |
| :agent-firebase | Firebase AI Logic model factory for the ADK provider | Firebase AI Logic, ADK Firebase integration |
| :agent-litertlm | LiteRT-LM model factory for the ADK provider | ADK LiteRT-LM integration, LiteRT-LM 0.13.1 |
| :agent-gemini-nano | ML Kit Agent API backend using host-declared AppFunctions | ML Kit Agent API alpha1 and GenAI Prompt beta5 |
| :sample-app | Offline Android Research Assistant composition example using a clearly labelled mock backend | Android, :agent-core |

## Backend usage

### ADK-backed Firebase and LiteRT-LM

Build an `AdkAgentBackendProvider` with either `FirebaseAiModelFactory` or `LiteRtLmModelFactory`. Both share ADK's runner, session, streaming event and core Toolset bridge. Firebase initialization and model selection belong to the host. The LiteRT-LM model file must already be installed and verified by the host; this module does not download weights.

### Gemini Nano

`GeminiNanoAgentBackendProvider` uses ML Kit Agent API and enables AppFunctions. It deliberately rejects a non-empty core ToolRegistry: AppFunctions are declared and implemented by the host app, not dynamically registered by this SDK. AppFunctions must enforce their own identity, permission and business authorization; the SDK's AgentPolicy does not intercept framework-dispatched AppFunctions. This provider requires API 36 by default and checks runtime availability.

## Validation status

The modules and integration code are present, but their compilation and runtime/device validation are pending CI and device tests. The source application used ADK 1.0.0, LiteRT-LM 0.13.1, ML Kit Agent API 1.0.0-alpha1 and ML Kit GenAI Prompt 1.0.0-beta5. Keep the source app's dependency graph aligned before consuming these adapters; resolve duplicate ADK/Firebase/LiteRT transitive versions deliberately.

## Host-application code

BOPIS/pickup instructions, inventory/order/store repositories, substitute-product rules, associate approval, pickup response parsing, fallback policy, and AppFunctions exposing those business operations remain in SunCommerzAssociate.
