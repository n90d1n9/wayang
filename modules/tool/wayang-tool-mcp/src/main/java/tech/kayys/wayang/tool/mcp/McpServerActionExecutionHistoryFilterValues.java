package tech.kayys.wayang.tool.mcp;

import java.util.Locale;

final class McpServerActionExecutionHistoryFilterValues {
    private McpServerActionExecutionHistoryFilterValues() {
    }

    static String normalizeActionId(String actionId) {
        return McpServerActionIdentity.normalizeActionId(actionId);
    }

    static String normalizeServerName(String serverName) {
        return McpServerActionIdentity.normalizeServerName(serverName);
    }

    static String normalizeActionCode(String actionCode) {
        return McpServerActionIdentity.normalizeActionCode(actionCode);
    }

    static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    static String normalizeExecutionMode(String executionMode) {
        return McpServerActionExecutionMode.normalize(executionMode);
    }

    static String normalizeRiskLevel(String riskLevel) {
        return McpServerActionRiskLevel.normalize(riskLevel);
    }
}
