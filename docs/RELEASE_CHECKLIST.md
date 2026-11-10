# Release readiness checklist

This checklist is maintained honestly for the `0.1.0-rc.1` review candidate. A green compile/test workflow is not a substitute for runtime, security, legal or release-signing evidence. Do not publish a stable release while any required gate below remains open.

See [EMULATOR_AND_DEVICE_TESTING.md](EMULATOR_AND_DEVICE_TESTING.md) for the detailed emulator test plan, physical-device test matrix, evidence template and deferred release gates.

## Verified in the repository

- [x] SDK CI on the current `main` commit passed: [run #137](https://github.com/sundeepkmallick/SunMobileAiAgentSdk/actions/runs/42604253210) (core unit tests and Android sample plus all adapter builds).
- [x] Release-candidate CI passed on `release/0.1.0-rc.1`: [run #138](https://github.com/sundeepkmallick/SunMobileAiAgentSdk/actions/runs/42606063632) (core tests and sample/adapter builds). This is build/test evidence, not hardware runtime qualification.
- [x] Adapter support matrix and known runtime limitations documented in [COMPATIBILITY.md](COMPATIBILITY.md).
- [x] Tool-policy behavior has core unit coverage for deny, require-approval fail-closed behavior, malformed required arguments and allowed execution. This is test evidence, not an independent security review.
- [x] Sample app labels its backend and research content as mocked/offline and makes no network calls; no credentials are present in the sample source.
- [x] SunCommerzAssociate integration CI passed after the host test fix: [run #37](https://github.com/sundeepkmallick/SunCommerzAssociate/actions/runs/42601525623). The host PR remains draft and this does not establish device/runtime behavior.

## Additional local validation reported on 2026-11-10

- [x] Local `./gradlew test` completed successfully (78 actionable tasks reported up-to-date).
- [x] Local `:agent-adk:check`, `:agent-firebase:check`, `:agent-gemini-nano:check` and `:agent-litertlm:check` tasks completed successfully. Most tasks were up-to-date; these results do not prove live backend or supported-device integration.
- [x] Available core XML reports show 8 tests, 0 failures, 0 errors and 0 skipped across AgentSessionTest, ToolExecutorTest and ToolRegistryTest. The XML timestamps are 2026-11-09 and the core test task was restored from cache; do not treat this as a fresh 2026-11-10 test run.
- [x] User-reported emulator lifecycle scenarios passed: portrait → landscape → portrait, mock action after rotation and after resume, and repeated transitions without duplicate requests, ANRs or unexpected errors. See [2026-11-10 emulator evidence](validation/evidence/emulator-lifecycle-2026-11-10.md).
- [ ] Action-correlated log review and sensitive-data/log-redaction review remain open; the latest grep returned no matching crash/error signatures, but the command transcript does not show a mock action between logcat clear and capture.

## Required before stable 0.1.0

- [ ] Emulator test plan executed and evidence recorded where the relevant backend/runtime is supported by the emulator. Emulator results do not replace physical-device tests.
- [ ] Unit, contract and adapter runtime tests pass; add instrumentation and physical-device coverage for each supported backend.
- [ ] Public API review and binary/API compatibility report completed and reviewed.
- [ ] Firebase, LiteRT-LM and Gemini Nano support matrix confirmed on real supported devices and service/runtime versions.
- [ ] Independent security review covers policy enforcement, bypass attempts, malformed inputs, cancellation, concurrency, lifecycle and error paths.
- [ ] Privacy review covers prompt/output logging, data minimization, retention and redaction.
- [ ] Model licenses, provenance, immutable revisions and SHA-256 checksums verified for every model artifact distributed or recommended.
- [ ] Android API/ABI/RAM/storage admission tests completed on the declared device matrix.
- [ ] ProGuard/R8 behavior, release signing, dependency licenses and vulnerability checks completed.
- [ ] Host-app integration is reviewed and accepted; host PR #4 is no longer draft and its rollout/rollback plan is approved.
- [ ] Maintainer explicitly approves the stable version, publication method and release tag.

## Distribution and publication notes

- The current SunCommerzAssociate integration consumes the SDK as a Git submodule plus Gradle composite build. Maven publication is not required for that integration.
- No Maven publication, POM/sources/Javadoc artifact, signing setup, publication credential or stable release tag is created by this candidate PR.
- If Maven distribution is chosen later, add and review publication coordinates, POM metadata, source/Javadoc artifacts, signing, repository credentials and a separate publishing workflow before enabling it.
- Do not commit credentials, signing keys, model weights or private model URLs.

## Release decision

`0.1.0-rc.1` is a review candidate only. Do not publish it as stable or create the `v0.1.0` tag until every required gate above is evidenced and explicitly approved.
