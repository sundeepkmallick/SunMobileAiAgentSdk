# Architecture

SunMobileAiAgentSdk provides reusable agent infrastructure. Host applications own their domain, data, tool implementations, identity, authorization decisions and UI. The SDK owns backend-neutral contracts, tool registration, capability reporting, session helpers and optional backend adapters.

## Dependency direction

```text
host application / sample
        |                 |
        +---- agent-core  +---- optional backend adapter
                                  |
                                  +---- agent-core
                                  +---- vendor runtime
```

`:agent-core` is Kotlin/JVM and must not depend on Android, Firebase, Google ADK, LiteRT-LM, ML Kit, AppFunctions, Compose, Hilt or application models.

## Backend boundaries

- **ADK-backed Firebase and LiteRT-LM:** `:agent-firebase` and `:agent-litertlm` provide model factories. `:agent-adk` owns the shared ADK runner, in-memory session service, dynamic Toolset bridge, policy-enforced core-tool execution and event translation.
- **Gemini Nano:** `:agent-gemini-nano` uses the ML Kit Agent API. Android AppFunctions are declared and implemented by the host; this is not an ADK model and does not expose the SDK ToolRegistry. The provider rejects a non-empty core registry by design.
- Providers advertise only capabilities supported by their runtime. Session persistence and multi-turn behavior are backend-specific.

## Tool safety and trust boundaries

Tool registration does not authorize execution. The host supplies an AgentPolicy and remains responsible for user consent, authentication, server-side authorization, business validation and confirmation of irreversible actions. Model output is untrusted input.

The core ToolExecutor validates a deliberately limited JSON Schema subset and applies the host policy before ADK tool execution. Unsupported schema keywords are not security guarantees; tool implementations must validate inputs again. Gemini Nano AppFunctions bypass the dynamic core ToolRegistry, so every host AppFunction must independently authenticate/authorize its caller and validate all arguments. The SDK does not execute arbitrary generated code. Never put secrets in prompts or tool descriptions.

## Lifecycle

- The host owns Agent and calls `close()`.
- ADK sessions use an in-memory session service; they are not durable across process death.
- The host should use unique session IDs where workflow isolation is required.
- Native engines must be closed before model files are replaced or removed. Hosts must serialize unsafe LiteRT-LM load/run/release operations.
- AgentSession serializes calls for a conversation but does not persist to disk.
- Adapters propagate cancellation; verify timeout, partial-stream and error behavior in the consuming app.

## SunCommerzAssociate integration example

The implementation in [SunCommerzAssociate PR #4](https://github.com/sundeepkmallick/SunCommerzAssociate/pull/4) demonstrates the intended boundary:

- The host composes BOPIS inventory/store tools and passes the active product, store, quantity and maximum search radius from authenticated workflow state.
- SDK ToolRegistry/ToolExecutor applies schema validation and a read-only host policy for Firebase and LiteRT-LM ADK-backed execution.
- Host tools reject model-selected scope outside the current order workflow. The host re-reads live inventory and fulfillment options before approval; model text never mutates an order.
- Pickup instructions, repositories/use cases, fallback selection, approval, UI and Gemini Nano AppFunctions remain host-owned.
- LiteRT-LM download provenance, checksum, catalogue admission and model lifecycle synchronization remain host-owned.

PR #4 is still draft. Its latest CI run compiled the app but failed two existing `LocalModelCompatibilityTest` cases; do not describe the host migration as fully validated or released.

## Model catalogue

The LiteRT-LM factory accepts an already-installed readable file and initializes the engine. It does not download weights or prove provenance, license, immutable revision, checksum, device suitability or workflow compatibility. Those checks belong to the host.

## Validation status

SDK CI run [#123](https://github.com/sundeepkmallick/SunMobileAiAgentSdk/actions/runs/42594131318) passed core unit tests and compiled the sample plus all adapter modules. This is compile/test evidence for the CI tasks only. Device execution, vendor-service availability, Firebase credentials/App Check, Gemini Nano AppFunctions, performance, binary/API compatibility and security review remain separate gates.
