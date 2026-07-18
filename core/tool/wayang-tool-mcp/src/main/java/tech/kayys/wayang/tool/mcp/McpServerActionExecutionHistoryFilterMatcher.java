package tech.kayys.wayang.tool.mcp;

record McpServerActionExecutionHistoryFilterMatcher(
        McpServerActionExecutionHistoryFilters filters) {

    static McpServerActionExecutionHistoryFilterMatcher from(
            McpServerActionExecutionHistoryFilters filters) {
        return new McpServerActionExecutionHistoryFilterMatcher(filters);
    }

    boolean matches(McpServerActionExecutionHistoryEntry entry) {
        if (entry == null) {
            return false;
        }
        return matchesActionId(entry)
                && matchesServerName(entry)
                && matchesActionCode(entry)
                && matchesStatus(entry)
                && matchesExecuted(entry)
                && matchesExecutionMode(entry)
                && matchesRiskLevel(entry)
                && matchesWarnings(entry)
                && matchesStartedAt(entry)
                && matchesFinishedAt(entry);
    }

    private boolean matchesActionId(McpServerActionExecutionHistoryEntry entry) {
        String actionId = filters.actionId();
        if (actionId == null) {
            return true;
        }
        if (actionId.equals(McpServerActionExecutionHistoryFilterValues.normalizeActionId(entry.actionId()))) {
            return true;
        }
        String serverName = McpServerActionExecutionHistoryFilterValues.normalizeServerName(entry.serverName());
        String actionCode = McpServerActionExecutionHistoryFilterValues.normalizeActionCode(entry.actionCode());
        return serverName != null
                && actionCode != null
                && actionId.equals(serverName + ":" + actionCode);
    }

    private boolean matchesServerName(McpServerActionExecutionHistoryEntry entry) {
        String serverName = filters.serverName();
        return serverName == null
                || (entry.serverName() != null
                        && serverName.equals(
                                McpServerActionExecutionHistoryFilterValues.normalizeServerName(entry.serverName())));
    }

    private boolean matchesActionCode(McpServerActionExecutionHistoryEntry entry) {
        String actionCode = filters.actionCode();
        return actionCode == null
                || actionCode.equals(
                        McpServerActionExecutionHistoryFilterValues.normalizeActionCode(entry.actionCode()));
    }

    private boolean matchesStatus(McpServerActionExecutionHistoryEntry entry) {
        String status = filters.status();
        return status == null
                || status.equals(McpServerActionExecutionHistoryFilterValues.normalizeStatus(entry.status()));
    }

    private boolean matchesExecuted(McpServerActionExecutionHistoryEntry entry) {
        Boolean executed = filters.executed();
        return executed == null || executed.equals(entry.executed());
    }

    private boolean matchesExecutionMode(McpServerActionExecutionHistoryEntry entry) {
        String executionMode = filters.executionMode();
        return executionMode == null
                || executionMode.equals(
                        McpServerActionExecutionHistoryFilterValues.normalizeExecutionMode(entry.executionMode()));
    }

    private boolean matchesRiskLevel(McpServerActionExecutionHistoryEntry entry) {
        String riskLevel = filters.riskLevel();
        return riskLevel == null
                || riskLevel.equals(
                        McpServerActionExecutionHistoryFilterValues.normalizeRiskLevel(entry.riskLevel()));
    }

    private boolean matchesWarnings(McpServerActionExecutionHistoryEntry entry) {
        Boolean hasWarnings = filters.hasWarnings();
        return hasWarnings == null
                || hasWarnings.equals(entry.warnings() != null && !entry.warnings().isEmpty());
    }

    private boolean matchesStartedAt(McpServerActionExecutionHistoryEntry entry) {
        return McpHistoryFilterSupport.matchesInstantRange(
                entry.startedAt(),
                filters.startedAtFrom(),
                filters.startedAtTo());
    }

    private boolean matchesFinishedAt(McpServerActionExecutionHistoryEntry entry) {
        return McpHistoryFilterSupport.matchesInstantRange(
                entry.finishedAt(),
                filters.finishedAtFrom(),
                filters.finishedAtTo());
    }
}
