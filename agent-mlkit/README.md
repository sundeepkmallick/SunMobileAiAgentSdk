# agent-mlkit

Gemini Nano backend for SunMobileAiAgentSdk, implemented with Google's ML Kit GenAI Agent and Prompt APIs.

## Dependency

```kotlin
dependencies {
    implementation("io.github.sundeepkmallick:agent-mlkit:<published-version>")
}
```

The artifact depends on `agent-core`; ML Kit Agent and Prompt implementation dependencies are internal and are not exposed in the public API.

## Usage

```kotlin
val provider = GeminiNanoAgentBackendProvider(
    context = applicationContext,
    configuration = GeminiNanoConfiguration(
        agentName = "PickupAssistant",
        instructions = "Follow the host application's pickup policy.",
        maxTurns = 10,
        enableAppFunctions = true,
    ),
)

when (val readiness = provider.checkAvailability()) {
    GeminiNanoAvailability.Available -> Unit
    is GeminiNanoAvailability.ModelNotReady -> {
        // Show a not-ready state; this API does not download the model.
    }
    is GeminiNanoAvailability.UnsupportedAndroidVersion -> {
        // Select another backend.
    }
    is GeminiNanoAvailability.StatusCheckFailed -> {
        // Log/report readiness diagnostics and select a fallback if appropriate.
    }
}

val agent = AgentBuilder()
    .configuration(AgentConfiguration("PickupAssistant", "Host-owned pickup instructions."))
    .backend(provider)
    .build()

try {
    val result = agent.execute(AgentRequest("Check the active pickup order."))
    println(result.text)
} finally {
    agent.close()
}
```

Consumers do not import ML Kit `Agent`, `AgentRunner`, `Generation` or `Content` types.

## Device and model requirements

- Android API 36 or later by default. The configured minimum API can be raised for a product's rollout policy, but cannot be set below the library's Android minSdk.
- A device/runtime with the Gemini Nano feature provisioned and reported as available by ML Kit.
- The app must use compatible versions of ML Kit GenAI Agent and Prompt APIs.
- Availability is device- and runtime-dependent. An Android version check alone does not imply that Gemini Nano is present.

## Model readiness and downloads

`checkAvailability()` calls the ML Kit Prompt API status check and closes its client. It reports available, not-ready, unsupported-Android and status-check-failed states. It does not initiate a model download. The selected ML Kit API versions expose readiness status here; this module does not promise a general download/progress API.

## AppFunctions boundary

The host app owns AppFunction declarations, service implementation, metadata, manifest registration and KSP/compiler processing. Enable AppFunctions in the Gemini Nano configuration when the host has registered its declarations.

AppFunctions are dispatched by Android's AppFunctions framework, not through the SDK's dynamic `ToolRegistry` or `AgentPolicy`. Consequently:

- The provider rejects non-empty dynamic SDK tool registries.
- Every host AppFunction must enforce caller identity, access level, argument validation and business authorization itself.
- Re-check authoritative business state inside the AppFunction before returning sensitive data or performing an operation.
- Keep domain-specific services (for example inventory, pickup and order services) in the host application.

## Streaming, cancellation and lifecycle

The backend maps ML Kit text messages to core `AgentEvent.TextDelta` events and function calls to `ToolStarted` events. Streaming is incremental where ML Kit emits partial text events. Coroutine cancellation is rethrown; the AgentRunner is closed in a `finally` block. Close the core Agent when the host no longer needs it.

This backend is a single-turn Agent API adapter. It does not advertise durable multi-turn session storage or dynamic SDK tool calling.

## Errors

`GeminiNanoException.code` distinguishes unsupported devices, model-not-ready states, initialization failures and inference failures. Readiness checks return `GeminiNanoAvailability` rather than throwing for normal unavailable states.

## Testing

The JVM unit tests cover configuration validation. Real ML Kit readiness, model inference, streaming, AppFunctions discovery/dispatch and device compatibility require instrumentation or manual testing on supported physical devices with Gemini Nano provisioned. JVM tests do not establish physical-device compatibility.
