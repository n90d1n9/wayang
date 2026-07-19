package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.McpServerRegistry;

import java.util.List;
import java.util.Map;

final class McpToolServerHealthEntries {

    private McpToolServerHealthEntries() {
    }

    static McpToolServerHealth.ServerHealth from(
            McpServerRegistry server,
            McpToolServerHealthInputs.ServerInput input) {
        McpToolLifecycleCounts toolCounts = input.toolCounts();
        McpToolDiscoverySyncHistoryEntry latest = input.latest();
        McpToolDiscoverySyncHistoryEntry latestSuccess = input.latestSuccess();
        McpToolDiscoverySyncHistoryEntry latestError = input.latestError();
        List<McpToolDiscoverySyncHistoryEntry> recentHistory = input.recentHistory();
        McpToolServerHealthSyncPolicyStatus syncPolicy = McpToolServerHealthSyncPolicy.from(server);
        String healthStatus = McpToolServerHealthStatusAssessment.status(server, latest, toolCounts, syncPolicy);
        int consecutiveFailures = McpToolServerHealthSyncHistory.consecutiveFailures(recentHistory);
        McpToolServerHealthIssues.Result issues = McpToolServerHealthIssues.forServer(
                server,
                latest,
                toolCounts,
                syncPolicy,
                consecutiveFailures);
        Map<String, Integer> issueSeverityCounts = McpToolServerHealthIssues.severityCounts(issues.details());
        String highestIssueSeverity = McpIssueSeverity.highest(issues.details());
        List<McpToolServerHealth.RecommendedAction> recommendedActions = McpToolServerHealthRecommendedActions.forServer(
                server,
                latest,
                toolCounts,
                syncPolicy,
                consecutiveFailures);
        return new McpToolServerHealth.ServerHealth(
                server.getName(),
                server.getTransport(),
                McpServerEndpoints.endpoint(server),
                server.isEnabled(),
                server.getSyncSchedule(),
                server.getLastSyncAt(),
                syncPolicy.nextSyncAt(),
                syncPolicy.syncDue(),
                syncPolicy.error(),
                healthStatus,
                consecutiveFailures,
                McpToolServerHealthStatusAssessment.attentionRequired(healthStatus, highestIssueSeverity),
                recommendedActions,
                issues.messages(),
                issues.codes(),
                issues.details(),
                issueSeverityCounts,
                highestIssueSeverity,
                latest == null ? null : latest.status(),
                latest == null ? null : latest.message(),
                latest == null ? 0 : latest.itemsAffected(),
                latest == null ? 0L : latest.durationMs(),
                latest == null ? null : latest.startedAt(),
                latest == null ? null : latest.finishedAt(),
                latestSuccess == null ? null : McpToolServerHealthSyncHistory.historyTime(latestSuccess),
                latestError == null ? null : McpToolServerHealthSyncHistory.historyTime(latestError),
                toolCounts.total(),
                toolCounts.enabled(),
                toolCounts.disabled(),
                toolCounts.stale(),
                toolCounts.active(),
                toolCounts.serverDisabled(),
                toolCounts.retired(),
                toolCounts.lifecycleStates());
    }
}
