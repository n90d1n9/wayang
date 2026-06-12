package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Copies persisted Agentic Commerce Wayang state between persistence stores.
 */
public final class AgenticCommerceWayangPersistenceTransfer {

    public static final String TRANSFER_ID = "agentic-commerce-wayang-persistence-transfer";
    public static final String DOCUMENT_RUNTIME_CONFIG =
            AgenticCommerceWayangPersistenceDocuments.RUNTIME_CONFIG.id();
    public static final String DOCUMENT_BOOTSTRAP_CONFIG =
            AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_CONFIG.id();
    public static final String DOCUMENT_BOOTSTRAP_REPORT =
            AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_REPORT.id();
    public static final String DOCUMENT_MANIFEST =
            AgenticCommerceWayangPersistenceDocuments.MANIFEST.id();

    private final AgenticCommerceWayangPersistenceTransferOptions options;

    public AgenticCommerceWayangPersistenceTransfer() {
        this(AgenticCommerceWayangPersistenceTransferOptions.defaults());
    }

    public AgenticCommerceWayangPersistenceTransfer(AgenticCommerceWayangPersistenceTransferOptions options) {
        this.options = options == null ? AgenticCommerceWayangPersistenceTransferOptions.defaults() : options;
    }

    public static AgenticCommerceWayangPersistenceTransfer copyAll() {
        return new AgenticCommerceWayangPersistenceTransfer();
    }

    public static AgenticCommerceWayangPersistenceTransfer configured(
            AgenticCommerceWayangPersistenceTransferOptions options) {
        return new AgenticCommerceWayangPersistenceTransfer(options);
    }

    public AgenticCommerceWayangPersistenceTransferOptions options() {
        return options;
    }

    public AgenticCommerceWayangPersistenceTransferPlan plan(
            AgenticCommerceWayangPersistenceStore source,
            AgenticCommerceWayangPersistenceStore target) {
        AgenticCommerceWayangPersistenceTransferReport report =
                configured(options.withDryRun(true)).copy(source, target);
        return AgenticCommerceWayangPersistenceTransferPlan.from(report, options);
    }

    public AgenticCommerceWayangPersistenceTransferReport copy(
            AgenticCommerceWayangPersistenceStore source,
            AgenticCommerceWayangPersistenceStore target) {
        AgenticCommerceWayangPersistenceStore resolvedSource = Objects.requireNonNull(source, "source");
        AgenticCommerceWayangPersistenceStore resolvedTarget = Objects.requireNonNull(target, "target");
        List<String> plannedDocuments = new ArrayList<>();
        List<String> skippedDocuments = new ArrayList<>();
        List<String> blockedDocuments = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        Map<String, Object> sourceStatus = status(resolvedSource, issues, "source_status_failed");
        Map<String, Object> targetStatusBefore = status(resolvedTarget, issues, "target_status_before_failed");

        boolean runtimeConfigCopied = copyRuntimeConfig(
                resolvedSource,
                resolvedTarget,
                plannedDocuments,
                skippedDocuments,
                blockedDocuments,
                issues);
        boolean bootstrapConfigCopied = copyBootstrapConfig(
                resolvedSource,
                resolvedTarget,
                plannedDocuments,
                skippedDocuments,
                blockedDocuments,
                issues);
        boolean bootstrapReportCopied = copyBootstrapReport(
                resolvedSource,
                resolvedTarget,
                plannedDocuments,
                skippedDocuments,
                blockedDocuments,
                issues);
        boolean manifestCopied = copyManifest(
                resolvedSource,
                resolvedTarget,
                plannedDocuments,
                skippedDocuments,
                blockedDocuments,
                issues);

        Map<String, Object> targetStatusAfter = status(resolvedTarget, issues, "target_status_after_failed");
        return new AgenticCommerceWayangPersistenceTransferReport(
                resolvedSource.storageKind(),
                resolvedTarget.storageKind(),
                runtimeConfigCopied,
                bootstrapConfigCopied,
                bootstrapReportCopied,
                manifestCopied,
                options,
                plannedDocuments,
                skippedDocuments,
                blockedDocuments,
                issues,
                sourceStatus,
                targetStatusBefore,
                targetStatusAfter,
                attributes());
    }

    private boolean copyRuntimeConfig(
            AgenticCommerceWayangPersistenceStore source,
            AgenticCommerceWayangPersistenceStore target,
            List<String> plannedDocuments,
            List<String> skippedDocuments,
            List<String> blockedDocuments,
            List<String> issues) {
        Optional<AgenticCommerceWayangRuntimeConfig> sourceValue = loadRuntimeConfig(source, issues);
        if (sourceValue.isEmpty()) {
            skippedDocuments.add(DOCUMENT_RUNTIME_CONFIG);
            return false;
        }
        plannedDocuments.add(DOCUMENT_RUNTIME_CONFIG);
        if (blockedByExistingRuntimeConfig(target, blockedDocuments, issues)) {
            return false;
        }
        if (options.dryRun()) {
            return false;
        }
        try {
            target.saveRuntimeConfig(sourceValue.orElseThrow());
        } catch (RuntimeException exception) {
            issues.add("runtime_config_save_failed");
            return false;
        }
        if (!options.verifyAfterCopy()) {
            return true;
        }
        Optional<AgenticCommerceWayangRuntimeConfig> targetValue = loadRuntimeConfig(target, issues);
        if (targetValue.isEmpty()) {
            issues.add("runtime_config_verify_missing");
            return false;
        }
        if (!sourceValue.orElseThrow().toStorageMap().equals(targetValue.orElseThrow().toStorageMap())) {
            issues.add("runtime_config_verify_mismatch");
            return false;
        }
        return true;
    }

    private boolean copyBootstrapConfig(
            AgenticCommerceWayangPersistenceStore source,
            AgenticCommerceWayangPersistenceStore target,
            List<String> plannedDocuments,
            List<String> skippedDocuments,
            List<String> blockedDocuments,
            List<String> issues) {
        Optional<AgenticCommerceWayangBootstrapConfig> sourceValue = loadBootstrapConfig(source, issues);
        if (sourceValue.isEmpty()) {
            skippedDocuments.add(DOCUMENT_BOOTSTRAP_CONFIG);
            return false;
        }
        plannedDocuments.add(DOCUMENT_BOOTSTRAP_CONFIG);
        if (blockedByExistingBootstrapConfig(target, blockedDocuments, issues)) {
            return false;
        }
        if (options.dryRun()) {
            return false;
        }
        try {
            target.saveBootstrapConfig(sourceValue.orElseThrow());
        } catch (RuntimeException exception) {
            issues.add("bootstrap_config_save_failed");
            return false;
        }
        if (!options.verifyAfterCopy()) {
            return true;
        }
        Optional<AgenticCommerceWayangBootstrapConfig> targetValue = loadBootstrapConfig(target, issues);
        if (targetValue.isEmpty()) {
            issues.add("bootstrap_config_verify_missing");
            return false;
        }
        if (!sourceValue.orElseThrow().toMap().equals(targetValue.orElseThrow().toMap())) {
            issues.add("bootstrap_config_verify_mismatch");
            return false;
        }
        return true;
    }

    private boolean copyBootstrapReport(
            AgenticCommerceWayangPersistenceStore source,
            AgenticCommerceWayangPersistenceStore target,
            List<String> plannedDocuments,
            List<String> skippedDocuments,
            List<String> blockedDocuments,
            List<String> issues) {
        Optional<Map<String, Object>> sourceValue = loadBootstrapReport(source, issues);
        if (sourceValue.isEmpty()) {
            skippedDocuments.add(DOCUMENT_BOOTSTRAP_REPORT);
            return false;
        }
        plannedDocuments.add(DOCUMENT_BOOTSTRAP_REPORT);
        if (blockedByExistingBootstrapReport(target, blockedDocuments, issues)) {
            return false;
        }
        if (options.dryRun()) {
            return false;
        }
        try {
            target.saveBootstrapReport(bootstrapReportFromMap(sourceValue.orElseThrow()));
        } catch (RuntimeException exception) {
            issues.add("bootstrap_report_save_failed");
            return false;
        }
        if (!options.verifyAfterCopy()) {
            return true;
        }
        Optional<Map<String, Object>> targetValue = loadBootstrapReport(target, issues);
        if (targetValue.isEmpty()) {
            issues.add("bootstrap_report_verify_missing");
            return false;
        }
        if (!sameBootstrapReportSummary(sourceValue.orElseThrow(), targetValue.orElseThrow())) {
            issues.add("bootstrap_report_verify_mismatch");
            return false;
        }
        return true;
    }

    private boolean copyManifest(
            AgenticCommerceWayangPersistenceStore source,
            AgenticCommerceWayangPersistenceStore target,
            List<String> plannedDocuments,
            List<String> skippedDocuments,
            List<String> blockedDocuments,
            List<String> issues) {
        Optional<Map<String, Object>> sourceValue = loadManifest(source, issues);
        if (sourceValue.isEmpty()) {
            skippedDocuments.add(DOCUMENT_MANIFEST);
            return false;
        }
        plannedDocuments.add(DOCUMENT_MANIFEST);
        if (blockedByExistingManifest(target, blockedDocuments, issues)) {
            return false;
        }
        if (options.dryRun()) {
            return false;
        }
        try {
            target.saveManifest(manifestFromMap(sourceValue.orElseThrow()));
        } catch (RuntimeException exception) {
            issues.add("manifest_save_failed");
            return false;
        }
        if (!options.verifyAfterCopy()) {
            return true;
        }
        Optional<Map<String, Object>> targetValue = loadManifest(target, issues);
        if (targetValue.isEmpty()) {
            issues.add("manifest_verify_missing");
            return false;
        }
        if (!sameManifestSummary(sourceValue.orElseThrow(), targetValue.orElseThrow())) {
            issues.add("manifest_verify_mismatch");
            return false;
        }
        return true;
    }

    private Optional<AgenticCommerceWayangRuntimeConfig> loadRuntimeConfig(
            AgenticCommerceWayangPersistenceStore store,
            List<String> issues) {
        try {
            return store.loadRuntimeConfig();
        } catch (RuntimeException exception) {
            issues.add(AgenticCommerceWayangPersistenceDocuments.RUNTIME_CONFIG.loadFailureIssue());
            return Optional.empty();
        }
    }

    private Optional<AgenticCommerceWayangBootstrapConfig> loadBootstrapConfig(
            AgenticCommerceWayangPersistenceStore store,
            List<String> issues) {
        try {
            return store.loadBootstrapConfig();
        } catch (RuntimeException exception) {
            issues.add(AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_CONFIG.loadFailureIssue());
            return Optional.empty();
        }
    }

    private Optional<Map<String, Object>> loadBootstrapReport(
            AgenticCommerceWayangPersistenceStore store,
            List<String> issues) {
        try {
            return store.loadBootstrapReport();
        } catch (RuntimeException exception) {
            issues.add(AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_REPORT.loadFailureIssue());
            return Optional.empty();
        }
    }

    private Optional<Map<String, Object>> loadManifest(
            AgenticCommerceWayangPersistenceStore store,
            List<String> issues) {
        try {
            return store.loadManifest();
        } catch (RuntimeException exception) {
            issues.add(AgenticCommerceWayangPersistenceDocuments.MANIFEST.loadFailureIssue());
            return Optional.empty();
        }
    }

    private boolean blockedByExistingRuntimeConfig(
            AgenticCommerceWayangPersistenceStore target,
            List<String> blockedDocuments,
            List<String> issues) {
        if (options.overwriteExisting()) {
            return false;
        }
        int issueCount = issues.size();
        Optional<AgenticCommerceWayangRuntimeConfig> existing = loadRuntimeConfig(target, issues);
        if (issues.size() > issueCount || existing.isPresent()) {
            blockedDocuments.add(DOCUMENT_RUNTIME_CONFIG);
            return true;
        }
        return false;
    }

    private boolean blockedByExistingBootstrapConfig(
            AgenticCommerceWayangPersistenceStore target,
            List<String> blockedDocuments,
            List<String> issues) {
        if (options.overwriteExisting()) {
            return false;
        }
        int issueCount = issues.size();
        Optional<AgenticCommerceWayangBootstrapConfig> existing = loadBootstrapConfig(target, issues);
        if (issues.size() > issueCount || existing.isPresent()) {
            blockedDocuments.add(DOCUMENT_BOOTSTRAP_CONFIG);
            return true;
        }
        return false;
    }

    private boolean blockedByExistingBootstrapReport(
            AgenticCommerceWayangPersistenceStore target,
            List<String> blockedDocuments,
            List<String> issues) {
        if (options.overwriteExisting()) {
            return false;
        }
        int issueCount = issues.size();
        Optional<Map<String, Object>> existing = loadBootstrapReport(target, issues);
        if (issues.size() > issueCount || existing.isPresent()) {
            blockedDocuments.add(DOCUMENT_BOOTSTRAP_REPORT);
            return true;
        }
        return false;
    }

    private boolean blockedByExistingManifest(
            AgenticCommerceWayangPersistenceStore target,
            List<String> blockedDocuments,
            List<String> issues) {
        if (options.overwriteExisting()) {
            return false;
        }
        int issueCount = issues.size();
        Optional<Map<String, Object>> existing = loadManifest(target, issues);
        if (issues.size() > issueCount || existing.isPresent()) {
            blockedDocuments.add(DOCUMENT_MANIFEST);
            return true;
        }
        return false;
    }

    private AgenticCommerceWayangBootstrapReport bootstrapReportFromMap(Map<String, Object> values) {
        AgenticCommerceWayangRuntimeConfig runtimeConfig = AgenticCommerceWayangRuntimeConfig.fromMap(
                map(values.get("runtimeConfig")));
        AgenticCommerceWayangBootstrapConfig bootstrapConfig = AgenticCommerceWayangBootstrapConfig.fromMap(
                map(values.get("bootstrapConfig")));
        return new AgenticCommerceWayangBootstrapReport(
                runtimeConfig,
                bootstrapConfig,
                AgenticCommerceWayangMaps.text(values.get("connector")),
                skillRegistrationFromMap(map(values.get("skillRegistration"))),
                AgenticCommerceWayangRuntimeConfig.fromMap(map(values.get("runtimeConfig")))
                        .buildInMemory()
                        .smokeProbe(),
                AgenticCommerceHttpBindingReport.fromConfig(runtimeConfig.httpConfig()),
                map(values.get("metadata")));
    }

    private AgenticCommerceSkillRegistration skillRegistrationFromMap(Map<String, Object> values) {
        return new AgenticCommerceSkillRegistration(
                AgenticCommerceWayangMaps.stringList(values.get("requestedSkillIds")),
                AgenticCommerceWayangMaps.stringList(values.get("registeredDefinitionIds")),
                AgenticCommerceWayangMaps.stringList(values.get("registeredRuntimeSkillIds")),
                AgenticCommerceWayangMaps.stringList(values.get("missingSkillIds")),
                map(values.get("metadata")));
    }

    private AgenticCommerceWayangManifest manifestFromMap(Map<String, Object> values) {
        return AgenticCommerceWayangManifest.configured(
                AgenticCommerceWayangRuntimeConfig.fromMap(map(values.get("runtimeConfig"))),
                AgenticCommerceWayangBootstrapConfig.fromMap(map(values.get("bootstrapConfig"))));
    }

    private boolean sameBootstrapReportSummary(Map<String, Object> source, Map<String, Object> target) {
        return sameBoolean(source, target, "ready")
                && sameNumber(source, target, "issueCount")
                && sameNumber(source, target, "bootstrapIssueCount")
                && sameNumber(
                        map(source.get("skillRegistration")),
                        map(target.get("skillRegistration")),
                        "requestedCount")
                && sameNumber(
                        map(source.get("skillRegistration")),
                        map(target.get("skillRegistration")),
                        "definitionCount")
                && sameNumber(
                        map(source.get("skillRegistration")),
                        map(target.get("skillRegistration")),
                        "runtimeSkillCount")
                && sameNumber(
                        map(source.get("skillRegistration")),
                        map(target.get("skillRegistration")),
                        "missingCount");
    }

    private boolean sameManifestSummary(Map<String, Object> source, Map<String, Object> target) {
        return sameNumber(source, target, "skillCount")
                && sameNumber(source, target, "routeCount")
                && sameNumber(source, target, "operationCount")
                && AgenticCommerceWayangMaps.stringList(source.get("skillIds"))
                        .equals(AgenticCommerceWayangMaps.stringList(target.get("skillIds")))
                && AgenticCommerceWayangMaps.stringList(source.get("operations"))
                        .equals(AgenticCommerceWayangMaps.stringList(target.get("operations")));
    }

    private static boolean sameBoolean(Map<String, Object> source, Map<String, Object> target, String key) {
        return Boolean.TRUE.equals(source.get(key)) == Boolean.TRUE.equals(target.get(key));
    }

    private static boolean sameNumber(Map<String, Object> source, Map<String, Object> target, String key) {
        return number(source.get(key)) == number(target.get(key));
    }

    private Map<String, Object> status(
            AgenticCommerceWayangPersistenceStore store,
            List<String> issues,
            String issue) {
        try {
            return store.toMap();
        } catch (RuntimeException exception) {
            issues.add(issue);
            return Map.of();
        }
    }

    private Map<String, Object> attributes() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("transferId", TRANSFER_ID);
        values.put("documentCount", AgenticCommerceWayangPersistenceDocuments.count());
        values.put("dryRun", options.dryRun());
        values.put("overwriteExisting", options.overwriteExisting());
        values.put("verifyAfterCopy", options.verifyAfterCopy());
        values.put("verified", options.verifyAfterCopy() && !options.dryRun());
        return Map.copyOf(values);
    }

    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? AgenticCommerceWayangMaps.copy(map) : Map.of();
    }

    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
