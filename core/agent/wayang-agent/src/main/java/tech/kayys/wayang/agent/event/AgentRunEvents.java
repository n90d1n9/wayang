package tech.kayys.wayang.agent.event;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.agent.run.AgentRunOutcomes;
import tech.kayys.wayang.agent.run.AgentRunStates;
import tech.kayys.wayang.client.SdkLists;
import tech.kayys.wayang.client.SdkText;

public record AgentRunEvents(
        String runId,
        AgentRunEventsQuery query,
        List<AgentRunEvent> events,
        int totalEvents,
        String message) {

    public AgentRunEvents(String runId, List<AgentRunEvent> events, String message) {
        this(runId, AgentRunEventsQuery.all(), events, events == null ? 0 : events.size(), message);
    }

    public AgentRunEvents(String runId, AgentRunEventsQuery query, List<AgentRunEvent> events, String message) {
        this(runId, query, events, events == null ? 0 : events.size(), message);
    }

    public AgentRunEvents {
        runId = SdkText.trimToEmpty(runId);
        query = query == null ? AgentRunEventsQuery.all() : query;
        events = SdkLists.copy(events);
        totalEvents = Math.max(totalEvents, events.size());
        message = SdkText.trimToEmpty(message);
    }

    public int returnedEvents() {
        return cursor().returnedEvents();
    }

    public long firstSequence() {
        return events.stream()
                .mapToLong(AgentRunEvent::sequence)
                .min()
                .orElse(0);
    }

    public long lastSequence() {
        return cursor().lastSequence();
    }

    public long nextAfterSequence() {
        return cursor().nextAfterSequence();
    }

    public boolean truncated() {
        return cursor().truncated();
    }

    public Map<String, Integer> stateCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (AgentRunEvent event : events) {
            String key = AgentRunStates.wireName(event.state());
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
        return counts.isEmpty() ? Map.of() : Collections.unmodifiableMap(counts);
    }

    public Map<String, Integer> typeCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (AgentRunEvent event : events) {
            String key = SdkText.trimToEmpty(event.type());
            if (!key.isEmpty()) {
                counts.put(key, counts.getOrDefault(key, 0) + 1);
            }
        }
        return counts.isEmpty() ? Map.of() : Collections.unmodifiableMap(counts);
    }

    public List<AgentRunEventFacetSummary> stateSummaries() {
        return AgentRunEventFacetSummary.fromCounts(stateCounts());
    }

    public List<AgentRunEventFacetSummary> typeSummaries() {
        return AgentRunEventFacetSummary.fromCounts(typeCounts());
    }

    public boolean empty() {
        return events.isEmpty();
    }

    public String outcome() {
        if (empty()) {
            return AgentRunOutcomes.EMPTY;
        }
        boolean terminal = events.stream()
                .anyMatch(event -> event.state().terminal());
        return terminal ? AgentRunOutcomes.TERMINAL : AgentRunOutcomes.PENDING;
    }

    public AgentRunEventsCursor cursor() {
        return new AgentRunEventsCursor(
                query.afterSequence(),
                firstSequence(),
                events.stream()
                        .mapToLong(AgentRunEvent::sequence)
                        .max()
                        .orElse(0),
                Math.max(query.afterSequence(), events.stream()
                        .mapToLong(AgentRunEvent::sequence)
                        .max()
                        .orElse(0)),
                query.limit(),
                totalEvents,
                events.size());
    }

    public AgentRunEventsSummary summary() {
        return new AgentRunEventsSummary(
                totalEvents,
                returnedEvents(),
                stateCounts(),
                typeCounts());
    }
}
