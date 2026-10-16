#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 5 ]]; then
  echo "usage: $0 <apk> <aapt> <expected-package> <expected-version-name> <expected-version-code>" >&2
  exit 2
fi

APK="$1"
AAPT="$2"
EXPECTED_PACKAGE="$3"
EXPECTED_VERSION_NAME="$4"
EXPECTED_VERSION_CODE="$5"

fail() {
  echo "::error::$1" >&2
  exit 1
}

[[ -f "$APK" ]] || fail "APK not found: $APK"
[[ -x "$AAPT" ]] || fail "aapt is not executable: $AAPT"
[[ "$EXPECTED_PACKAGE" =~ ^[A-Za-z0-9_.]+$ ]] || fail "Expected package name is invalid"
[[ "$EXPECTED_VERSION_NAME" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "Expected versionName is invalid"
[[ "$EXPECTED_VERSION_CODE" =~ ^[0-9]+$ ]] || fail "Expected versionCode is invalid"

PACKAGE_LINE="$("$AAPT" dump badging "$APK" | sed -n '1p')"
[[ -n "$PACKAGE_LINE" ]] || fail "aapt did not return APK package metadata"

ACTUAL_PACKAGE="$(sed -nE "s/.*package: name='([^']+)'.*/\1/p" <<<"$PACKAGE_LINE")"
ACTUAL_VERSION_CODE="$(sed -nE "s/.*versionCode='([^']+)'.*/\1/p" <<<"$PACKAGE_LINE")"
ACTUAL_VERSION_NAME="$(sed -nE "s/.*versionName='([^']+)'.*/\1/p" <<<"$PACKAGE_LINE")"

[[ "$ACTUAL_PACKAGE" == "$EXPECTED_PACKAGE" ]] || fail "APK package name mismatch: expected $EXPECTED_PACKAGE, got ${ACTUAL_PACKAGE:-missing}"
[[ "$ACTUAL_VERSION_NAME" == "$EXPECTED_VERSION_NAME" ]] || fail "APK versionName mismatch: expected $EXPECTED_VERSION_NAME, got ${ACTUAL_VERSION_NAME:-missing}"
[[ "$ACTUAL_VERSION_CODE" == "$EXPECTED_VERSION_CODE" ]] || fail "APK versionCode mismatch: expected $EXPECTED_VERSION_CODE, got ${ACTUAL_VERSION_CODE:-missing}"

echo "APK metadata matches package/version policy: $ACTUAL_PACKAGE $ACTUAL_VERSION_NAME ($ACTUAL_VERSION_CODE)"
