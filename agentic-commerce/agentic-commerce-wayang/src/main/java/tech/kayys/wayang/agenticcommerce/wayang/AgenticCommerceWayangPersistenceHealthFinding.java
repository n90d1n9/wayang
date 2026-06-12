package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Structured persistence health issue or warning.
 */
public record AgenticCommerceWayangPersistenceHealthFinding(
        String severity,
        String code,
        String source,
        String documentId,
        String documentFileName,
        Map<String, Object> attributes) {

    public static final String SEVERITY_ERROR = "error";
    public static final String SEVERITY_WARNING = "warning";
    public static final String SOURCE_STORE = "store";
    public static final String SOURCE_DOCUMENT = "document";

    public AgenticCommerceWayangPersistenceHealthFinding {
        code = AgenticCommerceWayangMaps.text(code);
        AgenticCommerceWayangPersistenceHealthFindingDefinition definition =
                AgenticCommerceWayangPersistenceHealthFindings.definition(code, severity, source);
        code = definition.code();
        severity = definition.severity();
        source = AgenticCommerceWayangMaps.text(source).isBlank()
                ? definition.source()
                : AgenticCommerceWayangPersistenceHealthFindings.normalizeSource(source);
        documentId = AgenticCommerceWayangMaps.text(documentId);
        documentFileName = AgenticCommerceWayangMaps.text(documentFileName);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceHealthFinding storeError(String code) {
        return new AgenticCommerceWayangPersistenceHealthFinding(
                SEVERITY_ERROR,
                code,
                SOURCE_STORE,
                "",
                "",
                Map.of());
    }

    public static AgenticCommerceWayangPersistenceHealthFinding storeWarning(String code) {
        return new AgenticCommerceWayangPersistenceHealthFinding(
                SEVERITY_WARNING,
                code,
                SOURCE_STORE,
                "",
                "",
                Map.of());
    }

    public static AgenticCommerceWayangPersistenceHealthFinding documentError(
            AgenticCommerceWayangPersistenceDocumentStatus document,
            String code) {
        return documentFinding(SEVERITY_ERROR, document, code);
    }

    public static AgenticCommerceWayangPersistenceHealthFinding documentWarning(
            AgenticCommerceWayangPersistenceDocumentStatus document,
            String code) {
        return documentFinding(SEVERITY_WARNING, document, code);
    }

    public boolean error() {
        return SEVERITY_ERROR.equals(severity);
    }

    public boolean warning() {
        return SEVERITY_WARNING.equals(severity);
    }

    public boolean documentScoped() {
        return SOURCE_DOCUMENT.equals(source) && !documentId.isBlank();
    }

    public AgenticCommerceWayangPersistenceHealthFindingDefinition definition() {
        return AgenticCommerceWayangPersistenceHealthFindings.definition(code, severity, source);
    }

    public String title() {
        return definition().title();
    }

    public String remediation() {
        return definition().remediation();
    }

    public boolean blocking() {
        return definition().blocking();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("severity", severity);
        values.put("code", code);
        values.put("source", source);
        values.put("title", title());
        values.put("remediation", remediation());
        values.put("blocking", blocking());
        if (!documentId.isBlank()) {
            values.put("documentId", documentId);
        }
        if (!documentFileName.isBlank()) {
            values.put("documentFileName", documentFileName);
        }
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static AgenticCommerceWayangPersistenceHealthFinding documentFinding(
            String severity,
            AgenticCommerceWayangPersistenceDocumentStatus document,
            String code) {
        AgenticCommerceWayangPersistenceDocumentStatus resolved = document == null
                ? new AgenticCommerceWayangPersistenceDocumentStatus(null, false, false, null, null, null)
                : document;
        return new AgenticCommerceWayangPersistenceHealthFinding(
                severity,
                code,
                SOURCE_DOCUMENT,
                resolved.id(),
                resolved.fileName(),
                resolved.attributes());
    }

}
