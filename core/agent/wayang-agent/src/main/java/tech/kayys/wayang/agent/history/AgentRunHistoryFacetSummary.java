package tech.kayys.wayang.agent.history;

import java.util.List;
import java.util.Map;

import tech.kayys.wayang.agent.run.AgentRunFacetSummaries;
import tech.kayys.wayang.client.SdkText;

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
