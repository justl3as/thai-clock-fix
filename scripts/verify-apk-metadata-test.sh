#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERIFIER="$ROOT/scripts/verify-apk-metadata.sh"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

APK="$TMP_DIR/app.apk"
FAKE_AAPT="$TMP_DIR/aapt"
: > "$APK"

cat > "$FAKE_AAPT" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf "package: name='%s' versionCode='%s' versionName='%s' platformBuildVersionName='17'\n"   "${FAKE_PACKAGE:-com.thaiclockfix}"   "${FAKE_VERSION_CODE:-1000001}"   "${FAKE_VERSION_NAME:-1.0.1}"
EOF
chmod +x "$FAKE_AAPT"

"$VERIFIER" "$APK" "$FAKE_AAPT" "com.thaiclockfix" "1.0.1" "1000001" >/dev/null

if FAKE_PACKAGE="com.example.wrong" "$VERIFIER" "$APK" "$FAKE_AAPT" "com.thaiclockfix" "1.0.1" "1000001" >/dev/null 2>&1; then
  echo "expected package mismatch to fail" >&2
  exit 1
fi

if FAKE_VERSION_NAME="1.0.2" "$VERIFIER" "$APK" "$FAKE_AAPT" "com.thaiclockfix" "1.0.1" "1000001" >/dev/null 2>&1; then
  echo "expected versionName mismatch to fail" >&2
  exit 1
fi

if FAKE_VERSION_CODE="1000002" "$VERIFIER" "$APK" "$FAKE_AAPT" "com.thaiclockfix" "1.0.1" "1000001" >/dev/null 2>&1; then
  echo "expected versionCode mismatch to fail" >&2
  exit 1
fi

echo "APK metadata verifier tests passed"
