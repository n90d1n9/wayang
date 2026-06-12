package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lookup-oriented view over persistence document health statuses.
 */
public record AgenticCommerceWayangPersistenceDocumentHealthIndex(
        List<AgenticCommerceWayangPersistenceDocumentStatus> documents,
        Map<String, AgenticCommerceWayangPersistenceDocumentStatus> byId) {

    public AgenticCommerceWayangPersistenceDocumentHealthIndex {
        documents = documents == null
                ? List.of()
                : documents.stream()
                        .filter(status -> status != null && !status.id().isBlank())
                        .toList();
        byId = normalizeById(documents, byId);
    }

    public static AgenticCommerceWayangPersistenceDocumentHealthIndex from(
            List<AgenticCommerceWayangPersistenceDocumentStatus> documents) {
        return new AgenticCommerceWayangPersistenceDocumentHealthIndex(documents, Map.of());
    }

    public Optional<AgenticCommerceWayangPersistenceDocumentStatus> status(String id) {
        return Optional.ofNullable(byId.get(AgenticCommerceWayangMaps.text(id)));
    }

    public Optional<AgenticCommerceWayangPersistenceDocumentStatus> status(
            AgenticCommerceWayangPersistenceDocument document) {
        return document == null ? Optional.empty() : status(document.id());
    }

    public boolean available(String id) {
        return status(id).map(AgenticCommerceWayangPersistenceDocumentStatus::available).orElse(false);
    }

    public boolean available(AgenticCommerceWayangPersistenceDocument document) {
        return status(document).map(AgenticCommerceWayangPersistenceDocumentStatus::available).orElse(false);
    }

    public List<AgenticCommerceWayangPersistenceDocumentStatus> missing() {
        return documents.stream()
                .filter(document -> !document.available())
                .toList();
    }

    public List<AgenticCommerceWayangPersistenceDocumentStatus> failed() {
        return documents.stream()
                .filter(document -> !document.loadable())
                .toList();
    }

    public List<String> missingIds() {
        return missing().stream()
                .map(AgenticCommerceWayangPersistenceDocumentStatus::id)
                .toList();
    }

    public List<String> failedIds() {
        return failed().stream()
                .map(AgenticCommerceWayangPersistenceDocumentStatus::id)
                .toList();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("documentCount", documents.size());
        values.put("missingDocumentCount", missing().size());
        values.put("failedDocumentCount", failed().size());
        values.put("missingDocumentIds", missingIds());
        values.put("failedDocumentIds", failedIds());
        values.put("documentsById", documentsByIdMap());
        return Map.copyOf(values);
    }

    private Map<String, Object> documentsByIdMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        byId.forEach((id, status) -> values.put(id, status.toMap()));
        return Map.copyOf(values);
    }

    private static Map<String, AgenticCommerceWayangPersistenceDocumentStatus> normalizeById(
            List<AgenticCommerceWayangPersistenceDocumentStatus> documents,
            Map<String, AgenticCommerceWayangPersistenceDocumentStatus> byId) {
        Map<String, AgenticCommerceWayangPersistenceDocumentStatus> values = new LinkedHashMap<>();
        documents.forEach(document -> values.put(document.id(), document));
        if (byId != null) {
            byId.forEach((id, status) -> {
                String normalized = AgenticCommerceWayangMaps.text(id);
                if (!normalized.isBlank() && status != null) {
                    values.put(normalized, status);
                }
            });
        }
        return Map.copyOf(values);
    }
}
