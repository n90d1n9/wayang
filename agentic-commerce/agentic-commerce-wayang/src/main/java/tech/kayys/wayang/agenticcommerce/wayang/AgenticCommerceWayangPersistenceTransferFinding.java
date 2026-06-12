package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Structured persistence transfer finding for operator and API consumers.
 */
public record AgenticCommerceWayangPersistenceTransferFinding(
        String severity,
        String code,
        String source,
        String documentId,
        String documentFileName,
        Map<String, Object> attributes) {

    public static final String SEVERITY_ERROR = "error";
    public static final String SEVERITY_WARNING = "warning";
    public static final String SEVERITY_INFO = "info";
    public static final String SOURCE_TRANSFER = "transfer";
    public static final String SOURCE_SOURCE = "source";
    public static final String SOURCE_TARGET = "target";
    public static final String SOURCE_DOCUMENT = "document";

    public AgenticCommerceWayangPersistenceTransferFinding {
        code = AgenticCommerceWayangMaps.text(code);
        AgenticCommerceWayangPersistenceTransferFindingDefinition definition =
                AgenticCommerceWayangPersistenceTransferFindings.definition(code, severity, source);
        code = definition.code();
        severity = definition.severity();
        source = AgenticCommerceWayangMaps.text(source).isBlank()
                ? definition.source()
                : AgenticCommerceWayangPersistenceTransferFindings.normalizeSource(source);
        documentId = AgenticCommerceWayangMaps.text(documentId);
        documentFileName = AgenticCommerceWayangMaps.text(documentFileName);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceTransferFinding transferError(String code) {
        return transferFinding(SEVERITY_ERROR, code, Map.of());
    }

    public static AgenticCommerceWayangPersistenceTransferFinding transferInfo(
            String code,
            Map<String, Object> attributes) {
        return transferFinding(SEVERITY_INFO, code, attributes);
    }

    public static AgenticCommerceWayangPersistenceTransferFinding sourceError(String code) {
        return new AgenticCommerceWayangPersistenceTransferFinding(
                SEVERITY_ERROR,
                code,
                SOURCE_SOURCE,
                "",
                "",
                Map.of());
    }

    public static AgenticCommerceWayangPersistenceTransferFinding targetError(String code) {
        return new AgenticCommerceWayangPersistenceTransferFinding(
                SEVERITY_ERROR,
                code,
                SOURCE_TARGET,
                "",
                "",
                Map.of());
    }

    public static AgenticCommerceWayangPersistenceTransferFinding documentError(
            AgenticCommerceWayangPersistenceTransferDocumentStatus document,
            String code) {
        return documentFinding(SEVERITY_ERROR, document, code);
    }

    public static AgenticCommerceWayangPersistenceTransferFinding documentWarning(
            AgenticCommerceWayangPersistenceTransferDocumentStatus document,
            String code) {
        return documentFinding(SEVERITY_WARNING, document, code);
    }

    public static AgenticCommerceWayangPersistenceTransferFinding documentInfo(
            AgenticCommerceWayangPersistenceTransferDocumentStatus document,
            String code) {
        return documentFinding(SEVERITY_INFO, document, code);
    }

    public boolean error() {
        return SEVERITY_ERROR.equals(severity);
    }

    public boolean warning() {
        return SEVERITY_WARNING.equals(severity);
    }

    public boolean info() {
        return SEVERITY_INFO.equals(severity);
    }

    public boolean documentScoped() {
        return SOURCE_DOCUMENT.equals(source) && !documentId.isBlank();
    }

    public AgenticCommerceWayangPersistenceTransferFindingDefinition definition() {
        return AgenticCommerceWayangPersistenceTransferFindings.definition(code, severity, source);
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

    private static AgenticCommerceWayangPersistenceTransferFinding transferFinding(
            String severity,
            String code,
            Map<String, Object> attributes) {
        return new AgenticCommerceWayangPersistenceTransferFinding(
                severity,
                code,
                SOURCE_TRANSFER,
                "",
                "",
                attributes);
    }

    private static AgenticCommerceWayangPersistenceTransferFinding documentFinding(
            String severity,
            AgenticCommerceWayangPersistenceTransferDocumentStatus document,
            String code) {
        if (document == null) {
            return new AgenticCommerceWayangPersistenceTransferFinding(
                    severity,
                    code,
                    SOURCE_DOCUMENT,
                    "",
                    "",
                    Map.of());
        }
        return new AgenticCommerceWayangPersistenceTransferFinding(
                severity,
                code,
                SOURCE_DOCUMENT,
                document.id(),
                document.fileName(),
                documentAttributes(document));
    }

    private static Map<String, Object> documentAttributes(
            AgenticCommerceWayangPersistenceTransferDocumentStatus document) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("action", document.action());
        values.put("planned", document.planned());
        values.put("copied", document.copied());
        values.put("skipped", document.skipped());
        values.put("blocked", document.blocked());
        values.put("copyable", document.copyable());
        values.put("dryRun", document.dryRun());
        values.put("planningOnly", document.planningOnly());
        return Map.copyOf(values);
    }
}
