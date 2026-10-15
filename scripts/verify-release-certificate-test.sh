#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERIFIER="$ROOT/scripts/verify-release-certificate.sh"
EXPECTED="$(
  tr -d '[:space:]:' < "$ROOT/.github/release-signing-cert.sha256" |
    tr '[:upper:]' '[:lower:]'
)"

if ! grep -Fxq "$EXPECTED" "$ROOT/SECURITY.md"; then
  echo "SECURITY.md signing fingerprint does not match .github/release-signing-cert.sha256" >&2
  exit 1
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

APK="$TMP_DIR/test.apk"
FAKE_APKSIGNER="$TMP_DIR/apksigner"
: > "$APK"

cat > "$FAKE_APKSIGNER" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

case "${FAKE_MODE:-match}" in
  match)
    printf 'Verifies\n'
    printf 'Signer #1 certificate SHA-256 digest: %s\n' "${FAKE_CERT_SHA256:?}"
    ;;
  mismatch)
    printf 'Verifies\n'
    printf 'Signer #1 certificate SHA-256 digest: %064d\n' 0
    ;;
  multiple)
    printf 'Verifies\n'
    printf 'Signer #1 certificate SHA-256 digest: %s\n' "${FAKE_CERT_SHA256:?}"
    printf 'Signer #2 certificate SHA-256 digest: %064d\n' 1
    ;;
  verify-failure)
    printf 'DOES NOT VERIFY\n' >&2
    exit 1
    ;;
  *)
    exit 2
    ;;
esac
EOF
chmod +x "$FAKE_APKSIGNER"

FAKE_MODE=match FAKE_CERT_SHA256="$EXPECTED" \
  "$VERIFIER" "$APK" "$FAKE_APKSIGNER" "$EXPECTED" >/dev/null

if FAKE_MODE=mismatch FAKE_CERT_SHA256="$EXPECTED" \
  "$VERIFIER" "$APK" "$FAKE_APKSIGNER" "$EXPECTED" >/dev/null 2>&1; then
  echo "expected mismatched certificate to fail" >&2
  exit 1
fi

if FAKE_MODE=multiple FAKE_CERT_SHA256="$EXPECTED" \
  "$VERIFIER" "$APK" "$FAKE_APKSIGNER" "$EXPECTED" >/dev/null 2>&1; then
  echo "expected multiple signing certificates to fail" >&2
  exit 1
fi

if FAKE_MODE=verify-failure FAKE_CERT_SHA256="$EXPECTED" \
  "$VERIFIER" "$APK" "$FAKE_APKSIGNER" "$EXPECTED" >/dev/null 2>&1; then
  echo "expected apksigner verification failure to fail" >&2
  exit 1
fi

if FAKE_MODE=match FAKE_CERT_SHA256="$EXPECTED" \
  "$VERIFIER" "$APK" "$FAKE_APKSIGNER" "not-a-sha256" >/dev/null 2>&1; then
  echo "expected malformed policy fingerprint to fail" >&2
  exit 1
fi

echo "release certificate verifier tests passed"
