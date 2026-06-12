package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime status for one persisted Agentic Commerce Wayang document.
 */
public record AgenticCommerceWayangPersistenceDocumentStatus(
        AgenticCommerceWayangPersistenceDocument document,
        boolean available,
        boolean loadable,
        List<String> issues,
        List<String> warnings,
        Map<String, Object> attributes) {

    public AgenticCommerceWayangPersistenceDocumentStatus {
        document = document == null
                ? AgenticCommerceWayangPersistenceDocuments.RUNTIME_CONFIG
                : document;
        issues = AgenticCommerceWayangMaps.stringList(issues);
        warnings = AgenticCommerceWayangMaps.stringList(warnings);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public String id() {
        return document.id();
    }

    public String fileName() {
        return document.fileName();
    }

    public int issueCount() {
        return issues.size();
    }

    public int warningCount() {
        return warnings.size();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id());
        values.put("fileName", fileName());
        values.put("available", available);
        values.put("loadable", loadable);
        values.put("issueCount", issueCount());
        values.put("warningCount", warningCount());
        values.put("issues", issues);
        values.put("warnings", warnings);
        values.put("document", document.toMap());
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }
}
