package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.McpServerRegistry;

final class McpToolServerHealthStatusAssessment {

    private McpToolServerHealthStatusAssessment() {
    }

    static String status(
            McpServerRegistry server,
            McpToolDiscoverySyncHistoryEntry latest,
            McpToolLifecycleCounts toolCounts,
            McpToolServerHealthSyncPolicyStatus syncPolicy) {
        McpToolLifecycleCounts effectiveToolCounts = toolCounts == null
                ? new McpToolLifecycleCounts()
                : toolCounts;
        McpToolServerHealthSyncPolicyStatus effectiveSyncPolicy = syncPolicy == null
                ? new McpToolServerHealthSyncPolicyStatus(null, false, null)
                : syncPolicy;
        if (server != null && !server.isEnabled()) {
            return McpServerHealthStatus.DISABLED;
        }
        if (latest == null) {
            return McpServerHealthStatus.UNSYNCED;
        }
        if (McpToolDiscoverySyncStatuses.isError(latest.status())) {
            return McpServerHealthStatus.UNHEALTHY;
        }
        if (effectiveSyncPolicy.error() != null) {
            return McpServerHealthStatus.DEGRADED;
        }
        if (effectiveToolCounts.stale() > 0) {
            return McpServerHealthStatus.DEGRADED;
        }
        if (effectiveToolCounts.serverDisabled() > 0) {
            return McpServerHealthStatus.DEGRADED;
        }
        return McpServerHealthStatus.HEALTHY;
    }

    static boolean attentionRequired(String healthStatus, String highestIssueSeverity) {
        return McpServerHealthStatus.rank(healthStatus) >= McpServerHealthStatus.rank(McpServerHealthStatus.UNSYNCED)
                || McpIssueSeverity.rank(highestIssueSeverity) >= McpIssueSeverity.rank(McpIssueSeverity.WARNING);
    }
}
