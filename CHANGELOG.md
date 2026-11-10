# Changelog

## 0.1.0-rc.1 — 2026-11-09

First release-candidate review snapshot. This is not a stable release.

### Included

- Backend-neutral Kotlin/JVM agent contracts, capabilities, session facade and builder.
- Tool registry, JSON-schema subset validation, risk metadata and fail-closed host policy execution.
- Google ADK runner and Firebase AI Logic / LiteRT-LM model factories.
- Gemini Nano ML Kit Agent API adapter with host-declared AppFunctions boundary.
- Offline Android sample with explicitly labelled mock data and no network calls.
- CI for core unit tests and Android sample/adapter compilation.
- SunCommerzAssociate host integration tracked separately in [PR #4](https://github.com/sundeepkmallick/SunCommerzAssociate/pull/4).

### Known limitations

- Physical-device and vendor-runtime validation remain outstanding.
- Independent security, privacy, API/binary compatibility and legal/model provenance reviews remain outstanding.
- The host integration PR remains draft; CI compile/unit-test success is not device or production validation.
- No Maven artifacts are published and no stable `v0.1.0` tag is created by this candidate.

See [Release Checklist](docs/RELEASE_CHECKLIST.md) before approving a stable release.
