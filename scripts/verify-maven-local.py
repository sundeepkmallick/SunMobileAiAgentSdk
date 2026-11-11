#!/usr/bin/env python3
"""Verify the six 0.1.0-rc.2 SDK publications in the local Maven repository."""
from pathlib import Path
import os
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET

VERSION = "0.1.0-rc.2"
GROUP_PATH = Path("io/github/sundeepkmallick")
GROUP = "io.github.sundeepkmallick"
ARTIFACTS = (
    "agent-core",
    "agent-adk",
    "agent-firebase",
    "agent-litertlm",
    "agent-mlkit",
    "agent-gemini-nano",
)
repo = Path(os.environ.get("MAVEN_LOCAL_REPO", Path.home() / ".m2/repository"))
errors = []
verify_signatures = os.environ.get("VERIFY_MAVEN_SIGNATURES") == "true"
gnupg_home = None

if verify_signatures:
    signing_key = os.environ.get("SIGNING_KEY", "")
    if not signing_key.strip():
        errors.append("SIGNING_KEY is required when VERIFY_MAVEN_SIGNATURES=true")
    else:
        gnupg_home = tempfile.TemporaryDirectory(prefix="sunmobile-gpg-")
        env = os.environ.copy()
        env["GNUPGHOME"] = gnupg_home.name
        imported = subprocess.run(
            ["gpg", "--batch", "--import"],
            input=signing_key,
            text=True,
            capture_output=True,
            env=env,
        )
        if imported.returncode != 0:
            errors.append("could not import the supplied signing key into a temporary GPG home")

for artifact in ARTIFACTS:
    directory = repo / GROUP_PATH / artifact / VERSION
    pom = directory / f"{artifact}-{VERSION}.pom"
    if not pom.is_file():
        errors.append(f"missing POM: {pom}")
        continue

    try:
        root = ET.parse(pom).getroot()
        ns = {"m": "http://maven.apache.org/POM/4.0.0"}
        actual = root.findtext("m:version", namespaces=ns)
        group = root.findtext("m:groupId", namespaces=ns)
        artifact_id = root.findtext("m:artifactId", namespaces=ns)
        if (group, artifact_id, actual) != (GROUP, artifact, VERSION):
            errors.append(f"incorrect coordinates in {pom}: {(group, artifact_id, actual)}")
        for required in ("name", "description", "url", "licenses", "developers", "scm"):
            if root.find(f"m:{required}", ns) is None:
                errors.append(f"missing POM metadata '{required}' in {pom}")
    except ET.ParseError as exc:
        errors.append(f"invalid POM XML {pom}: {exc}")

    binaries = list(directory.glob(f"{artifact}-{VERSION}.aar"))
    binaries += list(directory.glob(f"{artifact}-{VERSION}.jar"))
    if len(binaries) != 1:
        errors.append(f"expected exactly one primary AAR/JAR for {artifact}, found {len(binaries)}")

    for classifier in ("sources", "javadoc"):
        jar = directory / f"{artifact}-{VERSION}-{classifier}.jar"
        if not jar.is_file():
            errors.append(f"missing {classifier} JAR: {jar}")

    if verify_signatures and gnupg_home and len(binaries) == 1:
        env = os.environ.copy()
        env["GNUPGHOME"] = gnupg_home.name
        signed_files = [
            pom,
            binaries[0],
            directory / f"{artifact}-{VERSION}-sources.jar",
            directory / f"{artifact}-{VERSION}-javadoc.jar",
        ]
        for signed_file in signed_files:
            signature = Path(str(signed_file) + ".asc")
            if not signature.is_file():
                errors.append(f"missing detached signature: {signature}")
                continue
            verified = subprocess.run(
                ["gpg", "--batch", "--verify", str(signature), str(signed_file)],
                text=True,
                capture_output=True,
                env=env,
            )
            if verified.returncode != 0:
                errors.append(f"PGP signature verification failed for {signed_file}")

if gnupg_home:
    gnupg_home.cleanup()

if errors:
    print("Maven-local verification FAILED:")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print(f"Verified {len(ARTIFACTS)} Maven publications at version {VERSION} in {repo}")
if verify_signatures:
    print("Verified detached PGP signatures for each POM, binary, sources JAR and Javadoc JAR.")
