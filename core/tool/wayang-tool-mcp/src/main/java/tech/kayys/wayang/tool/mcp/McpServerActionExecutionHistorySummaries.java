package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class McpServerActionExecutionHistorySummaries {
    private McpServerActionExecutionHistorySummaries() {
    }

    static McpServerActionExecutionHistorySummary summary(
            List<McpServerActionExecutionHistoryEntry> entries) {
        HistorySummaryAccumulator total = new HistorySummaryAccumulator(null);
        Map<String, HistorySummaryAccumulator> servers = new LinkedHashMap<>();
        Map<String, HistorySummaryAccumulator> actions = new LinkedHashMap<>();
        for (McpServerActionExecutionHistoryEntry entry : entries) {
            total.add(entry);
            servers.computeIfAbsent(McpServerActionExecutionHistorySummaryKeys.serverSummaryKey(entry.serverName()),
                    ignored -> new HistorySummaryAccumulator(entry.serverName()))
                    .add(entry);
            actions.computeIfAbsent(McpServerActionExecutionHistorySummaryKeys.actionIdentityKey(entry),
                    ignored -> HistorySummaryAccumulator.action(entry))
                    .add(entry);
        }
        return total.toSummary(
                servers.values().stream()
                        .sorted(Comparator.comparing(HistorySummaryAccumulator::sortKey))
                        .map(HistorySummaryAccumulator::toServerSummary)
                        .toList(),
                actions.values().stream()
                        .sorted(Comparator.comparing(HistorySummaryAccumulator::actionSortKey))
                        .map(HistorySummaryAccumulator::toActionSummary)
                        .toList());
    }

    private static final class HistorySummaryAccumulator {
        private final String serverName;
        private final String actionId;
        private final String actionCode;
        private int total;
        private int executed;
        private int rejected;
        private int failed;
        private int invalid;
        private int notFound;
        private int withWarnings;
        private long totalDurationMs;
        private final Map<String, Integer> executionModes = new LinkedHashMap<>();
        private final Map<String, Integer> riskLevels = new LinkedHashMap<>();
        private String latestStatus;
        private String latestReason;
        private Instant lastStartedAt;
        private Instant lastFinishedAt;

        private HistorySummaryAccumulator(String serverName) {
            this(serverName, null, null);
        }

        private HistorySummaryAccumulator(String serverName, String actionId, String actionCode) {
            this.serverName = serverName;
            this.actionId = actionId;
            this.actionCode = actionCode;
        }

        private static HistorySummaryAccumulator action(McpServerActionExecutionHistoryEntry entry) {
            return new HistorySummaryAccumulator(
                    McpServerActionExecutionHistorySummaryKeys.actionSummaryServerName(entry),
                    McpServerActionExecutionHistorySummaryKeys.actionSummaryId(entry),
                    McpServerActionExecutionHistorySummaryKeys.actionSummaryCode(entry));
        }

        private void add(McpServerActionExecutionHistoryEntry entry) {
            total++;
            addStatus(entry.status());
            if (!entry.warnings().isEmpty()) {
                withWarnings++;
            }
            count(executionModes, McpServerActionExecutionHistorySummaryKeys.executionModeKey(entry.executionMode()));
            count(riskLevels, McpServerActionExecutionHistorySummaryKeys.riskLevelKey(entry.riskLevel()));
            totalDurationMs += Math.max(0L, entry.durationMs());
            if (isLatest(entry)) {
                latestStatus = entry.status();
                latestReason = entry.reason();
                lastStartedAt = entry.startedAt();
                lastFinishedAt = entry.finishedAt();
            }
        }

        private void addStatus(String rawStatus) {
            String status = McpServerActionExecutionHistorySummaryKeys.statusKey(rawStatus);
            if (status == null) {
                return;
            }
            switch (status) {
                case McpServerActionExecutionResult.STATUS_EXECUTED -> executed++;
                case McpServerActionExecutionResult.STATUS_REJECTED -> rejected++;
                case McpServerActionExecutionResult.STATUS_FAILED -> failed++;
                case McpServerActionExecutionResult.STATUS_INVALID -> invalid++;
                case McpServerActionExecutionResult.STATUS_NOT_FOUND -> notFound++;
                default -> {
                }
            }
        }

        private boolean isLatest(McpServerActionExecutionHistoryEntry entry) {
            Instant candidate = McpServerActionExecutionHistorySummaryKeys.sortFinishedAt(entry);
            Instant current = lastFinishedAt != null ? lastFinishedAt : lastStartedAt;
            if (current == null) {
                return true;
            }
            return candidate.isAfter(current);
        }

        private String sortKey() {
            return McpServerActionExecutionHistorySummaryKeys.sortServerName(serverName);
        }

        private String actionSortKey() {
            return McpServerActionExecutionHistorySummaryKeys.sortServerName(serverName)
                    + "|"
                    + McpServerActionExecutionHistorySummaryKeys.sortActionCode(actionCode)
                    + "|"
                    + McpServerActionExecutionHistorySummaryKeys.nullSafe(actionId);
        }

        private McpServerActionExecutionHistorySummary toSummary(
                List<McpServerActionExecutionHistorySummary.ServerSummary> servers,
                List<McpServerActionExecutionHistorySummary.ActionSummary> actions) {
            return new McpServerActionExecutionHistorySummary(
                    total,
                    executed,
                    rejected,
                    failed,
                    invalid,
                    notFound,
                    withWarnings,
                    totalDurationMs,
                    executionModes,
                    riskLevels,
                    latestStatus,
                    latestReason,
                    lastStartedAt,
                    lastFinishedAt,
                    servers,
                    actions);
        }

        private McpServerActionExecutionHistorySummary.ServerSummary toServerSummary() {
            return new McpServerActionExecutionHistorySummary.ServerSummary(
                    serverName,
                    total,
                    executed,
                    rejected,
                    failed,
                    invalid,
                    notFound,
                    withWarnings,
                    totalDurationMs,
                    executionModes,
                    riskLevels,
                    latestStatus,
                    latestReason,
                    lastStartedAt,
                    lastFinishedAt);
        }

        private McpServerActionExecutionHistorySummary.ActionSummary toActionSummary() {
            return new McpServerActionExecutionHistorySummary.ActionSummary(
                    actionId,
                    serverName,
                    actionCode,
                    total,
                    executed,
                    rejected,
                    failed,
                    invalid,
                    notFound,
                    withWarnings,
                    totalDurationMs,
                    executionModes,
                    riskLevels,
                    latestStatus,
                    latestReason,
                    lastStartedAt,
                    lastFinishedAt);
        }

        private static void count(Map<String, Integer> counts, String key) {
            if (key != null) {
                counts.merge(key, 1, Integer::sum);
            }
        }
    }
}
