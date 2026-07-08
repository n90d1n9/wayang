#!/bin/bash

set -euo pipefail

RESET="$(printf '\033[0m')"
BOLD="$(printf '\033[1m')"
GREEN="$(printf '\033[32m')"
CYAN="$(printf '\033[36m')"
YELLOW="$(printf '\033[33m')"
MAGENTA="$(printf '\033[35m')"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

for arg in "$@"; do
  case "$arg" in
    -h|--help|--prewarm-plan|--prewarm-plan=*|--verify-fast-only|--verify-fast-only=*|--verify-fast-m4-smoke-only)
      exec bash "$ROOT_DIR/scripts/install-local-runtime.sh" "$@"
      ;;
  esac
done

source "$ROOT_DIR/scripts/module-selection-current.env"
ARCHITECTURE_VALUE="${ARCHITECTURE_TARGETS:-${ARCHITECTURE_TARGET:-native-java,binding}}"

if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  JAVA_25_HOME="$(/usr/libexec/java_home -v 25 2>/dev/null || true)"
  if [[ -n "${JAVA_25_HOME}" ]]; then
    export JAVA_HOME="${JAVA_25_HOME}"
    export PATH="${JAVA_HOME}/bin:${PATH}"
  fi
fi

GRADLE_JAVA_HOME_ARG=()
if [[ -n "${JAVA_HOME:-}" ]]; then
  GRADLE_JAVA_HOME_ARG+=("-Dorg.gradle.java.home=${JAVA_HOME}")
fi
GRADLE_MAX_WORKERS_ARG=()
if [[ -n "${WAYANG_GRADLE_MAX_WORKERS:-1}" ]]; then
  GRADLE_MAX_WORKERS_ARG+=("--max-workers=${WAYANG_GRADLE_MAX_WORKERS:-1}")
fi

append_java_tool_option() {
  local option="$1"
  case " ${JAVA_TOOL_OPTIONS:-} " in
    *" ${option} "*) ;;
    *) JAVA_TOOL_OPTIONS="${option}${JAVA_TOOL_OPTIONS:+ ${JAVA_TOOL_OPTIONS}}" ;;
  esac
}

append_java_tool_option "--enable-native-access=ALL-UNNAMED"
append_java_tool_option "--add-modules=jdk.incubator.vector"
# Enable embedded Gollek only when explicitly requested (safer for dev).
# Set ENABLE_GOLLEK_EMBED=1 to force embedding on the JVM command line.
if [[ "${ENABLE_GOLLEK_EMBED:-0}" == "1" ]]; then
  append_java_tool_option "-Dwayang.gollek.enabled=true"
fi
export JAVA_TOOL_OPTIONS

# Use ~/.wayang/config.json as authoritative source for model/provider unless WAYANG_IGNORE_CONFIG is set
CFG="$HOME/.wayang/config.json"
if [ -f "$CFG" ] && [ -z "${WAYANG_IGNORE_CONFIG:-}" ]; then
  PROVIDER=$(grep -oE '"provider"[[:space:]]*:[[:space:]]*"[^"]+"' "$CFG" | sed -E 's/.*:[[:space:]]*"([^"]+)".*/\1/' | head -n1 || true)
  
  if [ -n "$PROVIDER" ]; then
    export WAYANG_PROVIDER="$PROVIDER"
  fi

  # Try to find provider-specific model first (e.g. "cerebrasModel": "...")
  MODEL=""
  if [ -n "$PROVIDER" ] && [ "$PROVIDER" != "gollek" ]; then
    MODEL=$(grep -oE "\"${PROVIDER}Model\"[[:space:]]*:[[:space:]]*\"[^\"]+\"" "$CFG" | sed -E 's/.*:[[:space:]]*"([^"]+)".*/\1/' | head -n1 || true)
  fi

  # If not found and provider is gollek (or not set), fall back to global defaultModel
  if [ -z "$MODEL" ] && { [ -z "$PROVIDER" ] || [ "$PROVIDER" = "gollek" ]; }; then
    MODEL=$(grep -oE '"(model|defaultModel|default_model)"[[:space:]]*:[[:space:]]*"[^"]+"' "$CFG" | sed -E 's/.*:[[:space:]]*"([^"]+)".*/\1/' | head -n1 || true)
  fi

  if [ -n "$MODEL" ]; then
    export WAYANG_MODEL="$MODEL"
  fi
fi

echo "${BOLD}${GREEN}:) Resolved backend targets:${RESET} ${BACKEND_TARGETS}"
echo "${BOLD}${GREEN}:) Resolved format targets:${RESET} ${FORMAT_TARGETS}"
echo "${BOLD}${GREEN}:) Resolved LLM targets:${RESET} ${LLM_TARGETS}"
echo "${BOLD}${MAGENTA}:) Architecture:${RESET} ${ARCHITECTURE_VALUE}"
echo "${BOLD}${MAGENTA}:) Profile:${RESET} ${BUILD_PROFILE}"
echo "${BOLD}${MAGENTA}:) Java home:${RESET} ${JAVA_HOME:-$(command -v java)}"
echo "${BOLD}${MAGENTA}:) Build mode:${RESET} Quarkus Dev"
echo "${BOLD}${GREEN}:) Selection manifest:${RESET} ${ROOT_DIR}/scripts/module-selection-current.env"
echo "$GREEN:) Module manifest resolved cleanly.$RESET"

cd "$ROOT_DIR"

# Forward arguments directly to the CLI (don't rewrite 'code' or 'agent')
if [[ "$#" -gt 0 ]]; then
  printf -v QUARKUS_ARGS '%q ' "$@"
else
  QUARKUS_ARGS=""
fi

# Prefer running already-built Wayang CLI artifact (no build). If not present, fall back to gollek-cli artifact.
WAYANG_CLI_DIR="$ROOT_DIR/cli/wayang-gollek-cli"
# Prefer assembled artifact that names the CLI explicitly and avoid repackager 'original-' jars
WAYANG_JAR_CANDIDATES=(
  "$WAYANG_CLI_DIR/target"/*wayang-gollek-cli*.jar
  "$WAYANG_CLI_DIR/build/libs"/*-runner.jar
  "$WAYANG_CLI_DIR/build/libs"/*-all.jar
  "$WAYANG_CLI_DIR/build/libs"/*.jar
  "$WAYANG_CLI_DIR/target"/*.jar
  "$WAYANG_CLI_DIR/target/quarkus-app/quarkus-run.jar"
  "$WAYANG_CLI_DIR/build/quarkus-app/quarkus-run.jar"
)
WAYANG_FOUND_JAR=""
for cand in "${WAYANG_JAR_CANDIDATES[@]}"; do
  for f in $cand; do
    if [[ -f $f ]]; then
      base=$(basename "$f")
      # skip repackager 'original-' jars which lack Main-Class
      if [[ "$base" == original-* ]]; then
        continue
      fi
      WAYANG_FOUND_JAR="$f"
      break 2
    fi
  done
done

if [[ -n "$WAYANG_FOUND_JAR" ]]; then
  echo "Running Wayang CLI from artifact: $WAYANG_FOUND_JAR"
  exec java -jar "$WAYANG_FOUND_JAR" $QUARKUS_ARGS
fi

# Prefer running already-built gollek-cli artifact (no build). If not present, refuse to build unless ALLOW_BUILD=1.
GOLLEK_CLI_DIR="$ROOT_DIR/ui/gollek-cli"
JAR_CANDIDATES=(
  "$GOLLEK_CLI_DIR/build/libs"/*-runner.jar
  "$GOLLEK_CLI_DIR/build/libs"/*-all.jar
  "$GOLLEK_CLI_DIR/build/libs"/*.jar
  "$GOLLEK_CLI_DIR/target/quarkus-app/quarkus-run.jar"
  "$GOLLEK_CLI_DIR/target"/*-runner.jar
  "$GOLLEK_CLI_DIR/target"/*gollek-cli*.jar
  "$GOLLEK_CLI_DIR/target"/*.jar
  "$GOLLEK_CLI_DIR/build/quarkus-app/quarkus-run.jar"
)
FOUND_JAR=""
for cand in "${JAR_CANDIDATES[@]}"; do
  # glob may remain literal if no matches; iterate expanded matches
  for f in $cand; do
    if [[ -f $f ]]; then
      FOUND_JAR="$f"
      break 2
    fi
  done
done

if [[ -n "$FOUND_JAR" ]]; then
  echo "Running gollek-cli from artifact: $FOUND_JAR"
  exec java -jar "$FOUND_JAR" $QUARKUS_ARGS
else
  if [[ "${ALLOW_BUILD:-0}" == "1" ]]; then
    echo "No built gollek-cli artifact found; ALLOW_BUILD=1 so falling back to gradle quarkusDev (this will build)."
    ./gradlew :ui:gollek-cli:quarkusDev --quarkus-args="$QUARKUS_ARGS" \
      -Pwayang.backend="${RESOLVED_BACKEND_PROPERTY}" \
      -Pwayang.profile="${BUILD_PROFILE}" \
      -Pwayang.model.formats="${FORMAT_TARGETS}" \
      -Pwayang.llm.types="${LLM_TARGETS}" \
      -Pwayang.architecture="${ARCHITECTURE_VALUE}"
  else
    echo "No built gollek-cli artifact found. To avoid building, run './scripts/build-wayang.sh' first or set ALLOW_BUILD=1 to permit building here."
    exit 1
  fi
fi