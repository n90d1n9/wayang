package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded runtime-event query result with matched and returned-window metadata.
 */
public record HermesRuntimeEventPage(
        List<HermesRuntimeEvent> events,
        int matchedEvents,
        int totalMatchedEvents,
        String previousCursor,
        String nextCursor,
        String firstCursor,
        String lastCursor,
        boolean hasPreviousPage,
        boolean hasNextPage,
        boolean cursorResolved) {

    public HermesRuntimeEventPage(
            List<HermesRuntimeEvent> events,
            int matchedEvents) {
        this(events, matchedEvents, matchedEvents, "", "", "", "", false, false, true);
    }

    public HermesRuntimeEventPage {
        events = HermesCollections.copyNonNull(events);
        matchedEvents = Math.max(matchedEvents, events.size());
        totalMatchedEvents = Math.max(totalMatchedEvents, matchedEvents);
        previousCursor = HermesText.trimToEmpty(previousCursor);
        nextCursor = HermesText.trimToEmpty(nextCursor);
        firstCursor = HermesText.trimToEmpty(firstCursor);
        lastCursor = HermesText.trimToEmpty(lastCursor);
    }

    public int returnedEvents() {
        return events.size();
    }

    public boolean truncated() {
        return matchedEvents > returnedEvents();
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("matchedEvents", matchedEvents);
        metadata.put("totalMatchedEvents", totalMatchedEvents);
        metadata.put("returnedEvents", returnedEvents());
        metadata.put("truncated", truncated());
        metadata.put("previousCursor", previousCursor);
        metadata.put("nextCursor", nextCursor);
        metadata.put("firstCursor", firstCursor);
        metadata.put("lastCursor", lastCursor);
        metadata.put("hasPreviousPage", hasPreviousPage);
        metadata.put("hasNextPage", hasNextPage);
        metadata.put("cursorResolved", cursorResolved);
        metadata.put("events", events.stream()
                .map(HermesRuntimeEvent::toMetadata)
                .toList());
        return Map.copyOf(metadata);
    }
}
