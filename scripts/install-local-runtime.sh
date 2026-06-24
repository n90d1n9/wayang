#!/bin/bash
# wayang - Local Development Installer
# Builds the CLI from source and installs a local shim for testing.
# Usage: ./scripts/install-local-runtime.sh [--native]

set -e

# --- Default Options ---
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLEW="${PROJECT_ROOT}/gradlew"


chmod +x "$GOLLEK_CLI_BIN"

prewarm_installed_models
verify_installed_fast_paths

# 5. Final Instructions
echo ""
echo -e "${GREEN}✅ wayang CLI installed locally to ${GOLLEK_CLI_BIN}${NC}"
echo ""

if [[ ":$PATH:" != *":$BIN_DIR:"* ]]; then
    echo -e "${YELLOW}⚠ ${BIN_DIR} is not in your PATH.${NC}"
    echo -e "Add this to your .zshrc or .bashrc:"
    echo -e "  ${BLUE}export PATH=\"\$HOME/.local/bin:\$PATH\"${NC}"
    echo ""
fi

echo -e "Try running: ${YELLOW}wayang --version${NC}"
echo -e "${BLUE}--------------------------------------------------${NC}"
