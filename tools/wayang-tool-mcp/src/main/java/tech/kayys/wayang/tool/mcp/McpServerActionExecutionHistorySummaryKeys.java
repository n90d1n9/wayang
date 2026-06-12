package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.Locale;

final class McpServerActionExecutionHistorySummaryKeys {
    private McpServerActionExecutionHistorySummaryKeys() {
    }

    static String statusKey(String status) {
        return McpServerActionExecutionHistoryFilterValues.normalizeStatus(status);
    }

    static String executionModeKey(String executionMode) {
        return McpServerActionExecutionHistoryFilterValues.normalizeExecutionMode(executionMode);
    }

    static String riskLevelKey(String riskLevel) {
        return McpServerActionExecutionHistoryFilterValues.normalizeRiskLevel(riskLevel);
    }

    static String serverSummaryKey(String serverName) {
        String normalized = McpServerActionExecutionHistoryFilterValues.normalizeServerName(serverName);
        return normalized == null ? "" : normalized;
    }

    static String actionIdentityKey(McpServerActionExecutionHistoryEntry entry) {
        String serverName = McpServerActionExecutionHistoryFilterValues.normalizeServerName(entry.serverName());
        String actionCode = McpServerActionExecutionHistoryFilterValues.normalizeActionCode(entry.actionCode());
        if (serverName != null || actionCode != null) {
            return nullSafe(serverName) + "|" + nullSafe(actionCode);
        }
        return nullSafe(entry.actionId()).toLowerCase(Locale.ROOT);
    }

    static String actionSummaryId(McpServerActionExecutionHistoryEntry entry) {
        String serverName = McpServerActionExecutionHistoryFilterValues.normalizeServerName(entry.serverName());
        String actionCode = McpServerActionExecutionHistoryFilterValues.normalizeActionCode(entry.actionCode());
        if (serverName != null && actionCode != null) {
            return serverName + ":" + actionCode;
        }
        return nullSafe(McpServerActionExecutionHistoryFilterValues.normalizeActionId(entry.actionId()));
    }

    static String actionSummaryServerName(McpServerActionExecutionHistoryEntry entry) {
        String serverName = McpServerActionExecutionHistoryFilterValues.normalizeServerName(entry.serverName());
        if (serverName != null) {
            return serverName;
        }
        String actionId = McpServerActionExecutionHistoryFilterValues.normalizeActionId(entry.actionId());
        int separator = actionId == null ? -1 : actionId.lastIndexOf(':');
        return separator > 0 ? actionId.substring(0, separator) : null;
    }

    static String actionSummaryCode(McpServerActionExecutionHistoryEntry entry) {
        String actionCode = McpServerActionExecutionHistoryFilterValues.normalizeActionCode(entry.actionCode());
        if (actionCode != null) {
            return actionCode;
        }
        String actionId = McpServerActionExecutionHistoryFilterValues.normalizeActionId(entry.actionId());
        int separator = actionId == null ? -1 : actionId.lastIndexOf(':');
        return separator > 0 && separator < actionId.length() - 1
                ? actionId.substring(separator + 1)
                : null;
    }

    static Instant sortFinishedAt(McpServerActionExecutionHistoryEntry entry) {
        if (entry.finishedAt() != null) {
            return entry.finishedAt();
        }
        return entry.startedAt() == null ? Instant.EPOCH : entry.startedAt();
    }

    static String sortServerName(String serverName) {
        String normalized = McpServerActionExecutionHistoryFilterValues.normalizeServerName(serverName);
        return normalized == null ? "" : normalized;
    }

    static String sortActionCode(String actionCode) {
        String normalized = McpServerActionExecutionHistoryFilterValues.normalizeActionCode(actionCode);
        return normalized == null ? "" : normalized;
    }

    static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
