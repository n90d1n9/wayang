package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Catalog and projection helpers for persistence transfer findings.
 */
public final class AgenticCommerceWayangPersistenceTransferFindings {

    public static final String UNKNOWN_CODE = "unknown_persistence_transfer_finding";
    public static final String TRANSFER_MISSING = "transfer_missing";
    public static final String DOCUMENT_BLOCKED_EXISTING = "document_blocked_existing";
    public static final String DOCUMENT_SKIPPED_MISSING_SOURCE = "document_skipped_missing_source";
    public static final String DRY_RUN_PREVIEW = "dry_run_preview";
    public static final String PLANNING_ONLY_PREVIEW = "planning_only_preview";

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition SOURCE_STATUS_FAILED =
            sourceError(
                    "source_status_failed",
                    "Source persistence status unavailable",
                    "Inspect the source persistence configuration, credentials, and connectivity.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition TARGET_STATUS_BEFORE_FAILED =
            targetError(
                    "target_status_before_failed",
                    "Target persistence status unavailable before transfer",
                    "Inspect the target persistence configuration, credentials, and connectivity before retrying.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition TARGET_STATUS_AFTER_FAILED =
            targetError(
                    "target_status_after_failed",
                    "Target persistence status unavailable after transfer",
                    "Inspect the target persistence backend and verify copied documents manually.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition RUNTIME_CONFIG_LOAD_FAILED =
            documentError(
                    AgenticCommerceWayangPersistenceDocuments.RUNTIME_CONFIG.loadFailureIssue(),
                    "Runtime config failed to load",
                    "Inspect the runtime config document in the source or target store.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition BOOTSTRAP_CONFIG_LOAD_FAILED =
            documentError(
                    AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_CONFIG.loadFailureIssue(),
                    "Bootstrap config failed to load",
                    "Inspect the bootstrap config document in the source or target store.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition BOOTSTRAP_REPORT_LOAD_FAILED =
            documentError(
                    AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_REPORT.loadFailureIssue(),
                    "Bootstrap report failed to load",
                    "Inspect or regenerate the bootstrap report document before retrying transfer.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition MANIFEST_LOAD_FAILED =
            documentError(
                    AgenticCommerceWayangPersistenceDocuments.MANIFEST.loadFailureIssue(),
                    "Manifest failed to load",
                    "Inspect or regenerate the manifest document before retrying transfer.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition RUNTIME_CONFIG_SAVE_FAILED =
            documentError(
                    "runtime_config_save_failed",
                    "Runtime config failed to save",
                    "Inspect target write permissions, credentials, and runtime config serialization.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition BOOTSTRAP_CONFIG_SAVE_FAILED =
            documentError(
                    "bootstrap_config_save_failed",
                    "Bootstrap config failed to save",
                    "Inspect target write permissions, credentials, and bootstrap config serialization.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition BOOTSTRAP_REPORT_SAVE_FAILED =
            documentError(
                    "bootstrap_report_save_failed",
                    "Bootstrap report failed to save",
                    "Inspect target write permissions, credentials, and bootstrap report serialization.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition MANIFEST_SAVE_FAILED =
            documentError(
                    "manifest_save_failed",
                    "Manifest failed to save",
                    "Inspect target write permissions, credentials, and manifest serialization.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition RUNTIME_CONFIG_VERIFY_MISSING =
            documentError(
                    "runtime_config_verify_missing",
                    "Runtime config verification target is missing",
                    "Verify target persistence visibility and retry transfer after the backend is consistent.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition BOOTSTRAP_CONFIG_VERIFY_MISSING =
            documentError(
                    "bootstrap_config_verify_missing",
                    "Bootstrap config verification target is missing",
                    "Verify target persistence visibility and retry transfer after the backend is consistent.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition BOOTSTRAP_REPORT_VERIFY_MISSING =
            documentError(
                    "bootstrap_report_verify_missing",
                    "Bootstrap report verification target is missing",
                    "Verify target persistence visibility and retry transfer after the backend is consistent.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition MANIFEST_VERIFY_MISSING =
            documentError(
                    "manifest_verify_missing",
                    "Manifest verification target is missing",
                    "Verify target persistence visibility and retry transfer after the backend is consistent.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition RUNTIME_CONFIG_VERIFY_MISMATCH =
            documentError(
                    "runtime_config_verify_mismatch",
                    "Runtime config verification mismatch",
                    "Compare the source and target runtime config documents before promoting the target store.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition BOOTSTRAP_CONFIG_VERIFY_MISMATCH =
            documentError(
                    "bootstrap_config_verify_mismatch",
                    "Bootstrap config verification mismatch",
                    "Compare the source and target bootstrap config documents before promoting the target store.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition BOOTSTRAP_REPORT_VERIFY_MISMATCH =
            documentError(
                    "bootstrap_report_verify_mismatch",
                    "Bootstrap report verification mismatch",
                    "Compare bootstrap report summaries before promoting the target store.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition MANIFEST_VERIFY_MISMATCH =
            documentError(
                    "manifest_verify_mismatch",
                    "Manifest verification mismatch",
                    "Compare manifest summaries before promoting the target store.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition DOCUMENT_BLOCKED =
            documentWarning(
                    DOCUMENT_BLOCKED_EXISTING,
                    "Document was blocked by no-overwrite policy",
                    "Enable overwrite for this transfer or remove the existing target document before retrying.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition DOCUMENT_SKIPPED =
            documentInfo(
                    DOCUMENT_SKIPPED_MISSING_SOURCE,
                    "Document was skipped because the source is missing",
                    "Persist the source document before transfer if this state should be migrated.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition DRY_RUN =
            transferInfo(
                    DRY_RUN_PREVIEW,
                    "Transfer was evaluated as a dry run",
                    "Review the copyable documents and run without dry-run to mutate the target.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition PLANNING_ONLY =
            transferInfo(
                    PLANNING_ONLY_PREVIEW,
                    "Transfer was evaluated as a plan",
                    "Review the plan and apply the transfer when the target changes are acceptable.");

    public static final AgenticCommerceWayangPersistenceTransferFindingDefinition MISSING_TRANSFER =
            transferError(
                    TRANSFER_MISSING,
                    "Transfer report is missing",
                    "Create a transfer report or plan before requesting transfer findings.");

    public static final List<AgenticCommerceWayangPersistenceTransferFindingDefinition> ALL = List.of(
            SOURCE_STATUS_FAILED,
            TARGET_STATUS_BEFORE_FAILED,
            TARGET_STATUS_AFTER_FAILED,
            RUNTIME_CONFIG_LOAD_FAILED,
            BOOTSTRAP_CONFIG_LOAD_FAILED,
            BOOTSTRAP_REPORT_LOAD_FAILED,
            MANIFEST_LOAD_FAILED,
            RUNTIME_CONFIG_SAVE_FAILED,
            BOOTSTRAP_CONFIG_SAVE_FAILED,
            BOOTSTRAP_REPORT_SAVE_FAILED,
            MANIFEST_SAVE_FAILED,
            RUNTIME_CONFIG_VERIFY_MISSING,
            BOOTSTRAP_CONFIG_VERIFY_MISSING,
            BOOTSTRAP_REPORT_VERIFY_MISSING,
            MANIFEST_VERIFY_MISSING,
            RUNTIME_CONFIG_VERIFY_MISMATCH,
            BOOTSTRAP_CONFIG_VERIFY_MISMATCH,
            BOOTSTRAP_REPORT_VERIFY_MISMATCH,
            MANIFEST_VERIFY_MISMATCH,
            DOCUMENT_BLOCKED,
            DOCUMENT_SKIPPED,
            DRY_RUN,
            PLANNING_ONLY,
            MISSING_TRANSFER);

    private AgenticCommerceWayangPersistenceTransferFindings() {
    }

    public static List<AgenticCommerceWayangPersistenceTransferFinding> from(
            AgenticCommerceWayangPersistenceTransferReport report) {
        if (report == null) {
            return List.of(AgenticCommerceWayangPersistenceTransferFinding.transferError(TRANSFER_MISSING));
        }
        List<AgenticCommerceWayangPersistenceTransferFinding> values = new ArrayList<>();
        addIssueFindings(values, report.issues(), report.documents());
        report.documents().stream()
                .filter(AgenticCommerceWayangPersistenceTransferDocumentStatus::blocked)
                .map(document -> AgenticCommerceWayangPersistenceTransferFinding.documentWarning(
                        document,
                        DOCUMENT_BLOCKED_EXISTING))
                .forEach(values::add);
        report.documents().stream()
                .filter(AgenticCommerceWayangPersistenceTransferDocumentStatus::skipped)
                .map(document -> AgenticCommerceWayangPersistenceTransferFinding.documentInfo(
                        document,
                        DOCUMENT_SKIPPED_MISSING_SOURCE))
                .forEach(values::add);
        if (report.options().dryRun() && report.summary().wouldMutateTarget()) {
            values.add(AgenticCommerceWayangPersistenceTransferFinding.transferInfo(
                    DRY_RUN_PREVIEW,
                    summaryAttributes(report.summary())));
        }
        return List.copyOf(values);
    }

    public static List<AgenticCommerceWayangPersistenceTransferFinding> from(
            AgenticCommerceWayangPersistenceTransferPlan plan) {
        if (plan == null) {
            return List.of(AgenticCommerceWayangPersistenceTransferFinding.transferError(TRANSFER_MISSING));
        }
        List<AgenticCommerceWayangPersistenceTransferFinding> values = new ArrayList<>();
        addIssueFindings(values, plan.issues(), plan.documents());
        plan.documents().stream()
                .filter(AgenticCommerceWayangPersistenceTransferDocumentStatus::blocked)
                .map(document -> AgenticCommerceWayangPersistenceTransferFinding.documentWarning(
                        document,
                        DOCUMENT_BLOCKED_EXISTING))
                .forEach(values::add);
        plan.documents().stream()
                .filter(AgenticCommerceWayangPersistenceTransferDocumentStatus::skipped)
                .map(document -> AgenticCommerceWayangPersistenceTransferFinding.documentInfo(
                        document,
                        DOCUMENT_SKIPPED_MISSING_SOURCE))
                .forEach(values::add);
        if (plan.options().dryRun() && plan.summary().wouldMutateTarget()) {
            values.add(AgenticCommerceWayangPersistenceTransferFinding.transferInfo(
                    DRY_RUN_PREVIEW,
                    summaryAttributes(plan.summary())));
        }
        if (plan.summary().wouldMutateTarget()) {
            values.add(AgenticCommerceWayangPersistenceTransferFinding.transferInfo(
                    PLANNING_ONLY_PREVIEW,
                    summaryAttributes(plan.summary())));
        }
        return List.copyOf(values);
    }

    public static Optional<AgenticCommerceWayangPersistenceTransferFindingDefinition> find(String code) {
        String normalized = AgenticCommerceWayangMaps.text(code);
        return ALL.stream()
                .filter(definition -> definition.code().equals(normalized))
                .findFirst();
    }

    public static AgenticCommerceWayangPersistenceTransferFindingDefinition definition(
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
                .map(AgenticCommerceWayangPersistenceTransferFindingDefinition::toMap)
                .toList();
    }

    static String normalizeSeverity(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value).toLowerCase(Locale.ROOT);
        if (AgenticCommerceWayangPersistenceTransferFinding.SEVERITY_ERROR.equals(normalized)
                || AgenticCommerceWayangPersistenceTransferFinding.SEVERITY_WARNING.equals(normalized)
                || AgenticCommerceWayangPersistenceTransferFinding.SEVERITY_INFO.equals(normalized)) {
            return normalized;
        }
        return AgenticCommerceWayangPersistenceTransferFinding.SEVERITY_INFO;
    }

    static String normalizeSource(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value).toLowerCase(Locale.ROOT);
        if (AgenticCommerceWayangPersistenceTransferFinding.SOURCE_TRANSFER.equals(normalized)
                || AgenticCommerceWayangPersistenceTransferFinding.SOURCE_SOURCE.equals(normalized)
                || AgenticCommerceWayangPersistenceTransferFinding.SOURCE_TARGET.equals(normalized)
                || AgenticCommerceWayangPersistenceTransferFinding.SOURCE_DOCUMENT.equals(normalized)) {
            return normalized;
        }
        return AgenticCommerceWayangPersistenceTransferFinding.SOURCE_TRANSFER;
    }

    private static void addIssueFindings(
            List<AgenticCommerceWayangPersistenceTransferFinding> values,
            List<String> issues,
            List<AgenticCommerceWayangPersistenceTransferDocumentStatus> documents) {
        Set<String> documentIssues = new HashSet<>();
        for (AgenticCommerceWayangPersistenceTransferDocumentStatus document : documents) {
            for (String issue : document.issues()) {
                documentIssues.add(issue);
                values.add(AgenticCommerceWayangPersistenceTransferFinding.documentError(document, issue));
            }
        }
        AgenticCommerceWayangMaps.stringList(issues).stream()
                .filter(issue -> !documentIssues.contains(issue))
                .map(AgenticCommerceWayangPersistenceTransferFindings::issueFinding)
                .forEach(values::add);
    }

    private static AgenticCommerceWayangPersistenceTransferFinding issueFinding(String issue) {
        if ("source_status_failed".equals(issue)) {
            return AgenticCommerceWayangPersistenceTransferFinding.sourceError(issue);
        }
        if ("target_status_before_failed".equals(issue) || "target_status_after_failed".equals(issue)) {
            return AgenticCommerceWayangPersistenceTransferFinding.targetError(issue);
        }
        return AgenticCommerceWayangPersistenceTransferFinding.transferError(issue);
    }

    private static Map<String, Object> summaryAttributes(
            AgenticCommerceWayangPersistenceTransferSummary summary) {
        return Map.of(
                "plannedDocumentCount",
                summary.plannedDocumentCount(),
                "copyableDocumentCount",
                summary.copyableDocumentCount(),
                "wouldMutateTarget",
                summary.wouldMutateTarget(),
                "copyableDocumentIds",
                summary.copyableDocumentIds());
    }

    private static AgenticCommerceWayangPersistenceTransferFindingDefinition fallback(
            String code,
            String severity,
            String source) {
        String resolvedSeverity = normalizeSeverity(severity);
        return new AgenticCommerceWayangPersistenceTransferFindingDefinition(
                code,
                resolvedSeverity,
                normalizeSource(source),
                fallbackTitle(code),
                "Inspect the persistence transfer report details for this finding.",
                AgenticCommerceWayangPersistenceTransferFinding.SEVERITY_ERROR.equals(resolvedSeverity),
                Map.of("catalogFallback", true));
    }

    private static AgenticCommerceWayangPersistenceTransferFindingDefinition sourceError(
            String code,
            String title,
            String remediation) {
        return error(code, AgenticCommerceWayangPersistenceTransferFinding.SOURCE_SOURCE, title, remediation);
    }

    private static AgenticCommerceWayangPersistenceTransferFindingDefinition targetError(
            String code,
            String title,
            String remediation) {
        return error(code, AgenticCommerceWayangPersistenceTransferFinding.SOURCE_TARGET, title, remediation);
    }

    private static AgenticCommerceWayangPersistenceTransferFindingDefinition transferError(
            String code,
            String title,
            String remediation) {
        return error(code, AgenticCommerceWayangPersistenceTransferFinding.SOURCE_TRANSFER, title, remediation);
    }

    private static AgenticCommerceWayangPersistenceTransferFindingDefinition documentError(
            String code,
            String title,
            String remediation) {
        return error(code, AgenticCommerceWayangPersistenceTransferFinding.SOURCE_DOCUMENT, title, remediation);
    }

    private static AgenticCommerceWayangPersistenceTransferFindingDefinition documentWarning(
            String code,
            String title,
            String remediation) {
        return definition(
                code,
                AgenticCommerceWayangPersistenceTransferFinding.SEVERITY_WARNING,
                AgenticCommerceWayangPersistenceTransferFinding.SOURCE_DOCUMENT,
                title,
                remediation,
                false);
    }

    private static AgenticCommerceWayangPersistenceTransferFindingDefinition documentInfo(
            String code,
            String title,
            String remediation) {
        return definition(
                code,
                AgenticCommerceWayangPersistenceTransferFinding.SEVERITY_INFO,
                AgenticCommerceWayangPersistenceTransferFinding.SOURCE_DOCUMENT,
                title,
                remediation,
                false);
    }

    private static AgenticCommerceWayangPersistenceTransferFindingDefinition transferInfo(
            String code,
            String title,
            String remediation) {
        return definition(
                code,
                AgenticCommerceWayangPersistenceTransferFinding.SEVERITY_INFO,
                AgenticCommerceWayangPersistenceTransferFinding.SOURCE_TRANSFER,
                title,
                remediation,
                false);
    }

    private static AgenticCommerceWayangPersistenceTransferFindingDefinition error(
            String code,
            String source,
            String title,
            String remediation) {
        return definition(
                code,
                AgenticCommerceWayangPersistenceTransferFinding.SEVERITY_ERROR,
                source,
                title,
                remediation,
                true);
    }

    private static AgenticCommerceWayangPersistenceTransferFindingDefinition definition(
            String code,
            String severity,
            String source,
            String title,
            String remediation,
            boolean blocking) {
        return new AgenticCommerceWayangPersistenceTransferFindingDefinition(
                code,
                severity,
                source,
                title,
                remediation,
                blocking,
                Map.of());
    }

    private static String fallbackTitle(String code) {
        String normalized = AgenticCommerceWayangMaps.text(code);
        if (normalized.isBlank() || UNKNOWN_CODE.equals(normalized)) {
            return "Unknown persistence transfer finding";
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
        return title.length() == 0 ? "Unknown persistence transfer finding" : title.toString();
    }
}
