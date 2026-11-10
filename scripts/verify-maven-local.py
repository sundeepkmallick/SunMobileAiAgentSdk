#!/usr/bin/env python3
"""Verify the five 0.1.0-rc.1 SDK publications in the local Maven repository."""
from pathlib import Path
import os
import sys
import xml.etree.ElementTree as ET

VERSION = "0.1.0-rc.1"
GROUP_PATH = Path("com/sunmobile/aiagent")
ARTIFACTS = (
    "agent-core",
    "agent-adk",
    "agent-firebase",
    "agent-litertlm",
    "agent-gemini-nano",
)
repo = Path(os.environ.get("MAVEN_LOCAL_REPO", Path.home() / ".m2/repository"))
errors = []

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
        if (group, artifact_id, actual) != ("com.sunmobile.aiagent", artifact, VERSION):
            errors.append(f"incorrect coordinates in {pom}: {(group, artifact_id, actual)}")
        for required in ("name", "description", "url", "licenses", "developers", "scm"):
            if root.find(f"m:{required}", ns) is None:
                errors.append(f"missing POM metadata '{required}' in {pom}")
    except ET.ParseError as exc:
        errors.append(f"invalid POM XML {pom}: {exc}")

    for classifier in ("sources", "javadoc"):
        jar = directory / f"{artifact}-{VERSION}-{classifier}.jar"
        if not jar.is_file():
            errors.append(f"missing {classifier} JAR: {jar}")

    if os.environ.get("VERIFY_MAVEN_SIGNATURES") == "true":
        # Android publications are AARs; the JVM publication is a JAR.
        # Verify detached signatures for every actual published binary and POM.
        binaries = list(directory.glob(f"{artifact}-{VERSION}.aar"))
        binaries += list(directory.glob(f"{artifact}-{VERSION}.jar"))
        for binary in binaries:
            signature = Path(str(binary) + ".asc")
            if not signature.is_file():
                errors.append(f"missing signature: {signature}")
        signature = Path(str(pom) + ".asc")
        if not signature.is_file():
            errors.append(f"missing signature: {signature}")

if errors:
    print("Maven-local verification FAILED:")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print(f"Verified {len(ARTIFACTS)} Maven publications at version {VERSION} in {repo}")
