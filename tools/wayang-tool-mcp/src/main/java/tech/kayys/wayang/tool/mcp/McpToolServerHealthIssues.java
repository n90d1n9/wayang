package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.McpServerRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class McpToolServerHealthIssues {

    static final String ISSUE_CONSECUTIVE_SYNC_FAILURES = "CONSECUTIVE_SYNC_FAILURES";
    static final String ISSUE_INVALID_SYNC_SCHEDULE = "INVALID_SYNC_SCHEDULE";
    static final String ISSUE_SERVER_DISABLED = "SERVER_DISABLED";
    static final String ISSUE_SERVER_DISABLED_TOOLS = "SERVER_DISABLED_TOOLS";
    static final String ISSUE_STALE_TOOLS = "STALE_TOOLS";
    static final String ISSUE_SYNC_ERROR = "SYNC_ERROR";
    static final String ISSUE_UNSYNCED = "UNSYNCED";

    private McpToolServerHealthIssues() {
    }

    static Result forServer(
            McpServerRegistry server,
            McpToolDiscoverySyncHistoryEntry latest,
            McpToolLifecycleCounts toolCounts,
            McpToolServerHealthSyncPolicyStatus syncPolicy,
            int consecutiveFailures) {
        List<String> messages = new ArrayList<>();
        List<String> codes = new ArrayList<>();
        List<McpToolServerHealth.IssueDetail> details = new ArrayList<>();
        McpToolLifecycleCounts effectiveToolCounts = toolCounts == null
                ? new McpToolLifecycleCounts()
                : toolCounts;
        McpToolServerHealthSyncPolicyStatus effectiveSyncPolicy = syncPolicy == null
                ? new McpToolServerHealthSyncPolicyStatus(null, false, null)
                : syncPolicy;
        boolean enabled = server == null || server.isEnabled();
        if (!enabled) {
            add(messages, codes, details, ISSUE_SERVER_DISABLED, "Server is disabled.");
        }
        if (latest == null && enabled) {
            add(messages, codes, details, ISSUE_UNSYNCED, "Server has no MCP sync history.");
        }
        if (latest != null && McpToolDiscoverySyncStatuses.isError(latest.status())) {
            add(messages, codes, details, ISSUE_SYNC_ERROR, "Latest sync failed: " + latest.message());
        }
        if (consecutiveFailures > 1) {
            add(
                    messages,
                    codes,
                    details,
                    ISSUE_CONSECUTIVE_SYNC_FAILURES,
                    "Server has " + consecutiveFailures + " consecutive sync failures.");
        }
        if (effectiveSyncPolicy.error() != null) {
            add(
                    messages,
                    codes,
                    details,
                    ISSUE_INVALID_SYNC_SCHEDULE,
                    "Sync schedule is invalid: " + effectiveSyncPolicy.error());
        }
        if (effectiveToolCounts.stale() > 0) {
            add(
                    messages,
                    codes,
                    details,
                    ISSUE_STALE_TOOLS,
                    effectiveToolCounts.stale() + " stale MCP tool(s) need review.");
        }
        if (effectiveToolCounts.serverDisabled() > 0) {
            add(
                    messages,
                    codes,
                    details,
                    ISSUE_SERVER_DISABLED_TOOLS,
                    effectiveToolCounts.serverDisabled() + " server-disabled MCP tool(s) need review.");
        }
        return new Result(List.copyOf(messages), List.copyOf(codes), List.copyOf(details));
    }

    static String severity(String issueCode) {
        return switch (issueCode) {
            case ISSUE_SYNC_ERROR, ISSUE_CONSECUTIVE_SYNC_FAILURES -> McpIssueSeverity.CRITICAL;
            case ISSUE_SERVER_DISABLED -> McpIssueSeverity.INFO;
            default -> McpIssueSeverity.WARNING;
        };
    }

    static Map<String, Integer> severityCounts(List<McpToolServerHealth.IssueDetail> issueDetails) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (issueDetails == null) {
            return counts;
        }
        for (McpToolServerHealth.IssueDetail detail : issueDetails) {
            counts.merge(detail.severity(), 1, Integer::sum);
        }
        return counts;
    }

    private static void add(
            List<String> messages,
            List<String> codes,
            List<McpToolServerHealth.IssueDetail> details,
            String code,
            String message) {
        messages.add(message);
        codes.add(code);
        details.add(new McpToolServerHealth.IssueDetail(code, severity(code), message));
    }

    record Result(
            List<String> messages,
            List<String> codes,
            List<McpToolServerHealth.IssueDetail> details) {
    }
}
