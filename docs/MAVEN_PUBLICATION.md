# Maven publication: 0.1.0-rc.2

This document describes local verification and the controlled Sonatype Central Portal staging upload for the six SDK modules. The verified namespace is `io.github.sundeepkmallick`. The release candidate remains `0.1.0-rc.2`; this workflow does not create a stable `0.1.0` release or a Git tag.

## Published modules and coordinates

| Module | Maven artifact |
| --- | --- |
| `:agent-core` | `io.github.sundeepkmallick:agent-core:0.1.0-rc.2` |
| `:agent-adk` | `io.github.sundeepkmallick:agent-adk:0.1.0-rc.2` |
| `:agent-firebase` | `io.github.sundeepkmallick:agent-firebase:0.1.0-rc.2` |
| `:agent-litertlm` | `io.github.sundeepkmallick:agent-litertlm:0.1.0-rc.2` |
| `:agent-mlkit` | `io.github.sundeepkmallick:agent-mlkit:0.1.0-rc.2` |
| `:agent-gemini-nano` | `io.github.sundeepkmallick:agent-gemini-nano:0.1.0-rc.2` |

The sample application is not published. `agent-gemini-nano` is retained as a legacy compatibility artifact; new Gemini Nano integrations should use `agent-mlkit`.

## Local build and verification

Requires JDK 17 and an installed Android SDK matching the project compile SDK.

```bash
./gradlew publishSdkToMavenLocal
python3 scripts/verify-maven-local.py
```

The local publication task writes only to `mavenLocal()`. The verification script checks all six POM coordinates and metadata and requires source and Javadoc JARs. It does not contact a remote repository.

## Sonatype Central staging upload

Sonatype does not currently provide an official Gradle plugin for the Central Publisher Portal. This repository therefore uses Gradle's Maven Publish and Signing plugins to create signed Maven artifacts locally, then a small Python standard-library client to assemble the Maven-layout bundle and call the Portal Publisher API. Sonatype documents the bundle layout and API at:

- [Central Portal bundle upload](https://central.sonatype.org/publish/publish-portal-upload/)
- [Central Portal Publisher API](https://central.sonatype.org/publish/publish-portal-api/)
- [Central publishing requirements](https://central.sonatype.org/publish/requirements/)

The manually dispatched workflow is `.github/workflows/maven-central-upload.yml`. It requires the exact confirmation string `UPLOAD-0.1.0-rc.2`, builds and signs all six publications, validates required POM metadata and signatures, creates checksums, retains the bundle as a workflow artifact, and uploads it with `publishingType=USER_MANAGED`.

**This upload does not publish to Maven Central.** After the upload, Sonatype validates the deployment. A maintainer must inspect the deployment in the Central Portal and explicitly select Publish. The workflow never calls the publish endpoint and never uses automatic publishing.

### GitHub Actions environment and secrets

Create a GitHub Actions environment named `maven-central-staging` and add these environment secrets:

| Secret | Value |
| --- | --- |
| `MAVEN_CENTRAL_USERNAME` | Username portion of a Sonatype Central Portal user token |
| `MAVEN_CENTRAL_PASSWORD` | Password portion of that user token |
| `SIGNING_KEY` | ASCII-armored private OpenPGP key |
| `SIGNING_PASSWORD` | Passphrase for the private key |

Generate a dedicated Portal user token from [Central Portal user tokens](https://central.sonatype.com/usertoken); do not use the Portal account password. Save the token when it is generated because the secret values are not shown again. Do not paste credentials or private signing material into chat, source files, workflow YAML, build logs or command-line arguments.

Publish the corresponding public OpenPGP key to a public keyserver before attempting the upload. Protect the environment and its secrets with the repository's normal access controls. The workflow runs only on manual dispatch; it is not triggered by pull requests, pushes, tags or releases.

### Upload and final publication sequence

1. Review and merge the publishing workflow.
2. Configure the `maven-central-staging` environment secrets.
3. Open Actions → Upload Maven Central staging deployment → Run workflow.
4. Enter exactly `UPLOAD-0.1.0-rc.2`.
5. Inspect the uploaded bundle and wait for Sonatype validation.
6. Review all six components and metadata in Central Portal. Only then, as a separate explicit action, select Publish.

Published Central components are immutable. Do not select Publish until the version, coordinates, POM metadata, signatures, dependencies and release qualification are accepted.

## Signing and bundle contents

Central requires a POM, primary artifact, sources JAR, Javadoc JAR, detached PGP signatures for each deployed artifact, and MD5/SHA-1 checksums. The bundle script includes the POM, one primary AAR/JAR, sources and Javadoc JARs, their detached `.asc` signatures, and MD5, SHA-1, SHA-256 and SHA-512 checksums. It validates coordinates and required POM metadata before uploading.

## Release qualification

A successful Central validation is not evidence that runtime or product qualification is complete. The following remain separate release gates: physical-device/backend validation, independent security and privacy review, public API/binary compatibility review, dependency/license and vulnerability review, and model provenance checks. Keep these visible and resolve them according to the project's release checklist before final publication.
