#!/usr/bin/env bash
# Wrapper gradlew that forwards to gollek's gradlew when top-level wrapper is absent
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [[ -x "$ROOT_DIR/../gollek/gradlew" ]]; then
  (cd "$ROOT_DIR/../gollek" && exec "./gradlew" "$@")
else
  echo "Error: upstream gradlew not found at $ROOT_DIR/../gollek/gradlew" >&2
  exit 1
fi
