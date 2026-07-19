package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.McpServerRegistry;

record McpServerHealthFilters(
        String serverName,
        Boolean enabled,
        String healthStatus,
        Boolean syncDue,
        Boolean hasIssues,
        String issueCode,
        String issueSeverity,
        String minIssueSeverity,
        Boolean hasStaleTools,
        Boolean hasServerDisabledTools,
        Boolean hasRetiredTools,
        String lifecycleState,
        String minHealthStatus,
        Boolean attentionRequired,
        String actionCode,
        String actionSeverity,
        String minActionSeverity,
        Boolean actionSafeToAutomate,
        Integer actionQueueLimit,
        Integer actionQueueOffset,
        Boolean actionCallable,
        String actionMethod,
        String actionPath,
        String actionExecutionMode) {

    McpServerHealthFilters {
        serverName = McpServerHealthFilterValues.blankToNull(serverName);
        healthStatus = McpServerHealthStatus.normalize(healthStatus);
        issueCode = McpServerHealthFilterValues.normalizeCode(issueCode);
        issueSeverity = McpIssueSeverity.normalize(issueSeverity);
        minIssueSeverity = McpIssueSeverity.normalize(minIssueSeverity);
        lifecycleState = McpServerHealthFilterValues.normalizeLifecycleState(lifecycleState);
        minHealthStatus = McpServerHealthStatus.normalize(minHealthStatus);
        actionCode = McpServerActionIdentity.normalizeActionCode(actionCode);
        actionSeverity = McpIssueSeverity.normalize(actionSeverity);
        minActionSeverity = McpIssueSeverity.normalize(minActionSeverity);
        actionQueueLimit = McpServerHealthFilterValues.normalizeLimit(actionQueueLimit);
        actionQueueOffset = McpServerHealthFilterValues.normalizeOffset(actionQueueOffset);
        actionMethod = McpServerHealthFilterValues.normalizeMethod(actionMethod);
        actionPath = McpServerHealthFilterValues.blankToNull(actionPath);
        actionExecutionMode = McpServerActionExecutionMode.normalize(actionExecutionMode);
    }

    static McpServerHealthFilterBuilder builder() {
        return new McpServerHealthFilterBuilder();
    }

    static McpServerHealthFilters byServerName(String serverName) {
        return builder()
                .withServerName(serverName)
                .build();
    }

    static McpServerHealthFilters byActionCode(String actionCode) {
        return builder()
                .withActionCode(actionCode)
                .build();
    }

    static McpServerHealthFilters byServerAction(String serverName, String actionCode) {
        return builder()
                .withServerName(serverName)
                .withActionCode(actionCode)
                .build();
    }

    static McpServerHealthFilters byActionSafeToAutomate(boolean actionSafeToAutomate) {
        return builder()
                .withActionSafeToAutomate(actionSafeToAutomate)
                .build();
    }

    static McpServerHealthFilters byMinActionSeverity(String minActionSeverity) {
        return builder()
                .withMinActionSeverity(minActionSeverity)
                .build();
    }

    static McpServerHealthFilters byActionQueueLimit(int actionQueueLimit) {
        return builder()
                .withActionQueueLimit(actionQueueLimit)
                .build();
    }

    static McpServerHealthFilters byActionQueueWindow(int actionQueueOffset, int actionQueueLimit) {
        return builder()
                .withActionQueueWindow(actionQueueOffset, actionQueueLimit)
                .build();
    }

    static McpServerHealthFilters byActionCallable(boolean actionCallable) {
        return builder()
                .withActionCallable(actionCallable)
                .build();
    }

    static McpServerHealthFilters byActionMethod(String actionMethod) {
        return builder()
                .withActionMethod(actionMethod)
                .build();
    }

    static McpServerHealthFilters byActionExecutionMode(String actionExecutionMode) {
        return builder()
                .withActionExecutionMode(actionExecutionMode)
                .build();
    }

    boolean matchesServer(McpServerRegistry server) {
        if (server == null) {
            return false;
        }
        return matchesServerName(server) && matchesEnabled(server);
    }

    boolean matchesHealth(McpToolServerHealth.ServerHealth health) {
        if (health == null) {
            return false;
        }
        return matchesHealthStatus(health)
                && matchesMinHealthStatus(health)
                && matchesSyncDue(health)
                && McpServerHealthIssueFilters.from(this).matches(health)
                && McpServerHealthLifecycleFilters.from(this).matches(health)
                && matchesAttentionRequired(health)
                && McpServerHealthActionFilters.from(this).matches(health);
    }

    private boolean matchesServerName(McpServerRegistry server) {
        return serverName == null
                || (server.getName() != null && server.getName().equalsIgnoreCase(serverName));
    }

    private boolean matchesEnabled(McpServerRegistry server) {
        return enabled == null || server.isEnabled() == enabled;
    }

    private boolean matchesHealthStatus(McpToolServerHealth.ServerHealth health) {
        return healthStatus == null || healthStatus.equals(McpServerHealthStatus.normalize(health.healthStatus()));
    }

    private boolean matchesMinHealthStatus(McpToolServerHealth.ServerHealth health) {
        if (minHealthStatus == null) {
            return true;
        }
        int requiredRank = McpServerHealthStatus.rank(minHealthStatus);
        return requiredRank > 0
                && McpServerHealthStatus.rank(health.healthStatus()) >= requiredRank;
    }

    private boolean matchesSyncDue(McpToolServerHealth.ServerHealth health) {
        return syncDue == null || health.syncDue() == syncDue;
    }

    private boolean matchesAttentionRequired(McpToolServerHealth.ServerHealth health) {
        return attentionRequired == null || health.attentionRequired() == attentionRequired;
    }
}
