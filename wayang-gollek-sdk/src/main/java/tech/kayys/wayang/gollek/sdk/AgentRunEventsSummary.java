package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;

public record AgentRunEventsSummary(
        int totalEvents,
        int returnedEvents,
        Map<String, Integer> stateCounts,
        Map<String, Integer> typeCounts) {

    public AgentRunEventsSummary {
        totalEvents = Math.max(0, totalEvents);
        returnedEvents = Math.max(0, returnedEvents);
        stateCounts = SdkCounts.copy(stateCounts);
        typeCounts = SdkCounts.copy(typeCounts);
    }

    public boolean empty() {
        return returnedEvents == 0;
    }

    public List<AgentRunEventFacetSummary> stateSummaries() {
        return AgentRunEventFacetSummary.fromCounts(stateCounts);
    }

    public List<AgentRunEventFacetSummary> typeSummaries() {
        return AgentRunEventFacetSummary.fromCounts(typeCounts);
    }
}
