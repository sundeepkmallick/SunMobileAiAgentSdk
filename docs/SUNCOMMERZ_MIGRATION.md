# SunCommerzAssociate integration and migration guide

The integration described here is implemented on [SunCommerzAssociate PR #4](https://github.com/sundeepkmallick/SunCommerzAssociate/pull/4), which remains draft. The user-reported local composite-build `:app:assembleDebug :app:testDebugUnitTest` invocation completed successfully. This guide records the integration architecture and remaining validation steps; it is not a production-readiness claim.

## 1. SDK wiring

The host app can consume the SDK through its pinned Git submodule and Gradle composite build during development. After the RC2 artifacts are published and independently resolvable from Maven Central, the host may use the Maven coordinates. Do not switch to the Maven source until that verification is complete.

Clone the application with submodules:

```bash
git clone --recurse-submodules https://github.com/sundeepkmallick/SunCommerzAssociate.git
cd SunCommerzAssociate
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

For an existing checkout:

```bash
git submodule update --init --recursive
```

The host settings support `sunAiSdkSource=includeBuild` with explicit module substitutions for the six SDK artifacts. For a local sibling checkout, configure `sunAiSdkSource=includeBuild` and `sunAiSdkDir=../SunMobileAiAgentSdk` in the app's local Gradle properties, or pass both as `-P` arguments. Keep machine-specific paths uncommitted. The SDK Maven path is a separate mode and requires the published coordinates.

## 2. Module responsibilities

The host declares `agent-core`, `agent-adk`, `agent-firebase`, `agent-litertlm` and `agent-mlkit` as SDK dependencies.

- `:agent-core` supplies the backend-neutral contracts, tool registry, schema validation and policy executor.
- `:agent-adk` supplies the shared Google ADK runner, in-memory session and dynamic core-tool bridge.
- `:agent-firebase` creates a Firebase AI Logic model from the host's initialized FirebaseApp.
- `:agent-litertlm` creates a LiteRT-LM model from an already-installed file.

Gemini Nano is provided through `agent-mlkit` (`GeminiNanoAgentBackendProvider` and `GeminiNanoConfiguration`). The host continues to own AppFunctions declarations, implementation, metadata, manifest registration and KSP processing. Framework-dispatched AppFunctions do not pass through SDK `AgentPolicy`; every host function must independently validate caller identity, permission, arguments and business authorization. The SDK rejects dynamic `ToolRegistry` tools for this backend.

## 3. Host-owned BOPIS tools and request scoping

The host composes read-only tools per active order item. It passes the current product ID, current store ID, requested quantity and maximum nearby-store radius from the active workflow. Tool implementations validate model arguments against those host-owned values.

The integration's safeguards include:
- stock lookup is restricted to the current order product;
- store lookup is restricted to the current store or nearby stores discovered by the host's stock-filtered nearby-store use case;
- requested quantity and radius cannot be expanded by model arguments;
- the model cannot select an associate, order, tenant or arbitrary store scope;
- the SDK policy permits only read-only tool risk levels in this composition;
- the host re-reads live inventory and fulfillment options before presenting/approving a proposal.

Model output is a proposal, not an order mutation. Associate approval and the authoritative order update remain in the host application.

## 4. LiteRT-LM lifecycle and model management

The host's download manager and local model catalogue remain responsible for artifact provenance, license, immutable revision, file size, SHA-256, installation and device/workflow admission. The SDK factory accepts a preinstalled file and does not download or verify model weights.

The host serializes SDK LiteRT-LM execution and model maintenance so an in-flight turn closes its native engine before a model file is replaced or removed. Keep this lifecycle guard until device tests prove a safer alternative.

## 5. CI and current validation

SDK candidate CI passed on commit `dda83079008701987480abfc819e09c264fa2649` ([run #185](https://github.com/sundeepkmallick/SunMobileAiAgentSdk/actions/runs/42823069913)); the Maven-local publication verification workflow also passed ([run #34](https://github.com/sundeepkmallick/SunMobileAiAgentSdk/actions/runs/42823069812)).

The host PR remains draft. The user's latest local Gradle invocation for app assemble and unit tests completed successfully using the SDK composite build. The placeholder Firebase configuration in CI is compile-only and cannot validate real Firebase requests.

Neither CI workflow establishes physical-device behavior, Firebase credentials/App Check, Gemini Nano availability, AppFunctions authorization, model performance or production readiness.

## 6. Required follow-up validation

Before enabling the integration for associates:
1. Obtain a fresh green host PR workflow on the latest PR head.
2. Run Firebase and LiteRT-LM on supported devices with the real host configuration.
3. Test Gemini Nano on eligible API 36+ devices, including AICore availability and AppFunctions authorization.
4. Exercise wrong-product/store scope, quantity/radius boundaries, unavailable models, low memory, model replacement, cancellation, retry and offline behavior.
5. Verify approval and authoritative inventory/order revalidation remain mandatory.
6. Roll out behind the host feature flag with telemetry and rollback to the existing implementation.

Do not remove legacy pickup classes until device tests, regression coverage and rollout telemetry justify doing so.
