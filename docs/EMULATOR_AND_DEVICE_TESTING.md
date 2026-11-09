# Emulator and physical-device qualification guide

This guide separates tests that can be completed without physical hardware from tests that must remain pending until supported devices are available. It is intended for the `0.1.0-rc.1` release candidate.

**Release status:** emulator results do not qualify the SDK for stable release. Do not mark hardware-dependent checks complete based on an emulator run.

## 1. Test environment and evidence

Use Android Studio's Device Manager to create a Pixel emulator using an Android API level supported by the SDK's declared minimum/target configuration. Prefer an x86_64 image for a typical desktop emulator. Record:

- Android Studio and Android Emulator versions
- AVD name, device profile, API level, image type and ABI
- Host OS and architecture
- SDK commit SHA and candidate branch
- Date/time, command output, test result and relevant logcat excerpt

Start the emulator and confirm ADB sees it:

```bash
adb devices -l
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.product.cpu.abi
adb shell cat /proc/meminfo
adb shell df -h /data
```

Save the output as emulator evidence. It is not physical-device evidence.

## 2. Emulator tests to run now

From the repository root:

```bash
./gradlew clean
./gradlew :agent-core:test
./gradlew :sample-app:assembleDebug
./gradlew :agent-adk:assembleDebug
./gradlew :agent-firebase:assembleDebug
./gradlew :agent-litertlm:assembleDebug
./gradlew :agent-gemini-nano:assembleDebug
```

Install the sample APK (adjust the path if the module's output path changes):

```bash
adb install -r sample-app/build/outputs/apk/debug/sample-app-debug.apk
```

Run and record these checks:

- [ ] Sample app launches without crashing.
- [ ] Mock/offline research flow completes and is visibly labelled as mock/offline.
- [ ] Empty, malformed and repeated user inputs are handled without a crash.
- [ ] Rotate the emulator while the screen is active; verify state and lifecycle behavior.
- [ ] Background and foreground the app; verify no unexpected work or duplicate requests.
- [ ] Navigate away and return; verify expected session/UI behavior.
- [ ] Clear the app/session and verify prior conversation content is no longer shown where clearing is expected.
- [ ] Inspect logcat for crashes, accidental prompt/response/token logging and unexpected exceptions.
- [ ] Run the relevant unit tests and retain the Gradle test reports.

For logcat, capture a bounded test window and redact any secrets before sharing:

```bash
adb logcat -c
# Reproduce the test in the emulator
adb logcat -d > emulator-logcat.txt
```

Do not put API keys, access tokens, Firebase service credentials, private model URLs or signing material in logs or evidence.

## 3. Backend integration tests

These tests may be attempted in an emulator where the backend supports the emulator's ABI/runtime and the required development configuration is available. A successful emulator run proves only that tested configuration; it does not prove support on physical hardware.

For each backend that can actually be initialized, record the exact provider/runtime version and test:

- [ ] Backend/client creation and initialization.
- [ ] A simple prompt and expected response.
- [ ] Tool registration and a valid tool call.
- [ ] Unknown tool and malformed tool arguments are rejected safely.
- [ ] Default-deny policy denies an unapproved tool.
- [ ] Approval-required operation fails closed when approval is absent or denied.
- [ ] Streaming completes correctly, if supported.
- [ ] Cancellation stops work and releases resources.
- [ ] Network unavailable, service error and model unavailable cases fail safely.
- [ ] Session clear and client close behave as documented.
- [ ] Concurrent requests do not corrupt session or policy state.

Do not record a backend as passed if it was only compiled, if the model could not be loaded, or if a mock implementation was used. Mark it **not run** or **blocked**, and explain why.

### Gemini Nano / AppFunctions special note

Gemini Nano availability is tied to supported device and system runtime capabilities. An ordinary emulator cannot establish real-device Gemini Nano availability or performance. Also, the SDK's AgentPolicy does not automatically intercept host-declared Android AppFunctions. The host application must independently authorize every AppFunction and validate identity, resource scope and approval before performing a sensitive operation.

### LiteRT-LM special note

An emulator may be useful for API wiring or an inference smoke test only if the chosen LiteRT-LM runtime, ABI and model are supported in that environment. Record whether inference truly ran, the model identifier/revision, load time, latency, memory behavior and any fallback. Do not extrapolate emulator performance to physical devices.

### Firebase / Google ADK special note

Use non-production development projects and least-privilege test credentials. Confirm network access and configuration are correct. Test authentication failure, permission denial, service unavailable and request cancellation. Never commit credentials or paste them into issue comments.

## 4. Tests that must wait for physical devices

Keep the following release gates open until run on supported physical hardware:

- [ ] Gemini Nano initialization and inference on each declared supported device class.
- [ ] LiteRT-LM model loading and inference on declared target hardware, including accelerator/fallback behavior.
- [ ] Firebase and ADK end-to-end flows on representative physical devices.
- [ ] Device-specific API/ABI compatibility, RAM/storage pressure and model download/storage behavior.
- [ ] Background/foreground, process death, cancellation and resource release under real device lifecycle conditions.
- [ ] Thermal throttling, sustained inference, battery impact and latency under realistic conditions.
- [ ] At least one low-memory/entry-level target if the SDK claims to support it.
- [ ] Host application BOPIS flow revalidation on device, including user/order scope, live inventory revalidation and fail-closed approval behavior.

For each device, record manufacturer, exact model, Android version/API, build fingerprint, ABI, available RAM/storage, backend/model version, test date, result and logs. Do not claim support for a device family from a single unrepresentative device.

## 5. Security and privacy review (not replaced by emulator testing)

Review the core tool boundary and host integration:

- [ ] Verify deny-by-default behavior and explicit allow rules.
- [ ] Verify approval-required actions fail closed for missing, denied, expired or malformed approval.
- [ ] Verify unknown tools, duplicate registrations, missing fields, wrong types and extra/untrusted arguments are handled safely.
- [ ] Attempt to invoke sensitive operations with a different user's/customer's/order's identifiers; host authorization must reject them.
- [ ] Verify the host re-reads live inventory and revalidates identity, ownership, order scope and approval immediately before a sensitive action.
- [ ] Review cancellation, concurrency, exceptions and resource cleanup.
- [ ] Review logs, analytics, crash reports and telemetry for prompts, responses, tool arguments, personal data and secrets.
- [ ] Document data minimization, retention, redaction and session-clearing behavior.
- [ ] Run dependency and license checks; record tool, database/date, findings and disposition.

Suggested source review targets include `ToolExecutor.kt`, `AgentPolicy.kt`, `ToolRegistry.kt` and `ToolDefinition.kt` under `agent-core`. Locate exact paths with:

```bash
find agent-core -type f
```

A dependency scan or emulator test is not an independent security/privacy review.

## 6. Model provenance and release hygiene

For every model artifact distributed, downloaded or recommended by the SDK, record the exact model identifier, provider, official model-card/source URL, immutable revision, SHA-256, license name/URL, commercial-use and redistribution terms, attribution requirements and restrictions. Compute a checksum for a locally available artifact with:

```bash
sha256sum /path/to/model-file
# macOS alternative:
shasum -a 256 /path/to/model-file
```

Model weights may have a license different from the SDK source license. Obtain appropriate legal/maintainer review before making licensing claims.

Before stable release, also complete public API/binary compatibility review, dependency/license and vulnerability review, R8/ProGuard checks, release signing review, reproducibility/release notes and explicit maintainer approval. Never commit signing keys, keystores, passwords, API keys or service-account files.

## 7. Evidence record template

Copy this template for each test session:

```text
SDK branch/commit:
Test date/time:
Tester:
Environment (emulator or physical device):
Device/AVD and Android API:
ABI:
Backend/runtime/model version:
Commands executed:
Test cases executed:
Passed:
Failed:
Blocked/not run (and reason):
Relevant report/log locations:
Secrets/personal data checked and redacted:
Reviewer:
Follow-up issue:
```

## 8. Release decision rule

The candidate may remain available for review as `0.1.0-rc.1`, but emulator success alone is not a stable-release approval. Keep every hardware-dependent, security, privacy, model-provenance, API-compatibility and release-signing gate open until its evidence has been reviewed and explicitly accepted. Do not create a stable `v0.1.0` tag or publish stable artifacts before that approval.
