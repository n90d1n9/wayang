package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compact operator summary for persistence health reports.
 */
public record AgenticCommerceWayangPersistenceHealthSummary(
        String healthStatus,
        boolean ready,
        boolean complete,
        int issueCount,
        int warningCount,
        int availableDocumentCount,
        int requiredDocumentCount,
        int missingDocumentCount,
        int failedDocumentCount,
        List<String> missingDocumentIds,
        List<String> failedDocumentIds,
        Map<String, Object> attributes) {

    public static final String STATUS_HEALTHY = "healthy";
    public static final String STATUS_DEGRADED = "degraded";
    public static final String STATUS_INCOMPLETE = "incomplete";
    public static final String STATUS_UNAVAILABLE = "unavailable";

    public AgenticCommerceWayangPersistenceHealthSummary {
        healthStatus = normalizeStatus(healthStatus);
        missingDocumentIds = AgenticCommerceWayangMaps.stringList(missingDocumentIds);
        failedDocumentIds = AgenticCommerceWayangMaps.stringList(failedDocumentIds);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceHealthSummary from(
            AgenticCommerceWayangPersistenceHealthReport report) {
        if (report == null) {
            return missingReport();
        }
        List<String> missingDocumentIds = report.missingDocuments().stream()
                .map(AgenticCommerceWayangPersistenceDocumentStatus::id)
                .toList();
        List<String> failedDocumentIds = report.failedDocuments().stream()
                .map(AgenticCommerceWayangPersistenceDocumentStatus::id)
                .toList();
        return new AgenticCommerceWayangPersistenceHealthSummary(
                status(report),
                report.ready(),
                report.complete(),
                report.issueCount(),
                report.warningCount(),
                report.availableDocumentCount(),
                report.requiredDocumentCount(),
                missingDocumentIds.size(),
                failedDocumentIds.size(),
                missingDocumentIds,
                failedDocumentIds,
                attributes(report));
    }

    public boolean healthy() {
        return STATUS_HEALTHY.equals(healthStatus);
    }

    public boolean degraded() {
        return STATUS_DEGRADED.equals(healthStatus);
    }

    public boolean incomplete() {
        return STATUS_INCOMPLETE.equals(healthStatus);
    }

    public boolean unavailable() {
        return STATUS_UNAVAILABLE.equals(healthStatus);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("healthStatus", healthStatus);
        values.put("ready", ready);
        values.put("complete", complete);
        values.put("healthy", healthy());
        values.put("degraded", degraded());
        values.put("incomplete", incomplete());
        values.put("unavailable", unavailable());
        values.put("issueCount", issueCount);
        values.put("warningCount", warningCount);
        values.put("availableDocumentCount", availableDocumentCount);
        values.put("requiredDocumentCount", requiredDocumentCount);
        values.put("missingDocumentCount", missingDocumentCount);
        values.put("failedDocumentCount", failedDocumentCount);
        values.put("missingDocumentIds", missingDocumentIds);
        values.put("failedDocumentIds", failedDocumentIds);
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static AgenticCommerceWayangPersistenceHealthSummary missingReport() {
        return new AgenticCommerceWayangPersistenceHealthSummary(
                STATUS_UNAVAILABLE,
                false,
                false,
                1,
                0,
                0,
                AgenticCommerceWayangPersistenceDocuments.count(),
                0,
                0,
                List.of(),
                List.of(),
                Map.of("reason", "health_report_missing"));
    }

    private static String status(AgenticCommerceWayangPersistenceHealthReport report) {
        if (!report.ready()) {
            return STATUS_UNAVAILABLE;
        }
        if (!report.complete()) {
            return STATUS_INCOMPLETE;
        }
        if (report.warningCount() > 0) {
            return STATUS_DEGRADED;
        }
        return STATUS_HEALTHY;
    }

    private static Map<String, Object> attributes(AgenticCommerceWayangPersistenceHealthReport report) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("storageKind", report.storageKind());
        values.put("statusReadable", report.statusReadable());
        values.put("durable", report.capabilities().durable());
        values.put("ephemeral", report.capabilities().ephemeral());
        values.put("cloudStorage", report.capabilities().cloudStorage());
        values.put("hybrid", report.capabilities().hybrid());
        values.put("mirrored", report.capabilities().mirrored());
        values.put("persistenceTarget", report.persistenceTarget());
        return Map.copyOf(values);
    }

    private static String normalizeStatus(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value);
        if (STATUS_HEALTHY.equals(normalized)
                || STATUS_DEGRADED.equals(normalized)
                || STATUS_INCOMPLETE.equals(normalized)
                || STATUS_UNAVAILABLE.equals(normalized)) {
            return normalized;
        }
        return STATUS_UNAVAILABLE;
    }
}
