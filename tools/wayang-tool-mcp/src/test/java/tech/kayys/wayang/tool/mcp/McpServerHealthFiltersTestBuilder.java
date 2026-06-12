package tech.kayys.wayang.tool.mcp;

final class McpServerHealthFiltersTestBuilder {
    private final McpServerHealthFilterBuilder delegate = McpServerHealthFilters.builder();

    private McpServerHealthFiltersTestBuilder() {
    }

    static McpServerHealthFiltersTestBuilder filters() {
        return new McpServerHealthFiltersTestBuilder();
    }

    McpServerHealthFiltersTestBuilder withServerName(String serverName) {
        delegate.withServerName(serverName);
        return this;
    }

    McpServerHealthFiltersTestBuilder withEnabled(Boolean enabled) {
        delegate.withEnabled(enabled);
        return this;
    }

    McpServerHealthFiltersTestBuilder withHealthStatus(String healthStatus) {
        delegate.withHealthStatus(healthStatus);
        return this;
    }

    McpServerHealthFiltersTestBuilder withSyncDue(Boolean syncDue) {
        delegate.withSyncDue(syncDue);
        return this;
    }

    McpServerHealthFiltersTestBuilder withIssues(Boolean hasIssues) {
        delegate.withIssues(hasIssues);
        return this;
    }

    McpServerHealthFiltersTestBuilder withIssueCode(String issueCode) {
        delegate.withIssueCode(issueCode);
        return this;
    }

    McpServerHealthFiltersTestBuilder withIssueSeverity(String issueSeverity) {
        delegate.withIssueSeverity(issueSeverity);
        return this;
    }

    McpServerHealthFiltersTestBuilder withMinIssueSeverity(String minIssueSeverity) {
        delegate.withMinIssueSeverity(minIssueSeverity);
        return this;
    }

    McpServerHealthFiltersTestBuilder withStaleTools(Boolean hasStaleTools) {
        delegate.withStaleTools(hasStaleTools);
        return this;
    }

    McpServerHealthFiltersTestBuilder withServerDisabledTools(Boolean hasServerDisabledTools) {
        delegate.withServerDisabledTools(hasServerDisabledTools);
        return this;
    }

    McpServerHealthFiltersTestBuilder withRetiredTools(Boolean hasRetiredTools) {
        delegate.withRetiredTools(hasRetiredTools);
        return this;
    }

    McpServerHealthFiltersTestBuilder withLifecycleState(String lifecycleState) {
        delegate.withLifecycleState(lifecycleState);
        return this;
    }

    McpServerHealthFiltersTestBuilder withMinHealthStatus(String minHealthStatus) {
        delegate.withMinHealthStatus(minHealthStatus);
        return this;
    }

    McpServerHealthFiltersTestBuilder withAttentionRequired(Boolean attentionRequired) {
        delegate.withAttentionRequired(attentionRequired);
        return this;
    }

    McpServerHealthFiltersTestBuilder withActionCode(String actionCode) {
        delegate.withActionCode(actionCode);
        return this;
    }

    McpServerHealthFiltersTestBuilder withActionSeverity(String actionSeverity) {
        delegate.withActionSeverity(actionSeverity);
        return this;
    }

    McpServerHealthFiltersTestBuilder withMinActionSeverity(String minActionSeverity) {
        delegate.withMinActionSeverity(minActionSeverity);
        return this;
    }

    McpServerHealthFiltersTestBuilder withActionSafeToAutomate(Boolean actionSafeToAutomate) {
        delegate.withActionSafeToAutomate(actionSafeToAutomate);
        return this;
    }

    McpServerHealthFiltersTestBuilder withActionQueueLimit(Integer actionQueueLimit) {
        delegate.withActionQueueLimit(actionQueueLimit);
        return this;
    }

    McpServerHealthFiltersTestBuilder withActionQueueWindow(Integer actionQueueOffset, Integer actionQueueLimit) {
        delegate.withActionQueueWindow(actionQueueOffset, actionQueueLimit);
        return this;
    }

    McpServerHealthFiltersTestBuilder withActionCallable(Boolean actionCallable) {
        delegate.withActionCallable(actionCallable);
        return this;
    }

    McpServerHealthFiltersTestBuilder withActionMethod(String actionMethod) {
        delegate.withActionMethod(actionMethod);
        return this;
    }

    McpServerHealthFiltersTestBuilder withActionPath(String actionPath) {
        delegate.withActionPath(actionPath);
        return this;
    }

    McpServerHealthFiltersTestBuilder withActionExecutionMode(String actionExecutionMode) {
        delegate.withActionExecutionMode(actionExecutionMode);
        return this;
    }

    McpServerHealthFilters build() {
        return delegate.build();
    }
}
