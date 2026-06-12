package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.McpServerRegistry;

import java.util.ArrayList;
import java.util.List;

final class McpToolServerHealthRecommendedActions {

    private McpToolServerHealthRecommendedActions() {
    }

    static List<McpToolServerHealth.RecommendedAction> forServer(
            McpServerRegistry server,
            McpToolDiscoverySyncHistoryEntry latest,
            McpToolLifecycleCounts toolCounts,
            McpToolServerHealthSyncPolicyStatus syncPolicy,
            int consecutiveFailures) {
        List<McpToolServerHealth.RecommendedAction> actions = new ArrayList<>();
        McpToolLifecycleCounts effectiveToolCounts = toolCounts == null
                ? new McpToolLifecycleCounts()
                : toolCounts;
        McpToolServerHealthSyncPolicyStatus effectiveSyncPolicy = syncPolicy == null
                ? new McpToolServerHealthSyncPolicyStatus(null, false, null)
                : syncPolicy;
        String serverName = server.getName();
        if (!server.isEnabled()) {
            actions.add(McpServerActionCatalog.enableServer(serverName));
            return List.copyOf(actions);
        }
        if (effectiveSyncPolicy.error() != null) {
            actions.add(McpServerActionCatalog.fixSyncSchedule(serverName));
        }
        if ((latest != null && McpToolDiscoverySyncStatuses.isError(latest.status())) || consecutiveFailures > 1) {
            actions.add(McpServerActionCatalog.checkEndpoint(McpServerEndpoints.endpoint(server)));
        } else if (latest == null) {
            actions.add(McpServerActionCatalog.initializeServerSync(serverName));
        } else if (effectiveSyncPolicy.syncDue()) {
            actions.add(McpServerActionCatalog.runScheduledSync(serverName));
        }
        if (effectiveToolCounts.stale() > 0) {
            actions.add(McpServerActionCatalog.reviewStaleTools(serverName, effectiveToolCounts.stale()));
        }
        if (effectiveToolCounts.serverDisabled() > 0) {
            actions.add(McpServerActionCatalog.reviewServerDisabledTools(
                    serverName,
                    effectiveToolCounts.serverDisabled()));
        }
        return List.copyOf(actions);
    }
}
