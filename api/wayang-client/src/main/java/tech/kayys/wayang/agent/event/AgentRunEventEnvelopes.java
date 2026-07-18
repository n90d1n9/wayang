package tech.kayys.wayang.agent.event;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.agent.lifecycle.AgentRunLifecycleContract;
import tech.kayys.wayang.agent.run.AgentRunEnvelopeMaps;
import tech.kayys.wayang.agent.run.AgentRunEnvelopes;
import tech.kayys.wayang.client.SdkText;

/**
 * Wire envelope factory for run event pages, event stats, and follow results.
 */
public final class AgentRunEventEnvelopes {

    private AgentRunEventEnvelopes() {
    }

    public static Map<String, Object> events(AgentRunEvents events) {
        AgentRunEvents model = normalizeEvents(events);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contract", AgentRunEnvelopes.lifecycleContract(AgentRunLifecycleContract.runEvents()));
        putEventPage(values, model);
        values.put("events", model.events().stream()
                .map(AgentRunEventEnvelopes::event)
                .toList());
        return AgentRunEnvelopeMaps.copy(values);
    }

    public static Map<String, Object> eventsStats(AgentRunEvents events) {
        AgentRunEvents model = normalizeEvents(events);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contract", AgentRunEnvelopes.lifecycleContract(AgentRunLifecycleContract.runEventsStats()));
        putEventPage(values, model);
        return AgentRunEnvelopeMaps.copy(values);
    }

    public static Map<String, Object> followResult(AgentRunEventsFollowResult result, boolean stats) {
        AgentRunEventsFollowResult model = normalizeFollowResult(result);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contract", AgentRunEnvelopes.lifecycleContract(AgentRunLifecycleContract.runEventsFollow()));
        values.put("runId", model.runId());
        values.put("successful", model.successful());
        values.put("outcome", model.outcome());
        values.put("terminal", model.terminal());
        values.put("terminalState", SdkText.blankToNull(model.terminalState()));
        values.put("terminalEventType", SdkText.blankToNull(model.terminalEventType()));
        values.put("terminalSequence", model.terminalSequence());
        values.put("maxPollsReached", model.maxPollsReached());
        values.put("polls", model.polls());
        values.put("elapsedMillis", model.elapsedMillis());
        values.put("initialQuery", query(model.initialQuery()));
        values.put("nextQuery", query(model.nextQuery()));
        values.put("nextAfterSequence", model.nextAfterSequence());
        values.put("empty", model.empty());
        values.put("message", model.message());
        values.put("metadata", model.metadata());
        values.put("lastEvents", stats ? eventsStats(model.lastEvents()) : events(model.lastEvents()));
        return AgentRunEnvelopeMaps.copy(values);
    }

    public static Map<String, Object> query(AgentRunEventsQuery query) {
        AgentRunEventsQuery model = query == null ? AgentRunEventsQuery.all() : query;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("state", model.state() == null ? null : model.state().name());
        values.put("type", SdkText.blankToNull(model.type()));
        values.put("afterSequence", model.afterSequence());
        values.put("limit", model.limit());
        values.put("filtered", model.filtered());
        return AgentRunEnvelopeMaps.copy(values);
    }

    private static void putEventPage(Map<String, Object> values, AgentRunEvents events) {
        values.put("runId", events.runId());
        values.put("outcome", events.outcome());
        values.put("query", query(events.query()));
        values.put("cursor", cursor(events.cursor()));
        values.put("summary", summary(events.summary()));
        values.put("totalEvents", events.totalEvents());
        values.put("returnedEvents", events.returnedEvents());
        values.put("firstSequence", events.firstSequence());
        values.put("lastSequence", events.lastSequence());
        values.put("nextAfterSequence", events.nextAfterSequence());
        values.put("truncated", events.truncated());
        values.put("stateCounts", events.stateCounts());
        values.put("typeCounts", events.typeCounts());
        values.put("stateSummaries", events.stateSummaries().stream()
                .map(AgentRunEventEnvelopes::facetSummary)
                .toList());
        values.put("typeSummaries", events.typeSummaries().stream()
                .map(AgentRunEventEnvelopes::facetSummary)
                .toList());
        values.put("empty", events.empty());
        values.put("message", events.message());
    }

    private static Map<String, Object> cursor(AgentRunEventsCursor cursor) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("afterSequence", cursor.afterSequence());
        values.put("firstSequence", cursor.firstSequence());
        values.put("lastSequence", cursor.lastSequence());
        values.put("nextAfterSequence", cursor.nextAfterSequence());
        values.put("limit", cursor.limit());
        values.put("totalEvents", cursor.totalEvents());
        values.put("returnedEvents", cursor.returnedEvents());
        values.put("remainingEvents", cursor.remainingEvents());
        values.put("advanced", cursor.advanced());
        values.put("truncated", cursor.truncated());
        values.put("empty", cursor.empty());
        return AgentRunEnvelopeMaps.copy(values);
    }

    private static Map<String, Object> summary(AgentRunEventsSummary summary) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("totalEvents", summary.totalEvents());
        values.put("returnedEvents", summary.returnedEvents());
        values.put("stateCounts", summary.stateCounts());
        values.put("typeCounts", summary.typeCounts());
        values.put("stateSummaries", summary.stateSummaries().stream()
                .map(AgentRunEventEnvelopes::facetSummary)
                .toList());
        values.put("typeSummaries", summary.typeSummaries().stream()
                .map(AgentRunEventEnvelopes::facetSummary)
                .toList());
        values.put("empty", summary.empty());
        return AgentRunEnvelopeMaps.copy(values);
    }

    private static Map<String, Object> facetSummary(AgentRunEventFacetSummary summary) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", summary.name());
        values.put("count", summary.count());
        return AgentRunEnvelopeMaps.copy(values);
    }

    private static Map<String, Object> event(AgentRunEvent event) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("runId", event.runId());
        values.put("sequence", event.sequence());
        values.put("type", event.type());
        values.put("state", event.state().name());
        values.put("message", event.message());
        values.put("metadata", event.metadata());
        return AgentRunEnvelopeMaps.copy(values);
    }

    private static AgentRunEvents normalizeEvents(AgentRunEvents events) {
        return events == null ? new AgentRunEvents("", AgentRunEventsQuery.all(), List.of(), 0, "") : events;
    }

    private static AgentRunEventsFollowResult normalizeFollowResult(AgentRunEventsFollowResult result) {
        if (result != null) {
            return result;
        }
        AgentRunEvents events = normalizeEvents(null);
        return new AgentRunEventsFollowResult(
                "",
                AgentRunEventsQuery.all(),
                AgentRunEventsQuery.all(),
                events,
                false,
                false,
                0,
                0,
                "",
                Map.of());
    }

}
