# Release readiness checklist

This checklist is maintained honestly for the `0.1.0-rc.1` review candidate. A green compile/test workflow is not a substitute for runtime, security, legal or release-signing evidence. Do not publish a stable release while any required gate below remains open.

## Verified in the repository

- [x] SDK CI on the current `main` commit passed: [run #137](https://github.com/sundeepkmallick/SunMobileAiAgentSdk/actions/runs/42604253210) (core unit tests and Android sample plus all adapter builds).
- [x] Adapter support matrix and known runtime limitations documented in [COMPATIBILITY.md](COMPATIBILITY.md).
- [x] Tool-policy behavior has core unit coverage for deny, require-approval fail-closed behavior, malformed required arguments and allowed execution. This is test evidence, not an independent security review.
- [x] Sample app labels its backend and research content as mocked/offline and makes no network calls; no credentials are present in the sample source.
- [x] SunCommerzAssociate integration CI passed after the host test fix: [run #37](https://github.com/sundeepkmallick/SunCommerzAssociate/actions/runs/42601525623). The host PR remains draft and this does not establish device/runtime behavior.

## Required before stable 0.1.0

- [ ] Run and pass all SDK CI jobs on this release-candidate commit.
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
