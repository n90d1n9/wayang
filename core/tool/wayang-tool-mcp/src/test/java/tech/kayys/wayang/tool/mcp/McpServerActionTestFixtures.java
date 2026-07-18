package tech.kayys.wayang.tool.mcp;

import java.util.List;
import java.util.Map;

import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.operation;

final class McpServerActionTestFixtures {

    static final String DEFAULT_SERVER_NAME = "docs";
    static final String DEFAULT_WARNING = "preview warning";

    private McpServerActionTestFixtures() {
    }

    static McpToolServerHealth.ActionQueueItem action(String actionCode) {
        return action(DEFAULT_SERVER_NAME, actionCode);
    }

    static McpToolServerHealth.ActionQueueItem action(
            String actionCode,
            boolean safeToAutomate,
            boolean structuredOperation) {
        return action(DEFAULT_SERVER_NAME, actionCode, safeToAutomate, structuredOperation);
    }

    static McpToolServerHealth.ActionQueueItem actionWithSeverity(
            String severity,
            boolean safeToAutomate) {
        return action(DEFAULT_SERVER_NAME, McpServerActionCatalog.ACTION_RUN_SYNC, severity, safeToAutomate, true);
    }

    static McpToolServerHealth.ActionQueueItem reviewStaleToolsAction(String serverName) {
        return new McpToolServerHealth.ActionQueueItem(
                serverName + ":" + McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                serverName,
                McpServerHealthStatus.DEGRADED,
                "http",
                "http://" + serverName + ".local/mcp",
                McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                McpIssueSeverity.WARNING,
                200,
                false,
                "Review stale MCP tools.",
                "GET /mcp/tools/registry?serverName=" + serverName + "&stale=true",
                operation(
                        "GET",
                        "/mcp/tools/registry",
                        Map.of("serverName", serverName, "stale", "true")));
    }

    static McpToolServerHealth.ActionQueueItem action(
            String serverName,
            String actionCode) {
        return action(serverName, actionCode, true);
    }

    static McpToolServerHealth.ActionQueueItem action(
            String serverName,
            String actionCode,
            boolean safeToAutomate) {
        return action(serverName, actionCode, safeToAutomate, true);
    }

    static McpToolServerHealth.ActionQueueItem action(
            String serverName,
            String actionCode,
            boolean safeToAutomate,
            boolean structuredOperation) {
        return action(serverName, actionCode, McpIssueSeverity.INFO, safeToAutomate, structuredOperation);
    }

    static McpToolServerHealth.ActionQueueItem action(
            String serverName,
            String actionCode,
            String severity,
            boolean safeToAutomate,
            boolean structuredOperation) {
        return new McpToolServerHealth.ActionQueueItem(
                serverName + ":" + actionCode,
                serverName,
                McpServerHealthStatus.HEALTHY,
                "http",
                "http://" + serverName + ".local/mcp",
                actionCode,
                severity,
                110,
                safeToAutomate,
                "Action " + actionCode,
                structuredOperation ? "POST /mcp/tools/discover/sync/" + serverName : "Handle manually",
                structuredOperation
                        ? operation("POST", "/mcp/tools/discover/sync/" + serverName)
                        : null);
    }

    static McpServerActionPreview preview(McpToolServerHealth.ActionQueueItem action) {
        return preview(action, List.of(DEFAULT_WARNING));
    }

    static McpServerActionPreview preview(
            McpToolServerHealth.ActionQueueItem action,
            List<String> warnings) {
        return McpServerActionPreview.from(
                McpServerActionIdentity.parse(action.id()),
                action,
                warnings);
    }

    static McpToolServerHealth health(McpToolServerHealth.ActionQueueItem action) {
        return health(action, List.of(DEFAULT_WARNING));
    }

    static McpToolServerHealth health(
            McpToolServerHealth.ActionQueueItem action,
            List<String> warnings) {
        return new McpToolServerHealth(
                1,
                1,
                0,
                McpServerHealthStatus.HEALTHY.equals(action.healthStatus()) ? 1 : 0,
                McpServerHealthStatus.DEGRADED.equals(action.healthStatus()) ? 1 : 0,
                McpServerHealthStatus.UNHEALTHY.equals(action.healthStatus()) ? 1 : 0,
                McpServerHealthStatus.UNSYNCED.equals(action.healthStatus()) ? 1 : 0,
                0,
                action.healthStatus(),
                McpServerHealthStatus.emptyCounts(),
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                McpToolLifecycleCounts.emptyLifecycleStates(),
                Map.of(),
                Map.of(),
                null,
                1,
                action.safeToAutomate() ? 1 : 0,
                action.safeToAutomate() ? 0 : 1,
                action.operation() == null ? 0 : 1,
                action.operation() == null ? 1 : 0,
                action.operation() == null || action.operation().method() == null
                        ? Map.of()
                        : Map.of(action.operation().method(), 1),
                Map.of(action.executionMode(), 1),
                Map.of(action.code(), 1),
                Map.of(action.severity(), 1),
                action.severity(),
                1,
                0,
                1,
                1,
                false,
                List.of(action),
                List.of(),
                warnings);
    }

    static McpToolServerHealthService healthService(
            McpToolServerHealth.ActionQueueItem action,
            String expectedRequestId,
            String expectedServerName) {
        return healthService(action, expectedRequestId, expectedServerName, null, List.of(DEFAULT_WARNING));
    }

    static McpToolServerHealthService healthService(
            McpToolServerHealth.ActionQueueItem action,
            String expectedRequestId,
            String expectedServerName,
            String expectedActionCode) {
        return healthService(action, expectedRequestId, expectedServerName, expectedActionCode,
                List.of(DEFAULT_WARNING));
    }

    static McpToolServerHealthService healthService(
            McpToolServerHealth.ActionQueueItem action,
            String expectedRequestId,
            String expectedServerName,
            String expectedActionCode,
            List<String> warnings) {
        return healthService(action, expectedRequestId, expectedServerName, expectedActionCode, true, warnings);
    }

    static McpToolServerHealthService healthServiceWithOptionalActionFilter(
            McpToolServerHealth.ActionQueueItem action,
            String expectedRequestId,
            String expectedServerName) {
        return healthService(action, expectedRequestId, expectedServerName, action.code(), false,
                List.of(DEFAULT_WARNING));
    }

    private static McpToolServerHealthService healthService(
            McpToolServerHealth.ActionQueueItem action,
            String expectedRequestId,
            String expectedServerName,
            String expectedActionCode,
            boolean requireActionCode,
            List<String> warnings) {
        return McpToolServerHealthServiceTestDouble.summarizing(health(action, warnings))
                .expectingRequestId(expectedRequestId)
                .expectingServerName(expectedServerName)
                .expectingActionCode(expectedActionCode, requireActionCode);
    }
}
