#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERIFIER="$ROOT/scripts/verify-release-candidate.sh"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

VERSION="1.0.1"
VERSION_CODE="1000001"
TAG="v1.0.1"
SOURCE_SHA="0123456789abcdef0123456789abcdef01234567"
RUN_ID="123456789"
CERT_SHA="753ffe59729248f199fa9ff6789a136c62e87ed963ef9798eb4a34534503e5c2"
APK="$TMP_DIR/ThaiClockFix-v1.0.1.apk"
MANIFEST="$TMP_DIR/release-manifest.json"

printf 'signed-apk-bytes\n' > "$APK"
(
  cd "$TMP_DIR"
  sha256sum "$(basename "$APK")" > "$(basename "$APK").sha256"
)
APK_SHA="$(sha256sum "$APK" | awk '{print $1}')"

write_manifest() {
  local version="${1:-$VERSION}"
  local source_sha="${2:-$SOURCE_SHA}"
  local run_id="${3:-$RUN_ID}"
  local cert_sha="${4:-$CERT_SHA}"
  local apk_sha="${5:-$APK_SHA}"

  jq -n     --arg version "$version"     --argjson versionCode "$VERSION_CODE"     --arg tag "$TAG"     --arg sourceSha "$source_sha"     --arg sourceRef "main"     --arg runId "$run_id"     --arg apk "$(basename "$APK")"     --arg apkSha256 "$apk_sha"     --arg certSha256 "$cert_sha"     '{
      schemaVersion: 1,
      version: $version,
      versionCode: $versionCode,
      tag: $tag,
      sourceSha: $sourceSha,
      sourceRef: $sourceRef,
      rcRunId: $runId,
      apk: $apk,
      apkSha256: $apkSha256,
      signingCertificateSha256: $certSha256
    }' > "$MANIFEST"
}

expect_failure() {
  if "$VERIFIER" "$MANIFEST" "$APK" "$VERSION" "$VERSION_CODE" "$TAG" "$SOURCE_SHA" "$RUN_ID" "$CERT_SHA" >/dev/null 2>&1; then
    echo "expected release candidate verification to fail" >&2
    exit 1
  fi
}

write_manifest
"$VERIFIER" "$MANIFEST" "$APK" "$VERSION" "$VERSION_CODE" "$TAG" "$SOURCE_SHA" "$RUN_ID" "$CERT_SHA" >/dev/null

write_manifest "1.0.2"
expect_failure

write_manifest "$VERSION" "ffffffffffffffffffffffffffffffffffffffff"
expect_failure

write_manifest "$VERSION" "$SOURCE_SHA" "999"
expect_failure

write_manifest "$VERSION" "$SOURCE_SHA" "$RUN_ID" "0000000000000000000000000000000000000000000000000000000000000000"
expect_failure

write_manifest "$VERSION" "$SOURCE_SHA" "$RUN_ID" "$CERT_SHA" "0000000000000000000000000000000000000000000000000000000000000000"
expect_failure

write_manifest
printf 'tampered\n' >> "$APK"
expect_failure

echo "release candidate verifier tests passed"
