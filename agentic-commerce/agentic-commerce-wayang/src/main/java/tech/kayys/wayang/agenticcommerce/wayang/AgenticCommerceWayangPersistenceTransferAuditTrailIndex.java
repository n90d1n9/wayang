package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lookup-oriented view over persistence transfer audit events.
 */
public record AgenticCommerceWayangPersistenceTransferAuditTrailIndex(
        List<AgenticCommerceWayangPersistenceTransferAuditEvent> events,
        Map<String, List<AgenticCommerceWayangPersistenceTransferAuditEvent>> byType,
        Map<String, List<AgenticCommerceWayangPersistenceTransferAuditEvent>> byStatus) {

    public AgenticCommerceWayangPersistenceTransferAuditTrailIndex {
        events = normalizeEvents(events);
        byType = normalizeBucket(byType, bucket(events, AgenticCommerceWayangPersistenceTransferAuditEvent::eventType));
        byStatus = normalizeBucket(
                byStatus,
                bucket(events, AgenticCommerceWayangPersistenceTransferAuditEvent::eventStatus));
    }

    public static AgenticCommerceWayangPersistenceTransferAuditTrailIndex from(
            List<AgenticCommerceWayangPersistenceTransferAuditEvent> events) {
        return new AgenticCommerceWayangPersistenceTransferAuditTrailIndex(events, Map.of(), Map.of());
    }

    public List<AgenticCommerceWayangPersistenceTransferAuditEvent> type(String eventType) {
        return byType.getOrDefault(AgenticCommerceWayangMaps.text(eventType), List.of());
    }

    public List<AgenticCommerceWayangPersistenceTransferAuditEvent> status(String eventStatus) {
        return byStatus.getOrDefault(AgenticCommerceWayangMaps.text(eventStatus), List.of());
    }

    public boolean hasType(String eventType) {
        return !type(eventType).isEmpty();
    }

    public boolean hasStatus(String eventStatus) {
        return !status(eventStatus).isEmpty();
    }

    public List<AgenticCommerceWayangPersistenceTransferAuditEvent> successful() {
        return events.stream()
                .filter(AgenticCommerceWayangPersistenceTransferAuditEvent::successful)
                .toList();
    }

    public List<AgenticCommerceWayangPersistenceTransferAuditEvent> blocked() {
        return events.stream()
                .filter(AgenticCommerceWayangPersistenceTransferAuditEvent::blocked)
                .toList();
    }

    public List<AgenticCommerceWayangPersistenceTransferAuditEvent> forced() {
        return events.stream()
                .filter(AgenticCommerceWayangPersistenceTransferAuditEvent::forced)
                .toList();
    }

    public List<AgenticCommerceWayangPersistenceTransferAuditEvent> dryRun() {
        return events.stream()
                .filter(AgenticCommerceWayangPersistenceTransferAuditEvent::dryRun)
                .toList();
    }

    public List<AgenticCommerceWayangPersistenceTransferAuditEvent> mutatedTarget() {
        return events.stream()
                .filter(AgenticCommerceWayangPersistenceTransferAuditEvent::mutatedTarget)
                .toList();
    }

    public List<String> eventTypes() {
        return List.copyOf(byType.keySet());
    }

    public List<String> eventStatuses() {
        return List.copyOf(byStatus.keySet());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("eventCount", events.size());
        values.put("successfulEventCount", successful().size());
        values.put("blockedEventCount", blocked().size());
        values.put("forcedEventCount", forced().size());
        values.put("dryRunEventCount", dryRun().size());
        values.put("mutatedTargetEventCount", mutatedTarget().size());
        values.put("eventTypes", eventTypes());
        values.put("eventStatuses", eventStatuses());
        values.put("eventsByType", bucketMap(byType));
        values.put("eventsByStatus", bucketMap(byStatus));
        return Map.copyOf(values);
    }

    private static List<AgenticCommerceWayangPersistenceTransferAuditEvent> normalizeEvents(
            List<AgenticCommerceWayangPersistenceTransferAuditEvent> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        return events.stream()
                .filter(event -> event != null && !event.eventType().isBlank())
                .toList();
    }

    private static Map<String, List<AgenticCommerceWayangPersistenceTransferAuditEvent>> bucket(
            List<AgenticCommerceWayangPersistenceTransferAuditEvent> events,
            java.util.function.Function<AgenticCommerceWayangPersistenceTransferAuditEvent, String> classifier) {
        Map<String, List<AgenticCommerceWayangPersistenceTransferAuditEvent>> values = new LinkedHashMap<>();
        events.forEach(event -> {
            String key = AgenticCommerceWayangMaps.text(classifier.apply(event));
            if (!key.isBlank()) {
                values.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(event);
            }
        });
        return copyBucket(values);
    }

    private static Map<String, List<AgenticCommerceWayangPersistenceTransferAuditEvent>> normalizeBucket(
            Map<String, List<AgenticCommerceWayangPersistenceTransferAuditEvent>> supplied,
            Map<String, List<AgenticCommerceWayangPersistenceTransferAuditEvent>> fallback) {
        if (supplied == null || supplied.isEmpty()) {
            return fallback;
        }
        Map<String, List<AgenticCommerceWayangPersistenceTransferAuditEvent>> values = new LinkedHashMap<>(fallback);
        supplied.forEach((key, bucket) -> {
            String normalized = AgenticCommerceWayangMaps.text(key);
            if (!normalized.isBlank()) {
                values.put(normalized, normalizeEvents(bucket));
            }
        });
        return copyBucket(values);
    }

    private static Map<String, List<AgenticCommerceWayangPersistenceTransferAuditEvent>> copyBucket(
            Map<String, List<AgenticCommerceWayangPersistenceTransferAuditEvent>> values) {
        Map<String, List<AgenticCommerceWayangPersistenceTransferAuditEvent>> copy = new LinkedHashMap<>();
        values.forEach((key, bucket) -> copy.put(key, List.copyOf(bucket)));
        return Map.copyOf(copy);
    }

    private static Map<String, Object> bucketMap(
            Map<String, List<AgenticCommerceWayangPersistenceTransferAuditEvent>> bucket) {
        Map<String, Object> values = new LinkedHashMap<>();
        bucket.forEach((key, events) -> values.put(key, events.stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditEvent::toMap)
                .toList()));
        return Map.copyOf(values);
    }
}
