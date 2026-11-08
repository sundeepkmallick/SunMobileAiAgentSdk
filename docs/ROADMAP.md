# Roadmap and extraction status

The original SDK repository contained only a README and Apache-2.0 license. This branch adds the initial project foundation.

- [x] Phase 1 — Gradle project, Kotlin/JVM core, Android sample, docs, CI.
- [x] Phase 2 — Tool definitions, registry, execution context/result, risk labels, authorization SPI.
- [x] Phase 3 — Provider SPI, capabilities, request/result/events, builder, session helper.
- [ ] Phase 4 — ADK, Firebase, LiteRT-LM, Gemini Nano adapters. Requires exact vendor API compilation and runtime tests.
- [x] Phase 5 — Offline Research Assistant sample with explicit mock-data disclosure.
- [ ] Phase 6 — SunCommerzAssociate migration after adapter tests; source app was not modified.
- [ ] Phase 7 — Device matrix, security review, compatibility, release signing, publishing, integration tests.

## Extraction mapping

| Source component | SDK destination |
| --- | --- |
| PickupAgent.kt | Rewrite as Agent in :agent-core; remove domain naming |
| PickupAdkAgentFactory.kt | Generalize into :agent-adk; remove inventory/store/substitute tools and pickup instructions |
| FirebasePickupAgent.kt | :agent-firebase using shared ADK lifecycle |
| LiteRtLmPickupAgent.kt | :agent-litertlm; preserve engine synchronization and release-before-replacement |
| GeminiNanoPickupAgent.kt | :agent-gemini-nano; preserve AICore availability and AppFunctions constraints |
| LocalModelCatalog.kt | Separate generic runtime metadata from host workflow validation |
| LiteRtLmModelDownloadManager.kt | Evaluate later as optional model-management artifact |
| HybridPickupAgentManager.kt | Keep in host app; fallback is product-specific |
| PickupAgentInstructions.kt, AppFunctions, repositories, ViewModels, UI | Keep in host app |

## Gate before phase 4 is complete

Compile adapters against pinned vendor artifacts; test generic tool calls and streaming; test cancellation, timeout, sessions, close/release, concurrent requests and model replacement; test Gemini Nano on eligible devices/API levels; test Firebase without committing credentials; verify model licenses and checksums.
