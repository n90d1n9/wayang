package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalog metadata for one persistence health finding code.
 */
public record AgenticCommerceWayangPersistenceHealthFindingDefinition(
        String code,
        String severity,
        String source,
        String title,
        String remediation,
        boolean blocking,
        Map<String, Object> attributes) {

    public AgenticCommerceWayangPersistenceHealthFindingDefinition {
        code = AgenticCommerceWayangMaps.text(code);
        severity = AgenticCommerceWayangPersistenceHealthFindings.normalizeSeverity(severity);
        source = AgenticCommerceWayangPersistenceHealthFindings.normalizeSource(source);
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
