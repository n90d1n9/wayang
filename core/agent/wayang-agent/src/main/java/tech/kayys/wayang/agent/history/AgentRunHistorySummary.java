package tech.kayys.wayang.agent.history;

import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.SdkCounts;

public record AgentRunHistorySummary(
        int totalRuns,
        int returnedRuns,
        Map<String, Integer> stateCounts,
        Map<String, Integer> surfaceCounts,
        Map<String, Integer> profileCounts,
        Map<String, Integer> strategyCounts) {

    public AgentRunHistorySummary {
        totalRuns = Math.max(0, totalRuns);
        returnedRuns = Math.max(0, returnedRuns);
        stateCounts = SdkCounts.copy(stateCounts);
        surfaceCounts = SdkCounts.copy(surfaceCounts);
        profileCounts = SdkCounts.copy(profileCounts);
        strategyCounts = SdkCounts.copy(strategyCounts);
    }

    public AgentRunHistorySummary(
            int totalRuns,
            int returnedRuns,
            Map<String, Integer> stateCounts,
            Map<String, Integer> surfaceCounts,
            Map<String, Integer> strategyCounts) {
        this(totalRuns, returnedRuns, stateCounts, surfaceCounts, Map.of(), strategyCounts);
    }

    public boolean empty() {
        return returnedRuns == 0;
    }

    public List<AgentRunHistoryFacetSummary> stateSummaries() {
        return AgentRunHistoryFacetSummary.fromCounts(stateCounts);
    }

    public List<AgentRunHistoryFacetSummary> surfaceSummaries() {
        return AgentRunHistoryFacetSummary.fromCounts(surfaceCounts);
    }

    public List<AgentRunHistoryFacetSummary> profileSummaries() {
        return AgentRunHistoryFacetSummary.fromCounts(profileCounts);
    }

    public List<AgentRunHistoryFacetSummary> strategySummaries() {
        return AgentRunHistoryFacetSummary.fromCounts(strategyCounts);
    }
}
