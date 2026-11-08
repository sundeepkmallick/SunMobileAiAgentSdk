# SunCommerzAssociate migration guide

This document describes a safe host-app migration. **The SunCommerzAssociate repository has not been modified by this SDK branch.** Do not replace the current pickup implementation until the integration tests and rollout gates below pass.

## 1. Add the SDK modules

While the SDK is still a local Git branch, consume it with a Gradle composite build or local checkout. For example, in the host app's settings:

```kotlin
includeBuild("../SunMobileAiAgentSdk")
```

The SDK root assigns group `com.sunmobile.aiagent` and version `0.1.0-SNAPSHOT`, so the composite build can substitute module coordinates automatically. Add the dependencies you need, for example:

```kotlin
dependencies {
    implementation("com.sunmobile.aiagent:agent-core:0.1.0-SNAPSHOT")
    implementation("com.sunmobile.aiagent:agent-adk:0.1.0-SNAPSHOT")
    implementation("com.sunmobile.aiagent:agent-firebase:0.1.0-SNAPSHOT")
}
```

Once the SDK has a reviewed release, replace this local wiring with published Maven coordinates. No artifact has been published by this branch.

Avoid resolving duplicate ADK versions. The adapters currently pin the source app's observed ADK 1.0.0 baseline, LiteRT-LM 0.13.1, ML Kit Agent API 1.0.0-alpha1 and GenAI Prompt 1.0.0-beta5. Compare the resolved dependency graph before enabling the adapters.

## 2. Port application tools

Port inventory, store, order and substitution capabilities as host-owned AgentTool implementations. Keep repositories/use cases and data sources in the host app. Give each tool:
- a stable name and accurate JSON object schema;
- an explicit ToolRiskLevel;
- argument validation at the tool boundary;
- authorization against the signed-in associate and current store/order;
- minimal output and explicit error results.

Use the SDK ToolRegistry and AgentPolicy for ADK-backed agents. The SDK ToolExecutor applies schema validation and policy before calling the tool. Do not allow model-supplied associate IDs, store IDs, permissions, stock quantities, prices or order state to become authoritative.

## 3. Select a backend

### Firebase AI Logic + ADK

Construct an AdkAgentBackendProvider with FirebaseAiModelFactory. Keep Firebase initialization, App Check, user auth and model selection in the host app.

### LiteRT-LM + ADK

Construct an AdkAgentBackendProvider with LiteRtLmModelFactory. Continue to use the host LocalModelCatalog and download manager to verify the model artifact, license, checksum, device requirements and free storage. Call the provider only after the host has verified and installed the selected file. Preserve the source app's release-before-replacement and serialized runtime lifecycle.

### Gemini Nano + AppFunctions

Use GeminiNanoAgentBackendProvider only when API 36+ and AICore reports the feature available. Keep PickupAppFunctionService and its declared AppFunctions in the host app. AppFunctions must independently validate caller identity, permissions, user consent and all business rules. The SDK AgentPolicy does not intercept Android framework AppFunction dispatch. This backend rejects a non-empty SDK ToolRegistry by design.

## 4. Preserve current pickup semantics

- Keep PickupAgentInstructions and all pickup/BOPIS instructions in the host app.
- Keep HybridPickupAgentManager and fallback selection in the host app.
- Preserve the existing stock-verification postcondition. The model saying it checked stock is not evidence; verify that the authoritative inventory tool was actually called and returned current data.
- Preserve associate approval for sensitive/high-impact operations. A ToolAuthorization.RequireApproval response is fail-closed; the host must obtain approval and then re-authorize.
- Preserve the current product substitution and store-radius business rules in host use cases.
- The source Firebase implementation deliberately used a fresh in-memory session service per request. The SDK ADK adapter keeps a session service for the agent lifetime. Use a new unique session ID per pickup attempt or explicitly clear the session when isolation is required.
- The source LiteRT-LM implementation serialized its runtime and released the native engine before model replacement. Test concurrent turns, cancellation, close and replacement against the adapter before removing the existing guard.
- Gemini Nano is single-turn in this adapter; do not assume persistent conversation history.

## 5. Rollout sequence

1. Add SDK dependencies behind a disabled feature flag.
2. Port read-only inventory/store tools and compare results with the current implementation.
3. Run shadow-mode evaluations without allowing model-generated mutations.
4. Add human approval gates and verify denial paths.
5. Test Firebase and LiteRT-LM on the source app's supported devices and model artifacts.
6. Test Gemini Nano/AppFunctions only on supported API 36+ devices with AICore availability.
7. Run regression tests for stock lookup, substitution, quantity/radius, wrong-store access, unavailable model, low memory, model replacement, cancellation, retry, and offline operation.
8. Enable by backend and cohort with rollback to the existing implementation.
9. Remove the old implementation only after production telemetry and business-owner approval.

## 6. Required regression tests

- A request for pickup cannot proceed without an authoritative current-store stock check.
- A tool call for another associate, tenant or unauthorized store is denied.
- A stale or malformed model argument cannot mutate an order.
- Approval-required actions do not run before explicit associate confirmation.
- A retry does not inherit an unrelated order's session state.
- A model switch releases LiteRT-LM resources before replacing or deleting model files.
- Gemini Nano unavailability or API-level mismatch cleanly falls back through the host manager.
- Tool failures, timeouts, cancellation and partial streams do not leave native resources or pending order mutations behind.

Record results in the host app's own test suite and update docs/COMPATIBILITY.md with device/build evidence.
