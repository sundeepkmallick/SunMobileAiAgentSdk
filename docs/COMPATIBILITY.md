# Compatibility and validation matrix

This document separates build evidence from runtime/device support. A successful compile does not prove that a vendor service, model artifact or host workflow works on a physical device.

## Dependency baseline

| Component | Baseline used by SDK/source integration | Current evidence |
| --- | --- | --- |
| JDK | 17 | Used by SDK CI |
| Google ADK Kotlin | 1.0.0 | Adapter modules compile in SDK CI |
| LiteRT-LM | 0.13.1 | Adapter module compiles; device/runtime tests pending |
| ML Kit Agent API | 1.0.0-alpha1 | Gemini Nano adapter compiles; eligible-device tests pending |
| ML Kit GenAI Prompt | 1.0.0-beta5 | Dependency baseline; device/runtime tests pending |
| Android | SDK modules use minSdk 26; Gemini Nano provider defaults to API 36 minimum | Sample and adapters compile against CI Android SDK; no device matrix claimed |
| Gradle / AGP / Kotlin | Versions pinned in Gradle wrapper and version catalog | SDK CI completes successfully |

## Verified SDK CI

The current `main` commit `2eb3d6db4759cc855f5853e6a0962d698c1a8647` passed [CI run #137](https://github.com/sundeepkmallick/SunMobileAiAgentSdk/actions/runs/42604253210) on 2026-11-09:

- `core-tests`: passed, including `:agent-core:test`.
- `sample-build`: passed, including `:sample-app:assembleDebug`, `:agent-adk:assembleDebug`, `:agent-firebase:assembleDebug`, `:agent-litertlm:assembleDebug` and `:agent-gemini-nano:assembleDebug`.

The `0.1.0-rc.1` candidate branch is subject to its own PR workflow. The main-branch result above is baseline evidence, not a substitute for candidate-commit checks.

## SunCommerzAssociate integration status

[SunCommerzAssociate PR #4](https://github.com/sundeepkmallick/SunCommerzAssociate/pull/4) remains a draft. After the `LocalModelCompatibilityTest` corrections, the latest host workflow [run #37](https://github.com/sundeepkmallick/SunCommerzAssociate/actions/runs/42601525623) passed on commit `784fa34033bbe081988568bebe9a337e676b8f22`. This is compile/unit-test evidence only; it does not validate physical devices, Firebase credentials/App Check, Gemini Nano service availability, AppFunctions dispatch or model performance.

## Required runtime/device evidence

Before declaring a backend supported for a release, record:

- exact artifact coordinates and resolved transitive dependency graph;
- JDK, Gradle, Kotlin, AGP, compileSdk, minSdk and targetSdk;
- device model, ABI, Android version, RAM and available storage;
- model artifact identifier, immutable revision, checksum, license and source;
- first load, warm run, streaming, cancellation, timeout, tool call and error results;
- session reuse/clear semantics and process-death behavior;
- close/release, model replacement, concurrent requests and low-memory behavior;
- Firebase initialization, App Check/auth configuration and real request behavior without committed credentials;
- Gemini Nano feature availability and host AppFunction authorization/dispatch behavior.

Gemini Nano availability depends on supported device/runtime conditions. AppFunctions declarations and permissions remain host responsibilities. CI compilation cannot replace physical-device coverage.
