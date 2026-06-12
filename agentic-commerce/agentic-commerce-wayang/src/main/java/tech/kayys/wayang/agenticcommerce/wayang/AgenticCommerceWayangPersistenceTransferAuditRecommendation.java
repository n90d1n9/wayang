package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_STORAGE_DISABLED;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_STORAGE_EPHEMERAL;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditContractIssues.SINK_BUILD_FAILED;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditDiagnosticsIssues.AUDIT_CONTRACT_NOT_RUN;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditDiagnosticsIssues.AUDIT_RELOAD_NOT_CHECKED;

/**
 * Operator action derived from transfer audit diagnostics.
 */
public record AgenticCommerceWayangPersistenceTransferAuditRecommendation(
        String action,
        String priority,
        String title,
        String rationale,
        boolean blocking,
        List<String> issues,
        List<String> warnings,
        Map<String, Object> attributes) {

    public static final String PRIORITY_PRIMARY = "primary";
    public static final String PRIORITY_SECONDARY = "secondary";

    public static final String ACTION_ENABLE_AUDIT_STORAGE = "enable_audit_storage";
    public static final String ACTION_CONFIGURE_DURABLE_AUDIT_STORAGE = "configure_durable_audit_storage";
    public static final String ACTION_RUN_AUDIT_CONTRACT = "run_audit_contract";
    public static final String ACTION_VERIFY_AUDIT_RELOAD = "verify_audit_reload";
    public static final String ACTION_FIX_AUDIT_CONFIG = "fix_audit_config";
    public static final String ACTION_RESOLVE_AUDIT_PROVIDER = "resolve_audit_provider";
    public static final String ACTION_REVIEW_AUDIT_CONTRACT = "review_audit_contract";

    public AgenticCommerceWayangPersistenceTransferAuditRecommendation {
        action = AgenticCommerceWayangMaps.required(action, "action");
        priority = normalizePriority(priority);
        title = AgenticCommerceWayangMaps.text(title);
        rationale = AgenticCommerceWayangMaps.text(rationale);
        issues = AgenticCommerceWayangMaps.stringList(issues);
        warnings = AgenticCommerceWayangMaps.stringList(warnings);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static List<AgenticCommerceWayangPersistenceTransferAuditRecommendation> from(
            AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics) {
        if (diagnostics == null) {
            return List.of();
        }
        java.util.ArrayList<AgenticCommerceWayangPersistenceTransferAuditRecommendation> values =
                new java.util.ArrayList<>();
        if (!diagnostics.auditEnabled()) {
            values.add(enableAuditStorage(diagnostics));
        }
        if (!diagnostics.validationReport().valid()) {
            values.add(fixAuditConfig(diagnostics));
            return List.copyOf(values);
        }
        if (diagnostics.config().memoryStore()) {
            values.add(configureDurableAuditStorage(diagnostics));
        }
        if (!diagnostics.contractAvailable()) {
            if (diagnostics.auditEnabled()) {
                values.add(runAuditContract(diagnostics));
            }
        } else {
            if (diagnostics.durableAuditStorage() && !diagnostics.contractReport().reloadAttempted()) {
                values.add(verifyAuditReload(diagnostics));
            }
            if (!diagnostics.contractReport().passed()) {
                if (diagnostics.issues().contains(SINK_BUILD_FAILED)) {
                    values.add(resolveAuditProvider(diagnostics));
                } else {
                    values.add(reviewAuditContract(diagnostics));
                }
            }
        }
        return List.copyOf(values);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("action", action);
        values.put("priority", priority);
        values.put("title", title);
        values.put("rationale", rationale);
        values.put("blocking", blocking);
        values.put("issues", issues);
        values.put("warnings", warnings);
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static AgenticCommerceWayangPersistenceTransferAuditRecommendation enableAuditStorage(
            AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics) {
        return recommendation(
                ACTION_ENABLE_AUDIT_STORAGE,
                PRIORITY_SECONDARY,
                "Enable transfer audit storage",
                "Transfer audit storage is disabled, so transfer operations will not retain audit evidence.",
                false,
                diagnostics.issues(),
                List.of(AUDIT_STORAGE_DISABLED),
                baseAttributes(diagnostics));
    }

    private static AgenticCommerceWayangPersistenceTransferAuditRecommendation configureDurableAuditStorage(
            AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics) {
        return recommendation(
                ACTION_CONFIGURE_DURABLE_AUDIT_STORAGE,
                PRIORITY_SECONDARY,
                "Configure durable audit storage",
                "In-memory audit storage is useful for local diagnostics but will not survive a restart.",
                false,
                diagnostics.issues(),
                List.of(AUDIT_STORAGE_EPHEMERAL),
                baseAttributes(diagnostics));
    }

    private static AgenticCommerceWayangPersistenceTransferAuditRecommendation runAuditContract(
            AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics) {
        return recommendation(
                ACTION_RUN_AUDIT_CONTRACT,
                diagnostics.durableAuditStorage() ? PRIORITY_PRIMARY : PRIORITY_SECONDARY,
                "Run audit storage contract",
                "The configured audit backend has not been checked against the retained-history contract.",
                false,
                diagnostics.issues(),
                List.of(AUDIT_CONTRACT_NOT_RUN),
                baseAttributes(diagnostics));
    }

    private static AgenticCommerceWayangPersistenceTransferAuditRecommendation verifyAuditReload(
            AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics) {
        return recommendation(
                ACTION_VERIFY_AUDIT_RELOAD,
                PRIORITY_SECONDARY,
                "Verify audit reload",
                "Durable audit storage passed the live contract but reload behavior was not checked.",
                false,
                diagnostics.issues(),
                List.of(AUDIT_RELOAD_NOT_CHECKED),
                baseAttributes(diagnostics));
    }

    private static AgenticCommerceWayangPersistenceTransferAuditRecommendation fixAuditConfig(
            AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics) {
        return recommendation(
                ACTION_FIX_AUDIT_CONFIG,
                PRIORITY_PRIMARY,
                "Fix audit storage config",
                "The configured audit storage has validation errors that should be resolved before contract checks.",
                true,
                diagnostics.validationReport().errorCodes(),
                diagnostics.validationReport().warningCodes(),
                fixAuditConfigAttributes(diagnostics));
    }

    private static AgenticCommerceWayangPersistenceTransferAuditRecommendation resolveAuditProvider(
            AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics) {
        return recommendation(
                ACTION_RESOLVE_AUDIT_PROVIDER,
                PRIORITY_PRIMARY,
                "Resolve audit provider configuration",
                "The configured audit backend could not be constructed by the provider registry.",
                true,
                diagnostics.issues(),
                diagnostics.warnings(),
                baseAttributes(diagnostics));
    }

    private static AgenticCommerceWayangPersistenceTransferAuditRecommendation reviewAuditContract(
            AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics) {
        return recommendation(
                ACTION_REVIEW_AUDIT_CONTRACT,
                PRIORITY_PRIMARY,
                "Review audit contract failures",
                "The audit backend was constructed but failed retained-history, query, record, or reload checks.",
                true,
                diagnostics.issues(),
                diagnostics.warnings(),
                baseAttributes(diagnostics));
    }

    private static Map<String, Object> baseAttributes(
            AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("storageKind", diagnostics.config().storageKind());
        values.put("diagnosticStatus", diagnostics.diagnosticStatus());
        values.put("auditEnabled", diagnostics.auditEnabled());
        values.put("contractAvailable", diagnostics.contractAvailable());
        values.put("durableAuditStorage", diagnostics.durableAuditStorage());
        values.put("issueCount", diagnostics.issueCount());
        values.put("warningCount", diagnostics.warningCount());
        return Map.copyOf(values);
    }

    private static Map<String, Object> fixAuditConfigAttributes(
            AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics) {
        Map<String, Object> values = new LinkedHashMap<>(baseAttributes(diagnostics));
        values.put("validationErrorCodes", diagnostics.validationReport().errorCodes());
        values.put("validationWarningCodes", diagnostics.validationReport().warningCodes());
        List<AgenticCommerceWayangPersistenceTransferAuditConfigRemediation> remediations =
                diagnostics.validationReport().remediations();
        List<Map<String, Object>> remediation = remediations.stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditConfigRemediation::toMap)
                .toList();
        List<Map<String, Object>> patches = remediations.stream()
                .flatMap(item -> item.patches().stream())
                .map(AgenticCommerceWayangPersistenceTransferAuditConfigPatch::toMap)
                .toList();
        values.put("remediationCount", remediation.size());
        values.put("remediation", remediation);
        values.put("patchCount", patches.size());
        values.put("patches", patches);
        values.put(
                "patchApplication",
                AgenticCommerceWayangPersistenceTransferAuditConfigPatchApplicationReport
                        .fromConfig(diagnostics.config())
                        .toSummaryMap());
        return Map.copyOf(values);
    }

    private static AgenticCommerceWayangPersistenceTransferAuditRecommendation recommendation(
            String action,
            String priority,
            String title,
            String rationale,
            boolean blocking,
            List<String> issues,
            List<String> warnings,
            Map<String, Object> attributes) {
        return new AgenticCommerceWayangPersistenceTransferAuditRecommendation(
                action,
                priority,
                title,
                rationale,
                blocking,
                issues,
                warnings,
                attributes);
    }

    private static String normalizePriority(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value);
        if (PRIORITY_PRIMARY.equals(normalized) || PRIORITY_SECONDARY.equals(normalized)) {
            return normalized;
        }
        return PRIORITY_SECONDARY;
    }
}
