package tech.kayys.wayang.agent.history;

import java.util.LinkedHashMap;
import java.util.Map;

import tech.kayys.wayang.agent.lifecycle.AgentRunLifecycleContract;
import tech.kayys.wayang.agent.run.AgentRunEnvelopeMaps;
import tech.kayys.wayang.agent.run.AgentRunEnvelopes;
import tech.kayys.wayang.client.SdkText;

/**
 * Wire envelope factory for run history pages, history stats, and query facets.
 */
public final class AgentRunHistoryEnvelopes {

    private AgentRunHistoryEnvelopes() {
    }

    public static Map<String, Object> history(AgentRunHistory history) {
        AgentRunHistory model = normalizeHistory(history);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contract", AgentRunEnvelopes.lifecycleContract(AgentRunLifecycleContract.runList()));
        values.put("outcome", model.outcome());
        values.putAll(historyPayload(model));
        values.put("runs", model.runs().stream()
                .map(AgentRunEnvelopes::status)
                .toList());
        return AgentRunEnvelopeMaps.copy(values);
    }

    public static Map<String, Object> stats(AgentRunHistory history) {
        AgentRunHistory model = normalizeHistory(history);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contract", AgentRunEnvelopes.lifecycleContract(AgentRunLifecycleContract.runStats()));
        values.put("outcome", model.outcome());
        values.put("query", query(model.query()));
        values.put("page", page(model.page()));
        values.put("summary", summary(model.summary()));
        values.put("totalRuns", model.totalRuns());
        values.put("returnedRuns", model.returnedRuns());
        putCountsAndSummaries(values, model);
        values.put("empty", model.empty());
        values.put("message", model.message());
        return AgentRunEnvelopeMaps.copy(values);
    }

    public static Map<String, Object> query(AgentRunHistoryQuery query) {
        AgentRunHistoryQuery model = query == null ? AgentRunHistoryQuery.all() : query;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("state", model.state() == null ? null : model.state().name());
        values.put("limit", model.limit());
        values.put("offset", model.offset());
        values.put("tenantId", SdkText.blankToNull(model.tenantId()));
        values.put("sessionId", SdkText.blankToNull(model.sessionId()));
        values.put("surfaceId", SdkText.blankToNull(model.surfaceId()));
        values.put("profileId", SdkText.blankToNull(model.profileId()));
        values.put("filtered", model.filtered());
        return AgentRunEnvelopeMaps.copy(values);
    }

    public static Map<String, Object> page(AgentRunHistoryPage page) {
        AgentRunHistoryPage model = page == null ? new AgentRunHistoryPage(0, 0, 0, 0) : page;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("totalRuns", model.totalRuns());
        values.put("returnedRuns", model.returnedRuns());
        values.put("pageSize", model.pageSize());
        values.put("offset", model.offset());
        values.put("windowStart", model.windowStart());
        values.put("windowEnd", model.windowEnd());
        values.put("previousOffset", model.previousOffset());
        values.put("hasPrevious", model.hasPrevious());
        values.put("nextOffset", model.nextOffset());
        values.put("hasMore", model.hasMore());
        values.put("truncated", model.truncated());
        values.put("empty", model.empty());
        return AgentRunEnvelopeMaps.copy(values);
    }

    public static Map<String, Object> summary(AgentRunHistorySummary summary) {
        AgentRunHistorySummary model = summary == null
                ? new AgentRunHistorySummary(0, 0, Map.of(), Map.of(), Map.of(), Map.of())
                : summary;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("totalRuns", model.totalRuns());
        values.put("returnedRuns", model.returnedRuns());
        values.put("stateCounts", model.stateCounts());
        values.put("surfaceCounts", model.surfaceCounts());
        values.put("profileCounts", model.profileCounts());
        values.put("strategyCounts", model.strategyCounts());
        values.put("stateSummaries", model.stateSummaries().stream()
                .map(AgentRunHistoryEnvelopes::facetSummary)
                .toList());
        values.put("surfaceSummaries", model.surfaceSummaries().stream()
                .map(AgentRunHistoryEnvelopes::facetSummary)
                .toList());
        values.put("profileSummaries", model.profileSummaries().stream()
                .map(AgentRunHistoryEnvelopes::facetSummary)
                .toList());
        values.put("strategySummaries", model.strategySummaries().stream()
                .map(AgentRunHistoryEnvelopes::facetSummary)
                .toList());
        values.put("empty", model.empty());
        return AgentRunEnvelopeMaps.copy(values);
    }

    public static Map<String, Object> facetSummary(AgentRunHistoryFacetSummary summary) {
        AgentRunHistoryFacetSummary model = summary == null
                ? new AgentRunHistoryFacetSummary("", 0)
                : summary;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", model.name());
        values.put("count", model.count());
        return AgentRunEnvelopeMaps.copy(values);
    }

    private static Map<String, Object> historyPayload(AgentRunHistory history) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("query", query(history.query()));
        values.put("page", page(history.page()));
        values.put("summary", summary(history.summary()));
        values.put("totalRuns", history.totalRuns());
        values.put("returnedRuns", history.returnedRuns());
        values.put("pageSize", history.pageSize());
        values.put("offset", history.offset());
        values.put("windowStart", history.windowStart());
        values.put("windowEnd", history.windowEnd());
        values.put("previousOffset", history.previousOffset());
        values.put("hasPrevious", history.hasPrevious());
        values.put("nextOffset", history.nextOffset());
        values.put("hasMore", history.hasMore());
        values.put("truncated", history.truncated());
        putCountsAndSummaries(values, history);
        values.put("empty", history.empty());
        values.put("message", history.message());
        return AgentRunEnvelopeMaps.copy(values);
    }

    private static void putCountsAndSummaries(Map<String, Object> values, AgentRunHistory history) {
        values.put("stateCounts", history.stateCounts());
        values.put("surfaceCounts", history.surfaceCounts());
        values.put("profileCounts", history.profileCounts());
        values.put("strategyCounts", history.strategyCounts());
        values.put("stateSummaries", history.stateSummaries().stream()
                .map(AgentRunHistoryEnvelopes::facetSummary)
                .toList());
        values.put("surfaceSummaries", history.surfaceSummaries().stream()
                .map(AgentRunHistoryEnvelopes::facetSummary)
                .toList());
        values.put("profileSummaries", history.profileSummaries().stream()
                .map(AgentRunHistoryEnvelopes::facetSummary)
                .toList());
        values.put("strategySummaries", history.strategySummaries().stream()
                .map(AgentRunHistoryEnvelopes::facetSummary)
                .toList());
    }

    private static AgentRunHistory normalizeHistory(AgentRunHistory history) {
        return history == null ? AgentRunHistory.empty("") : history;
    }

}
