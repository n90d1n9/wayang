package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Catalog of persistence health finding codes and operator guidance.
 */
public final class AgenticCommerceWayangPersistenceHealthFindings {

    public static final String UNKNOWN_CODE = "unknown_persistence_health_finding";

    public static final AgenticCommerceWayangPersistenceHealthFindingDefinition STORE_STATUS_FAILED =
            error(
                    "store_status_failed",
                    AgenticCommerceWayangPersistenceHealthFinding.SOURCE_STORE,
                    "Persistence store status unavailable",
                    "Inspect the persistence store configuration, credentials, and connectivity.",
                    true);

    public static final AgenticCommerceWayangPersistenceHealthFindingDefinition RUNTIME_CONFIG_LOAD_FAILED =
            documentError(
                    AgenticCommerceWayangPersistenceDocuments.RUNTIME_CONFIG.loadFailureIssue(),
                    "Runtime config failed to load",
                    "Inspect the runtime config document and repair or replace the persisted JSON.");

    public static final AgenticCommerceWayangPersistenceHealthFindingDefinition BOOTSTRAP_CONFIG_LOAD_FAILED =
            documentError(
                    AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_CONFIG.loadFailureIssue(),
                    "Bootstrap config failed to load",
                    "Inspect the bootstrap config document and repair or replace the persisted JSON.");

    public static final AgenticCommerceWayangPersistenceHealthFindingDefinition BOOTSTRAP_REPORT_LOAD_FAILED =
            documentError(
                    AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_REPORT.loadFailureIssue(),
                    "Bootstrap report failed to load",
                    "Inspect the bootstrap report document and regenerate the persisted bootstrap report.");

    public static final AgenticCommerceWayangPersistenceHealthFindingDefinition MANIFEST_LOAD_FAILED =
            documentError(
                    AgenticCommerceWayangPersistenceDocuments.MANIFEST.loadFailureIssue(),
                    "Manifest failed to load",
                    "Inspect the manifest document and regenerate the persisted Agentic Commerce manifest.");

    public static final AgenticCommerceWayangPersistenceHealthFindingDefinition PERSISTENCE_STORE_EPHEMERAL =
            warning(
                    "persistence_store_ephemeral",
                    AgenticCommerceWayangPersistenceHealthFinding.SOURCE_STORE,
                    "Persistence store is ephemeral",
                    "Use a durable file, object, hybrid, or database-backed store for production recovery.");

    public static final AgenticCommerceWayangPersistenceHealthFindingDefinition PERSISTENCE_STORE_NOT_DURABLE =
            warning(
                    "persistence_store_not_durable",
                    AgenticCommerceWayangPersistenceHealthFinding.SOURCE_STORE,
                    "Persistence store is not durable",
                    "Configure a durable persistence backend before relying on the runtime for recovery.");

    public static final AgenticCommerceWayangPersistenceHealthFindingDefinition HYBRID_WRITES_NOT_MIRRORED =
            warning(
                    "hybrid_writes_not_mirrored",
                    AgenticCommerceWayangPersistenceHealthFinding.SOURCE_STORE,
                    "Hybrid writes are not mirrored",
                    "Enable mirrored fallback writes or verify the primary store has its own recovery path.");

    public static final AgenticCommerceWayangPersistenceHealthFindingDefinition RUNTIME_CONFIG_MISSING =
            documentWarning(
                    AgenticCommerceWayangPersistenceDocuments.RUNTIME_CONFIG.missingWarning(),
                    "Runtime config is missing",
                    "Persist a runtime config or accept default runtime configuration for this environment.");

    public static final AgenticCommerceWayangPersistenceHealthFindingDefinition BOOTSTRAP_CONFIG_MISSING =
            documentWarning(
                    AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_CONFIG.missingWarning(),
                    "Bootstrap config is missing",
                    "Persist a bootstrap config or accept default checkout skill bootstrap settings.");

    public static final AgenticCommerceWayangPersistenceHealthFindingDefinition BOOTSTRAP_REPORT_MISSING =
            documentWarning(
                    AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_REPORT.missingWarning(),
                    "Bootstrap report is missing",
                    "Run bootstrap and persist the resulting report to capture the installed skill surface.");

    public static final AgenticCommerceWayangPersistenceHealthFindingDefinition MANIFEST_MISSING =
            documentWarning(
                    AgenticCommerceWayangPersistenceDocuments.MANIFEST.missingWarning(),
                    "Manifest is missing",
                    "Generate and persist the Agentic Commerce manifest for discovery and audit consumers.");

    public static final List<AgenticCommerceWayangPersistenceHealthFindingDefinition> ALL = List.of(
            STORE_STATUS_FAILED,
            RUNTIME_CONFIG_LOAD_FAILED,
            BOOTSTRAP_CONFIG_LOAD_FAILED,
            BOOTSTRAP_REPORT_LOAD_FAILED,
            MANIFEST_LOAD_FAILED,
            PERSISTENCE_STORE_EPHEMERAL,
            PERSISTENCE_STORE_NOT_DURABLE,
            HYBRID_WRITES_NOT_MIRRORED,
            RUNTIME_CONFIG_MISSING,
            BOOTSTRAP_CONFIG_MISSING,
            BOOTSTRAP_REPORT_MISSING,
            MANIFEST_MISSING);

    private AgenticCommerceWayangPersistenceHealthFindings() {
    }

    public static Optional<AgenticCommerceWayangPersistenceHealthFindingDefinition> find(String code) {
        String normalized = AgenticCommerceWayangMaps.text(code);
        return ALL.stream()
                .filter(definition -> definition.code().equals(normalized))
                .findFirst();
    }

    public static AgenticCommerceWayangPersistenceHealthFindingDefinition definition(
            String code,
            String severity,
            String source) {
        String normalized = AgenticCommerceWayangMaps.text(code);
        if (normalized.isBlank()) {
            normalized = UNKNOWN_CODE;
        }
        String resolvedCode = normalized;
        return find(resolvedCode).orElseGet(() -> fallback(resolvedCode, severity, source));
    }

    public static List<Map<String, Object>> toMapList() {
        return ALL.stream()
                .map(AgenticCommerceWayangPersistenceHealthFindingDefinition::toMap)
                .toList();
    }

    static String normalizeSeverity(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value).toLowerCase(Locale.ROOT);
        if (AgenticCommerceWayangPersistenceHealthFinding.SEVERITY_ERROR.equals(normalized)
                || AgenticCommerceWayangPersistenceHealthFinding.SEVERITY_WARNING.equals(normalized)) {
            return normalized;
        }
        return AgenticCommerceWayangPersistenceHealthFinding.SEVERITY_WARNING;
    }

    static String normalizeSource(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value).toLowerCase(Locale.ROOT);
        if (AgenticCommerceWayangPersistenceHealthFinding.SOURCE_DOCUMENT.equals(normalized)
                || AgenticCommerceWayangPersistenceHealthFinding.SOURCE_STORE.equals(normalized)) {
            return normalized;
        }
        return AgenticCommerceWayangPersistenceHealthFinding.SOURCE_STORE;
    }

    private static AgenticCommerceWayangPersistenceHealthFindingDefinition fallback(
            String code,
            String severity,
            String source) {
        String resolvedSeverity = normalizeSeverity(severity);
        return new AgenticCommerceWayangPersistenceHealthFindingDefinition(
                code,
                resolvedSeverity,
                normalizeSource(source),
                fallbackTitle(code),
                "Inspect the persistence health report details for this finding.",
                AgenticCommerceWayangPersistenceHealthFinding.SEVERITY_ERROR.equals(resolvedSeverity),
                Map.of("catalogFallback", true));
    }

    private static AgenticCommerceWayangPersistenceHealthFindingDefinition error(
            String code,
            String source,
            String title,
            String remediation,
            boolean blocking) {
        return new AgenticCommerceWayangPersistenceHealthFindingDefinition(
                code,
                AgenticCommerceWayangPersistenceHealthFinding.SEVERITY_ERROR,
                source,
                title,
                remediation,
                blocking,
                Map.of());
    }

    private static AgenticCommerceWayangPersistenceHealthFindingDefinition warning(
            String code,
            String source,
            String title,
            String remediation) {
        return new AgenticCommerceWayangPersistenceHealthFindingDefinition(
                code,
                AgenticCommerceWayangPersistenceHealthFinding.SEVERITY_WARNING,
                source,
                title,
                remediation,
                false,
                Map.of());
    }

    private static AgenticCommerceWayangPersistenceHealthFindingDefinition documentError(
            String code,
            String title,
            String remediation) {
        return error(code, AgenticCommerceWayangPersistenceHealthFinding.SOURCE_DOCUMENT, title, remediation, true);
    }

    private static AgenticCommerceWayangPersistenceHealthFindingDefinition documentWarning(
            String code,
            String title,
            String remediation) {
        return warning(code, AgenticCommerceWayangPersistenceHealthFinding.SOURCE_DOCUMENT, title, remediation);
    }

    private static String fallbackTitle(String code) {
        String normalized = AgenticCommerceWayangMaps.text(code);
        if (normalized.isBlank() || UNKNOWN_CODE.equals(normalized)) {
            return "Unknown persistence health finding";
        }
        String[] tokens = normalized.split("_+");
        StringBuilder title = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            if (title.length() > 0) {
                title.append(' ');
            }
            title.append(token.substring(0, 1).toUpperCase(Locale.ROOT));
            if (token.length() > 1) {
                title.append(token.substring(1));
            }
        }
        return title.length() == 0 ? "Unknown persistence health finding" : title.toString();
    }
}
