package tech.kayys.wayang.tool.mcp;

record McpServerHealthLifecycleFilters(
        Boolean hasStaleTools,
        Boolean hasServerDisabledTools,
        Boolean hasRetiredTools,
        String lifecycleState) {

    static McpServerHealthLifecycleFilters from(McpServerHealthFilters filters) {
        return new McpServerHealthLifecycleFilters(
                filters.hasStaleTools(),
                filters.hasServerDisabledTools(),
                filters.hasRetiredTools(),
                filters.lifecycleState());
    }

    boolean matches(McpToolServerHealth.ServerHealth health) {
        return matchesHasStaleTools(health)
                && matchesHasServerDisabledTools(health)
                && matchesHasRetiredTools(health)
                && matchesLifecycleState(health);
    }

    private boolean matchesHasStaleTools(McpToolServerHealth.ServerHealth health) {
        return hasStaleTools == null || (health.staleTools() > 0) == hasStaleTools;
    }

    private boolean matchesHasServerDisabledTools(McpToolServerHealth.ServerHealth health) {
        return hasServerDisabledTools == null || (health.serverDisabledTools() > 0) == hasServerDisabledTools;
    }

    private boolean matchesHasRetiredTools(McpToolServerHealth.ServerHealth health) {
        return hasRetiredTools == null || (health.retiredTools() > 0) == hasRetiredTools;
    }

    private boolean matchesLifecycleState(McpToolServerHealth.ServerHealth health) {
        return lifecycleState == null || health.lifecycleStates().getOrDefault(lifecycleState, 0) > 0;
    }
}
