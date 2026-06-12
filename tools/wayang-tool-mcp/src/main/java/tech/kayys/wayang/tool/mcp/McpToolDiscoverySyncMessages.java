package tech.kayys.wayang.tool.mcp;

final class McpToolDiscoverySyncMessages {

    private static final String SERVER_REGISTRY_NOT_CONFIGURED =
            "MCP server registry is not configured";

    private McpToolDiscoverySyncMessages() {
    }

    static String registryModeDisabled() {
        return "Live MCP discovery sync skipped: MCP registry database mode is not enabled.";
    }

    static String serverNameRequired() {
        return "Live MCP tools sync failed: serverName is required";
    }

    static String serverRegistryNotConfigured() {
        return "Live MCP tools sync failed: " + SERVER_REGISTRY_NOT_CONFIGURED;
    }

    static String serverRegistryNotConfigured(String serverName) {
        return syncFailedForServer(serverName, SERVER_REGISTRY_NOT_CONFIGURED);
    }

    static String serverNotFound(String serverName) {
        return syncFailedForServer(serverName, "MCP server was not found");
    }

    static String serverDisabled(String serverName) {
        return "MCP server `" + serverName + "` is disabled";
    }

    static String syncFailedForServer(String serverName, String message) {
        return "Live MCP tools sync failed for server " + serverName + ": " + message;
    }

    static String invalidSchedule(String serverName, String message) {
        return "Live MCP tools sync skipped for server " + serverName + ": " + message;
    }

    static String success(McpToolDiscoveryImportResult result) {
        return "Live MCP tools synced"
                + " (imported=" + result.imported()
                + ", stale=" + result.stale()
                + ", reactivated=" + result.reactivated()
                + ")";
    }
}
