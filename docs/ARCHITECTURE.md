# Architecture

SunMobileAiAgentSdk is reusable agent infrastructure. Host applications own their domain, data, tool implementations, authorization decisions, and UI. The SDK owns backend-neutral contracts, tool registration, capability reporting, session helpers, and optional adapters.

## Dependency direction

```text
host application / sample
        |                 |
        +---- agent-core  +---- optional backend adapter
                                  |
                                  +---- agent-core
                                  +---- vendor runtime
```

`:agent-core` is Kotlin/JVM and must not depend on Android, Firebase, Google ADK, LiteRT-LM, ML Kit, AppFunctions, Compose, Hilt, or application models.

## Backend boundaries

- Firebase Gemini and LiteRT-LM share :agent-adk. :agent-firebase and :agent-litertlm provide vendor model factories, while the shared adapter owns the ADK runner, session service, dynamic Toolset bridge, and event translation.
- Gemini Nano is separate in :agent-gemini-nano: ML Kit Agent API runs the agent while AppFunctions are declared and implemented by the host application. It is not an interchangeable ADK model and does not expose the SDK ToolRegistry.
- Providers advertise only capabilities actually supported by the chosen model/runtime.
- Sessions are not a promise of durable persistence. Firebase, LiteRT-LM, and Gemini Nano have different lifecycle and context constraints.

## Tool safety

Tool registration does not authorize execution. The host supplies an AgentPolicy and remains responsible for user consent, authentication, server-side authorization, business validation, and confirmation of irreversible actions. Model output is untrusted input. Never treat a model's proposal as permission to transact.

The core ToolExecutor validates a deliberately limited JSON Schema subset and applies the host AgentPolicy before ADK tool execution. Unsupported schema keywords are not interpreted as guarantees; tool implementations must still validate their inputs. The Gemini Nano AppFunctions route bypasses the dynamic core ToolRegistry, so each host AppFunction must independently authorize its caller and validate all arguments. The SDK does not execute arbitrary generated code. Secrets must not be placed in prompts or tool descriptions.

## Lifecycle

- The host owns Agent and calls close().
- A backend owns native engines and serializes unsafe load/run/release/model-replacement operations.
- AgentSession serializes requests for a conversation but does not persist to disk.
- Android lifecycle-bound resources must not enter the JVM core.
- Adapters propagate cancellation and enforce sensible timeouts.

## Model catalogue

The LiteRT-LM model factory accepts an already-installed file and initializes the engine; it does not download or verify model weights. Model URLs, licenses, immutable revisions, checksums, device admission rules, and host-app workflow validation are distinct facts. Runtime support or downloadability does not prove suitability for a host application's tools.


## Validation status

The adapter modules are present, but CI compilation and device-level execution are not yet verified. The source app baseline and backend constraints are recorded in docs/COMPATIBILITY.md. Do not treat an adapter as production-ready until its pinned dependency graph and runtime/device test matrix pass.
