#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUNDLE_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
WAYANG_HOME="${WAYANG_HOME:-$HOME/.wayang}"
BUNDLED_WAYANG_HOME="${BUNDLE_ROOT}/.wayang"

if [ -d "$BUNDLED_WAYANG_HOME" ] && [ "$BUNDLED_WAYANG_HOME" != "$WAYANG_HOME" ]; then
  mkdir -p "$WAYANG_HOME"
  tar -C "$BUNDLED_WAYANG_HOME" -cf - . | tar -C "$WAYANG_HOME" -xf -
fi

WAYANG_CONFIG_DIR="${WAYANG_CONFIG_DIR:-$WAYANG_HOME/config}"
WAYANG_LOG_DIR="${WAYANG_LOG_DIR:-$WAYANG_HOME/logs}"
WAYANG_SERVER_LOG_DIR="${WAYANG_SERVER_LOG_DIR:-$WAYANG_LOG_DIR/server}"
WAYANG_LOG_FILE_PATH="${WAYANG_LOG_FILE_PATH:-$WAYANG_SERVER_LOG_DIR/server.log}"
WAYANG_PLUGINS_DIR="${WAYANG_PLUGINS_DIR:-$WAYANG_HOME/plugins}"
WAYANG_SECRETS_DIR="${WAYANG_SECRETS_DIR:-$WAYANG_HOME/secrets}"
WAYANG_MODELS_DIR="${WAYANG_MODELS_DIR:-$WAYANG_HOME/models}"
WAYANG_MCP_DIR="${WAYANG_MCP_DIR:-$WAYANG_HOME/mcp}"
WAYANG_RUN_DIR="${WAYANG_RUN_DIR:-$WAYANG_HOME/run}"
WAYANG_VECTOR_DIR="${WAYANG_VECTOR_DIR:-$WAYANG_HOME/vector}"
WAYANG_GOLLEK_HOME="${WAYANG_GOLLEK_HOME:-$WAYANG_HOME/gollek}"
LEGACY_GOLLEK_HOME="${GOLLEK_HOME:-$HOME/.gollek}"

if mkdir -p "$WAYANG_GOLLEK_HOME" 2>/dev/null; then
  GOLLEK_HOME="$WAYANG_GOLLEK_HOME"
else
  mkdir -p "$LEGACY_GOLLEK_HOME"
  GOLLEK_HOME="$LEGACY_GOLLEK_HOME"
fi

WAYANG_GOLLEK_MODELS_DIR="${WAYANG_GOLLEK_MODELS_DIR:-$GOLLEK_HOME/models}"
WAYANG_GOLLEK_STORAGE_DIR="${WAYANG_GOLLEK_STORAGE_DIR:-$GOLLEK_HOME/storage}"

mkdir -p \
  "$WAYANG_CONFIG_DIR" \
  "$WAYANG_SERVER_LOG_DIR" \
  "$WAYANG_PLUGINS_DIR" \
  "$WAYANG_SECRETS_DIR" \
  "$WAYANG_MODELS_DIR" \
  "$WAYANG_MCP_DIR" \
  "$WAYANG_RUN_DIR" \
  "$WAYANG_VECTOR_DIR" \
  "$WAYANG_GOLLEK_MODELS_DIR" \
  "$WAYANG_GOLLEK_STORAGE_DIR" \
  "$HOME/Library/LaunchAgents"

WAYANG_EXECUTABLE="${WAYANG_EXECUTABLE:-$WAYANG_HOME/bin/wayang}"
if [ ! -x "$WAYANG_EXECUTABLE" ]; then
  chmod +x "$WAYANG_EXECUTABLE" 2>/dev/null || true
fi
if [ ! -x "$WAYANG_EXECUTABLE" ]; then
  echo "Wayang executable not found or not executable: $WAYANG_EXECUTABLE" >&2
  exit 1
fi

PLIST_FILE="$HOME/Library/LaunchAgents/tech.kayys.wayang.plist"
cat > "$PLIST_FILE" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>tech.kayys.wayang</string>
  <key>ProgramArguments</key>
  <array>
    <string>$WAYANG_EXECUTABLE</string>
  </array>
  <key>WorkingDirectory</key>
  <string>$WAYANG_HOME</string>
  <key>RunAtLoad</key>
  <true/>
  <key>KeepAlive</key>
  <true/>
  <key>StandardOutPath</key>
  <string>$WAYANG_SERVER_LOG_DIR/stdout.log</string>
  <key>StandardErrorPath</key>
  <string>$WAYANG_SERVER_LOG_DIR/stderr.log</string>
  <key>EnvironmentVariables</key>
  <dict>
    <key>WAYANG_HOME</key><string>$WAYANG_HOME</string>
    <key>WAYANG_CONFIG_DIR</key><string>$WAYANG_CONFIG_DIR</string>
    <key>WAYANG_LOG_DIR</key><string>$WAYANG_LOG_DIR</string>
    <key>WAYANG_SERVER_LOG_DIR</key><string>$WAYANG_SERVER_LOG_DIR</string>
    <key>WAYANG_LOG_FILE_PATH</key><string>$WAYANG_LOG_FILE_PATH</string>
    <key>WAYANG_PLUGINS_DIR</key><string>$WAYANG_PLUGINS_DIR</string>
    <key>WAYANG_SECRETS_DIR</key><string>$WAYANG_SECRETS_DIR</string>
    <key>WAYANG_MODELS_DIR</key><string>$WAYANG_MODELS_DIR</string>
    <key>WAYANG_MCP_DIR</key><string>$WAYANG_MCP_DIR</string>
    <key>WAYANG_RUN_DIR</key><string>$WAYANG_RUN_DIR</string>
    <key>WAYANG_VECTOR_DIR</key><string>$WAYANG_VECTOR_DIR</string>
    <key>WAYANG_GOLLEK_HOME</key><string>$WAYANG_GOLLEK_HOME</string>
    <key>GOLLEK_HOME</key><string>$GOLLEK_HOME</string>
    <key>WAYANG_GOLLEK_MODELS_DIR</key><string>$WAYANG_GOLLEK_MODELS_DIR</string>
    <key>WAYANG_GOLLEK_STORAGE_DIR</key><string>$WAYANG_GOLLEK_STORAGE_DIR</string>
  </dict>
</dict>
</plist>
EOF

launchctl unload "$PLIST_FILE" 2>/dev/null || true
launchctl load "$PLIST_FILE"
echo "Installed Wayang launchd agent at $PLIST_FILE"
