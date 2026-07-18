package tech.kayys.wayang.tool.mcp;

final class McpServerHealthFilterBuilder {
    private String serverName;
    private Boolean enabled;
    private String healthStatus;
    private Boolean syncDue;
    private Boolean hasIssues;
    private String issueCode;
    private String issueSeverity;
    private String minIssueSeverity;
    private Boolean hasStaleTools;
    private Boolean hasServerDisabledTools;
    private Boolean hasRetiredTools;
    private String lifecycleState;
    private String minHealthStatus;
    private Boolean attentionRequired;
    private String actionCode;
    private String actionSeverity;
    private String minActionSeverity;
    private Boolean actionSafeToAutomate;
    private Integer actionQueueLimit;
    private Integer actionQueueOffset;
    private Boolean actionCallable;
    private String actionMethod;
    private String actionPath;
    private String actionExecutionMode;

    McpServerHealthFilterBuilder withServerName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    McpServerHealthFilterBuilder withEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    McpServerHealthFilterBuilder withHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
        return this;
    }

    McpServerHealthFilterBuilder withSyncDue(Boolean syncDue) {
        this.syncDue = syncDue;
        return this;
    }

    McpServerHealthFilterBuilder withIssues(Boolean hasIssues) {
        this.hasIssues = hasIssues;
        return this;
    }

    McpServerHealthFilterBuilder withIssueCode(String issueCode) {
        this.issueCode = issueCode;
        return this;
    }

    McpServerHealthFilterBuilder withIssueSeverity(String issueSeverity) {
        this.issueSeverity = issueSeverity;
        return this;
    }

    McpServerHealthFilterBuilder withMinIssueSeverity(String minIssueSeverity) {
        this.minIssueSeverity = minIssueSeverity;
        return this;
    }

    McpServerHealthFilterBuilder withStaleTools(Boolean hasStaleTools) {
        this.hasStaleTools = hasStaleTools;
        return this;
    }

    McpServerHealthFilterBuilder withServerDisabledTools(Boolean hasServerDisabledTools) {
        this.hasServerDisabledTools = hasServerDisabledTools;
        return this;
    }

    McpServerHealthFilterBuilder withRetiredTools(Boolean hasRetiredTools) {
        this.hasRetiredTools = hasRetiredTools;
        return this;
    }

    McpServerHealthFilterBuilder withLifecycleState(String lifecycleState) {
        this.lifecycleState = lifecycleState;
        return this;
    }

    McpServerHealthFilterBuilder withMinHealthStatus(String minHealthStatus) {
        this.minHealthStatus = minHealthStatus;
        return this;
    }

    McpServerHealthFilterBuilder withAttentionRequired(Boolean attentionRequired) {
        this.attentionRequired = attentionRequired;
        return this;
    }

    McpServerHealthFilterBuilder withActionCode(String actionCode) {
        this.actionCode = actionCode;
        return this;
    }

    McpServerHealthFilterBuilder withActionSeverity(String actionSeverity) {
        this.actionSeverity = actionSeverity;
        return this;
    }

    McpServerHealthFilterBuilder withMinActionSeverity(String minActionSeverity) {
        this.minActionSeverity = minActionSeverity;
        return this;
    }

    McpServerHealthFilterBuilder withActionSafeToAutomate(Boolean actionSafeToAutomate) {
        this.actionSafeToAutomate = actionSafeToAutomate;
        return this;
    }

    McpServerHealthFilterBuilder withActionQueueLimit(Integer actionQueueLimit) {
        this.actionQueueLimit = actionQueueLimit;
        return this;
    }

    McpServerHealthFilterBuilder withActionQueueOffset(Integer actionQueueOffset) {
        this.actionQueueOffset = actionQueueOffset;
        return this;
    }

    McpServerHealthFilterBuilder withActionQueueWindow(Integer actionQueueOffset, Integer actionQueueLimit) {
        this.actionQueueOffset = actionQueueOffset;
        this.actionQueueLimit = actionQueueLimit;
        return this;
    }

    McpServerHealthFilterBuilder withActionCallable(Boolean actionCallable) {
        this.actionCallable = actionCallable;
        return this;
    }

    McpServerHealthFilterBuilder withActionMethod(String actionMethod) {
        this.actionMethod = actionMethod;
        return this;
    }

    McpServerHealthFilterBuilder withActionPath(String actionPath) {
        this.actionPath = actionPath;
        return this;
    }

    McpServerHealthFilterBuilder withActionExecutionMode(String actionExecutionMode) {
        this.actionExecutionMode = actionExecutionMode;
        return this;
    }

    McpServerHealthFilters build() {
        return new McpServerHealthFilters(
                serverName,
                enabled,
                healthStatus,
                syncDue,
                hasIssues,
                issueCode,
                issueSeverity,
                minIssueSeverity,
                hasStaleTools,
                hasServerDisabledTools,
                hasRetiredTools,
                lifecycleState,
                minHealthStatus,
                attentionRequired,
                actionCode,
                actionSeverity,
                minActionSeverity,
                actionSafeToAutomate,
                actionQueueLimit,
                actionQueueOffset,
                actionCallable,
                actionMethod,
                actionPath,
                actionExecutionMode);
    }
}
