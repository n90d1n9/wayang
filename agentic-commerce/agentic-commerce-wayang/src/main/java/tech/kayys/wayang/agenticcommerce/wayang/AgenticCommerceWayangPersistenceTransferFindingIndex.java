package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lookup-oriented view over persistence transfer findings.
 */
public record AgenticCommerceWayangPersistenceTransferFindingIndex(
        List<AgenticCommerceWayangPersistenceTransferFinding> findings,
        Map<String, List<AgenticCommerceWayangPersistenceTransferFinding>> byCode,
        Map<String, List<AgenticCommerceWayangPersistenceTransferFinding>> bySeverity,
        Map<String, List<AgenticCommerceWayangPersistenceTransferFinding>> bySource,
        Map<String, List<AgenticCommerceWayangPersistenceTransferFinding>> byDocumentId) {

    public AgenticCommerceWayangPersistenceTransferFindingIndex {
        findings = normalizeFindings(findings);
        byCode = normalizeBucket(byCode, bucket(findings, AgenticCommerceWayangPersistenceTransferFinding::code));
        bySeverity = normalizeBucket(
                bySeverity,
                bucket(findings, AgenticCommerceWayangPersistenceTransferFinding::severity));
        bySource = normalizeBucket(
                bySource,
                bucket(findings, AgenticCommerceWayangPersistenceTransferFinding::source));
        byDocumentId = normalizeBucket(
                byDocumentId,
                bucket(findings, AgenticCommerceWayangPersistenceTransferFinding::documentId));
    }

    public static AgenticCommerceWayangPersistenceTransferFindingIndex from(
            List<AgenticCommerceWayangPersistenceTransferFinding> findings) {
        return new AgenticCommerceWayangPersistenceTransferFindingIndex(findings, Map.of(), Map.of(), Map.of(), Map.of());
    }

    public List<AgenticCommerceWayangPersistenceTransferFinding> code(String code) {
        return byCode.getOrDefault(AgenticCommerceWayangMaps.text(code), List.of());
    }

    public List<AgenticCommerceWayangPersistenceTransferFinding> severity(String severity) {
        return bySeverity.getOrDefault(
                AgenticCommerceWayangPersistenceTransferFindings.normalizeSeverity(severity),
                List.of());
    }

    public List<AgenticCommerceWayangPersistenceTransferFinding> source(String source) {
        return bySource.getOrDefault(
                AgenticCommerceWayangPersistenceTransferFindings.normalizeSource(source),
                List.of());
    }

    public List<AgenticCommerceWayangPersistenceTransferFinding> document(String documentId) {
        return byDocumentId.getOrDefault(AgenticCommerceWayangMaps.text(documentId), List.of());
    }

    public boolean hasCode(String code) {
        return !code(code).isEmpty();
    }

    public List<AgenticCommerceWayangPersistenceTransferFinding> errors() {
        return severity(AgenticCommerceWayangPersistenceTransferFinding.SEVERITY_ERROR);
    }

    public List<AgenticCommerceWayangPersistenceTransferFinding> warnings() {
        return severity(AgenticCommerceWayangPersistenceTransferFinding.SEVERITY_WARNING);
    }

    public List<AgenticCommerceWayangPersistenceTransferFinding> infos() {
        return severity(AgenticCommerceWayangPersistenceTransferFinding.SEVERITY_INFO);
    }

    public List<AgenticCommerceWayangPersistenceTransferFinding> blocking() {
        return findings.stream()
                .filter(AgenticCommerceWayangPersistenceTransferFinding::blocking)
                .toList();
    }

    public List<AgenticCommerceWayangPersistenceTransferFinding> documentScoped() {
        return findings.stream()
                .filter(AgenticCommerceWayangPersistenceTransferFinding::documentScoped)
                .toList();
    }

    public List<String> codes() {
        return List.copyOf(byCode.keySet());
    }

    public List<String> severities() {
        return List.copyOf(bySeverity.keySet());
    }

    public List<String> sources() {
        return List.copyOf(bySource.keySet());
    }

    public List<String> documentIds() {
        return List.copyOf(byDocumentId.keySet());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("findingCount", findings.size());
        values.put("errorFindingCount", errors().size());
        values.put("warningFindingCount", warnings().size());
        values.put("infoFindingCount", infos().size());
        values.put("blockingFindingCount", blocking().size());
        values.put("documentFindingCount", documentScoped().size());
        values.put("codes", codes());
        values.put("severities", severities());
        values.put("sources", sources());
        values.put("documentIds", documentIds());
        values.put("findingsByCode", bucketMap(byCode));
        values.put("findingsBySeverity", bucketMap(bySeverity));
        values.put("findingsBySource", bucketMap(bySource));
        values.put("findingsByDocumentId", bucketMap(byDocumentId));
        return Map.copyOf(values);
    }

    private static List<AgenticCommerceWayangPersistenceTransferFinding> normalizeFindings(
            List<AgenticCommerceWayangPersistenceTransferFinding> findings) {
        if (findings == null || findings.isEmpty()) {
            return List.of();
        }
        return findings.stream()
                .filter(finding -> finding != null && !finding.code().isBlank())
                .toList();
    }

    private static Map<String, List<AgenticCommerceWayangPersistenceTransferFinding>> bucket(
            List<AgenticCommerceWayangPersistenceTransferFinding> findings,
            java.util.function.Function<AgenticCommerceWayangPersistenceTransferFinding, String> classifier) {
        Map<String, List<AgenticCommerceWayangPersistenceTransferFinding>> values = new LinkedHashMap<>();
        findings.forEach(finding -> {
            String key = AgenticCommerceWayangMaps.text(classifier.apply(finding));
            if (!key.isBlank()) {
                values.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(finding);
            }
        });
        return copyBucket(values);
    }

    private static Map<String, List<AgenticCommerceWayangPersistenceTransferFinding>> normalizeBucket(
            Map<String, List<AgenticCommerceWayangPersistenceTransferFinding>> supplied,
            Map<String, List<AgenticCommerceWayangPersistenceTransferFinding>> fallback) {
        if (supplied == null || supplied.isEmpty()) {
            return fallback;
        }
        Map<String, List<AgenticCommerceWayangPersistenceTransferFinding>> values = new LinkedHashMap<>(fallback);
        supplied.forEach((key, bucket) -> {
            String normalized = AgenticCommerceWayangMaps.text(key);
            if (!normalized.isBlank()) {
                values.put(normalized, normalizeFindings(bucket));
            }
        });
        return copyBucket(values);
    }

    private static Map<String, List<AgenticCommerceWayangPersistenceTransferFinding>> copyBucket(
            Map<String, List<AgenticCommerceWayangPersistenceTransferFinding>> values) {
        Map<String, List<AgenticCommerceWayangPersistenceTransferFinding>> copy = new LinkedHashMap<>();
        values.forEach((key, bucket) -> copy.put(key, List.copyOf(bucket)));
        return Map.copyOf(copy);
    }

    private static Map<String, Object> bucketMap(
            Map<String, List<AgenticCommerceWayangPersistenceTransferFinding>> bucket) {
        Map<String, Object> values = new LinkedHashMap<>();
        bucket.forEach((key, findings) -> values.put(key, findings.stream()
                .map(AgenticCommerceWayangPersistenceTransferFinding::toMap)
                .toList()));
        return Map.copyOf(values);
    }
}
