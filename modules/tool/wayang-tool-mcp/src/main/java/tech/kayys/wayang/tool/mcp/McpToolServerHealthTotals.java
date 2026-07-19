package tech.kayys.wayang.tool.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class McpToolServerHealthTotals {

    private final List<McpToolServerHealth.ActionQueueItem> actionQueue = new ArrayList<>();
    private final Map<String, Integer> actionMethodCounts = new LinkedHashMap<>();
    private final Map<String, Integer> actionExecutionModeCounts = new LinkedHashMap<>();
    private final Map<String, Integer> actionCounts = new LinkedHashMap<>();
    private final Map<String, Integer> actionSeverityCounts = new LinkedHashMap<>();
    private final Map<String, Integer> issueCounts = new LinkedHashMap<>();
    private final Map<String, Integer> issueSeverityCounts = new LinkedHashMap<>();
    private final Map<String, Integer> lifecycleStates = McpToolLifecycleCounts.emptyLifecycleStates();
    private final Map<String, Integer> healthStatusCounts = McpServerHealthStatus.emptyCounts();
    private String highestActionSeverity;
    private String highestIssueSeverity;
    private int totalServers;
    private int enabledServers;
    private int disabledServers;
    private int healthyServers;
    private int degradedServers;
    private int unhealthyServers;
    private int unsyncedServers;
    private int attentionRequiredServers;
    private String highestHealthStatus;
    private int totalTools;
    private int enabledTools;
    private int disabledTools;
    private int staleTools;
    private int activeTools;
    private int serverDisabledTools;
    private int retiredTools;
    private int recommendedActions;
    private int automatableActions;
    private int manualActions;
    private int callableActions;
    private int nonCallableActions;

    void add(McpToolServerHealth.ServerHealth server) {
        totalServers++;
        if (server.enabled()) {
            enabledServers++;
        } else {
            disabledServers++;
        }
        switch (server.healthStatus()) {
            case McpServerHealthStatus.HEALTHY -> healthyServers++;
            case McpServerHealthStatus.DEGRADED -> degradedServers++;
            case McpServerHealthStatus.UNHEALTHY -> unhealthyServers++;
            case McpServerHealthStatus.UNSYNCED -> unsyncedServers++;
            default -> {
            }
        }
        if (server.attentionRequired()) {
            attentionRequiredServers++;
        }
        String normalizedHealthStatus = McpServerHealthStatus.normalize(server.healthStatus());
        if (normalizedHealthStatus != null) {
            healthStatusCounts.merge(normalizedHealthStatus, 1, Integer::sum);
        }
        highestHealthStatus = McpServerHealthStatus.higher(highestHealthStatus, server.healthStatus());
        totalTools += server.totalTools();
        enabledTools += server.enabledTools();
        disabledTools += server.disabledTools();
        staleTools += server.staleTools();
        activeTools += server.activeTools();
        serverDisabledTools += server.serverDisabledTools();
        retiredTools += server.retiredTools();
        server.issueCodes().forEach(issueCode ->
                issueCounts.merge(issueCode, 1, Integer::sum));
        server.issueDetails().forEach(issueDetail ->
                issueSeverityCounts.merge(issueDetail.severity(), 1, Integer::sum));
        for (McpToolServerHealth.RecommendedAction action : server.recommendedActions()) {
            addAction(server, action);
        }
        highestIssueSeverity = McpIssueSeverity.higher(highestIssueSeverity, server.highestIssueSeverity());
        server.lifecycleStates().forEach((state, count) ->
                lifecycleStates.merge(state, count, Integer::sum));
    }

    McpToolServerHealth toHealth(
            List<McpToolServerHealth.ServerHealth> entries,
            McpServerHealthFilters filters) {
        McpToolServerHealthActionQueue.Window queueWindow = McpToolServerHealthActionQueue.window(
                McpToolServerHealthActionQueue.sorted(actionQueue),
                filters.actionQueueOffset(),
                filters.actionQueueLimit());

        return new McpToolServerHealth(
                totalServers,
                enabledServers,
                disabledServers,
                healthyServers,
                degradedServers,
                unhealthyServers,
                unsyncedServers,
                attentionRequiredServers,
                highestHealthStatus,
                healthStatusCounts,
                totalTools,
                enabledTools,
                disabledTools,
                staleTools,
                activeTools,
                serverDisabledTools,
                retiredTools,
                lifecycleStates,
                issueCounts,
                issueSeverityCounts,
                highestIssueSeverity,
                recommendedActions,
                automatableActions,
                manualActions,
                callableActions,
                nonCallableActions,
                actionMethodCounts,
                actionExecutionModeCounts,
                actionCounts,
                actionSeverityCounts,
                highestActionSeverity,
                queueWindow.total(),
                queueWindow.offset(),
                queueWindow.limit(),
                queueWindow.returned(),
                queueWindow.truncated(),
                queueWindow.items(),
                entries,
                List.of());
    }

    private void addAction(
            McpToolServerHealth.ServerHealth server,
            McpToolServerHealth.RecommendedAction action) {
        recommendedActions++;
        actionCounts.merge(action.code(), 1, Integer::sum);
        actionSeverityCounts.merge(action.severity(), 1, Integer::sum);
        actionExecutionModeCounts.merge(action.executionMode(), 1, Integer::sum);
        highestActionSeverity = McpIssueSeverity.higher(highestActionSeverity, action.severity());
        actionQueue.add(McpToolServerHealthActionQueue.item(server, action));
        if (action.operation() == null) {
            nonCallableActions++;
        } else {
            callableActions++;
            if (action.operation().method() != null) {
                actionMethodCounts.merge(action.operation().method(), 1, Integer::sum);
            }
        }
        if (action.safeToAutomate()) {
            automatableActions++;
        } else {
            manualActions++;
        }
    }
}
