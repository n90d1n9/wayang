package tech.kayys.wayang.tool.mcp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record McpServerActionQueue(
        int total,
        int offset,
        Integer limit,
        int returned,
        boolean truncated,
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
        List<McpToolServerHealth.ActionQueueItem> actions,
        List<String> warnings) {

    public McpServerActionQueue {
        actionMethodCounts = copyCounts(actionMethodCounts);
        actionExecutionModeCounts = copyCounts(actionExecutionModeCounts);
        actionCounts = copyCounts(actionCounts);
        actionSeverityCounts = copyCounts(actionSeverityCounts);
        actions = actions == null ? List.of() : List.copyOf(actions);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    static McpServerActionQueue from(McpToolServerHealth health) {
        if (health == null) {
            return new McpServerActionQueue(
                    0,
                    0,
                    null,
                    0,
                    false,
                    0,
                    0,
                    0,
                    0,
                    0,
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    null,
                    List.of(),
                    List.of());
        }
        return new McpServerActionQueue(
                health.actionQueueTotal(),
                health.actionQueueOffset(),
                health.actionQueueLimit(),
                health.actionQueueReturned(),
                health.actionQueueTruncated(),
                health.recommendedActions(),
                health.automatableActions(),
                health.manualActions(),
                health.callableActions(),
                health.nonCallableActions(),
                health.actionMethodCounts(),
                health.actionExecutionModeCounts(),
                health.actionCounts(),
                health.actionSeverityCounts(),
                health.highestActionSeverity(),
                health.actionQueue(),
                health.warnings());
    }

    private static Map<String, Integer> copyCounts(Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(counts));
    }
}
