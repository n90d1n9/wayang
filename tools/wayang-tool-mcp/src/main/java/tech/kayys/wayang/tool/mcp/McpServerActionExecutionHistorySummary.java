package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record McpServerActionExecutionHistorySummary(
        int total,
        int executed,
        int rejected,
        int failed,
        int invalid,
        int notFound,
        int withWarnings,
        long totalDurationMs,
        Map<String, Integer> executionModes,
        Map<String, Integer> riskLevels,
        String latestStatus,
        String latestReason,
        Instant lastStartedAt,
        Instant lastFinishedAt,
        List<ServerSummary> servers,
        List<ActionSummary> actions) {

    public McpServerActionExecutionHistorySummary {
        executionModes = executionModes == null ? Map.of() : Map.copyOf(executionModes);
        riskLevels = riskLevels == null ? Map.of() : Map.copyOf(riskLevels);
        servers = servers == null ? List.of() : List.copyOf(servers);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public record ServerSummary(
            String serverName,
            int total,
            int executed,
            int rejected,
            int failed,
            int invalid,
            int notFound,
            int withWarnings,
            long totalDurationMs,
            Map<String, Integer> executionModes,
            Map<String, Integer> riskLevels,
            String latestStatus,
            String latestReason,
            Instant lastStartedAt,
            Instant lastFinishedAt) {

        public ServerSummary {
            executionModes = executionModes == null ? Map.of() : Map.copyOf(executionModes);
            riskLevels = riskLevels == null ? Map.of() : Map.copyOf(riskLevels);
        }
    }

    public record ActionSummary(
            String actionId,
            String serverName,
            String actionCode,
            int total,
            int executed,
            int rejected,
            int failed,
            int invalid,
            int notFound,
            int withWarnings,
            long totalDurationMs,
            Map<String, Integer> executionModes,
            Map<String, Integer> riskLevels,
            String latestStatus,
            String latestReason,
            Instant lastStartedAt,
            Instant lastFinishedAt) {

        public ActionSummary {
            executionModes = executionModes == null ? Map.of() : Map.copyOf(executionModes);
            riskLevels = riskLevels == null ? Map.of() : Map.copyOf(riskLevels);
        }
    }
}
