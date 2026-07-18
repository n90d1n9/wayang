package tech.kayys.wayang.tool.mcp;

record McpServerHealthActionFilters(
        String actionCode,
        String actionSeverity,
        String minActionSeverity,
        Boolean actionSafeToAutomate,
        Boolean actionCallable,
        String actionMethod,
        String actionPath,
        String actionExecutionMode) {

    static McpServerHealthActionFilters from(McpServerHealthFilters filters) {
        return new McpServerHealthActionFilters(
                filters.actionCode(),
                filters.actionSeverity(),
                filters.minActionSeverity(),
                filters.actionSafeToAutomate(),
                filters.actionCallable(),
                filters.actionMethod(),
                filters.actionPath(),
                filters.actionExecutionMode());
    }

    boolean matches(McpToolServerHealth.ServerHealth health) {
        return matchesActionCode(health)
                && matchesActionSeverity(health)
                && matchesMinActionSeverity(health)
                && matchesActionSafeToAutomate(health)
                && matchesActionCallable(health)
                && matchesActionMethod(health)
                && matchesActionPath(health)
                && matchesActionExecutionMode(health);
    }

    private boolean matchesActionCode(McpToolServerHealth.ServerHealth health) {
        return actionCode == null || health.recommendedActions().stream()
                .anyMatch(action -> actionCode.equals(McpServerActionIdentity.normalizeActionCode(action.code())));
    }

    private boolean matchesActionSeverity(McpToolServerHealth.ServerHealth health) {
        return actionSeverity == null || health.recommendedActions().stream()
                .anyMatch(action -> actionSeverity.equals(McpIssueSeverity.normalize(action.severity())));
    }

    private boolean matchesMinActionSeverity(McpToolServerHealth.ServerHealth health) {
        if (minActionSeverity == null) {
            return true;
        }
        int requiredRank = McpIssueSeverity.rank(minActionSeverity);
        return requiredRank > 0 && health.recommendedActions().stream()
                .anyMatch(action -> McpIssueSeverity.rank(action.severity()) >= requiredRank);
    }

    private boolean matchesActionSafeToAutomate(McpToolServerHealth.ServerHealth health) {
        return actionSafeToAutomate == null || health.recommendedActions().stream()
                .anyMatch(action -> action.safeToAutomate() == actionSafeToAutomate);
    }

    private boolean matchesActionCallable(McpToolServerHealth.ServerHealth health) {
        return actionCallable == null || health.recommendedActions().stream()
                .anyMatch(action -> (action.operation() != null) == actionCallable);
    }

    private boolean matchesActionMethod(McpToolServerHealth.ServerHealth health) {
        return actionMethod == null || health.recommendedActions().stream()
                .map(McpToolServerHealth.RecommendedAction::operation)
                .anyMatch(operation -> operation != null
                        && actionMethod.equals(McpServerHealthFilterValues.normalizeMethod(operation.method())));
    }

    private boolean matchesActionPath(McpToolServerHealth.ServerHealth health) {
        return actionPath == null || health.recommendedActions().stream()
                .map(McpToolServerHealth.RecommendedAction::operation)
                .anyMatch(operation -> operation != null && actionPath.equals(operation.path()));
    }

    private boolean matchesActionExecutionMode(McpToolServerHealth.ServerHealth health) {
        return actionExecutionMode == null || health.recommendedActions().stream()
                .anyMatch(action -> actionExecutionMode.equals(
                        McpServerActionExecutionMode.normalize(action.executionMode())));
    }

}
