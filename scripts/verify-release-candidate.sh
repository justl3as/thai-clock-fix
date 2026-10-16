#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 8 ]]; then
  echo "usage: $0 <manifest> <apk> <version> <version-code> <tag> <source-sha> <rc-run-id> <cert-sha256>" >&2
  exit 2
fi

MANIFEST="$1"
APK="$2"
EXPECTED_VERSION="$3"
EXPECTED_VERSION_CODE="$4"
EXPECTED_TAG="$5"
EXPECTED_SOURCE_SHA="$6"
EXPECTED_RUN_ID="$7"
EXPECTED_CERT_SHA256="$8"

fail() {
  echo "::error::$1" >&2
  exit 1
}

normalize_digest() {
  printf '%s' "$1" | tr -d '[:space:]:' | tr '[:upper:]' '[:lower:]'
}

[[ -f "$MANIFEST" ]] || fail "Missing release candidate manifest: $MANIFEST"
[[ -f "$APK" ]] || fail "Missing release candidate APK: $APK"
[[ -f "$APK.sha256" ]] || fail "Missing release candidate checksum: $APK.sha256"
[[ "$EXPECTED_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "Expected version is invalid"
[[ "$EXPECTED_VERSION_CODE" =~ ^[0-9]+$ ]] || fail "Expected versionCode is invalid"
[[ "$EXPECTED_TAG" == "v$EXPECTED_VERSION" ]] || fail "Expected tag does not match expected version"
[[ "$EXPECTED_SOURCE_SHA" =~ ^[0-9a-f]{40}$ ]] || fail "Expected source SHA is invalid"
[[ "$EXPECTED_RUN_ID" =~ ^[0-9]+$ ]] || fail "Expected RC run ID is invalid"

EXPECTED_CERT_SHA256="$(normalize_digest "$EXPECTED_CERT_SHA256")"
[[ "$EXPECTED_CERT_SHA256" =~ ^[0-9a-f]{64}$ ]] || fail "Expected certificate SHA-256 is invalid"

MANIFEST_SCHEMA="$(jq -er '.schemaVersion' "$MANIFEST")"
MANIFEST_VERSION="$(jq -er '.version' "$MANIFEST")"
MANIFEST_VERSION_CODE="$(jq -er '.versionCode' "$MANIFEST")"
MANIFEST_TAG="$(jq -er '.tag' "$MANIFEST")"
MANIFEST_SOURCE_SHA="$(jq -er '.sourceSha' "$MANIFEST")"
MANIFEST_SOURCE_REF="$(jq -er '.sourceRef' "$MANIFEST")"
MANIFEST_RUN_ID="$(jq -er '.rcRunId' "$MANIFEST")"
MANIFEST_APK="$(jq -er '.apk' "$MANIFEST")"
MANIFEST_APK_SHA256="$(normalize_digest "$(jq -er '.apkSha256' "$MANIFEST")")"
MANIFEST_CERT_SHA256="$(normalize_digest "$(jq -er '.signingCertificateSha256' "$MANIFEST")")"

[[ "$MANIFEST_SCHEMA" == "1" ]] || fail "Unsupported release manifest schema"
[[ "$MANIFEST_VERSION" == "$EXPECTED_VERSION" ]] || fail "RC version does not match requested release version"
[[ "$MANIFEST_VERSION_CODE" == "$EXPECTED_VERSION_CODE" ]] || fail "RC versionCode does not match resolved release versionCode"
[[ "$MANIFEST_TAG" == "$EXPECTED_TAG" ]] || fail "RC tag metadata does not match requested release tag"
[[ "$MANIFEST_SOURCE_SHA" == "$EXPECTED_SOURCE_SHA" ]] || fail "RC manifest source SHA does not match the workflow run"
[[ "$MANIFEST_SOURCE_REF" == "main" ]] || fail "RC manifest was not built from main"
[[ "$MANIFEST_RUN_ID" == "$EXPECTED_RUN_ID" ]] || fail "RC manifest run ID does not match the selected workflow run"
[[ "$MANIFEST_APK" == "$(basename "$APK")" ]] || fail "RC manifest APK name does not match the downloaded APK"
[[ "$MANIFEST_CERT_SHA256" == "$EXPECTED_CERT_SHA256" ]] || fail "RC certificate fingerprint does not match repository policy"

ACTUAL_APK_SHA256="$(sha256sum "$APK" | awk '{print $1}')"
[[ "$MANIFEST_APK_SHA256" == "$ACTUAL_APK_SHA256" ]] || fail "RC APK SHA-256 does not match its manifest"

(
  cd "$(dirname "$APK")"
  sha256sum -c "$(basename "$APK").sha256"
) >/dev/null || fail "RC APK SHA-256 checksum file does not match"

echo "Release candidate provenance matches the selected workflow run and repository policy"
