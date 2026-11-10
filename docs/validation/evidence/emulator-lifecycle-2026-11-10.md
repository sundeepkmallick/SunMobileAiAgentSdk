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
- [x] User reports no duplicate requests during repeated lifecycle transitions and mock-action checks.
- [x] User reports no ANRs or unexpected errors during the manual lifecycle scenarios.
- [ ] Mock action explicitly triggered between logcat clear and log capture; action-correlated log review remains outstanding.
- [ ] Sensitive-data/log-redaction review completed.

On the latest log check, the user ran `adb logcat -c`, then captured `adb logcat -d` to `validation/evidence/emulator-lifecycle-log.txt`; the supplied grep returned no matching lines for `FATAL EXCEPTION`, `ANR in`, `OutOfMemoryError`, or `SecurityException`. This is a clean grep result for that captured interval. The command transcript does not show a mock action being triggered between clearing and capturing logcat, so it is not yet action-correlated evidence. The user separately reports that repeated transitions and mock actions worked without duplicate requests, ANRs, or unexpected errors.

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

**PASS for the reported emulator lifecycle scenarios; PARTIAL PASS for overall emulator validation.**

The user confirms portrait → landscape → portrait, successful mock-action execution after rotation and resume, no duplicate requests, and no ANRs or unexpected errors during repeated transitions. The supplied log grep returned no matching crash/error signatures, but the transcript does not establish that the mock action occurred between logcat clear and capture. Sensitive-data/log-redaction review and physical-device/backend validation remain open.

## Follow-up evidence required

1. For action-correlated log evidence, clear logcat, trigger the mock action after rotation/resume, then save and review the log for crashes, ANRs, memory errors, security exceptions, and sensitive data.
2. Record whether output is expected to reset on Activity recreation; the sample currently does not save the displayed output across recreation.
3. Continue with supported physical-device and backend-specific validation as described in the release checklist.
