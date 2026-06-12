package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.gollek.sdk.WayangReadinessReport;
import tech.kayys.wayang.gollek.sdk.WayangReadinessReports;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

record AgenticCommerceWayangPersistenceTransferAuditReadiness(
        AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics) {

    AgenticCommerceWayangPersistenceTransferAuditReadiness {
        diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    static AgenticCommerceWayangPersistenceTransferAuditReadiness from(
            AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics) {
        return new AgenticCommerceWayangPersistenceTransferAuditReadiness(diagnostics);
    }

    WayangReadinessReport toReport() {
        return WayangReadinessReport.from(
                AgenticCommerceWayangPersistenceTransferAuditDiagnostics.READINESS_ID,
                diagnostics.ready(),
                WayangReadinessReports.exitCode(diagnostics.ready()),
                diagnostics.issueCount(),
                probes(),
                issues(),
                attributes());
    }

    private List<Map<String, Object>> probes() {
        return List.of(
                configProbe(),
                storageProbe(),
                contractProbe());
    }

    private Map<String, Object> configProbe() {
        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport validation =
                diagnostics.validationReport();
        return WayangReadinessReports.probe(
                "auditConfig",
                true,
                validation.valid(),
                validation.errorCount(),
                validation.toMap());
    }

    private Map<String, Object> storageProbe() {
        Map<String, Object> report = diagnostics.toMap();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("auditEnabled", diagnostics.auditEnabled());
        attributes.put("durableAuditStorage", diagnostics.durableAuditStorage());
        attributes.put("storage", report.get("storage"));
        attributes.put("target", report.get("target"));
        return WayangReadinessReports.probe(
                "auditStorage",
                false,
                !diagnostics.auditEnabled() || diagnostics.validationReport().valid(),
                diagnostics.validationReport().valid() ? 0 : diagnostics.validationReport().errorCount(),
                attributes);
    }

    private Map<String, Object> contractProbe() {
        AgenticCommerceWayangPersistenceTransferAuditContractReport contract = diagnostics.contractReport();
        if (contract == null) {
            return WayangReadinessReports.probe(
                    "auditContract",
                    false,
                    true,
                    0,
                    Map.of("contractAvailable", false));
        }
        return WayangReadinessReports.probe(
                "auditContract",
                false,
                contract.passed(),
                contract.issueCount(),
                contractAttributes(contract));
    }

    private List<Map<String, Object>> issues() {
        return diagnostics.issues().stream()
                .map(this::issue)
                .toList();
    }

    private Map<String, Object> issue(String code) {
        return WayangReadinessReports.issue(code, issueSource(code), code.replace('_', ' '));
    }

    private String issueSource(String code) {
        AgenticCommerceWayangPersistenceTransferAuditContractReport contract = diagnostics.contractReport();
        if (contract != null && contract.issues().contains(code)) {
            return "contract";
        }
        if (diagnostics.validationReport().errorCodes().contains(code)) {
            return "config";
        }
        return "audit";
    }

    private Map<String, Object> attributes() {
        Map<String, Object> report = diagnostics.toMap();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("diagnosticStatus", diagnostics.diagnosticStatus());
        values.put("auditEnabled", diagnostics.auditEnabled());
        values.put("contractAvailable", diagnostics.contractAvailable());
        values.put("durableAuditStorage", diagnostics.durableAuditStorage());
        values.put("warningCount", diagnostics.warningCount());
        values.put("warnings", diagnostics.warnings());
        values.put("recommendationCount", diagnostics.recommendationCount());
        values.put("blockingRecommendationCount", diagnostics.blockingRecommendationCount());
        values.put("recommendationActions", diagnostics.recommendationActions());
        values.put("recommendations", report.get("recommendations"));
        values.put("validation", report.get("validation"));
        values.put("storage", report.get("storage"));
        values.put("target", report.get("target"));
        values.put("provider", report.get("provider"));
        if (diagnostics.contractReport() != null) {
            values.put("contract", contractAttributes(diagnostics.contractReport()));
        }
        return AgenticCommerceWayangMaps.copy(values);
    }

    private static Map<String, Object> contractAttributes(
            AgenticCommerceWayangPersistenceTransferAuditContractReport contract) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contractAvailable", true);
        values.put("contractId", contract.contractId());
        values.put("passed", contract.passed());
        values.put("issueCount", contract.issueCount());
        values.put("issues", contract.issues());
        values.put("expectedRetainedTrailCount", contract.expectedRetainedTrailCount());
        values.put("retainedTrailCount", contract.retainedTrailCount());
        values.put("reloadAttempted", contract.reloadAttempted());
        values.put("reloadTrailCount", contract.reloadTrailCount());
        return AgenticCommerceWayangMaps.copy(values);
    }
}
