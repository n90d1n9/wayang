package tech.kayys.wayang.agent.event;

import java.util.Comparator;
import java.util.List;

/**
 * Read-side ordering and windowing policy for run event timelines.
 */
public final class AgentRunEventTimelines {

    private AgentRunEventTimelines() {
    }

    public static List<AgentRunEvent> ordered(List<AgentRunEvent> events) {
        return (events == null ? List.<AgentRunEvent>of() : events).stream()
                .filter(event -> event != null)
                .sorted(Comparator.comparingLong(AgentRunEvent::sequence))
                .toList();
    }

    public static List<AgentRunEvent> latest(List<AgentRunEvent> events, int limit) {
        List<AgentRunEvent> ordered = ordered(events);
        if (ordered.size() <= limit) {
            return ordered;
        }
        return ordered.subList(ordered.size() - limit, ordered.size());
    }
}
