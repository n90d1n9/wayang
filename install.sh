#!/usr/bin/env bash
#
# Wayang Platform Installation Script
# Supports Native (GraalVM) and JVM modes across macOS, Linux, and Windows (MSYS/Cygwin).
#
# Usage:
#   curl -sSL https://raw.githubusercontent.com/<owner>/<repo>/main/install.sh | bash
#   curl -sSL ... | bash -s -- --jvm
#   curl -sSL ... | bash -s -- --version v1.0.0
#

set -e

REPO_OWNER="kayys" # Replace with actual owner if different
REPO_NAME="wayang" # Replace with actual repo if different
GITHUB_API="https://api.github.com/repos/$REPO_OWNER/$REPO_NAME"

INSTALL_DIR="${HOME}/.local/bin"
WAYANG_HOME="${HOME}/.wayang"
WAYANG_BIN_DIR="${WAYANG_HOME}/bin"

MODE="native"
TARGET_VERSION="latest"

# -----------------------------------------------------------------------------
# Utility Functions
# -----------------------------------------------------------------------------

log() {
  echo -e "\033[1;34m==>\033[0m $1"
}

warn() {
  echo -e "\033[1;33m==> WARNING:\033[0m $1"
}

error() {
  echo -e "\033[1;31m==> ERROR:\033[0m $1"
  exit 1
}

check_dependencies() {
  if ! command -v curl >/dev/null 2>&1; then
    error "curl is required but not installed."
  fi
  if ! command -v jq >/dev/null 2>&1; then
    error "jq is required but not installed."
  fi
}

detect_os_arch() {
  local os
  local arch

  os=$(uname -s)
  arch=$(uname -m)

  case "$os" in
    Linux*)     OS_NAME="linux" ;;
    Darwin*)    OS_NAME="osx" ;;
    CYGWIN*|MINGW*|MSYS*) OS_NAME="windows" ;;
    *)          error "Unsupported OS: $os" ;;
  esac

  case "$arch" in
    x86_64|amd64) ARCH_NAME="x86_64" ;;
    arm64|aarch64) ARCH_NAME="aarch_64" ;;
    *)             error "Unsupported Architecture: $arch" ;;
  esac

  log "Detected Platform: $OS_NAME ($ARCH_NAME)"
}

fetch_release_info() {
  log "Fetching release information..."
  if [ "$TARGET_VERSION" = "latest" ]; then
    RELEASE_URL="$GITHUB_API/releases/latest"
  else
    RELEASE_URL="$GITHUB_API/releases/tags/$TARGET_VERSION"
  fi

  RELEASE_JSON=$(curl -sSL "$RELEASE_URL")
  
  if echo "$RELEASE_JSON" | jq -e '.message == "Not Found"' >/dev/null; then
    error "Release not found. Check if the repository is public and the version exists."
  fi

  RELEASE_TAG=$(echo "$RELEASE_JSON" | jq -r '.tag_name')
  log "Targeting Release: $RELEASE_TAG"
}

install_native() {
  local asset_name="wayang-standalone-${OS_NAME}-${ARCH_NAME}"
  if [ "$OS_NAME" = "windows" ]; then
    asset_name="${asset_name}.exe"
  fi

  log "Looking for native asset: $asset_name"
  DOWNLOAD_URL=$(echo "$RELEASE_JSON" | jq -r ".assets[] | select(.name == \"$asset_name\") | .browser_download_url")

  if [ -z "$DOWNLOAD_URL" ] || [ "$DOWNLOAD_URL" = "null" ]; then
    warn "Native binary not found for $OS_NAME $ARCH_NAME in release $RELEASE_TAG."
    warn "Falling back to JVM mode."
    install_jvm
    return
  fi

  log "Downloading native binary..."
  mkdir -p "$INSTALL_DIR"
  local target_file="${INSTALL_DIR}/wayang"
  if [ "$OS_NAME" = "windows" ]; then
    target_file="${INSTALL_DIR}/wayang.exe"
  fi

  curl -sSL -o "$target_file" "$DOWNLOAD_URL"
  chmod +x "$target_file"
  
  log "Successfully installed native Wayang to $target_file"
}

install_jvm() {
  if ! command -v java >/dev/null 2>&1; then
    error "Java (JRE/JDK) is required for JVM mode but not found in PATH."
  fi

  # Release action creates: artifacts/wayang-runtime-standalone-<version>.jar
  local release_version="${RELEASE_TAG#v}"
  local asset_name="wayang-runtime-standalone-${release_version}.jar"
  
  log "Looking for JVM asset: $asset_name"
  DOWNLOAD_URL=$(echo "$RELEASE_JSON" | jq -r ".assets[] | select(.name | test(\"wayang-runtime-standalone-.*\\\\.jar\")) | .browser_download_url" | head -n 1)

  if [ -z "$DOWNLOAD_URL" ] || [ "$DOWNLOAD_URL" = "null" ]; then
    error "JVM jar not found in release $RELEASE_TAG."
  fi

  log "Downloading JVM Jar..."
  mkdir -p "$WAYANG_BIN_DIR"
  local target_jar="${WAYANG_BIN_DIR}/wayang-runtime-standalone.jar"
  
  curl -sSL -o "$target_jar" "$DOWNLOAD_URL"
  
  # Create wrapper
  mkdir -p "$INSTALL_DIR"
  local wrapper_file="${INSTALL_DIR}/wayang"
  
  cat << 'EOF' > "$wrapper_file"
#!/usr/bin/env bash
exec java -jar "${HOME}/.wayang/bin/wayang-runtime-standalone.jar" "$@"
EOF
  chmod +x "$wrapper_file"

  log "Successfully installed JVM Wayang wrapper to $wrapper_file"
}

check_path() {
  if [[ ":$PATH:" != *":$INSTALL_DIR:"* ]]; then
    warn ""
    warn "The installation directory ($INSTALL_DIR) is not in your PATH."
    warn "Please add the following line to your ~/.bashrc, ~/.zshrc, or profile:"
    warn "  export PATH=\"$INSTALL_DIR:\$PATH\""
    warn ""
  fi
}

# -----------------------------------------------------------------------------
# Main Script
# -----------------------------------------------------------------------------

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --jvm)
      MODE="jvm"
      shift
      ;;
    --native)
      MODE="native"
      shift
      ;;
    --version)
      TARGET_VERSION="$2"
      shift 2
      ;;
    -h|--help)
      echo "Usage: install.sh [--native | --jvm] [--version <tag>]"
      exit 0
      ;;
    *)
      error "Unknown option: $1"
      ;;
  esac
done

check_dependencies
detect_os_arch
fetch_release_info

if [ "$MODE" = "native" ]; then
  install_native
else
  install_jvm
fi

check_path
log "Installation complete! Run 'wayang /help' to get started."
