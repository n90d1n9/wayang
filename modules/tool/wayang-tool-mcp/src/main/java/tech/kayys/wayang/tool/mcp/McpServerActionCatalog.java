package tech.kayys.wayang.tool.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

final class McpServerActionCatalog {

    static final String ACTION_CHECK_ENDPOINT = "CHECK_ENDPOINT";
    static final String ACTION_ENABLE_SERVER = "ENABLE_SERVER";
    static final String ACTION_FIX_SYNC_SCHEDULE = "FIX_SYNC_SCHEDULE";
    static final String ACTION_REVIEW_SERVER_DISABLED_TOOLS = "REVIEW_SERVER_DISABLED_TOOLS";
    static final String ACTION_REVIEW_STALE_TOOLS = "REVIEW_STALE_TOOLS";
    static final String ACTION_RUN_SYNC = "RUN_SYNC";

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String PATH_TOOLS_REGISTRY = "/mcp/tools/registry";

    private static final Definition CHECK_ENDPOINT = new Definition(ACTION_CHECK_ENDPOINT, false);
    private static final Definition ENABLE_SERVER = new Definition(ACTION_ENABLE_SERVER, false);
    private static final Definition FIX_SYNC_SCHEDULE = new Definition(ACTION_FIX_SYNC_SCHEDULE, false);
    private static final Definition REVIEW_SERVER_DISABLED_TOOLS =
            new Definition(ACTION_REVIEW_SERVER_DISABLED_TOOLS, false);
    private static final Definition REVIEW_STALE_TOOLS = new Definition(ACTION_REVIEW_STALE_TOOLS, false);
    private static final Definition RUN_SYNC = new Definition(ACTION_RUN_SYNC, true);

    private McpServerActionCatalog() {
    }

    static McpToolServerHealth.RecommendedAction enableServer(String serverName) {
        String path = enableServerPath(serverName);
        return ENABLE_SERVER.action(
                McpIssueSeverity.INFO,
                "Enable this MCP server before using its imported tools.",
                METHOD_POST + " " + path,
                operation(METHOD_POST, path));
    }

    static McpToolServerHealth.RecommendedAction fixSyncSchedule(String serverName) {
        return FIX_SYNC_SCHEDULE.action(
                McpIssueSeverity.WARNING,
                "Fix the MCP sync schedule.",
                "Update registry syncSchedule for " + serverName,
                null);
    }

    static McpToolServerHealth.RecommendedAction checkEndpoint(String endpoint) {
        return CHECK_ENDPOINT.action(
                McpIssueSeverity.CRITICAL,
                "Check MCP endpoint, credentials, and transport logs.",
                "Inspect " + endpoint,
                null);
    }

    static McpToolServerHealth.RecommendedAction initializeServerSync(String serverName) {
        String path = syncServerPath(serverName);
        return RUN_SYNC.action(
                McpIssueSeverity.WARNING,
                "Run MCP discovery sync to initialize this server.",
                METHOD_POST + " " + path,
                operation(METHOD_POST, path));
    }

    static McpToolServerHealth.RecommendedAction runScheduledSync(String serverName) {
        String path = syncServerPath(serverName);
        return RUN_SYNC.action(
                McpIssueSeverity.INFO,
                "Run scheduled MCP discovery sync for this server.",
                METHOD_POST + " " + path,
                operation(METHOD_POST, path));
    }

    static McpToolServerHealth.RecommendedAction reviewStaleTools(
            String serverName,
            int staleTools) {
        return REVIEW_STALE_TOOLS.action(
                McpIssueSeverity.WARNING,
                "Review " + staleTools + " stale MCP tool(s).",
                METHOD_GET + " " + PATH_TOOLS_REGISTRY + "?serverName=" + serverName + "&stale=true",
                operation(METHOD_GET, PATH_TOOLS_REGISTRY, queryParameters(
                        "serverName", serverName,
                        "stale", "true")));
    }

    static McpToolServerHealth.RecommendedAction reviewServerDisabledTools(
            String serverName,
            int serverDisabledTools) {
        return REVIEW_SERVER_DISABLED_TOOLS.action(
                McpIssueSeverity.WARNING,
                "Review " + serverDisabledTools + " server-disabled MCP tool(s).",
                METHOD_GET + " " + PATH_TOOLS_REGISTRY + "?serverName=" + serverName + "&serverDisabled=true",
                operation(METHOD_GET, PATH_TOOLS_REGISTRY, queryParameters(
                        "serverName", serverName,
                        "serverDisabled", "true")));
    }

    private static String enableServerPath(String serverName) {
        return "/mcp/servers/" + serverName + "/enable";
    }

    private static String syncServerPath(String serverName) {
        return "/mcp/tools/discover/sync/" + serverName;
    }

    private static McpToolServerHealth.ActionOperation operation(String method, String path) {
        return operation(method, path, Map.of());
    }

    private static McpToolServerHealth.ActionOperation operation(
            String method,
            String path,
            Map<String, String> queryParameters) {
        return new McpToolServerHealth.ActionOperation(method, path, queryParameters);
    }

    private static Map<String, String> queryParameters(String... values) {
        Map<String, String> parameters = new LinkedHashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            parameters.put(values[index], values[index + 1]);
        }
        return parameters;
    }

    private record Definition(
            String code,
            boolean safeToAutomate) {

        McpToolServerHealth.RecommendedAction action(
                String severity,
                String message,
                String actionHint,
                McpToolServerHealth.ActionOperation operation) {
            return new McpToolServerHealth.RecommendedAction(
                    code,
                    severity,
                    safeToAutomate,
                    message,
                    actionHint,
                    operation);
        }
    }
}
