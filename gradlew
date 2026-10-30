#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if ! command -v gradle >/dev/null 2>&1; then
  echo "ERROR: Gradle is not installed or not on PATH."
  exit 1
fi

exec gradle "$@"
