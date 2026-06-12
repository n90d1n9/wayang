package tech.kayys.wayang.agent.api;

import java.util.Map;

/**
 * Stable REST projection of Hermes runtime journal aggregate counts.
 */
public record HermesJournalSummaryResponse(
        int scannedEvents,
        int matchedEvents,
        boolean truncated,
        long failedEvents,
        long successfulEvents,
        long distinctRequests,
        String firstOccurredAt,
        String latestOccurredAt,
        String latestEventId,
        String latestRequestId,
        Map<String, Long> typeCounts,
        Map<String, Long> outcomeCounts,
        Map<String, Long> tenantCounts) {

    public HermesJournalSummaryResponse {
        scannedEvents = Math.max(scannedEvents, 0);
        matchedEvents = Math.max(matchedEvents, scannedEvents);
        failedEvents = Math.max(failedEvents, 0L);
        successfulEvents = Math.max(successfulEvents, 0L);
        distinctRequests = Math.max(distinctRequests, 0L);
        firstOccurredAt = HermesResponseMetadata.text(firstOccurredAt, "");
        latestOccurredAt = HermesResponseMetadata.text(latestOccurredAt, "");
        latestEventId = HermesResponseMetadata.text(latestEventId, "");
        latestRequestId = HermesResponseMetadata.text(latestRequestId, "");
        typeCounts = typeCounts == null ? Map.of() : Map.copyOf(typeCounts);
        outcomeCounts = outcomeCounts == null ? Map.of() : Map.copyOf(outcomeCounts);
        tenantCounts = tenantCounts == null ? Map.of() : Map.copyOf(tenantCounts);
    }

    static HermesJournalSummaryResponse empty() {
        return from(Map.of());
    }

    static HermesJournalSummaryResponse from(Map<String, Object> summary) {
        Map<String, Object> values = summary == null ? Map.of() : summary;
        return new HermesJournalSummaryResponse(
                HermesResponseMetadata.integer(values.get("scannedEvents")),
                HermesResponseMetadata.integer(values.get("matchedEvents")),
                HermesResponseMetadata.bool(values.get("truncated")),
                HermesResponseMetadata.longValue(values.get("failedEvents")),
                HermesResponseMetadata.longValue(values.get("successfulEvents")),
                HermesResponseMetadata.longValue(values.get("distinctRequests")),
                HermesResponseMetadata.text(values.get("firstOccurredAt"), ""),
                HermesResponseMetadata.text(values.get("latestOccurredAt"), ""),
                HermesResponseMetadata.text(values.get("latestEventId"), ""),
                HermesResponseMetadata.text(values.get("latestRequestId"), ""),
                HermesResponseMetadata.longMap(values.get("typeCounts")),
                HermesResponseMetadata.longMap(values.get("outcomeCounts")),
                HermesResponseMetadata.longMap(values.get("tenantCounts")));
    }
}
