# Emulator Lifecycle and Local Test Evidence

Date: 2026-11-10
SDK version: 0.1.0-rc.1
Branch: release/0.1.0-rc.1
Sample package: com.sunmobile.aiagent.sample

## Emulator environment

- Emulator serial: `emulator-5554`
- Emulator product/model: `sdk_gphone16k_arm64`
- Android API: 37
- ABI: `arm64-v8a`
- Total RAM: 12,395,728 kB (~11.8 GiB)
- Available RAM at capture: 9,849,232 kB (~9.4 GiB)
- `/data` filesystem: 10 GiB total, 8.3 GiB used, 1.2 GiB available (~87% used)

**Storage note:** only about 1.2 GiB was available. Do not download or stage large model artifacts on this emulator until storage is reclaimed and rechecked.

## Lifecycle smoke checks

- [x] Sample app launch requested with `adb shell monkey -p com.sunmobile.aiagent.sample 1`; monkey reported one event injected.
- [x] Activity observed as `RESUMED` and process alive after background/foreground smoke sequence.
- [x] Earlier captured log interval contained no matching app crash signatures (`FATAL EXCEPTION`, `ANR in`, `OutOfMemoryError`, or `SecurityException`).
- [x] User manually verified the portrait → landscape → portrait transition.
- [x] User confirmed the mock action worked after rotation.
- [x] User confirmed the mock action worked after backgrounding and resuming the app.
- [ ] No duplicate requests independently verified after resume.
- [ ] Mock action executed during a fresh log-capture interval and corresponding logs reviewed.
- [ ] Sensitive-data/log-redaction review completed.

The earlier attempted log capture cleared logcat but did not establish that a mock action was exercised during the capture interval (the explanatory shell comment was pasted into zsh as a command). The user's subsequent manual lifecycle test confirms the UI and mock action behaved as expected after rotation and resume, but a fresh action-correlated log review is still outstanding.

## Core test results

XML reports available under `agent-core/build/test-results/test/`:

| Test suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| AgentSessionTest | 1 | 0 | 0 | 0 |
| ToolExecutorTest | 4 | 0 | 0 | 0 |
| ToolRegistryTest | 3 | 0 | 0 | 0 |
| **Total** | **8** | **0** | **0** | **0** |

The XML report timestamps are 2026-11-09 and the reported `:agent-core:test` task was restored from Gradle cache. These results document the available test reports; they are not evidence that those tests freshly executed on 2026-11-10.

## SDK and adapter Gradle checks

Reported on 2026-11-10:

- [x] `./gradlew test` — BUILD SUCCESSFUL; 78 actionable tasks, all up-to-date.
- [x] `./gradlew :agent-adk:check` — BUILD SUCCESSFUL; 39 actionable tasks, 1 executed and 38 up-to-date.
- [x] `./gradlew :agent-firebase:check` — BUILD SUCCESSFUL; 71 actionable tasks, 1 executed and 70 up-to-date.
- [x] `./gradlew :agent-gemini-nano:check` — BUILD SUCCESSFUL; 39 actionable tasks, 1 executed and 38 up-to-date.
- [x] `./gradlew :agent-litertlm:check` — BUILD SUCCESSFUL; 71 actionable tasks, 1 executed and 70 up-to-date.

These are Gradle task/check results. Because most tasks were up-to-date, they do not by themselves prove fresh test execution, live backend connectivity, supported-device operation, model availability, or physical-device qualification.

## Result

**PASS for the manually reported emulator lifecycle scenarios; PARTIAL PASS for the overall emulator validation.**

The user manually confirmed portrait → landscape → portrait and successful mock-action execution after rotation and after resume. This is user-reported functional evidence; no independent video/log artifact was supplied. Action-correlated log review, duplicate-request verification, sensitive-data/log-redaction review, and physical-device/backend validation remain open.

## Follow-up evidence required

1. Clear logcat, trigger the mock action after rotation/resume, save the resulting log, and review it for crashes, ANRs, memory errors, security exceptions, duplicate requests, and sensitive data.
2. Record whether output is expected to reset on Activity recreation; the sample currently does not save the displayed output across recreation.
3. Continue with supported physical-device and backend-specific validation as described in the release checklist.
