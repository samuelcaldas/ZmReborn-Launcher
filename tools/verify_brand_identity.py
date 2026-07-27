#!/usr/bin/env python3
"""Verify ZM Reborn identity, assets, and protected provenance."""

from __future__ import annotations

import hashlib
import html
import re
import struct
import subprocess
import sys
from pathlib import Path

APPLICATION_ID = "org.zmreborn"
APPLICATION_NAME = "ZM Reborn"
PROTECTED_APK_HASH = "29dcb7913414922c953fad81c409458e834921f27992042de76e5c98540fb9de"
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
ICON_SIZES = {
    "drawable-mdpi/ic_launcher.png": 48,
    "drawable-hdpi/ic_launcher.png": 72,
    "drawable-xhdpi/ic_launcher.png": 96,
    "drawable-xxhdpi/ic_launcher.png": 144,
    "drawable-xxxhdpi/ic_launcher.png": 192,
}
PALETTE = {
    "zm_reborn_slate": "#ff121a21",
    "zm_reborn_glass": "#d9121a21",
    "zm_reborn_fog": "#ffeaf0f3",
    "zm_reborn_steel": "#ffb8c2c8",
    "zm_reborn_amber": "#fff2b64a",
    "zm_reborn_ember": "#ffd95c4f",
}
ROOT = Path(__file__).resolve().parents[1]


def fail(message: str) -> None:
    raise RuntimeError(message)


def read(relative_path: str) -> str:
    return (ROOT / relative_path).read_text(encoding="utf-8")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(65536), b""):
            digest.update(block)
    return digest.hexdigest()


def verify_provenance() -> None:
    apk = ROOT / "docs/reference/zeam-launcher-3-1-10-en-android.apk"
    if sha256(apk) != PROTECTED_APK_HASH:
        fail("Historical APK SHA-256 changed")


def verify_identity_files() -> None:
    build = read("app/build.gradle")
    manifest = read("app/src/main/AndroidManifest.xml")
    if f"namespace '{APPLICATION_ID}'" not in build:
        fail("Gradle namespace mismatch")
    if f'applicationId "{APPLICATION_ID}"' not in build:
        fail("Gradle applicationId mismatch")
    for suffix in (".provider", ".core", ".permission.READ_SETTINGS"):
        if "${applicationId}" + suffix not in manifest:
            fail(f"Manifest placeholder missing: {suffix}")


def verify_labels_and_palette() -> None:
    base = xml_values("app/src/main/res/values/strings.xml", "string")
    portuguese = xml_values("app/src/main/res/values-pt-rBR/strings.xml", "string")
    colors = xml_values("app/src/main/res/values/colors.xml", "color")
    if base.get("application_name") != APPLICATION_NAME:
        fail("Base application label mismatch")
    if portuguese.get("application_name") != APPLICATION_NAME:
        fail("Portuguese application label mismatch")
    if {name: colors.get(name) for name in PALETTE} != PALETTE:
        fail("ZM Reborn palette mismatch")


def xml_values(relative_path: str, tag_name: str) -> dict[str, str]:
    content = read(relative_path)
    pattern = re.compile(
        rf'<{tag_name}\s+name="([^"]+)"[^>]*>(.*?)</{tag_name}>', re.DOTALL
    )
    return {
        name: html.unescape(value.strip())
        for name, value in pattern.findall(content)
    }


def png_size(path: Path) -> tuple[int, int]:
    data = path.read_bytes()[:24]
    if len(data) != 24 or data[:8] != PNG_SIGNATURE or data[12:16] != b"IHDR":
        fail(f"Invalid PNG header: {path.relative_to(ROOT)}")
    return struct.unpack(">II", data[16:24])


def verify_icons() -> None:
    resource_root = ROOT / "app/src/main/res"
    for relative_path, expected_size in ICON_SIZES.items():
        path = resource_root / relative_path
        if png_size(path) != (expected_size, expected_size):
            fail(f"Unexpected icon size: {relative_path}")


def active_files() -> list[Path]:
    roots = [ROOT / "app/src", ROOT / ".github/workflows"]
    files = [ROOT / "app/build.gradle", ROOT / "settings.gradle"]
    for root in roots:
        files.extend(path for path in root.rglob("*") if path.is_file())
    return files


def verify_legacy_identifiers_absent() -> None:
    legacy_tokens = legacy_identity_tokens()
    for path in active_files():
        if path.suffix not in {".java", ".xml", ".gradle", ".yml", ".yaml"}:
            continue
        content = path.read_text(encoding="utf-8", errors="ignore")
        for token in legacy_tokens:
            if token in content:
                fail(f"Legacy identifier {token!r} remains in {path.relative_to(ROOT)}")


def legacy_identity_tokens() -> tuple[str, ...]:
    old_package = "org" + ".zeam"
    old_path = "org" + "/zeam"
    old_label = "Zeam" + " Launcher"
    old_domain = "zeam" + ".org"
    old_palette = "zeam" + "_"
    return old_package, old_path, old_label, old_domain, old_palette


def verify_source_layout() -> None:
    minimum_counts = {
        "app/src/main/java/org/zmreborn": 70,
        "app/src/test/java/org/zmreborn": 10,
        "app/src/androidTest/java/org/zmreborn": 3,
    }
    for relative_path, minimum_count in minimum_counts.items():
        count = len(list((ROOT / relative_path).glob("*.java")))
        if count < minimum_count:
            fail(f"Missing Java files from {relative_path}: found {count}")


def verify_generated_outputs_untracked() -> None:
    result = subprocess.run(
        ["git", "ls-files"], cwd=ROOT, check=True, capture_output=True, text=True
    )
    forbidden = ("app/build/", ".gradle/", ".class", ".apk")
    tracked = result.stdout.splitlines()
    protected_apk = "docs/reference/zeam-launcher-3-1-10-en-android.apk"
    for path in tracked:
        if path == protected_apk:
            continue
        if path.startswith(forbidden[:2]) or path.endswith(forbidden[2:]):
            fail(f"Generated output tracked: {path}")


def main() -> int:
    """Run all deterministic brand identity checks."""
    checks = (
        verify_provenance,
        verify_identity_files,
        verify_labels_and_palette,
        verify_icons,
        verify_legacy_identifiers_absent,
        verify_source_layout,
        verify_generated_outputs_untracked,
    )
    for check in checks:
        check()
    print("ZM Reborn identity verification passed")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, RuntimeError, subprocess.SubprocessError) as error:
        print(f"ZM Reborn identity verification failed: {error}", file=sys.stderr)
        sys.exit(1)
