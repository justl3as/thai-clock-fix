#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 <MAJOR.MINOR.PATCH|vMAJOR.MINOR.PATCH>" >&2
  exit 2
fi

RAW_VERSION="$1"
VERSION="${RAW_VERSION#v}"

if [[ ! "$VERSION" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  echo "release version must be MAJOR.MINOR.PATCH or vMAJOR.MINOR.PATCH" >&2
  exit 1
fi

MAJOR="${BASH_REMATCH[1]}"
MINOR="${BASH_REMATCH[2]}"
PATCH="${BASH_REMATCH[3]}"

if (( 10#$MINOR > 999 || 10#$PATCH > 999 )); then
  echo "MINOR and PATCH must be <= 999" >&2
  exit 1
fi

VERSION_CODE=$((10#$MAJOR * 1000000 + 10#$MINOR * 1000 + 10#$PATCH))
if (( VERSION_CODE <= 0 || VERSION_CODE > 2100000000 )); then
  echo "computed Android versionCode is out of range" >&2
  exit 1
fi

printf 'VERSION_NAME=%s\n' "$VERSION"
printf 'VERSION_CODE=%s\n' "$VERSION_CODE"
printf 'TAG_NAME=v%s\n' "$VERSION"
