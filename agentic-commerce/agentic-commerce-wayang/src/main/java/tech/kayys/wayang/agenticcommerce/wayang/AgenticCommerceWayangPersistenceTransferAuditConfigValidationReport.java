package tech.kayys.wayang.agenticcommerce.wayang;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_COMPOSITE_DUPLICATE_TARGETS;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_COMPOSITE_FIRST_CHILD_NOT_DURABLE;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_COMPOSITE_WITHOUT_DURABLE_CHILD;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_DATABASE_PROVIDER_GENERIC;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_DATABASE_PROVIDER_UNKNOWN;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_JOURNAL_NAME_ABSOLUTE;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_JOURNAL_NAME_MISSING;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_JOURNAL_NOT_JSONL;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_JOURNAL_PATH_INVALID;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_JOURNAL_PATH_MISSING;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_OBJECT_STORE_ENDPOINT_MISSING;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_OBJECT_STORE_PROVIDER_GENERIC;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_OBJECT_STORE_PROVIDER_UNKNOWN;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_BELOW_CONTRACT;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_BYTE_LIMIT_BELOW_CONTRACT;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_BYTE_LIMIT_INVALID;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_COUNT_INVALID;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_STORAGE_DISABLED;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_STORAGE_EPHEMERAL;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.UNSUPPORTED_AUDIT_STORAGE_KIND;

/**
 * Operator-facing validation report for transfer audit storage configuration.
 */
public record AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport(
        String storageKind,
        Map<String, Object> target,
        List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues) {

    public AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport {
        storageKind = AgenticCommerceWayangMaps.text(storageKind);
        target = AgenticCommerceWayangMaps.copy(target);
        issues = normalizeIssues(issues);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport from(
            AgenticCommerceWayangPersistenceTransferAuditConfig config) {
        return from(config, AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults());
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport from(
            AgenticCommerceWayangPersistenceTransferAuditConfig config,
            AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers) {
        AgenticCommerceWayangPersistenceTransferAuditConfig resolved = Objects.requireNonNull(config, "config");
        AgenticCommerceWayangPersistenceTransferAuditStoreProviders resolvedProviders = providers == null
                ? AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults()
                : providers;
        List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues = new ArrayList<>();
        validate(resolved, resolvedProviders, "$", issues);
        return new AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport(
                resolved.storageKind(),
                target(resolved),
                issues);
    }

    public boolean valid() {
        return errorCount() == 0;
    }

    public int issueCount() {
        return issues.size();
    }

    public int errorCount() {
        return (int) issues.stream()
                .filter(AgenticCommerceWayangPersistenceConfigValidationIssue::error)
                .count();
    }

    public int warningCount() {
        return (int) issues.stream()
                .filter(AgenticCommerceWayangPersistenceConfigValidationIssue::warning)
                .count();
    }

    public List<String> errorCodes() {
        return issues.stream()
                .filter(AgenticCommerceWayangPersistenceConfigValidationIssue::error)
                .map(AgenticCommerceWayangPersistenceConfigValidationIssue::code)
                .toList();
    }

    public List<String> warningCodes() {
        return issues.stream()
                .filter(AgenticCommerceWayangPersistenceConfigValidationIssue::warning)
                .map(AgenticCommerceWayangPersistenceConfigValidationIssue::code)
                .toList();
    }

    public List<AgenticCommerceWayangPersistenceTransferAuditConfigRemediation> remediations() {
        return AgenticCommerceWayangPersistenceTransferAuditConfigRemediation.fromIssues(issues);
    }

    public int remediationCount() {
        return remediations().size();
    }

    public List<AgenticCommerceWayangPersistenceTransferAuditConfigPatch> patches() {
        return remediations().stream()
                .flatMap(remediation -> remediation.patches().stream())
                .toList();
    }

    public int patchCount() {
        return patches().size();
    }

    public Map<String, Object> applyPatchesTo(Map<?, ?> config) {
        return AgenticCommerceWayangPersistenceTransferAuditConfigPatch.applyAll(config, patches());
    }

    public AgenticCommerceWayangPersistenceTransferAuditConfigPatchApplicationReport patchApplicationReport(
            Map<?, ?> config) {
        return AgenticCommerceWayangPersistenceTransferAuditConfigPatchApplicationReport.from(config);
    }

    public AgenticCommerceWayangPersistenceTransferAuditConfigPatchApplicationReport patchApplicationReport(
            Map<?, ?> config,
            AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers) {
        return AgenticCommerceWayangPersistenceTransferAuditConfigPatchApplicationReport.from(config, providers);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        List<AgenticCommerceWayangPersistenceTransferAuditConfigRemediation> remediations = remediations();
        List<AgenticCommerceWayangPersistenceTransferAuditConfigPatch> patches = patches();
        values.put("valid", valid());
        values.put("storageKind", storageKind);
        values.put("issueCount", issueCount());
        values.put("errorCount", errorCount());
        values.put("warningCount", warningCount());
        values.put("errorCodes", errorCodes());
        values.put("warningCodes", warningCodes());
        values.put("remediationCount", remediations.size());
        values.put("remediation", remediations.stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditConfigRemediation::toMap)
                .toList());
        values.put("patchCount", patches.size());
        values.put("patches", patches.stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditConfigPatch::toMap)
                .toList());
        values.put("target", target);
        values.put("issues", issues.stream()
                .map(AgenticCommerceWayangPersistenceConfigValidationIssue::toMap)
                .toList());
        return Map.copyOf(values);
    }

    private static void validate(
            AgenticCommerceWayangPersistenceTransferAuditConfig config,
            AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers,
            String path,
            List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues) {
        if (providers.provider(config).isEmpty()) {
            issues.add(error(
                    UNSUPPORTED_AUDIT_STORAGE_KIND,
                    path,
                    "No transfer audit store provider is registered for storage kind " + config.storageKind(),
                    Map.of("storageKind", config.storageKind())));
        }
        boolean retentionCountInvalid = !config.noopStore()
                && !config.compositeStore()
                && config.retentionPolicy().maxTrailsParseInvalid();
        if (retentionCountInvalid) {
            issues.add(error(
                    AUDIT_RETENTION_COUNT_INVALID,
                    path + ".retentionPolicy.maxTrails",
                    "Transfer audit retained-history count is not a valid integer value.",
                    config.retentionPolicy().maxTrailsParse()));
        }
        boolean retentionCountBelowContract = !config.noopStore()
                && !config.compositeStore()
                && !retentionCountInvalid
                && config.maxTrails()
                < AgenticCommerceWayangPersistenceTransferAuditContractHarness.DEFAULT_EXPECTED_RETAINED_TRAIL_COUNT;
        if (retentionCountBelowContract) {
            issues.add(error(
                    AUDIT_RETENTION_BELOW_CONTRACT,
                    path + ".maxTrails",
                    "Transfer audit retention is below the retained-history contract minimum.",
                    Map.of(
                            "maxTrails",
                            config.maxTrails(),
                            "minimumRetainedTrailCount",
                            AgenticCommerceWayangPersistenceTransferAuditContractHarness
                                    .DEFAULT_EXPECTED_RETAINED_TRAIL_COUNT)));
        }
        validateRetentionPolicy(config, path, retentionCountInvalid || retentionCountBelowContract, issues);
        if (config.noopStore()) {
            issues.add(warning(
                    AUDIT_STORAGE_DISABLED,
                    path,
                    "Transfer audit storage is disabled.",
                    Map.of("storageKind", config.storageKind())));
            return;
        }
        if (config.memoryStore()) {
            issues.add(warning(
                    AUDIT_STORAGE_EPHEMERAL,
                    path,
                    "In-memory transfer audit storage is ephemeral and restart-local.",
                    Map.of("storageKind", config.storageKind())));
            return;
        }
        if (config.fileStore()) {
            validateJournal(config.journalPath(), path + ".journalPath", issues);
            return;
        }
        if (config.objectStoreBacked()) {
            validateObjectStore(config, path, issues);
            return;
        }
        if (config.databaseBacked()) {
            validateDatabase(config, path, issues);
            return;
        }
        if (config.compositeStore()) {
            validateComposite(config, providers, path, issues);
        }
    }

    private static void validateRetentionPolicy(
            AgenticCommerceWayangPersistenceTransferAuditConfig config,
            String path,
            boolean retentionCountBelowContract,
            List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues) {
        if (retentionCountBelowContract
                || config.noopStore()
                || config.memoryStore()
                || config.compositeStore()) {
            return;
        }
        if (config.retentionPolicy().maxBytesParseInvalid()) {
            issues.add(error(
                    AUDIT_RETENTION_BYTE_LIMIT_INVALID,
                    path + ".retentionPolicy.maxBytes",
                    "Transfer audit byte retention limit is not a valid byte-size value.",
                    config.retentionPolicy().maxBytesParse()));
            return;
        }
        AgenticCommerceWayangPersistenceTransferAuditRetentionPolicyAssessment assessment =
                retentionAssessment(config);
        if (assessment.byteLimited()
                && !assessment.satisfiesContract()) {
            issues.add(error(
                    AUDIT_RETENTION_BYTE_LIMIT_BELOW_CONTRACT,
                    path + ".retentionPolicy.maxBytes",
                    "Transfer audit byte retention is below the retained-history contract minimum.",
                    assessment.toMap()));
        }
    }

    private static void validateObjectStore(
            AgenticCommerceWayangPersistenceTransferAuditConfig config,
            String path,
            List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues) {
        AgenticCommerceObjectStoreConfig objectStore = config.objectStoreConfig();
        String provider = objectStore.provider();
        if (AgenticCommerceObjectStoreConfig.PROVIDER_OBJECT_STORE.equals(provider)) {
            issues.add(warning(
                    AUDIT_OBJECT_STORE_PROVIDER_GENERIC,
                    path + ".objectStore.provider",
                    "Generic object-store audit provider should be replaced with s3 or rustfs when known.",
                    objectStore.toMap()));
        } else if (!AgenticCommerceObjectStoreConfig.PROVIDER_S3.equals(provider)
                && !AgenticCommerceObjectStoreConfig.PROVIDER_RUSTFS.equals(provider)) {
            issues.add(warning(
                    AUDIT_OBJECT_STORE_PROVIDER_UNKNOWN,
                    path + ".objectStore.provider",
                    "Object-store audit provider is not one of the built-in provider aliases.",
                    objectStore.toMap()));
        }
        if (AgenticCommerceObjectStoreConfig.PROVIDER_RUSTFS.equals(provider)
                && objectStore.endpoint().isBlank()) {
            issues.add(warning(
                    AUDIT_OBJECT_STORE_ENDPOINT_MISSING,
                    path + ".objectStore.endpoint",
                    "RustFS transfer audit storage usually needs an endpoint.",
                    objectStore.toMap()));
        }
        validateObjectName(config.journalPath(), path + ".journalPath", "audit object", issues);
    }

    private static void validateDatabase(
            AgenticCommerceWayangPersistenceTransferAuditConfig config,
            String path,
            List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues) {
        AgenticCommerceDatabasePersistenceConfig database = config.databaseConfig();
        String provider = database.provider();
        if (AgenticCommerceDatabasePersistenceConfig.PROVIDER_DATABASE.equals(provider)) {
            issues.add(warning(
                    AUDIT_DATABASE_PROVIDER_GENERIC,
                    path + ".database.provider",
                    "Generic database audit provider should be replaced with jdbc or postgres when known.",
                    database.toMap()));
        } else if (!AgenticCommerceDatabasePersistenceConfig.PROVIDER_JDBC.equals(provider)
                && !AgenticCommerceDatabasePersistenceConfig.PROVIDER_POSTGRES.equals(provider)) {
            issues.add(warning(
                    AUDIT_DATABASE_PROVIDER_UNKNOWN,
                    path + ".database.provider",
                    "Database audit provider is not one of the built-in provider aliases.",
                    database.toMap()));
        }
        validateObjectName(config.journalPath(), path + ".journalPath", "audit document", issues);
    }

    private static void validateComposite(
            AgenticCommerceWayangPersistenceTransferAuditConfig config,
            AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers,
            String path,
            List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues) {
        for (int i = 0; i < config.children().size(); i++) {
            validate(config.children().get(i), providers, path + ".children[" + i + "]", issues);
        }
        if (config.children().stream().noneMatch(
                AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport::durable)) {
            issues.add(warning(
                    AUDIT_COMPOSITE_WITHOUT_DURABLE_CHILD,
                    path,
                    "Composite transfer audit storage has no durable child.",
                    Map.of("childStorageKinds", childStorageKinds(config))));
        }
        if (!config.children().isEmpty()
                && !durable(config.children().get(0))) {
            issues.add(warning(
                    AUDIT_COMPOSITE_FIRST_CHILD_NOT_DURABLE,
                    path + ".children[0]",
                    "Composite audit reads and reload checks use the first readable child.",
                    Map.of("firstChildStorageKind", config.children().get(0).storageKind())));
        }
        if (sameTargetChildren(config)) {
            issues.add(warning(
                    AUDIT_COMPOSITE_DUPLICATE_TARGETS,
                    path,
                    "Composite transfer audit children resolve to duplicate storage targets.",
                    Map.of("childStorageKinds", childStorageKinds(config))));
        }
    }

    private static void validateJournal(
            String journalPath,
            String path,
            List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues) {
        String normalized = AgenticCommerceWayangMaps.text(journalPath);
        if (normalized.isBlank()) {
            issues.add(error(
                    AUDIT_JOURNAL_PATH_MISSING,
                    path,
                    "File transfer audit storage requires a journal path.",
                    Map.of()));
            return;
        }
        try {
            Path.of(normalized);
        } catch (InvalidPathException exception) {
            issues.add(error(
                    AUDIT_JOURNAL_PATH_INVALID,
                    path,
                    "File transfer audit journal path is not valid for the local filesystem.",
                    Map.of("journalPath", normalized)));
        }
        validateJsonlName(normalized, path, "audit journal", issues);
    }

    private static void validateObjectName(
            String name,
            String path,
            String label,
            List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues) {
        String normalized = AgenticCommerceWayangMaps.text(name);
        if (normalized.isBlank()) {
            issues.add(error(
                    AUDIT_JOURNAL_NAME_MISSING,
                    path,
                    "Transfer audit " + label + " name must not be blank.",
                    Map.of()));
            return;
        }
        if (normalized.startsWith("/")) {
            issues.add(warning(
                    AUDIT_JOURNAL_NAME_ABSOLUTE,
                    path,
                    "Transfer audit " + label + " should be relative to its configured storage namespace.",
                    Map.of("journalName", normalized)));
        }
        validateJsonlName(normalized, path, label, issues);
    }

    private static void validateJsonlName(
            String name,
            String path,
            String label,
            List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues) {
        if (!AgenticCommerceWayangMaps.text(name).toLowerCase(java.util.Locale.ROOT).endsWith(".jsonl")) {
            issues.add(warning(
                    AUDIT_JOURNAL_NOT_JSONL,
                    path,
                    "Transfer audit " + label + " should use a .jsonl suffix.",
                    Map.of("journalName", name)));
        }
    }

    private static boolean durable(AgenticCommerceWayangPersistenceTransferAuditConfig config) {
        return config.fileStore()
                || config.objectStoreBacked()
                || config.databaseBacked()
                || config.children().stream()
                .anyMatch(AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport::durable);
    }

    private static List<String> childStorageKinds(AgenticCommerceWayangPersistenceTransferAuditConfig config) {
        return config.children().stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditConfig::storageKind)
                .toList();
    }

    private static boolean sameTargetChildren(AgenticCommerceWayangPersistenceTransferAuditConfig config) {
        List<Map<String, Object>> targets = config.children().stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport::target)
                .toList();
        return targets.stream().distinct().count() < targets.size();
    }

    private static Map<String, Object> target(AgenticCommerceWayangPersistenceTransferAuditConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("storageKind", config.storageKind());
        values.put("journalPath", config.journalPath());
        values.put("maxTrails", config.maxTrails());
        values.put("retentionPolicy", config.retentionPolicy().toMap());
        values.put("retentionAssessment", retentionAssessment(config).toMap());
        values.put("readable", !config.noopStore());
        values.put("durable", durable(config));
        values.put("ephemeral", config.memoryStore());
        values.put("fileBacked", config.fileStore());
        values.put("objectStoreBacked", config.objectStoreBacked());
        values.put("databaseBacked", config.databaseBacked());
        values.put("composite", config.compositeStore());
        values.put("childCount", config.children().size());
        if (config.objectStoreBacked()) {
            values.put("objectStore", config.objectStoreConfig().toMap());
            values.put("location", config.objectStoreConfig().bucket() + "/"
                    + config.objectStoreConfig().objectKey(config.journalPath()));
        }
        if (config.databaseBacked()) {
            values.put("database", config.databaseConfig().toMap());
            values.put("location", config.databaseConfig().tableName() + "/"
                    + config.databaseConfig().documentKey(config.journalPath()));
        }
        if (!config.children().isEmpty()) {
            values.put("children", config.children().stream()
                    .map(AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport::target)
                    .toList());
        }
        return Map.copyOf(values);
    }

    private static AgenticCommerceWayangPersistenceTransferAuditRetentionPolicyAssessment retentionAssessment(
            AgenticCommerceWayangPersistenceTransferAuditConfig config) {
        return config.retentionPolicy().assess(
                AgenticCommerceWayangPersistenceTransferAuditContractHarness.contractJournalLineSamples(),
                AgenticCommerceWayangPersistenceTransferAuditContractHarness.DEFAULT_EXPECTED_RETAINED_TRAIL_COUNT);
    }

    private static AgenticCommerceWayangPersistenceConfigValidationIssue error(
            String code,
            String path,
            String message,
            Map<String, Object> attributes) {
        return AgenticCommerceWayangPersistenceConfigValidationIssue.error(code, path, message, attributes);
    }

    private static AgenticCommerceWayangPersistenceConfigValidationIssue warning(
            String code,
            String path,
            String message,
            Map<String, Object> attributes) {
        return AgenticCommerceWayangPersistenceConfigValidationIssue.warning(code, path, message, attributes);
    }

    private static List<AgenticCommerceWayangPersistenceConfigValidationIssue> normalizeIssues(
            List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }
        return issues.stream()
                .filter(Objects::nonNull)
                .toList();
    }
}
