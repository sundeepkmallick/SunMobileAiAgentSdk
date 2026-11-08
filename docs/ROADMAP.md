# Roadmap and extraction status

This branch establishes a backend-neutral foundation and adds tests/security guidance. It does **not** claim production completion of vendor adapters or host-app migration.

- [x] Phase 1 — Gradle project, Kotlin/JVM core, Android sample, docs, CI workflow.
- [x] Phase 2 — Tool definitions, registry, risk labels, execution context, fail-closed policy executor, unit tests.
- [x] Phase 3 — Provider SPI, capabilities, request/result/events, builder, session facade and capability checks.
- [ ] Phase 4 — ADK, Firebase, LiteRT-LM and Gemini Nano adapters. Vendor-specific modules remain unimplemented pending a verified bridge and compatibility test matrix.
- [x] Phase 5 — Offline Research Assistant sample with explicit mock-data disclosure.
- [ ] Phase 6 — SunCommerzAssociate migration. The source application is unchanged; migration must follow backend integration and regression tests.
- [ ] Phase 7 — Production readiness. CI, security guidance, compatibility matrix and release checklist are present; device matrix, security review, API compatibility and release verification remain.

## Extraction mapping

| Source component | SDK destination / decision |
| --- | --- |
| PickupAgent.kt | General Agent contract in :agent-core; remove domain naming |
| PickupAdkAgentFactory.kt | Generalize into :agent-adk; remove inventory/store/substitute tools and pickup instructions |
| FirebasePickupAgent.kt | :agent-firebase via shared ADK lifecycle |
| LiteRtLmPickupAgent.kt | :agent-litertlm; preserve engine synchronization and release-before-replacement |
| GeminiNanoPickupAgent.kt | :agent-gemini-nano only after confirming the current ML Kit Agent API feature set and host AppFunctions boundary |
| LocalModelCatalog.kt | Separate runtime metadata from host workflow validation |
| LiteRtLmModelDownloadManager.kt | Evaluate later as optional model-management artifact; require verified checksum, provenance, license and atomic install |
| HybridPickupAgentManager.kt | Keep in host app; fallback is product-specific |
| PickupAgentInstructions.kt, AppFunctions, repositories, ViewModels, UI | Keep in host app |

## Current evidence and adapter constraints

The source app's tested baseline was ADK Kotlin 1.0.0, LiteRT-LM 0.13.1, ML Kit Agent API 1.0.0-alpha1, ML Kit GenAI Prompt 1.0.0-beta5 and Android API 36 for its Gemini Nano AppFunctions service. Do not silently upgrade those dependencies or copy the app's tool assumptions into core.

The current ADK Kotlin documentation describes Firebase AI and LiteRT-LM as ADK model backends with tool support. The ML Kit / Gemini Nano ADK integration is documented as prompt/generation only and currently does not support tool calling; the app's separate Agent API + AppFunctions route therefore remains host-specific and must be checked against the exact ML Kit Agent API artifact used by the app.

## Gate before phase 4 is complete

- Compile adapters against pinned artifacts and check dependency conflicts with host app versions.
- Test generic tool calls and streaming, cancellation, timeout, session semantics, close/release, concurrent requests and model replacement.
- Test Gemini Nano on eligible devices/API levels, including AppFunctions permission and service behavior.
- Test Firebase with host Firebase initialization and App Check/auth configuration, without committing credentials.
- Verify model licenses, immutable revisions, SHA-256, device compatibility and download/install integrity.
- Record results in docs/COMPATIBILITY.md. See docs/SECURITY.md and docs/RELEASE_CHECKLIST.md.
