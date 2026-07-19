package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.entity.McpTool;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

final class McpToolServerHealthTestFixtures {
    private static final String DEFAULT_REQUEST_ID = "tenant-1";
    private static final String DEFAULT_SERVER_NAME = "docs";

    private McpToolServerHealthTestFixtures() {
    }

    static McpServerRegistry server(String name) {
        return server(DEFAULT_REQUEST_ID, name);
    }

    static McpServerRegistry server(String requestId, String name) {
        return server(requestId, name, "http", "http://" + name + ".local/mcp", true);
    }

    static McpServerRegistry server(boolean enabled) {
        return server(DEFAULT_SERVER_NAME, "http://docs.local/mcp", enabled);
    }

    static McpServerRegistry server(
            String name,
            String endpoint,
            boolean enabled) {
        return server(DEFAULT_REQUEST_ID, name, "http", endpoint, enabled);
    }

    static McpServerRegistry server(
            String name,
            String transport,
            String endpoint,
            boolean enabled) {
        return server(DEFAULT_REQUEST_ID, name, transport, endpoint, enabled);
    }

    static McpServerRegistry server(
            String requestId,
            String name,
            String transport,
            String endpoint,
            boolean enabled) {
        McpServerRegistry server = new McpServerRegistry();
        server.setRequestId(requestId);
        server.setName(name);
        server.setTransport(transport);
        if ("http".equals(transport)) {
            server.setUrl(endpoint);
        } else {
            server.setCommand(endpoint);
        }
        server.setEnabled(enabled);
        return server;
    }

    static McpServerRegistry scheduledServer(
            String syncSchedule,
            Instant lastSyncAt,
            boolean enabled) {
        McpServerRegistry server = server(enabled);
        server.setSyncSchedule(syncSchedule);
        server.setLastSyncAt(lastSyncAt);
        return server;
    }

    static McpServerRegistry healthServer(
            String requestId,
            String name,
            String transport,
            String endpoint,
            boolean enabled) {
        Instant now = Instant.now();
        McpServerRegistry server = server(requestId, name, transport, endpoint, enabled);
        server.setSyncSchedule("PT5M");
        server.setLastSyncAt(now);
        server.setCreatedAt(now);
        server.setUpdatedAt(now);
        return server;
    }

    static McpTool tool(
            String toolId,
            boolean enabled,
            Set<String> capabilities,
            Set<String> tags) {
        return tool(DEFAULT_REQUEST_ID, toolId, enabled, capabilities, tags);
    }

    static McpTool tool(
            String requestId,
            String toolId,
            boolean enabled,
            Set<String> capabilities,
            Set<String> tags) {
        McpTool tool = new McpTool();
        tool.setRequestId(requestId);
        tool.setToolId(toolId);
        tool.setNamespace("mcp");
        tool.setName(toolId);
        tool.setInputSchema(Map.of("type", "object"));
        tool.setEnabled(enabled);
        tool.setCapabilities(capabilities);
        tool.setTags(tags);
        return tool;
    }

    static McpToolDiscoverySyncHistoryEntry successHistory(String serverName) {
        return history(serverName, McpToolDiscoverySyncStatuses.SUCCESS);
    }

    static McpToolDiscoverySyncHistoryEntry errorHistory(String serverName) {
        return history(serverName, McpToolDiscoverySyncStatuses.ERROR);
    }

    static McpToolDiscoverySyncHistoryEntry history(String serverName, String status) {
        return history(serverName, status, "done");
    }

    static McpToolDiscoverySyncHistoryEntry history(
            String serverName,
            String status,
            String message) {
        return new McpToolDiscoverySyncHistoryEntry(serverName, status, message, 1, 10, null, null);
    }

    static McpToolDiscoverySyncHistoryEntry history(
            String serverName,
            String status,
            Instant startedAt) {
        return history(serverName, status, status, startedAt);
    }

    static McpToolDiscoverySyncHistoryEntry history(
            String serverName,
            String status,
            String message,
            Instant startedAt) {
        return history(serverName, status, message, 1, startedAt);
    }

    static McpToolDiscoverySyncHistoryEntry history(
            String serverName,
            String status,
            String message,
            int itemsAffected,
            Instant startedAt) {
        return new McpToolDiscoverySyncHistoryEntry(
                serverName,
                status,
                message,
                itemsAffected,
                10,
                startedAt,
                startedAt.plusMillis(10));
    }

    static McpToolLifecycleCounts lifecycleCounts(String serverName) {
        return lifecycleCounts(serverName, true, true);
    }

    static McpToolLifecycleCounts lifecycleCounts(
            String serverName,
            boolean stale,
            boolean serverDisabled) {
        McpToolLifecycleCounts counts = new McpToolLifecycleCounts();
        if (stale) {
            counts.add(tool(
                    serverName + ".stale",
                    true,
                    McpToolLifecycle.importCapabilities(serverName, true),
                    Set.of(serverName, McpToolLifecycle.STALE_TAG)));
        }
        if (serverDisabled) {
            counts.add(tool(
                    serverName + ".disabled",
                    false,
                    McpToolLifecycle.importCapabilities(serverName, true),
                    Set.of(serverName, McpToolLifecycle.SERVER_DISABLED_TAG)));
        }
        return counts;
    }

    static McpToolServerHealth.ActionOperation operation(String method, String path) {
        return operation(method, path, Map.of());
    }

    static McpToolServerHealth.ActionOperation operation(
            String method,
            String path,
            Map<String, String> queryParameters) {
        return new McpToolServerHealth.ActionOperation(method, path, queryParameters);
    }

    static McpToolServerHealth.RecommendedAction recommendedAction(
            String code,
            String severity,
            boolean safeToAutomate,
            String message,
            String actionHint) {
        return recommendedAction(code, severity, safeToAutomate, message, actionHint, null);
    }

    static McpToolServerHealth.RecommendedAction recommendedAction(
            String code,
            String severity,
            boolean safeToAutomate,
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

    static McpToolServerHealth.IssueDetail issueDetail(
            String code,
            String severity,
            String message) {
        return new McpToolServerHealth.IssueDetail(code, severity, message);
    }
}
