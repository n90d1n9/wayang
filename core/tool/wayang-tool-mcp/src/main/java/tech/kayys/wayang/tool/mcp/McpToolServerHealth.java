package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record McpToolServerHealth(
        int totalServers,
        int enabledServers,
        int disabledServers,
        int healthyServers,
        int degradedServers,
        int unhealthyServers,
        int unsyncedServers,
        int attentionRequiredServers,
        String highestHealthStatus,
        Map<String, Integer> healthStatusCounts,
        int totalTools,
        int enabledTools,
        int disabledTools,
        int staleTools,
        int activeTools,
        int serverDisabledTools,
        int retiredTools,
        Map<String, Integer> lifecycleStates,
        Map<String, Integer> issueCounts,
        Map<String, Integer> issueSeverityCounts,
        String highestIssueSeverity,
        int recommendedActions,
        int automatableActions,
        int manualActions,
        int callableActions,
        int nonCallableActions,
        Map<String, Integer> actionMethodCounts,
        Map<String, Integer> actionExecutionModeCounts,
        Map<String, Integer> actionCounts,
        Map<String, Integer> actionSeverityCounts,
        String highestActionSeverity,
        int actionQueueTotal,
        int actionQueueOffset,
        Integer actionQueueLimit,
        int actionQueueReturned,
        boolean actionQueueTruncated,
        List<ActionQueueItem> actionQueue,
        List<ServerHealth> servers,
        List<String> warnings) {

    public McpToolServerHealth {
        lifecycleStates = McpToolLifecycleCounts.copyLifecycleStates(lifecycleStates);
        healthStatusCounts = copyCounts(healthStatusCounts);
        issueCounts = copyCounts(issueCounts);
        issueSeverityCounts = copyCounts(issueSeverityCounts);
        actionMethodCounts = copyCounts(actionMethodCounts);
        actionExecutionModeCounts = copyCounts(actionExecutionModeCounts);
        actionCounts = copyCounts(actionCounts);
        actionSeverityCounts = copyCounts(actionSeverityCounts);
        actionQueue = actionQueue == null ? List.of() : List.copyOf(actionQueue);
        servers = servers == null ? List.of() : List.copyOf(servers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

   
    public record IssueDetail(
            String code,
            String severity,
            String message) {
    }

    public record RecommendedAction(
            String code,
            String severity,
            boolean safeToAutomate,
            String message,
            String actionHint,
            ActionOperation operation,
            String executionMode) {

        public RecommendedAction {
            executionMode = McpServerActionExecutionMode.resolve(executionMode, safeToAutomate, operation != null);
        }

        public RecommendedAction(
                String code,
                String severity,
                boolean safeToAutomate,
                String message,
                String actionHint) {
            this(code, severity, safeToAutomate, message, actionHint, null);
        }

        public RecommendedAction(
                String code,
                String severity,
                boolean safeToAutomate,
                String message,
                String actionHint,
                ActionOperation operation) {
            this(code, severity, safeToAutomate, message, actionHint, operation, null);
        }
    }

    public record ActionQueueItem(
            String id,
            String serverName,
            String healthStatus,
            String transport,
            String endpoint,
            String code,
            String severity,
            int priority,
            boolean safeToAutomate,
            String message,
            String actionHint,
            ActionOperation operation,
            String executionMode) {

        public ActionQueueItem {
            executionMode = McpServerActionExecutionMode.resolve(executionMode, safeToAutomate, operation != null);
        }

        public ActionQueueItem(
                String id,
                String serverName,
                String healthStatus,
                String transport,
                String endpoint,
                String code,
                String severity,
                int priority,
                boolean safeToAutomate,
                String message,
                String actionHint) {
            this(
                    id,
                    serverName,
                    healthStatus,
                    transport,
                    endpoint,
                    code,
                    severity,
                    priority,
                    safeToAutomate,
                    message,
                    actionHint,
                    null,
                    null);
        }

        public ActionQueueItem(
                String id,
                String serverName,
                String healthStatus,
                String transport,
                String endpoint,
                String code,
                String severity,
                int priority,
                boolean safeToAutomate,
                String message,
                String actionHint,
                ActionOperation operation) {
            this(
                    id,
                    serverName,
                    healthStatus,
                    transport,
                    endpoint,
                    code,
                    severity,
                    priority,
                    safeToAutomate,
                    message,
                    actionHint,
                    operation,
                    null);
        }
    }

    public record ActionOperation(
            String method,
            String path,
            Map<String, String> queryParameters) {

        public ActionOperation {
            method = blankToNull(method);
            path = blankToNull(path);
            queryParameters = copyStringMap(queryParameters);
        }
    }

    public record ServerHealth(
            String serverName,
            String transport,
            String endpoint,
            boolean enabled,
            String syncSchedule,
            Instant lastSyncAt,
            Instant nextSyncAt,
            boolean syncDue,
            String syncScheduleError,
            String healthStatus,
            int consecutiveFailures,
            boolean attentionRequired,
            List<RecommendedAction> recommendedActions,
            List<String> issues,
            List<String> issueCodes,
            List<IssueDetail> issueDetails,
            Map<String, Integer> issueSeverityCounts,
            String highestIssueSeverity,
            String latestSyncStatus,
            String latestSyncMessage,
            int latestItemsAffected,
            long latestDurationMs,
            Instant latestStartedAt,
            Instant latestFinishedAt,
            Instant lastSuccessAt,
            Instant lastErrorAt,
            int totalTools,
            int enabledTools,
            int disabledTools,
            int staleTools,
            int activeTools,
            int serverDisabledTools,
            int retiredTools,
            Map<String, Integer> lifecycleStates) {

        public ServerHealth {
            lifecycleStates = McpToolLifecycleCounts.copyLifecycleStates(lifecycleStates);
            issues = issues == null ? List.of() : List.copyOf(issues);
            issueCodes = issueCodes == null ? List.of() : List.copyOf(issueCodes);
            issueDetails = issueDetails == null ? List.of() : List.copyOf(issueDetails);
            recommendedActions = recommendedActions == null ? List.of() : List.copyOf(recommendedActions);
            issueSeverityCounts = copyCounts(issueSeverityCounts);
        }
    }

    private static Map<String, Integer> copyCounts(Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(counts));
    }

    private static Map<String, String> copyStringMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}
