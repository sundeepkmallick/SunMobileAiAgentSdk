# Release readiness checklist

- [ ] All core and sample CI jobs pass on the release commit.
- [ ] Unit, contract, adapter, instrumentation, and device tests pass.
- [ ] Public API review and binary/API compatibility report completed.
- [ ] Firebase, LiteRT-LM, and Gemini Nano adapter support matrix documented.
- [ ] Tool policy bypass, malformed arguments, cancellation and error paths reviewed.
- [ ] Privacy review: prompt/output logging, data minimization, retention and redaction.
- [ ] Model licenses, provenance, immutable revisions and SHA-256 verified.
- [ ] Android API/ABI/RAM/storage admission tests completed.
- [ ] ProGuard/R8, release signing, dependency licenses and vulnerability checks completed.
- [ ] Sample app clearly labels mocks and does not ship credentials.
- [ ] Maven coordinates, POM metadata, sources/Javadoc artifacts and versioning reviewed.
- [ ] Publication credentials configured as GitHub Actions secrets; none committed.
- [ ] Maintainer explicitly approves publishing and release tag.

Do not publish while any required adapter is represented only by an interface or unverified port.
