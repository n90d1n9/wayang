package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.gollek.sdk.WayangReadinessReport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_STORAGE_DISABLED;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_STORAGE_EPHEMERAL;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditDiagnosticsIssues.AUDIT_CONTRACT_NOT_RUN;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditDiagnosticsIssues.AUDIT_RELOAD_NOT_CHECKED;

/**
 * Compact operator diagnostics for configured transfer audit storage.
 */
public record AgenticCommerceWayangPersistenceTransferAuditDiagnostics(
        AgenticCommerceWayangPersistenceTransferAuditConfig config,
        AgenticCommerceWayangPersistenceTransferAuditContractReport contractReport) {

    public static final String READINESS_ID = "agentic-commerce.persistence.transfer-audit.readiness";

    public static final String STATUS_HEALTHY = "healthy";
    public static final String STATUS_DEGRADED = "degraded";
    public static final String STATUS_DISABLED = "disabled";
    public static final String STATUS_UNAVAILABLE = "unavailable";

    public AgenticCommerceWayangPersistenceTransferAuditDiagnostics {
        config = config == null ? AgenticCommerceWayangPersistenceTransferAuditConfig.defaults() : config;
    }

    public static AgenticCommerceWayangPersistenceTransferAuditDiagnostics from(
            AgenticCommerceWayangPersistenceTransferAuditConfig config) {
        return new AgenticCommerceWayangPersistenceTransferAuditDiagnostics(config, null);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditDiagnostics from(
            AgenticCommerceWayangPersistenceTransferAuditConfig config,
            AgenticCommerceWayangPersistenceTransferAuditContractReport contractReport) {
        return new AgenticCommerceWayangPersistenceTransferAuditDiagnostics(config, contractReport);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditDiagnostics check(
            AgenticCommerceWayangPersistenceTransferAuditConfig config) {
        return check(config, false);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditDiagnostics check(
            AgenticCommerceWayangPersistenceTransferAuditConfig config,
            boolean verifyReload) {
        return check(config, null, null, verifyReload);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditDiagnostics check(
            AgenticCommerceWayangPersistenceTransferAuditConfig config,
            AgenticCommerceObjectStoreClientResolver objectStoreClientResolver,
            AgenticCommerceDatabasePersistenceClientResolver databasePersistenceClientResolver,
            boolean verifyReload) {
        AgenticCommerceWayangPersistenceTransferAuditConfig resolved =
                config == null ? AgenticCommerceWayangPersistenceTransferAuditConfig.defaults() : config;
        AgenticCommerceWayangPersistenceTransferAuditContractReport report =
                AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness
                        .retainedLatestTwo(objectStoreClientResolver, databasePersistenceClientResolver)
                        .run(resolved, verifyReload);
        return from(resolved, report);
    }

    public boolean auditEnabled() {
        return !config.noopStore();
    }

    public boolean contractAvailable() {
        return contractReport != null;
    }

    public boolean ready() {
        return STATUS_DISABLED.equals(diagnosticStatus())
                || STATUS_HEALTHY.equals(diagnosticStatus())
                || STATUS_DEGRADED.equals(diagnosticStatus());
    }

    public String diagnosticStatus() {
        if (!auditEnabled()) {
            return STATUS_DISABLED;
        }
        if (!validationReport().valid()) {
            return STATUS_UNAVAILABLE;
        }
        if (contractReport != null && !contractReport.passed()) {
            return STATUS_UNAVAILABLE;
        }
        return warningCount() == 0 ? STATUS_HEALTHY : STATUS_DEGRADED;
    }

    public int issueCount() {
        return issues().size();
    }

    public int warningCount() {
        return warnings().size();
    }

    public int recommendationCount() {
        return recommendations().size();
    }

    public int blockingRecommendationCount() {
        return blockingRecommendations().size();
    }

    public boolean durableAuditStorage() {
        return durable(config);
    }

    public AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport validationReport() {
        return config.validationReport();
    }

    public List<String> issues() {
        List<String> values = new ArrayList<>(validationReport().errorCodes());
        if (contractReport != null) {
            values.addAll(contractReport.issues());
        }
        return values.stream()
                .distinct()
                .toList();
    }

    public List<String> warnings() {
        List<String> values = new ArrayList<>();
        if (!auditEnabled()) {
            values.add(AUDIT_STORAGE_DISABLED);
        }
        if (config.memoryStore()) {
            values.add(AUDIT_STORAGE_EPHEMERAL);
        }
        if (contractReport == null) {
            values.add(AUDIT_CONTRACT_NOT_RUN);
        } else if (durable(config) && !contractReport.reloadAttempted()) {
            values.add(AUDIT_RELOAD_NOT_CHECKED);
        }
        return List.copyOf(values);
    }

    public List<AgenticCommerceWayangPersistenceTransferAuditRecommendation> recommendations() {
        return AgenticCommerceWayangPersistenceTransferAuditRecommendation.from(this);
    }

    public List<AgenticCommerceWayangPersistenceTransferAuditRecommendation> blockingRecommendations() {
        return recommendations().stream()
                .filter(AgenticCommerceWayangPersistenceTransferAuditRecommendation::blocking)
                .toList();
    }

    public List<String> recommendationActions() {
        return recommendations().stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditRecommendation::action)
                .toList();
    }

    public Map<String, Object> toMap() {
        List<AgenticCommerceWayangPersistenceTransferAuditRecommendation> recommendations = recommendations();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ready", ready());
        values.put("diagnosticStatus", diagnosticStatus());
        values.put("auditEnabled", auditEnabled());
        values.put("contractAvailable", contractAvailable());
        values.put("issueCount", issueCount());
        values.put("warningCount", warningCount());
        values.put("issues", issues());
        values.put("warnings", warnings());
        values.put("recommendationCount", recommendations.size());
        values.put("blockingRecommendationCount", blockingRecommendationCount());
        values.put("recommendationActions", recommendationActions());
        values.put("recommendations", recommendations.stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditRecommendation::toMap)
                .toList());
        values.put("validation", validationReport().toMap());
        values.put("storage", storageSummary(config));
        values.put("target", targetSummary(config));
        values.put("provider", providerSummary());
        if (contractReport != null) {
            values.put("contract", contractSummary(contractReport));
        }
        return Map.copyOf(values);
    }

    public WayangReadinessReport standardReadiness() {
        return AgenticCommerceWayangPersistenceTransferAuditReadiness.from(this).toReport();
    }

    private Map<String, Object> providerSummary() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("providerContract", false);
        if (contractReport == null) {
            return Map.copyOf(values);
        }
        Map<String, Object> attributes = contractReport.attributes();
        copyValue(values, attributes, "providerContract");
        copyValue(values, attributes, "providerMatched");
        copyValue(values, attributes, "providerStorageKind");
        copyValue(values, attributes, "providerStorageKinds");
        copyValue(values, attributes, "configuredMaxTrails");
        copyValue(values, attributes, "configuredReadable");
        copyValue(values, attributes, "verifyReload");
        return Map.copyOf(values);
    }

    private static Map<String, Object> contractSummary(
            AgenticCommerceWayangPersistenceTransferAuditContractReport report) {
        Map<String, Object> retainedPage = report.retainedPage();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passed", report.passed());
        values.put("contractId", report.contractId());
        values.put("issueCount", report.issueCount());
        values.put("issues", report.issues());
        values.put("expectedRetainedTrailCount", report.expectedRetainedTrailCount());
        values.put("retainedTrailCount", report.retainedTrailCount());
        values.put("reloadAttempted", report.reloadAttempted());
        values.put("reloadTrailCount", report.reloadTrailCount());
        copyValue(values, retainedPage, "trailTypes");
        copyValue(values, retainedPage, "outcomeStatuses");
        copyValue(values, retainedPage, "nextActions");
        return Map.copyOf(values);
    }

    private static Map<String, Object> storageSummary(
            AgenticCommerceWayangPersistenceTransferAuditConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("storageKind", config.storageKind());
        values.put("maxTrails", config.maxTrails());
        values.put("readable", !config.noopStore());
        values.put("durable", durable(config));
        values.put("ephemeral", config.memoryStore());
        values.put("fileBacked", config.fileStore());
        values.put("objectStoreBacked", config.objectStoreBacked());
        values.put("databaseBacked", config.databaseBacked());
        values.put("composite", config.compositeStore());
        values.put("childCount", config.children().size());
        if (!config.children().isEmpty()) {
            values.put("childStorageKinds", config.children().stream()
                    .map(AgenticCommerceWayangPersistenceTransferAuditConfig::storageKind)
                    .toList());
        }
        return Map.copyOf(values);
    }

    private static Map<String, Object> targetSummary(
            AgenticCommerceWayangPersistenceTransferAuditConfig config) {
        if (config.fileStore()) {
            return Map.of(
                    "targetKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_FILE,
                    "journalPath", config.journalPath(),
                    "durable", true);
        }
        if (config.objectStoreBacked()) {
            AgenticCommerceObjectStoreConfig objectStore = config.objectStoreConfig();
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("targetKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_OBJECT_STORE);
            values.put("provider", objectStore.provider());
            values.put("bucket", objectStore.bucket());
            values.put("keyPrefix", objectStore.keyPrefix());
            values.put("auditObject", config.journalPath());
            values.put("auditObjectKey", objectStore.objectKey(config.journalPath()));
            values.put("endpointConfigured", !objectStore.endpoint().isBlank());
            values.put("cloudStorage", true);
            values.put("durable", true);
            return Map.copyOf(values);
        }
        if (config.databaseBacked()) {
            AgenticCommerceDatabasePersistenceConfig database = config.databaseConfig();
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("targetKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_DATABASE);
            values.put("provider", database.provider());
            values.put("tableName", database.tableName());
            values.put("namespace", database.namespace());
            values.put("auditDocument", config.journalPath());
            values.put("auditDocumentKey", database.documentKey(config.journalPath()));
            values.put("databaseStorage", true);
            values.put("durable", true);
            return Map.copyOf(values);
        }
        if (config.compositeStore()) {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("targetKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_COMPOSITE);
            values.put("durable", durable(config));
            values.put("childCount", config.children().size());
            values.put("children", config.children().stream()
                    .map(AgenticCommerceWayangPersistenceTransferAuditDiagnostics::targetSummary)
                    .toList());
            return Map.copyOf(values);
        }
        return Map.of(
                "targetKind", config.storageKind(),
                "durable", false);
    }

    private static boolean durable(AgenticCommerceWayangPersistenceTransferAuditConfig config) {
        return config.fileStore()
                || config.objectStoreBacked()
                || config.databaseBacked()
                || config.children().stream()
                .anyMatch(AgenticCommerceWayangPersistenceTransferAuditDiagnostics::durable);
    }

    private static void copyValue(
            Map<String, Object> target,
            Map<String, Object> source,
            String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }
}
