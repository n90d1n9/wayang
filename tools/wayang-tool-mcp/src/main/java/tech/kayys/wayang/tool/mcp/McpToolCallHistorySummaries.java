package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class McpToolCallHistorySummaries {
    private McpToolCallHistorySummaries() {
    }

    static McpToolCallHistorySummary summary(List<McpToolCallHistoryEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return new McpToolCallHistorySummary(
                    0,
                    0,
                    0,
                    0,
                    0,
                    null,
                    null,
                    Map.of(),
                    Map.of(),
                    Map.of());
        }
        int succeeded = 0;
        long totalDurationMs = 0;
        long totalMcpDurationMs = 0;
        Instant oldestFinishedAt = null;
        Instant newestFinishedAt = null;
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        Map<String, Integer> byFailureType = new LinkedHashMap<>();
        Map<String, Integer> byToolId = new LinkedHashMap<>();

        for (McpToolCallHistoryEntry entry : entries) {
            if (entry.success()) {
                succeeded++;
            }
            totalDurationMs += Math.max(0, entry.durationMs());
            totalMcpDurationMs += Math.max(0, entry.mcpDurationMs());
            oldestFinishedAt = min(oldestFinishedAt, entry.finishedAt());
            newestFinishedAt = max(newestFinishedAt, entry.finishedAt());
            count(byStatus, McpToolCallHistorySummaryKeys.defaultKey(entry.status()));
            count(byToolId, McpToolCallHistorySummaryKeys.defaultKey(entry.toolId()));
            if (entry.failureType() != null && !entry.failureType().isBlank()) {
                count(byFailureType, entry.failureType());
            }
        }

        int total = entries.size();
        return new McpToolCallHistorySummary(
                total,
                succeeded,
                total - succeeded,
                totalDurationMs / total,
                totalMcpDurationMs / total,
                oldestFinishedAt,
                newestFinishedAt,
                byStatus,
                byFailureType,
                byToolId);
    }

    static McpToolCallHistoryToolSummaries toolSummaries(List<McpToolCallHistoryEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return new McpToolCallHistoryToolSummaries(
                    0,
                    0,
                    List.of(),
                    Instant.now());
        }
        Map<String, ToolSummaryAccumulator> summaries = new LinkedHashMap<>();
        for (McpToolCallHistoryEntry entry : entries) {
            String key = McpToolCallHistorySummaryKeys.defaultKey(entry.toolId());
            summaries.computeIfAbsent(key, ignored -> new ToolSummaryAccumulator(key))
                    .add(entry);
        }
        List<McpToolCallHistoryToolSummary> tools = summaries.values().stream()
                .map(ToolSummaryAccumulator::toSummary)
                .toList();
        return new McpToolCallHistoryToolSummaries(
                tools.size(),
                entries.size(),
                tools,
                Instant.now());
    }

    static McpToolCallHistoryFailureSummaries failureSummaries(List<McpToolCallHistoryEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return new McpToolCallHistoryFailureSummaries(
                    0,
                    0,
                    List.of(),
                    Instant.now());
        }
        Map<String, FailureSummaryAccumulator> summaries = new LinkedHashMap<>();
        int totalFailures = 0;
        for (McpToolCallHistoryEntry entry : entries) {
            if (entry == null || entry.success()) {
                continue;
            }
            totalFailures++;
            String key = McpToolCallHistorySummaryKeys.defaultKey(entry.failureType());
            summaries.computeIfAbsent(key, ignored -> new FailureSummaryAccumulator(key))
                    .add(entry);
        }
        List<McpToolCallHistoryFailureSummary> failureTypes = summaries.values().stream()
                .map(FailureSummaryAccumulator::toSummary)
                .toList();
        return new McpToolCallHistoryFailureSummaries(
                failureTypes.size(),
                totalFailures,
                failureTypes,
                Instant.now());
    }

    private static Instant min(Instant left, Instant right) {
        if (right == null) {
            return left;
        }
        if (left == null || right.isBefore(left)) {
            return right;
        }
        return left;
    }

    private static Instant max(Instant left, Instant right) {
        if (right == null) {
            return left;
        }
        if (left == null || right.isAfter(left)) {
            return right;
        }
        return left;
    }

    private static void count(Map<String, Integer> counts, String key) {
        counts.merge(key, 1, Integer::sum);
    }

    private static final class ToolSummaryAccumulator {

        private final String toolId;
        private final Map<String, Integer> byStatus = new LinkedHashMap<>();
        private final Map<String, Integer> byFailureType = new LinkedHashMap<>();
        private int total;
        private int succeeded;
        private long totalDurationMs;
        private long totalMcpDurationMs;
        private String latestStatus;
        private boolean latestSuccess;
        private String latestFailureType;
        private String latestError;
        private Instant oldestFinishedAt;
        private Instant newestFinishedAt;

        private ToolSummaryAccumulator(String toolId) {
            this.toolId = toolId;
        }

        private void add(McpToolCallHistoryEntry entry) {
            if (entry == null) {
                return;
            }
            if (newestFinishedAt == null) {
                latestStatus = entry.status();
                latestSuccess = entry.success();
                latestFailureType = entry.failureType();
                latestError = entry.error();
            }
            total++;
            if (entry.success()) {
                succeeded++;
            }
            totalDurationMs += Math.max(0, entry.durationMs());
            totalMcpDurationMs += Math.max(0, entry.mcpDurationMs());
            oldestFinishedAt = min(oldestFinishedAt, entry.finishedAt());
            newestFinishedAt = max(newestFinishedAt, entry.finishedAt());
            count(byStatus, McpToolCallHistorySummaryKeys.defaultKey(entry.status()));
            if (entry.failureType() != null && !entry.failureType().isBlank()) {
                count(byFailureType, entry.failureType());
            }
        }

        private McpToolCallHistoryToolSummary toSummary() {
            return new McpToolCallHistoryToolSummary(
                    toolId,
                    total,
                    succeeded,
                    total - succeeded,
                    total == 0 ? 0 : totalDurationMs / total,
                    total == 0 ? 0 : totalMcpDurationMs / total,
                    latestStatus,
                    latestSuccess,
                    latestFailureType,
                    latestError,
                    oldestFinishedAt,
                    newestFinishedAt,
                    byStatus,
                    byFailureType);
        }
    }

    private static final class FailureSummaryAccumulator {

        private final String failureType;
        private final Map<String, Integer> byToolId = new LinkedHashMap<>();
        private int total;
        private String latestToolId;
        private String latestError;
        private Instant oldestFinishedAt;
        private Instant newestFinishedAt;

        private FailureSummaryAccumulator(String failureType) {
            this.failureType = failureType;
        }

        private void add(McpToolCallHistoryEntry entry) {
            if (entry == null) {
                return;
            }
            if (newestFinishedAt == null) {
                latestToolId = entry.toolId();
                latestError = entry.error();
            }
            total++;
            oldestFinishedAt = min(oldestFinishedAt, entry.finishedAt());
            newestFinishedAt = max(newestFinishedAt, entry.finishedAt());
            count(byToolId, McpToolCallHistorySummaryKeys.defaultKey(entry.toolId()));
        }

        private McpToolCallHistoryFailureSummary toSummary() {
            return new McpToolCallHistoryFailureSummary(
                    failureType,
                    total,
                    latestToolId,
                    latestError,
                    oldestFinishedAt,
                    newestFinishedAt,
                    byToolId);
        }
    }
}
