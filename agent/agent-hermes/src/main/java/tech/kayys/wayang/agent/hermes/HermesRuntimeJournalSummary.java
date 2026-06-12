package tech.kayys.wayang.agent.hermes;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Aggregate view over a bounded runtime-journal query window.
 */
public record HermesRuntimeJournalSummary(
        int scannedEvents,
        int matchedEvents,
        boolean truncated,
        long failedEvents,
        long successfulEvents,
        long distinctRequests,
        Instant firstOccurredAt,
        Instant latestOccurredAt,
        String latestEventId,
        String latestRequestId,
        Map<String, Long> typeCounts,
        Map<String, Long> outcomeCounts,
        Map<String, Long> tenantCounts) {

    public HermesRuntimeJournalSummary {
        scannedEvents = Math.max(scannedEvents, 0);
        matchedEvents = Math.max(matchedEvents, scannedEvents);
        latestEventId = HermesText.trimToEmpty(latestEventId);
        latestRequestId = HermesText.trimToEmpty(latestRequestId);
        typeCounts = copy(typeCounts);
        outcomeCounts = copy(outcomeCounts);
        tenantCounts = copy(tenantCounts);
    }

    public static HermesRuntimeJournalSummary empty() {
        return from(new HermesRuntimeEventPage(List.of(), 0));
    }

    public static HermesRuntimeJournalSummary from(HermesRuntimeEventPage page) {
        HermesRuntimeEventPage resolved = page == null
                ? new HermesRuntimeEventPage(List.of(), 0)
                : page;
        List<HermesRuntimeEvent> events = resolved.events();
        HermesRuntimeEvent latest = events.stream()
                .max(Comparator.comparing(HermesRuntimeEvent::occurredAt)
                        .thenComparing(HermesRuntimeEvent::eventId))
                .orElse(null);
        return new HermesRuntimeJournalSummary(
                events.size(),
                resolved.matchedEvents(),
                resolved.truncated(),
                events.stream().filter(HermesRuntimeJournalSummary::failed).count(),
                events.stream().filter(HermesRuntimeJournalSummary::successful).count(),
                events.stream()
                        .map(HermesRuntimeEvent::requestId)
                        .filter(value -> !HermesText.trimToEmpty(value).isBlank())
                        .distinct()
                        .count(),
                events.stream()
                        .map(HermesRuntimeEvent::occurredAt)
                        .filter(Objects::nonNull)
                        .min(Comparator.naturalOrder())
                        .orElse(null),
                latest == null ? null : latest.occurredAt(),
                latest == null ? "" : latest.eventId(),
                latest == null ? "" : latest.requestId(),
                counts(events, HermesRuntimeEvent::type, true),
                counts(events, HermesRuntimeEvent::outcome, true),
                counts(events, HermesRuntimeEvent::tenantId, false));
    }

    public boolean hasEvents() {
        return scannedEvents > 0;
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("scannedEvents", scannedEvents);
        metadata.put("matchedEvents", matchedEvents);
        metadata.put("truncated", truncated);
        metadata.put("failedEvents", failedEvents);
        metadata.put("successfulEvents", successfulEvents);
        metadata.put("distinctRequests", distinctRequests);
        metadata.put("firstOccurredAt", firstOccurredAt == null ? "" : firstOccurredAt.toString());
        metadata.put("latestOccurredAt", latestOccurredAt == null ? "" : latestOccurredAt.toString());
        metadata.put("latestEventId", latestEventId);
        metadata.put("latestRequestId", latestRequestId);
        metadata.put("typeCounts", typeCounts);
        metadata.put("outcomeCounts", outcomeCounts);
        metadata.put("tenantCounts", tenantCounts);
        return Map.copyOf(metadata);
    }

    private static boolean failed(HermesRuntimeEvent event) {
        return event != null
                && (HermesRuntimeEvent.TYPE_RESPONSE_FAILED.equals(event.type())
                || "failed".equalsIgnoreCase(event.outcome()));
    }

    private static boolean successful(HermesRuntimeEvent event) {
        return event != null && "successful".equalsIgnoreCase(event.outcome());
    }

    private static Map<String, Long> counts(
            List<HermesRuntimeEvent> events,
            Function<HermesRuntimeEvent, String> classifier,
            boolean includeBlank) {
        Map<String, Long> counts = new LinkedHashMap<>();
        if (events == null) {
            return counts;
        }
        for (HermesRuntimeEvent event : events) {
            if (event == null) {
                continue;
            }
            String value = HermesText.trimToEmpty(classifier.apply(event));
            if (value.isBlank() && !includeBlank) {
                continue;
            }
            counts.merge(value.isBlank() ? "unknown" : value, 1L, Long::sum);
        }
        return counts;
    }

    private static Map<String, Long> copy(Map<String, Long> values) {
        return values == null ? Map.of() : Map.copyOf(values);
    }

}
