package tech.kayys.wayang.gollek.sdk;

import java.util.Comparator;
import java.util.List;

/**
 * Read-side ordering and windowing policy for run event timelines.
 */
final class AgentRunEventTimelines {

    private AgentRunEventTimelines() {
    }

    static List<AgentRunEvent> ordered(List<AgentRunEvent> events) {
        return (events == null ? List.<AgentRunEvent>of() : events).stream()
                .filter(event -> event != null)
                .sorted(Comparator.comparingLong(AgentRunEvent::sequence))
                .toList();
    }

    static List<AgentRunEvent> latest(List<AgentRunEvent> events, int limit) {
        List<AgentRunEvent> ordered = ordered(events);
        if (ordered.size() <= limit) {
            return ordered;
        }
        return ordered.subList(ordered.size() - limit, ordered.size());
    }
}
