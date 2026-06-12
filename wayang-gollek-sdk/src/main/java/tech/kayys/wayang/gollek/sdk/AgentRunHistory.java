package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;

public record AgentRunHistory(
        AgentRunHistoryQuery query,
        List<AgentRunStatus> runs,
        int totalRuns,
        String message) {

    public AgentRunHistory(
            List<AgentRunStatus> runs,
            int totalRuns,
            String message) {
        this(AgentRunHistoryQuery.all(), runs, totalRuns, message);
    }

    public AgentRunHistory {
        query = query == null ? AgentRunHistoryQuery.all() : query;
        runs = SdkLists.copy(runs);
        totalRuns = new AgentRunHistoryPage(
                totalRuns,
                runs.size(),
                query.limit(),
                query.offset()).totalRuns();
        message = SdkText.trimToEmpty(message);
    }

    public static AgentRunHistory empty(String message) {
        return new AgentRunHistory(
                AgentRunHistoryQuery.all(),
                List.of(),
                0,
                SdkText.trimToDefault(message, "No run statuses are recorded."));
    }

    public boolean empty() {
        return runs.isEmpty();
    }

    public String outcome() {
        if (empty()) {
            return AgentRunOutcomes.EMPTY;
        }
        if (runs.stream().anyMatch(status -> !status.known())) {
            return AgentRunOutcomes.UNKNOWN;
        }
        return runs.stream().allMatch(status -> status.handle().terminal())
                ? AgentRunOutcomes.TERMINAL
                : AgentRunOutcomes.PENDING;
    }

    public int returnedRuns() {
        return page().returnedRuns();
    }

    public boolean truncated() {
        return page().truncated();
    }

    public Map<String, Integer> stateCounts() {
        return AgentRunMetadata.count(runs, status -> AgentRunStates.wireName(status.handle().state()));
    }

    public Map<String, Integer> surfaceCounts() {
        return AgentRunMetadata.count(runs, AgentRunMetadata::surface);
    }

    public Map<String, Integer> strategyCounts() {
        return AgentRunMetadata.count(runs, status -> status.handle().strategy());
    }

    public Map<String, Integer> profileCounts() {
        return AgentRunMetadata.count(runs, AgentRunMetadata::profile);
    }

    public List<AgentRunHistoryFacetSummary> stateSummaries() {
        return AgentRunHistoryFacetSummary.fromCounts(stateCounts());
    }

    public List<AgentRunHistoryFacetSummary> surfaceSummaries() {
        return AgentRunHistoryFacetSummary.fromCounts(surfaceCounts());
    }

    public List<AgentRunHistoryFacetSummary> profileSummaries() {
        return AgentRunHistoryFacetSummary.fromCounts(profileCounts());
    }

    public List<AgentRunHistoryFacetSummary> strategySummaries() {
        return AgentRunHistoryFacetSummary.fromCounts(strategyCounts());
    }

    public int offset() {
        return page().offset();
    }

    public int pageSize() {
        return page().pageSize();
    }

    public int windowStart() {
        return page().windowStart();
    }

    public int windowEnd() {
        return page().windowEnd();
    }

    public int previousOffset() {
        return page().previousOffset();
    }

    public boolean hasPrevious() {
        return page().hasPrevious();
    }

    public int nextOffset() {
        return page().nextOffset();
    }

    public boolean hasMore() {
        return page().hasMore();
    }

    public AgentRunHistoryPage page() {
        return new AgentRunHistoryPage(
                totalRuns,
                runs.size(),
                query.limit(),
                query.offset());
    }

    public AgentRunHistorySummary summary() {
        return new AgentRunHistorySummary(
                totalRuns,
                returnedRuns(),
                stateCounts(),
                surfaceCounts(),
                profileCounts(),
                strategyCounts());
    }

}
