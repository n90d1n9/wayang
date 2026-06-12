package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;

public record AgentRunEventFacetSummary(
        String name,
        int count) {

    public AgentRunEventFacetSummary {
        name = SdkText.trimToEmpty(name);
        count = Math.max(0, count);
    }

    static List<AgentRunEventFacetSummary> fromCounts(Map<String, Integer> counts) {
        return AgentRunFacetSummaries.fromCounts(counts, AgentRunEventFacetSummary::new);
    }
}
