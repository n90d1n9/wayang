package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Preview of applying safe transfer audit config remediation patches.
 */
public record AgenticCommerceWayangPersistenceTransferAuditConfigPatchApplicationReport(
        Map<String, Object> originalConfig,
        Map<String, Object> patchedConfig,
        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport beforeValidation,
        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport afterValidation,
        List<AgenticCommerceWayangPersistenceTransferAuditConfigPatch> patches) {

    public AgenticCommerceWayangPersistenceTransferAuditConfigPatchApplicationReport {
        originalConfig = AgenticCommerceWayangMaps.copy(originalConfig);
        patchedConfig = AgenticCommerceWayangMaps.copy(patchedConfig);
        beforeValidation = java.util.Objects.requireNonNull(beforeValidation, "beforeValidation");
        afterValidation = java.util.Objects.requireNonNull(afterValidation, "afterValidation");
        patches = patches == null
                ? List.of()
                : patches.stream()
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfigPatchApplicationReport from(
            Map<?, ?> config) {
        return from(config, AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults());
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfigPatchApplicationReport from(
            Map<?, ?> config,
            AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers) {
        AgenticCommerceWayangPersistenceTransferAuditStoreProviders resolvedProviders = providers == null
                ? AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults()
                : providers;
        Map<String, Object> original = AgenticCommerceWayangMaps.copy(config);
        AgenticCommerceWayangPersistenceTransferAuditConfig beforeConfig =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(original);
        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport before =
                beforeConfig.validationReport(resolvedProviders);
        List<AgenticCommerceWayangPersistenceTransferAuditConfigPatch> patches = before.patches();
        Map<String, Object> patched = AgenticCommerceWayangPersistenceTransferAuditConfigPatch.applyAll(
                original,
                patches);
        AgenticCommerceWayangPersistenceTransferAuditConfig afterConfig =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(patched);
        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport after =
                afterConfig.validationReport(resolvedProviders);
        return new AgenticCommerceWayangPersistenceTransferAuditConfigPatchApplicationReport(
                original,
                patched,
                before,
                after,
                patches);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfigPatchApplicationReport fromConfig(
            AgenticCommerceWayangPersistenceTransferAuditConfig config) {
        return fromConfig(config, AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults());
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfigPatchApplicationReport fromConfig(
            AgenticCommerceWayangPersistenceTransferAuditConfig config,
            AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers) {
        AgenticCommerceWayangPersistenceTransferAuditStoreProviders resolvedProviders = providers == null
                ? AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults()
                : providers;
        AgenticCommerceWayangPersistenceTransferAuditConfig resolvedConfig = config == null
                ? AgenticCommerceWayangPersistenceTransferAuditConfig.defaults()
                : config;
        Map<String, Object> original = resolvedConfig.toMap();
        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport before =
                resolvedConfig.validationReport(resolvedProviders);
        List<AgenticCommerceWayangPersistenceTransferAuditConfigPatch> patches = before.patches();
        Map<String, Object> patched = AgenticCommerceWayangPersistenceTransferAuditConfigPatch.applyAll(
                original,
                patches);
        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport after = patches.isEmpty()
                ? before
                : AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(patched)
                .validationReport(resolvedProviders);
        return new AgenticCommerceWayangPersistenceTransferAuditConfigPatchApplicationReport(
                original,
                patched,
                before,
                after,
                patches);
    }

    public int patchCount() {
        return patches.size();
    }

    public boolean patchable() {
        return patchCount() > 0;
    }

    public boolean beforeValid() {
        return beforeValidation.valid();
    }

    public boolean afterValid() {
        return afterValidation.valid();
    }

    public boolean resolved() {
        return !beforeValid() && afterValid();
    }

    public boolean improved() {
        return afterValidation.errorCount() < beforeValidation.errorCount();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("patchable", patchable());
        values.put("patchCount", patchCount());
        values.put("resolved", resolved());
        values.put("improved", improved());
        values.put("beforeValid", beforeValid());
        values.put("afterValid", afterValid());
        values.put("before", validationSummary(beforeValidation));
        values.put("after", validationSummary(afterValidation));
        values.put("patches", patches.stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditConfigPatch::toMap)
                .toList());
        values.put("originalConfig", originalConfig);
        values.put("patchedConfig", patchedConfig);
        return AgenticCommerceWayangMaps.copy(values);
    }

    public Map<String, Object> toSummaryMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("patchable", patchable());
        values.put("patchCount", patchCount());
        values.put("resolved", resolved());
        values.put("improved", improved());
        values.put("beforeValid", beforeValid());
        values.put("afterValid", afterValid());
        values.put("beforeErrorCodes", beforeValidation.errorCodes());
        values.put("afterErrorCodes", afterValidation.errorCodes());
        values.put("beforeWarningCodes", beforeValidation.warningCodes());
        values.put("afterWarningCodes", afterValidation.warningCodes());
        return AgenticCommerceWayangMaps.copy(values);
    }

    private static Map<String, Object> validationSummary(
            AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport report) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("valid", report.valid());
        values.put("storageKind", report.storageKind());
        values.put("issueCount", report.issueCount());
        values.put("errorCount", report.errorCount());
        values.put("warningCount", report.warningCount());
        values.put("errorCodes", report.errorCodes());
        values.put("warningCodes", report.warningCodes());
        values.put("patchCount", report.patchCount());
        return AgenticCommerceWayangMaps.copy(values);
    }
}
