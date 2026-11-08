# Compatibility and validation matrix

This is a planned validation matrix, not a claim that these combinations have passed.

| Component | Baseline observed in source app | SDK status |
| --- | --- | --- |
| Kotlin | 2.4.10 | Core build is CI-gated |
| Android Gradle Plugin | 9.5.0 | Sample build is CI-gated |
| Google ADK Kotlin | 1.0.0 in source app | No SDK adapter yet |
| LiteRT-LM | 0.13.1 | No SDK adapter yet |
| ML Kit Agent API | 1.0.0-alpha1 | Host app owns AppFunctions |
| ML Kit GenAI Prompt | 1.0.0-beta5 | No SDK adapter yet |
| Android | Source app uses minSdk 26; Gemini Nano service is API 36-gated | Sample minSdk 26; no device validation |

Before declaring a backend supported, record:
- exact artifact coordinates and transitive dependency graph;
- JDK, Gradle, Kotlin, AGP, compileSdk, minSdk and targetSdk;
- emulator/device model, ABI, Android version, RAM and available storage;
- model artifact identifier, immutable revision, checksum, license and source;
- first load, warm run, streaming, cancellation, timeout, tool call and error results;
- session reuse/clear semantics and process-death behavior;
- close/release, model replacement, concurrent requests and low-memory behavior;
- Firebase initialization and App Check/auth configuration without committed credentials.

Gemini Nano availability depends on device/runtime support. AppFunctions declarations and permissions remain a host application responsibility. No SDK build can substitute for physical-device coverage.
