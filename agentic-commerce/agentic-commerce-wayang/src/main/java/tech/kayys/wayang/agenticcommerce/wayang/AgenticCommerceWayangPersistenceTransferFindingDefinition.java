package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalog metadata for one persistence transfer finding code.
 */
public record AgenticCommerceWayangPersistenceTransferFindingDefinition(
        String code,
        String severity,
        String source,
        String title,
        String remediation,
        boolean blocking,
        Map<String, Object> attributes) {

    public AgenticCommerceWayangPersistenceTransferFindingDefinition {
        code = AgenticCommerceWayangMaps.text(code);
        severity = AgenticCommerceWayangPersistenceTransferFindings.normalizeSeverity(severity);
        source = AgenticCommerceWayangPersistenceTransferFindings.normalizeSource(source);
        title = AgenticCommerceWayangMaps.text(title);
        remediation = AgenticCommerceWayangMaps.text(remediation);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", code);
        values.put("severity", severity);
        values.put("source", source);
        values.put("title", title);
        values.put("remediation", remediation);
        values.put("blocking", blocking);
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }
}
