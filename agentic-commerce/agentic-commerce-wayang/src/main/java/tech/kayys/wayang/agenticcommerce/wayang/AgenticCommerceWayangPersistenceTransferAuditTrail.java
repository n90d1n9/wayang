package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered audit trail for persistence transfer operations.
 */
public record AgenticCommerceWayangPersistenceTransferAuditTrail(
        String trailType,
        List<AgenticCommerceWayangPersistenceTransferAuditEvent> events,
        Map<String, Object> attributes) {

    public AgenticCommerceWayangPersistenceTransferAuditTrail {
        trailType = AgenticCommerceWayangMaps.required(trailType, "trailType");
        events = copyEvents(events);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditTrail from(
            AgenticCommerceWayangPersistenceTransferPreflightReport preflight) {
        AgenticCommerceWayangPersistenceTransferPreflightReport resolved =
                Objects.requireNonNull(preflight, "preflight");
        return new AgenticCommerceWayangPersistenceTransferAuditTrail(
                AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT,
                List.of(resolved.auditEvent()),
                preflightAttributes(resolved));
    }

    public static AgenticCommerceWayangPersistenceTransferAuditTrail from(
            AgenticCommerceWayangPersistenceTransferReport report) {
        AgenticCommerceWayangPersistenceTransferReport resolved =
                Objects.requireNonNull(report, "report");
        return new AgenticCommerceWayangPersistenceTransferAuditTrail(
                AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY,
                List.of(resolved.auditEvent()),
                transferAttributes(resolved));
    }

    public static AgenticCommerceWayangPersistenceTransferAuditTrail from(
            AgenticCommerceWayangPersistenceTransferApplyReport report) {
        AgenticCommerceWayangPersistenceTransferApplyReport resolved =
                Objects.requireNonNull(report, "report");
        List<AgenticCommerceWayangPersistenceTransferAuditEvent> events = new ArrayList<>();
        events.add(resolved.preflight().auditEvent());
        if (resolved.transferReportAvailable()) {
            events.add(resolved.transferReport().auditEvent());
        }
        events.add(resolved.auditEvent());
        return new AgenticCommerceWayangPersistenceTransferAuditTrail(
                AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY,
                events,
                applyAttributes(resolved));
    }

    public static AgenticCommerceWayangPersistenceTransferAuditTrail fromMap(Map<?, ?> values) {
        Map<String, Object> resolved = AgenticCommerceWayangMaps.copy(values);
        Object nestedTrail = AgenticCommerceWayangMaps.first(resolved, "trail", "auditTrail");
        if (nestedTrail instanceof Map<?, ?> nestedMap) {
            return fromMap(nestedMap);
        }
        return new AgenticCommerceWayangPersistenceTransferAuditTrail(
                AgenticCommerceWayangMaps.firstText(resolved, "trailType", "type"),
                eventsFrom(AgenticCommerceWayangMaps.first(resolved, "events", "auditEvents")),
                AgenticCommerceWayangMaps.firstMap(resolved, "attributes", "metadata", "details"));
    }

    public AgenticCommerceWayangPersistenceTransferAuditEvent latestEvent() {
        return events.get(events.size() - 1);
    }

    public String trailStatus() {
        return latestEvent().eventStatus();
    }

    public boolean successful() {
        return latestEvent().successful();
    }

    public boolean dryRun() {
        return events.stream().anyMatch(AgenticCommerceWayangPersistenceTransferAuditEvent::dryRun);
    }

    public boolean forced() {
        return events.stream().anyMatch(AgenticCommerceWayangPersistenceTransferAuditEvent::forced);
    }

    public boolean mutatedTarget() {
        return events.stream().anyMatch(AgenticCommerceWayangPersistenceTransferAuditEvent::mutatedTarget);
    }

    public boolean blocked() {
        return events.stream().anyMatch(AgenticCommerceWayangPersistenceTransferAuditEvent::blocked);
    }

    public int eventCount() {
        return events.size();
    }

    public int successfulEventCount() {
        return (int) events.stream()
                .filter(AgenticCommerceWayangPersistenceTransferAuditEvent::successful)
                .count();
    }

    public int blockedEventCount() {
        return (int) events.stream()
                .filter(AgenticCommerceWayangPersistenceTransferAuditEvent::blocked)
                .count();
    }

    public int issueCount() {
        return latestEvent().issueCount();
    }

    public int warningCount() {
        return latestEvent().warningCount();
    }

    public int findingCount() {
        return latestEvent().findingCount();
    }

    public int plannedDocumentCount() {
        return latestEvent().plannedDocumentCount();
    }

    public int copyableDocumentCount() {
        return latestEvent().copyableDocumentCount();
    }

    public int copiedDocumentCount() {
        return latestEvent().copiedDocumentCount();
    }

    public int skippedDocumentCount() {
        return latestEvent().skippedDocumentCount();
    }

    public int blockedDocumentCount() {
        return latestEvent().blockedDocumentCount();
    }

    public List<String> eventTypes() {
        return events.stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditEvent::eventType)
                .toList();
    }

    public List<String> eventStatuses() {
        return events.stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditEvent::eventStatus)
                .toList();
    }

    public AgenticCommerceWayangPersistenceTransferAuditTrailIndex eventIndex() {
        return AgenticCommerceWayangPersistenceTransferAuditTrailIndex.from(events);
    }

    public AgenticCommerceWayangPersistenceTransferAuditSummary summary() {
        return AgenticCommerceWayangPersistenceTransferAuditSummary.from(this);
    }

    public boolean hasEventType(String eventType) {
        String normalized = AgenticCommerceWayangMaps.text(eventType);
        return eventIndex().hasType(normalized);
    }

    public boolean hasEventStatus(String eventStatus) {
        String normalized = AgenticCommerceWayangMaps.text(eventStatus);
        return eventIndex().hasStatus(normalized);
    }

    public List<String> recommendationActions() {
        return events.stream()
                .flatMap(event -> event.recommendationActions().stream())
                .distinct()
                .toList();
    }

    public List<String> attentionReasons() {
        return events.stream()
                .flatMap(event -> event.attentionReasons().stream())
                .distinct()
                .toList();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("trailType", trailType);
        values.put("trailStatus", trailStatus());
        values.put("successful", successful());
        values.put("dryRun", dryRun());
        values.put("forced", forced());
        values.put("mutatedTarget", mutatedTarget());
        values.put("blocked", blocked());
        values.put("eventCount", eventCount());
        values.put("successfulEventCount", successfulEventCount());
        values.put("blockedEventCount", blockedEventCount());
        values.put("issueCount", issueCount());
        values.put("warningCount", warningCount());
        values.put("findingCount", findingCount());
        values.put("plannedDocumentCount", plannedDocumentCount());
        values.put("copyableDocumentCount", copyableDocumentCount());
        values.put("copiedDocumentCount", copiedDocumentCount());
        values.put("skippedDocumentCount", skippedDocumentCount());
        values.put("blockedDocumentCount", blockedDocumentCount());
        values.put("eventTypes", eventTypes());
        values.put("eventStatuses", eventStatuses());
        values.put("recommendationActions", recommendationActions());
        values.put("attentionReasons", attentionReasons());
        values.put("summary", summary().toMap());
        values.put("eventIndex", eventIndex().toMap());
        values.put("latestEvent", latestEvent().toMap());
        values.put("events", events.stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditEvent::toMap)
                .toList());
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static List<AgenticCommerceWayangPersistenceTransferAuditEvent> copyEvents(
            List<AgenticCommerceWayangPersistenceTransferAuditEvent> events) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("events must not be empty");
        }
        List<AgenticCommerceWayangPersistenceTransferAuditEvent> copy = events.stream()
                .filter(Objects::nonNull)
                .toList();
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("events must not be empty");
        }
        return List.copyOf(copy);
    }

    private static List<AgenticCommerceWayangPersistenceTransferAuditEvent> eventsFrom(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(AgenticCommerceWayangPersistenceTransferAuditEvent::fromMap)
                .toList();
    }

    private static Map<String, Object> preflightAttributes(
            AgenticCommerceWayangPersistenceTransferPreflightReport preflight) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("preflightStatus", preflight.preflightStatus());
        values.put("readyToApply", preflight.readyToApply());
        values.put("sourceStoreKind", preflight.sourceHealth().storageKind());
        values.put("targetStoreKind", preflight.targetHealth().storageKind());
        return Map.copyOf(values);
    }

    private static Map<String, Object> transferAttributes(
            AgenticCommerceWayangPersistenceTransferReport report) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("transferStatus", report.summary().transferStatus());
        values.put("sourceStoreKind", report.sourceStoreKind());
        values.put("targetStoreKind", report.targetStoreKind());
        values.put("verified", report.attributes().getOrDefault("verified", false));
        return Map.copyOf(values);
    }

    private static Map<String, Object> applyAttributes(
            AgenticCommerceWayangPersistenceTransferApplyReport report) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("applyStatus", report.applyStatus());
        values.put("forced", report.forced());
        values.put("transferAttempted", report.transferAttempted());
        values.put("transferReportAvailable", report.transferReportAvailable());
        values.put("blockedByPreflight", report.blockedByPreflight());
        values.put("preflightStatus", report.preflight().preflightStatus());
        values.put("preflightReadyToApply", report.preflight().readyToApply());
        if (report.transferReportAvailable()) {
            values.put("transferStatus", report.transferReport().summary().transferStatus());
        }
        return Map.copyOf(values);
    }
}
