# Maven Central publication and release paths

This guide documents the actual RC2 publication and the repeatable path for a future SDK version. The six `0.1.0-rc.2` components are **published** to Maven Central. The workflow in this branch uploads a new bundle as a user-managed deployment; it does not itself perform the final Publish action.

## 1. Published coordinates and repository paths

Group: `io.github.sundeepkmallick`  
Version: `0.1.0-rc.2`

| Module | Maven coordinate | Maven Central directory |
|---|---|---|
| `:agent-core` | `io.github.sundeepkmallick:agent-core:0.1.0-rc.2` | [agent-core](https://repo1.maven.org/maven2/io/github/sundeepkmallick/agent-core/0.1.0-rc.2/) |
| `:agent-adk` | `io.github.sundeepkmallick:agent-adk:0.1.0-rc.2` | [agent-adk](https://repo1.maven.org/maven2/io/github/sundeepkmallick/agent-adk/0.1.0-rc.2/) |
| `:agent-firebase` | `io.github.sundeepkmallick:agent-firebase:0.1.0-rc.2` | [agent-firebase](https://repo1.maven.org/maven2/io/github/sundeepkmallick/agent-firebase/0.1.0-rc.2/) |
| `:agent-litertlm` | `io.github.sundeepkmallick:agent-litertlm:0.1.0-rc.2` | [agent-litertlm](https://repo1.maven.org/maven2/io/github/sundeepkmallick/agent-litertlm/0.1.0-rc.2/) |
| `:agent-mlkit` | `io.github.sundeepkmallick:agent-mlkit:0.1.0-rc.2` | [agent-mlkit](https://repo1.maven.org/maven2/io/github/sundeepkmallick/agent-mlkit/0.1.0-rc.2/) |
| `:agent-gemini-nano` | `io.github.sundeepkmallick:agent-gemini-nano:0.1.0-rc.2` | [agent-gemini-nano](https://repo1.maven.org/maven2/io/github/sundeepkmallick/agent-gemini-nano/0.1.0-rc.2/) |

The sample app is not published. `agent-gemini-nano` remains as a compatibility artifact; new Gemini Nano integrations should use `agent-mlkit`.

## 2. Repository and file paths

All publication commands below must be run from the **SunMobileAiAgentSdk repository root**, not from the consuming app repository.

| Path | Purpose |
|---|---|
| `build.gradle.kts` | Maven group, version, six published module paths, POM metadata, signing and `publishSdkToMavenLocal` task |
| `gradle/libs.versions.toml` | Gradle plugin and dependency versions |
| `agent-*/build.gradle.kts` | Module Android/JVM publication component configuration and sources/Javadoc artifacts |
| `.github/workflows/maven-central-upload.yml` | Manually dispatched CI upload workflow |
| `scripts/verify-maven-local.py` | Checks six local publications, coordinates, POM metadata, AAR/JARs and sources/Javadoc; optionally verifies PGP signatures |
| `scripts/prepare-central-bundle.py` | Assembles Maven-layout ZIP, checks required files/signatures and optionally uploads to Central |
| `build/central-bundle.zip` | Generated Central upload bundle (not committed) |
| `~/.m2/repository/io/github/sundeepkmallick/` | Local Maven publication output on the machine/runner |

The scripts currently encode the RC2 version and six artifact IDs. For a new release, update the version and artifact list consistently in the root Gradle publication configuration, verification/bundle scripts and workflow confirmation/name, then review all module publication metadata and release docs before uploading. Do not simply change the version in one file.

## 3. Prerequisites and credentials

Required tools: JDK 17, Android SDK platform/build-tools matching the project, Python 3 and GnuPG.

The publishing process needs four values, stored only in a secure secret manager or GitHub Actions environment:

| Environment variable / GitHub secret | Purpose |
|---|---|
| `SIGNING_KEY` | ASCII-armored private OpenPGP signing key |
| `SIGNING_PASSWORD` | Private key passphrase |
| `MAVEN_CENTRAL_USERNAME` | Username portion of a Central Portal user token |
| `MAVEN_CENTRAL_PASSWORD` | Password portion of that user token |

Generate a dedicated user token in [Central Portal](https://central.sonatype.com/usertoken); do not use the account password. Publish the matching public key to a public keyserver. Never commit secrets, paste them into chat, put them in source files, or pass them as command-line arguments. The GitHub workflow reads these as environment secrets from `maven-central-staging`.

## 4. Local build, sign, verify and prepare (no upload)

Run from the SDK repository root, on the release branch/commit you intend to publish:

```bash
./gradlew --no-daemon publishSdkToMavenLocal
```

This writes the six artifacts to the current user's local Maven repository only. It does not contact Maven Central.

If signing variables are configured, Gradle signs the publications. Verify the six coordinates, POM metadata, primary binaries and source/Javadoc JARs:

```python
python3 scripts/verify-maven-local.py
```

To also cryptographically verify detached PGP signatures, make `SIGNING_KEY` available in the environment and run:

```bash
VERIFY_MAVEN_SIGNATURES=true python3 scripts/verify-maven-local.py
```

Prepare and validate the bundle without uploading:

```bash
python3 scripts/prepare-central-bundle.py --bundle build/central-bundle.zip
```

Expected output location: `SunMobileAiAgentSdk/build/central-bundle.zip`. Inspect the ZIP before upload. The script checks all six POM coordinates/required metadata, exactly one primary AAR/JAR per module, sources and Javadoc JARs, detached `.asc` signatures and checksum files. It creates MD5, SHA-1, SHA-256 and SHA-512 checksums.

## 5. Upload a user-managed deployment

### Option A — local upload

Only after reviewing the bundle and deciding to submit this exact version, provide the four required variables securely to the process environment and run from the SDK repository root:

```bash
python3 scripts/prepare-central-bundle.py --bundle build/central-bundle.zip --upload
```

The script sends `build/central-bundle.zip` to `https://central.sonatype.com/api/v1/publisher/upload` with `publishingType=USER_MANAGED`. It uses `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` for the Portal user-token authorization. The response is the deployment ID. Keep that ID with the release record.

### Option B — GitHub Actions

Workflow file: [`.github/workflows/maven-central-upload.yml`](../.github/workflows/maven-central-upload.yml)

The workflow:
1. Requires the exact confirmation string configured in that workflow (for RC2: `UPLOAD-0.1.0-rc.2`).
2. Checks out the selected source ref and installs JDK/Android build tools.
3. Runs `publishSdkToMavenLocal`.
4. Verifies all six artifacts and PGP signatures.
5. Creates `build/central-bundle.zip` and retains it as a 30-day Actions artifact.
6. Uploads the bundle as `USER_MANAGED`.

Configure GitHub Actions environment `maven-central-staging` with the four secrets above. Protect access to that environment.

**Important workflow-ref check:** GitHub's manual-dispatch UI can involve a workflow definition from the default branch as well as a selected run ref. Do not assume that choosing a feature branch guarantees that the feature-branch workflow YAML is the definition being executed. Before confirming an upload, inspect the run's workflow/ref and commit, the confirmation text, the version, the six-artifact verification step and the upload step. If the run shows an older RC1 workflow, a different version, or unexpected artifacts, cancel it. The default-branch workflow may not match the RC2 branch workflow. When in doubt, use the local process above rather than dispatching an unverified workflow.

## 6. Central validation and final Publish are separate

The upload creates a deployment in Central Portal; it does **not** make the artifacts publicly available. The workflow deliberately uses `USER_MANAGED`, never `AUTOMATIC`.

After upload:

1. Open [Central Portal → Deployments](https://central.sonatype.com/publishing/deployments).
2. Locate the deployment using its name and deployment ID.
3. Wait for validation. Understand the states: `PENDING`, `VALIDATING`, `VALIDATED`, `PUBLISHING`, `PUBLISHED`, or `FAILED`.
4. If validation fails, inspect the reported errors, correct the source/build, and create a new version/deployment as appropriate. Published coordinates are immutable.
5. Review the deployment name, all six components, group/artifact/version coordinates, POM metadata, signatures and checksums.
6. Only with explicit release approval, click **Publish** in Central Portal.
7. Wait until the deployment state is `PUBLISHED`, then verify the component files at the Maven Central paths listed above.

For RC2, the published Central deployment ID recorded by the release owner is `282ed337-f336-411e-86c4-54daec62685f`. Its reported state is **PUBLISHED**.

## 7. Using a published release

Consumers use Maven Central normally; no staging credentials or special repository URL are required:

```kotlin
dependencies {
    implementation("io.github.sundeepkmallick:agent-core:0.1.0-rc.2")
    implementation("io.github.sundeepkmallick:agent-adk:0.1.0-rc.2")
    implementation("io.github.sundeepkmallick:agent-firebase:0.1.0-rc.2")
    implementation("io.github.sundeepkmallick:agent-litertlm:0.1.0-rc.2")
    implementation("io.github.sundeepkmallick:agent-mlkit:0.1.0-rc.2")
}
```

Add only the modules the application needs. New Gemini Nano integrations should depend on `agent-mlkit`; the legacy `agent-gemini-nano` artifact is retained for compatibility. See the [SunCommerzAssociate SDK source-selection guide](https://github.com/sundeepkmallick/SunCommerzAssociate/blob/feature/integrate-sunmobile-ai-agent-sdk/docs/SUNMOBILE_AI_AGENT_SDK_INTEGRATION.md) for switching between Maven Central, Maven Local and composite builds.

## 8. Official Sonatype references

- [Central Publisher Portal guide](https://central.sonatype.org/publish/publish-portal-guide/)
- [Upload a bundle](https://central.sonatype.org/publish/publish-portal-upload/)
- [Publisher API](https://central.sonatype.org/publish/publish-portal-api/)
- [Central Portal user tokens](https://central.sonatype.com/usertoken)

This project uses the Publisher API upload endpoint and a Maven-layout bundle. Sonatype's API documentation describes user-token authorization, deployment status, and the separate publish operation. The current project script uses `USER_MANAGED` to preserve an explicit human approval step.

## 9. Release safety

A successful bundle build, upload, Central validation or publication is not evidence of physical-device runtime qualification, security/privacy review, API/binary compatibility review, dependency/license review or model provenance qualification. Track those gates independently in [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md). Do not publish a stable version or create a stable tag without explicit approval and completed release gates.
