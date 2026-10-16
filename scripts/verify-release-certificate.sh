#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "usage: $0 <apk> <apksigner> <expected-cert-sha256>" >&2
  exit 2
fi

APK="$1"
APKSIGNER="$2"
EXPECTED_CERT_SHA256="$3"

normalize_digest() {
  printf '%s' "$1" | tr -d '[:space:]:' | tr '[:upper:]' '[:lower:]'
}

EXPECTED_CERT_SHA256="$(normalize_digest "$EXPECTED_CERT_SHA256")"
if [[ ! "$EXPECTED_CERT_SHA256" =~ ^[0-9a-f]{64}$ ]]; then
  echo "::error::Expected release certificate SHA-256 must be exactly 64 hexadecimal characters"
  exit 1
fi

if [[ ! -f "$APK" ]]; then
  echo "::error::APK not found: $APK"
  exit 1
fi

if [[ ! -x "$APKSIGNER" ]]; then
  echo "::error::apksigner is not executable: $APKSIGNER"
  exit 1
fi

if ! VERIFY_OUTPUT="$("$APKSIGNER" verify --verbose --print-certs "$APK" 2>&1)"; then
  printf '%s\n' "$VERIFY_OUTPUT"
  echo "::error::APK signature verification failed"
  exit 1
fi

printf '%s\n' "$VERIFY_OUTPUT"

CERT_DIGESTS="$(
  printf '%s\n' "$VERIFY_OUTPUT" |
    sed -nE \
      -e 's/^Signer #[0-9]+ certificate SHA-256 digest:[[:space:]]*//p' \
      -e 's/^V[0-9.]+ Signer: certificate SHA-256 digest:[[:space:]]*//p' |
    sed 's/[[:space:]:]//g' |
    tr '[:upper:]' '[:lower:]' |
    sed '/^$/d' |
    sort -u
)"
CERT_COUNT="$(
  printf '%s\n' "$CERT_DIGESTS" |
    sed '/^[[:space:]]*$/d' |
    wc -l |
    tr -d '[:space:]'
)"

if [[ "$CERT_COUNT" != "1" ]]; then
  echo "::error::Expected exactly one unique APK signing certificate, found $CERT_COUNT"
  exit 1
fi

ACTUAL_CERT_SHA256="$(printf '%s\n' "$CERT_DIGESTS" | sed -n '1p')"
if [[ ! "$ACTUAL_CERT_SHA256" =~ ^[0-9a-f]{64}$ ]]; then
  echo "::error::Could not parse a valid signer certificate SHA-256 digest from apksigner output"
  exit 1
fi

if [[ "$ACTUAL_CERT_SHA256" != "$EXPECTED_CERT_SHA256" ]]; then
  echo "::error::Release signing certificate mismatch"
  echo "::error::Expected: $EXPECTED_CERT_SHA256"
  echo "::error::Actual:   $ACTUAL_CERT_SHA256"
  exit 1
fi

echo "Release signing certificate matches the published fingerprint: $ACTUAL_CERT_SHA256"
