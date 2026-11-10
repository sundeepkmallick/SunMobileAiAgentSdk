#!/usr/bin/env python3
"""Build and optionally upload a user-managed Sonatype Central deployment bundle.

The script deliberately uploads with publishingType=USER_MANAGED. It never
publishes a deployment to Maven Central.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import os
from pathlib import Path
import sys
import urllib.parse
import urllib.request
import uuid
import xml.etree.ElementTree as ET
from zipfile import ZIP_DEFLATED, ZipFile

GROUP = "io.github.sundeepkmallick"
VERSION = "0.1.0-rc.2"
ARTIFACTS = (
    "agent-core",
    "agent-adk",
    "agent-firebase",
    "agent-litertlm",
    "agent-mlkit",
    "agent-gemini-nano",
)
CHECKSUMS = ("md5", "sha1", "sha256", "sha512")
CENTRAL_UPLOAD_URL = "https://central.sonatype.com/api/v1/publisher/upload"


def fail(message: str) -> None:
    raise SystemExit(f"Central bundle preparation failed: {message}")


def validate_pom(pom: Path, artifact: str) -> None:
    try:
        root = ET.parse(pom).getroot()
    except ET.ParseError as exc:
        fail(f"invalid POM XML at {pom}: {exc}")
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    actual = (
        root.findtext("m:groupId", namespaces=ns),
        root.findtext("m:artifactId", namespaces=ns),
        root.findtext("m:version", namespaces=ns),
    )
    expected = (GROUP, artifact, VERSION)
    if actual != expected:
        fail(f"wrong coordinates in {pom}: expected {expected}, got {actual}")
    for field in ("name", "description", "url", "licenses", "developers", "scm"):
        if root.find(f"m:{field}", ns) is None:
            fail(f"required POM metadata '{field}' is missing from {pom}")


def sha_file(path: Path, algorithm: str) -> str:
    digest = hashlib.new(algorithm)
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def collect_component(repo: Path, artifact: str) -> list[Path]:
    directory = repo / Path(*GROUP.split(".")) / artifact / VERSION
    pom = directory / f"{artifact}-{VERSION}.pom"
    if not pom.is_file():
        fail(f"missing POM: {pom}")
    validate_pom(pom, artifact)

    binaries = [
        path for suffix in (".aar", ".jar")
        for path in directory.glob(f"{artifact}-{VERSION}{suffix}")
    ]
    if len(binaries) != 1:
        fail(f"expected exactly one primary AAR/JAR for {artifact}, found {binaries}")

    required = [
        pom,
        binaries[0],
        directory / f"{artifact}-{VERSION}-sources.jar",
        directory / f"{artifact}-{VERSION}-javadoc.jar",
    ]
    for path in required:
        if not path.is_file():
            fail(f"missing required Maven Central artifact: {path}")
        signature = Path(str(path) + ".asc")
        if not signature.is_file() or signature.stat().st_size == 0:
            fail(f"missing detached PGP signature: {signature}")

    files: list[Path] = []
    for path in required:
        files.append(path)
        files.append(Path(str(path) + ".asc"))
        for algorithm in CHECKSUMS:
            checksum_path = Path(str(path) + "." + algorithm)
            checksum_path.write_text(sha_file(path, algorithm) + "\n", encoding="ascii")
            files.append(checksum_path)
    return files


def build_bundle(repo: Path, output: Path) -> int:
    if not repo.is_dir():
        fail(f"Maven local repository does not exist: {repo}")
    output.parent.mkdir(parents=True, exist_ok=True)
    count = 0
    with ZipFile(output, "w", compression=ZIP_DEFLATED, compresslevel=9) as archive:
        for artifact in ARTIFACTS:
            directory = repo / Path(*GROUP.split(".")) / artifact / VERSION
            files = collect_component(repo, artifact)
            for path in files:
                archive_name = Path(*GROUP.split(".")) / artifact / VERSION / path.name
                archive.write(path, archive_name.as_posix())
                count += 1
    print(f"Created {output} with {len(ARTIFACTS)} components and {count} files.")
    return count


def upload_bundle(bundle: Path, deployment_name: str) -> str:
    username = os.environ.get("MAVEN_CENTRAL_USERNAME", "")
    password = os.environ.get("MAVEN_CENTRAL_PASSWORD", "")
    if not username or not password:
        fail("MAVEN_CENTRAL_USERNAME and MAVEN_CENTRAL_PASSWORD must be configured as CI secrets")
    if not bundle.is_file():
        fail(f"bundle does not exist: {bundle}")

    boundary = "----SunMobileCentral" + uuid.uuid4().hex
    body = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="bundle"; filename="{bundle.name}"\r\n'
        "Content-Type: application/octet-stream\r\n\r\n"
    ).encode("utf-8") + bundle.read_bytes() + f"\r\n--{boundary}--\r\n".encode("ascii")
    token = base64.b64encode(f"{username}:{password}".encode("utf-8")).decode("ascii")
    query = urllib.parse.urlencode({
        "name": deployment_name,
        "publishingType": "USER_MANAGED",
    })
    request = urllib.request.Request(
        f"{CENTRAL_UPLOAD_URL}?{query}",
        data=body,
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": f"multipart/form-data; boundary={boundary}",
            "Content-Length": str(len(body)),
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=180) as response:
            deployment_id = response.read().decode("utf-8").strip()
    except Exception as exc:
        fail(f"Sonatype upload request failed: {exc}")
    if not deployment_id:
        fail("Sonatype returned an empty deployment ID")
    print(f"Uploaded user-managed deployment. Deployment ID: {deployment_id}")
    print("The deployment still requires validation and a manual Publish action in Central Portal.")
    return deployment_id


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path.home() / ".m2/repository")
    parser.add_argument("--bundle", type=Path, default=Path("build/central-bundle.zip"))
    parser.add_argument("--upload", action="store_true", help="upload as USER_MANAGED; never auto-publish")
    args = parser.parse_args()

    build_bundle(args.repo, args.bundle)
    if args.upload:
        upload_bundle(args.bundle, f"SunMobile AI Agent SDK {VERSION}")


if __name__ == "__main__":
    main()
