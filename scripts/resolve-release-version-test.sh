#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RESOLVER="$ROOT/scripts/resolve-release-version.sh"

assert_resolves() {
  local input="$1"
  local expected_name="$2"
  local expected_code="$3"
  local expected_tag="$4"
  local output

  output="$("$RESOLVER" "$input")"
  grep -Fxq "VERSION_NAME=$expected_name" <<<"$output"
  grep -Fxq "VERSION_CODE=$expected_code" <<<"$output"
  grep -Fxq "TAG_NAME=$expected_tag" <<<"$output"
  grep -Fxq "THAI_CLOCK_FIX_VERSION_NAME=$expected_name" <<<"$output"
  grep -Fxq "THAI_CLOCK_FIX_VERSION_CODE=$expected_code" <<<"$output"
}

assert_rejects() {
  local input="$1"
  if "$RESOLVER" "$input" >/dev/null 2>&1; then
    echo "expected invalid version to fail: $input" >&2
    exit 1
  fi
}

assert_resolves "1.0.1" "1.0.1" "1000001" "v1.0.1"
assert_resolves "v2.3.4" "2.3.4" "2003004" "v2.3.4"

assert_rejects "1.1000.0"
assert_rejects "1.0.1000"
assert_rejects "0.0.0"
assert_rejects "release-1.0.1"
assert_rejects "1.0"

echo "release version resolver tests passed"
