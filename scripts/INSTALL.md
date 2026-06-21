# Wayang Service Installation

Release service bundles are laid out as a portable `.wayang` home. Extract the archive, then run the installer for your platform:

- Linux: `scripts/service/linux/install-systemd-user.sh`
- macOS: `scripts/service/macos/install-launchd-agent.sh`
- Windows: `scripts/service/windows/install-service.ps1`

The installers copy bundled files into `~/.wayang/*` before registering the service.

Runtime defaults:

- `WAYANG_HOME`: `~/.wayang`
- `WAYANG_CONFIG_DIR`: `~/.wayang/config`
- `WAYANG_LOG_DIR`: `~/.wayang/logs`
- `WAYANG_SERVER_LOG_DIR`: `~/.wayang/logs/server`
- `WAYANG_LOG_FILE_PATH`: `~/.wayang/logs/server/server.log`
- `WAYANG_PLUGINS_DIR`: `~/.wayang/plugins`
- `WAYANG_SECRETS_DIR`: `~/.wayang/secrets`
- `WAYANG_MODELS_DIR`: `~/.wayang/models`
- `WAYANG_MCP_DIR`: `~/.wayang/mcp`
- `WAYANG_RUN_DIR`: `~/.wayang/run`
- `WAYANG_VECTOR_DIR`: `~/.wayang/vector`
- `WAYANG_GOLLEK_HOME`: `~/.wayang/gollek`
- `WAYANG_GOLLEK_MODELS_DIR`: `~/.wayang/gollek/models`
- `WAYANG_GOLLEK_STORAGE_DIR`: `~/.wayang/gollek/storage`

Gollek compatibility defaults to `~/.wayang/gollek`. Set `GOLLEK_HOME=~/.gollek` only when a legacy deployment must keep using the old Gollek directory.
