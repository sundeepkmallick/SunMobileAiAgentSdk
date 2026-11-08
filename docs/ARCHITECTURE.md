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

- Firebase Gemini and LiteRT-LM can share an ADK integration layer: the source app uses ADK model, agent, runner, session-service, and event abstractions for both.
- Gemini Nano is separate: ML Kit Agent API runs the agent while AppFunctions are declared and implemented by the host application. It is not an interchangeable ADK model.
- Providers advertise only capabilities actually supported by the chosen model/runtime.
- Sessions are not a promise of durable persistence. Firebase, LiteRT-LM, and Gemini Nano have different lifecycle and context constraints.

## Tool safety

Tool registration does not authorize execution. The host supplies an AgentPolicy and remains responsible for user consent, authentication, server-side authorization, business validation, and confirmation of irreversible actions. Model output is untrusted input. Never treat a model's proposal as permission to transact.

Adapters must validate arguments against supported schema features and reject unsupported constructs. The SDK does not execute arbitrary generated code. Secrets must not be placed in prompts or tool descriptions.

## Lifecycle

- The host owns Agent and calls close().
- A backend owns native engines and serializes unsafe load/run/release/model-replacement operations.
- AgentSession serializes requests for a conversation but does not persist to disk.
- Android lifecycle-bound resources must not enter the JVM core.
- Adapters propagate cancellation and enforce sensible timeouts.

## Model catalogue

No generic model-management artifact is included in this foundation. Model URLs, licenses, checksums, device admission rules, and host-app workflow validation are distinct facts. Runtime support or downloadability does not prove suitability for a host application's tools.
