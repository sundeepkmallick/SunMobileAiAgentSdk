# Release readiness checklist

This checklist is maintained honestly for the `0.1.0-rc.1` review candidate. No physical Android device is available for this validation cycle. We will keep the RC branch and may merge code/documentation gates that do not depend on physical hardware after review and required CI pass. Hardware-dependent checks remain explicitly deferred; merging to `main` is not stable-release approval. Do not publish stable `0.1.0` while any stable-release gate remains open.

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

## Merge-to-main scope without physical hardware

The following work can be completed and reviewed without a physical Android device. Keep each item open until its own evidence exists; do not infer completion from emulator lifecycle success.

- [ ] Finish source/API review and generate a public API and binary compatibility report.
- [ ] Complete core/adapter unit, contract and mock-based tests; retain fresh test reports (avoid relying only on up-to-date or cached task output).
- [ ] Complete independent source-level security review of tool policy, authorization boundaries, malformed inputs, cancellation, concurrency and error handling.
- [ ] Complete privacy/logging review, including prompt/output/tool-argument redaction, retention, data minimization and session-clearing behavior.
- [ ] Verify model provenance, official source, immutable revision, SHA-256 and license terms for every model artifact the project distributes or recommends. If model artifacts are not bundled/recommended, document that scope explicitly.
- [ ] Run dependency vulnerability and license scans and record tool/database date, findings and disposition.
- [ ] Validate R8/ProGuard consumer rules and release build configuration using CI/build tooling.
- [ ] Review release signing configuration and reproducibility without committing or exposing keys.
- [ ] Resolve documentation accuracy, sample behavior, host integration review and rollout/rollback documentation.
- [ ] Ensure all applicable CI checks pass on the final PR head and obtain maintainer review/approval for merging code to main.

**Merge boundary:** a reviewed merge to main may include the work above once completed, but it must preserve the explicit pending hardware-validation status below. Do not create a stable tag or publish stable artifacts as part of that merge.

## Deferred: requires supported physical device(s)

- [ ] Gemini Nano initialization, availability checks and inference on eligible physical devices.
- [ ] LiteRT-LM model loading/inference, accelerator and fallback behavior on declared target hardware.
- [ ] Firebase and ADK end-to-end integration on representative physical devices where device-specific behavior is part of the support claim.
- [ ] Device-specific API/ABI, RAM/storage pressure and model download/storage behavior.
- [ ] Real-device process death, background execution, cancellation/resource release, thermal behavior, sustained inference, battery impact and latency.
- [ ] Validate the declared low-memory/entry-level device support claim.
- [ ] Revalidate host-app user/order scope and sensitive BOPIS flow on actual target hardware.

These are **deferred/not run**, not failed and not passed. The emulator API 37 ARM64 results do not substitute for them. Reopen this section when supported physical hardware becomes available.

## Required before stable 0.1.0

- [x] Emulator lifecycle scenarios exercised on API 37 / arm64-v8a and recorded in [2026-11-10 evidence](validation/evidence/emulator-lifecycle-2026-11-10.md). This does not qualify hardware-dependent backend support.
- [ ] Emulator-supported backend/runtime tests and action-correlated log/privacy review completed where applicable.
- [ ] Unit/contract/adapter test evidence refreshed and reviewed; physical-device instrumentation/backend coverage remains deferred as listed above.
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
