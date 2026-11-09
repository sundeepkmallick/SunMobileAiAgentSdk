# Roadmap and extraction status

This branch contains the backend-neutral SDK, four adapter modules, tests, security guidance and an offline sample. The adapter modules compile in SDK CI. Host-app integration has been implemented in a separate draft PR; CI compile/unit-test checks now pass, but production readiness is not claimed.

## Phases

- [x] Phase 1 — Gradle project, Kotlin/JVM core, Android sample, documentation and CI workflow.
- [x] Phase 2 — Tool definitions, registry, risk labels, execution context, fail-closed policy executor and unit tests.
- [x] Phase 3 — Provider SPI, capabilities, request/result/events, builder, session facade and capability checks.
- [x] Phase 4 — ADK, Firebase, LiteRT-LM and Gemini Nano adapter modules and model factories. SDK CI compilation passed; runtime/device validation remains.
- [x] Phase 5 — Offline Research Assistant sample with explicit mock-data disclosure.
- [x] Phase 6 — SunCommerzAssociate host integration implemented in [PR #4](https://github.com/sundeepkmallick/SunCommerzAssociate/pull/4). Host Android CI passed on the latest fix ([run #37](https://github.com/sundeepkmallick/SunCommerzAssociate/actions/runs/42601525623)); the host PR remains draft and device/runtime validation is not complete.
- [ ] Phase 7 — Production readiness: physical-device matrix, Firebase/Gemini Nano runtime verification, security/privacy review, API/binary compatibility, release signing and publication decision.

## Extraction mapping

| Source application component | SDK destination / decision |
| --- | --- |
| PickupAgent abstractions | General Agent contract in `:agent-core`; domain naming removed |
| Pickup ADK factory/runner | Generalized in `:agent-adk`; host retains domain tools and pickup instructions |
| Firebase pickup model setup | `:agent-firebase` model factory using shared ADK lifecycle |
| LiteRT-LM pickup model setup | `:agent-litertlm` model factory; host retains download/integrity and serialized engine lifecycle |
| Gemini Nano pickup path | Separate `:agent-gemini-nano` adapter using host-declared AppFunctions |
| Local model catalogue/download manager | Remains host-owned; optional future model-management artifact only after provenance/integrity design |
| Hybrid fallback manager | Remains host-owned; fallback is application/product-specific |
| Pickup instructions, AppFunctions, repositories, ViewModels and UI | Remain in host application |

## Integration design demonstrated by PR #4

- SDK is consumed as a Git submodule with a pinned commit and included through Gradle composite build; no Maven publication is required.
- Firebase and LiteRT-LM use the SDK's ADK runner, ToolRegistry, schema validation and ToolExecutor.
- BOPIS tools are host-owned and composed per active order item. Product, current store, requested quantity and maximum radius are fixed from host workflow state rather than trusted from model input.
- The tools are read-only. The host re-reads live inventory/fulfillment options before showing proposals; associate approval and authoritative order mutation remain host responsibilities.
- Gemini Nano remains on the host ML Kit Agent API + AppFunctions path because AppFunctions are host declarations and are not intercepted by the SDK's dynamic ToolRegistry.
- LiteRT-LM artifact provenance, license, SHA-256, device admission, installation and model replacement synchronization remain host responsibilities.

## CI evidence and remaining gates

- SDK `main` commit `2eb3d6db4759cc855f5853e6a0962d698c1a8647` passed [CI run #137](https://github.com/sundeepkmallick/SunMobileAiAgentSdk/actions/runs/42604253210): core unit tests and sample plus all adapter module builds succeeded.
- SunCommerzAssociate PR #4 head commit `784fa34033bbe081988568bebe9a337e676b8f22` passed [host CI run #37](https://github.com/sundeepkmallick/SunCommerzAssociate/actions/runs/42601525623): app compilation and unit tests succeeded.
- The SDK release candidate must pass its own PR checks before approval. Neither CI result establishes physical-device behavior, production readiness or independent security/privacy review.

Before stable release:
- Test generic tool calls, streaming, cancellation, timeouts, sessions, close/release, concurrency and model replacement on supported devices.
- Test Firebase with real host initialization, App Check/auth and non-production credentials.
- Test Gemini Nano on eligible devices/API levels, including AppFunctions permission and service behavior.
- Verify model licenses, immutable revisions, SHA-256, device compatibility and download/install integrity.
- Complete security, privacy, API/binary compatibility and release reviews. Record evidence in [COMPATIBILITY.md](COMPATIBILITY.md), [SECURITY.md](SECURITY.md) and [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md).
