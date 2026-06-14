package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;

public record AgentRunHistoryFacetSummary(
        String name,
        int count) {

    public AgentRunHistoryFacetSummary {
        name = SdkText.trimToEmpty(name);
        count = Math.max(0, count);
    }

    static List<AgentRunHistoryFacetSummary> fromCounts(Map<String, Integer> counts) {
        return AgentRunFacetSummaries.fromCounts(counts, AgentRunHistoryFacetSummary::new);
    }
}
