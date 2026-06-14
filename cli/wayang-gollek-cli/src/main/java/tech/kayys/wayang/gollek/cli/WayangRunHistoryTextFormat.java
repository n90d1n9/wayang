package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

final class WayangRunHistoryTextFormat {

    private WayangRunHistoryTextFormat() {
    }

    static String historyText(AgentRunHistory history) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang runs").append(System.lineSeparator());
        output.append("outcome: ").append(history.outcome()).append(System.lineSeparator());
        output.append("page: ")
                .append(history.windowStart())
                .append("-")
                .append(history.windowEnd())
                .append(" of ")
                .append(history.totalRuns())
                .append(System.lineSeparator());
        output.append("total: ").append(history.totalRuns()).append(System.lineSeparator());
        output.append("returned: ").append(history.returnedRuns()).append(System.lineSeparator());
        output.append("pageSize: ").append(history.pageSize()).append(System.lineSeparator());
        output.append("offset: ").append(history.offset()).append(System.lineSeparator());
        output.append("windowStart: ").append(history.windowStart()).append(System.lineSeparator());
        output.append("windowEnd: ").append(history.windowEnd()).append(System.lineSeparator());
        output.append("previousOffset: ").append(history.previousOffset()).append(System.lineSeparator());
        output.append("hasPrevious: ").append(history.hasPrevious()).append(System.lineSeparator());
        output.append("nextOffset: ").append(history.nextOffset()).append(System.lineSeparator());
        output.append("hasMore: ").append(history.hasMore()).append(System.lineSeparator());
        output.append("truncated: ").append(history.truncated()).append(System.lineSeparator());
        appendSummary(output, history);
        output.append("state: ").append(history.query().state() == null
                ? "all"
                : history.query().state().name().toLowerCase()).append(System.lineSeparator());
        output.append("limit: ").append(history.query().limit()).append(System.lineSeparator());
        if (!history.query().tenantId().isBlank()) {
            output.append("tenant: ").append(history.query().tenantId()).append(System.lineSeparator());
        }
        if (!history.query().sessionId().isBlank()) {
            output.append("session: ").append(history.query().sessionId()).append(System.lineSeparator());
        }
        if (!history.query().surfaceId().isBlank()) {
            output.append("surface: ").append(history.query().surfaceId()).append(System.lineSeparator());
        }
        if (!history.query().profileId().isBlank()) {
            output.append("profile: ").append(history.query().profileId()).append(System.lineSeparator());
        }
        if (!history.message().isBlank()) {
            output.append("message: ").append(history.message()).append(System.lineSeparator());
        }
        if (!history.runs().isEmpty()) {
            output.append(System.lineSeparator()).append("runs:").append(System.lineSeparator());
            for (AgentRunStatus status : history.runs()) {
                output.append("- ")
                        .append(status.handle().runId())
                        .append(" ")
                        .append(status.handle().state().name().toLowerCase())
                        .append(" known=")
                        .append(status.known())
                        .append(" strategy=")
                        .append(status.handle().strategy())
                        .append(System.lineSeparator());
            }
        }
        return output.toString();
    }

    static String historyStatsText(AgentRunHistory history) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang run stats").append(System.lineSeparator());
        output.append("outcome: ").append(history.outcome()).append(System.lineSeparator());
        output.append("page: ")
                .append(history.windowStart())
                .append("-")
                .append(history.windowEnd())
                .append(" of ")
                .append(history.totalRuns())
                .append(System.lineSeparator());
        output.append("total: ").append(history.totalRuns()).append(System.lineSeparator());
        output.append("returned: ").append(history.returnedRuns()).append(System.lineSeparator());
        appendSummary(output, history);
        if (!history.query().profileId().isBlank()) {
            output.append("profile: ").append(history.query().profileId()).append(System.lineSeparator());
        }
        if (!history.message().isBlank()) {
            output.append("message: ").append(history.message()).append(System.lineSeparator());
        }
        return output.toString();
    }

    private static void appendSummary(StringBuilder output, AgentRunHistory history) {
        output.append("summary: ").append(history.summary()).append(System.lineSeparator());
        output.append("stateCounts: ").append(history.stateCounts()).append(System.lineSeparator());
        output.append("surfaceCounts: ").append(history.surfaceCounts()).append(System.lineSeparator());
        output.append("profileCounts: ").append(history.profileCounts()).append(System.lineSeparator());
        output.append("strategyCounts: ").append(history.strategyCounts()).append(System.lineSeparator());
        output.append("stateSummaries: ").append(history.stateSummaries()).append(System.lineSeparator());
        output.append("surfaceSummaries: ").append(history.surfaceSummaries()).append(System.lineSeparator());
        output.append("profileSummaries: ").append(history.profileSummaries()).append(System.lineSeparator());
        output.append("strategySummaries: ").append(history.strategySummaries()).append(System.lineSeparator());
    }
}
