# Maven publication: 0.1.0-rc.1

This document describes the **local-only** Maven publication candidate. It does not publish to Maven Central, Sonatype, GitHub Packages or any other remote repository. The project remains at `0.1.0-rc.1`; this work does not create a stable `0.1.0` release or tag.

## Published modules and coordinates

All five SDK modules use group `com.sunmobile.aiagent` and version `0.1.0-rc.1`.

| Module | Maven artifact |
| --- | --- |
| `:agent-core` | `com.sunmobile.aiagent:agent-core:0.1.0-rc.1` |
| `:agent-adk` | `com.sunmobile.aiagent:agent-adk:0.1.0-rc.1` |
| `:agent-firebase` | `com.sunmobile.aiagent:agent-firebase:0.1.0-rc.1` |
| `:agent-litertlm` | `com.sunmobile.aiagent:agent-litertlm:0.1.0-rc.1` |
| `:agent-gemini-nano` | `com.sunmobile.aiagent:agent-gemini-nano:0.1.0-rc.1` |

The sample application is intentionally not published.

## Artifact contents and metadata

Each publication is built from its Gradle Java or Android release component so dependency metadata is derived from the actual component rather than manually copied. The JVM module and Android release variants attach sources and Javadoc JARs. Generated POMs include project name/description, project URL, Apache-2.0 license, developer contact and SCM coordinates.

Android artifacts publish the `release` variant. Consumers must use a compatible Android Gradle Plugin and Android SDK; the SDK baseline is documented in [Compatibility](COMPATIBILITY.md). Publication does not imply that a backend is runtime-qualified on physical devices.

## Publish to the local Maven repository

Requires JDK 17 and an installed Android SDK matching the project compile SDK.

```bash
./gradlew publishSdkToMavenLocal
python3 scripts/verify-maven-local.py
```

The aggregate task publishes only to `mavenLocal()`, normally `~/.m2/repository`. The verification script checks the five POM coordinates and metadata and requires both sources and Javadoc JARs. It does not contact a remote artifact repository.

To test a separate consumer build after local publication:

```bash
./gradlew -p validation/maven-consumer :consumer:assembleDebug
```

That fixture resolves all five coordinates from Maven local and compiles an independent Android library project. CI runs the same publication, artifact verification and consumer-resolution sequence.

## Signing

The Gradle Signing plugin is configured for all five publications. Signing is enabled when both environment variables are present:

- `SIGNING_KEY`: ASCII-armored private OpenPGP key material
- `SIGNING_PASSWORD`: passphrase for that key

Alternatively, Gradle project properties `signingKey` and `signingPassword` may be supplied through a private, untracked `~/.gradle/gradle.properties` file. Never put keys or passphrases in source control, CI logs, command-line arguments, sample files or repository configuration. Use the CI secret store if signing is later added to a release workflow. Without signing material, local publication is unsigned; the verification workflow deliberately does not pretend otherwise.

A local signing dry run can be performed with a dedicated test key in a secure environment. Do not add production signing secrets to this repository.

## Publication safety boundary

- No remote Maven repository is configured.
- No Sonatype/Maven Central credentials or publishing secrets are required.
- No workflow invokes a remote publish task.
- No stable `0.1.0` version, tag or release is created.
- Do not enable remote publication until namespace ownership, repository account setup, signing-key custody, publication validation and maintainer approval are separately confirmed.

## Release qualification

This publication setup does not close any item in [Release Checklist](RELEASE_CHECKLIST.md). In particular, device/runtime validation, independent security and privacy reviews, public API/binary compatibility review, dependency/license review and model provenance checks remain separate release gates.
