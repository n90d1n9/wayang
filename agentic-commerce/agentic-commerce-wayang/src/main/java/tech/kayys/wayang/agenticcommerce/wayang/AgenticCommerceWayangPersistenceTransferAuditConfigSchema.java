package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_COMPOSITE;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_DATABASE;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_FILE;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_IN_MEMORY;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_NOOP;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_OBJECT_STORE;

/**
 * Machine-readable schema for transfer audit config, validation, and remediation surfaces.
 */
public final class AgenticCommerceWayangPersistenceTransferAuditConfigSchema {

    public static final String SCHEMA_ID = "agentic-commerce-wayang-persistence-transfer-audit-config";
    public static final int SCHEMA_VERSION = 1;

    private final AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers;

    public AgenticCommerceWayangPersistenceTransferAuditConfigSchema(
            AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers) {
        this.providers = providers == null
                ? AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults()
                : providers;
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfigSchema defaults() {
        return new AgenticCommerceWayangPersistenceTransferAuditConfigSchema(
                AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults());
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfigSchema fromProviders(
            AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers) {
        return new AgenticCommerceWayangPersistenceTransferAuditConfigSchema(providers);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("schemaId", SCHEMA_ID);
        values.put("schemaVersion", SCHEMA_VERSION);
        values.put("storage", storage());
        values.put("fields", fields());
        values.put("retentionPolicy", retentionPolicy());
        values.put("objectStore", objectStore());
        values.put("database", database());
        values.put("validation", validation());
        values.put("remediation", remediation());
        values.put("providerRegistry", providers.toMap());
        return AgenticCommerceWayangMaps.copy(values);
    }

    private Map<String, Object> storage() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("defaultStorageKind", STORAGE_IN_MEMORY);
        values.put("storageKinds", providers.storageKinds());
        values.put("builtInStorageKinds", builtInStorageKinds());
        values.put("readableStorageKinds", List.of(STORAGE_IN_MEMORY, STORAGE_FILE, STORAGE_OBJECT_STORE,
                STORAGE_DATABASE, STORAGE_COMPOSITE));
        values.put("durableStorageKinds", List.of(STORAGE_FILE, STORAGE_OBJECT_STORE, STORAGE_DATABASE));
        values.put("ephemeralStorageKinds", List.of(STORAGE_IN_MEMORY));
        values.put("disabledStorageKinds", List.of(STORAGE_NOOP));
        values.put("aliases", storageAliases());
        return AgenticCommerceWayangMaps.copy(values);
    }

    private Map<String, Object> fields() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("storageKind", field(
                "string",
                true,
                STORAGE_IN_MEMORY,
                List.of("auditStorageKind", "auditKind", "kind", "type", "mode")));
        values.put("journalPath", field(
                "string",
                false,
                AgenticCommerceWayangPersistenceTransferAuditConfig.DEFAULT_JOURNAL_PATH,
                List.of("journal", "filePath", "auditPath", "path", "journalObject", "journalDocument")));
        values.put("maxTrails", field(
                "integer",
                false,
                AgenticCommerceWayangPersistenceTransferAuditConfig.DEFAULT_MAX_TRAILS,
                retentionCountAliases()));
        values.put("retentionPolicy", field(
                "object",
                false,
                Map.of(),
                List.of("retention", "auditRetention", "auditRetentionPolicy")));
        values.put("objectStore", field(
                "object",
                false,
                Map.of(),
                List.of("objectStorage", "s3", "rustfs", "cloudStorage")));
        values.put("database", field(
                "object",
                false,
                Map.of(),
                List.of("db", "jdbc", "postgres", "postgresql")));
        values.put("children", field(
                "array<object>",
                false,
                List.of(),
                List.of("sinks", "stores", "auditSinks", "auditStores", "auditTargets")));
        return AgenticCommerceWayangMaps.copy(values);
    }

    private Map<String, Object> retentionPolicy() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("fields", Map.of(
                "maxTrails",
                field("integer", false, AgenticCommerceWayangPersistenceTransferAuditConfig.DEFAULT_MAX_TRAILS,
                        retentionCountAliases()),
                "maxBytes",
                field("byte-size", false, AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.UNLIMITED_BYTES,
                        retentionByteAliases())));
        values.put("countAliases", retentionCountAliases());
        values.put("byteAliases", retentionByteAliases());
        values.put("unlimitedBytes", AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.UNLIMITED_BYTES);
        values.put("byteSizeExamples", List.of("64 KiB", "1MiB", "unlimited"));
        values.put("contractMinimumRetainedTrailCount",
                AgenticCommerceWayangPersistenceTransferAuditContractHarness.DEFAULT_EXPECTED_RETAINED_TRAIL_COUNT);
        return AgenticCommerceWayangMaps.copy(values);
    }

    private Map<String, Object> objectStore() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("providers", List.of(
                AgenticCommerceObjectStoreConfig.PROVIDER_OBJECT_STORE,
                AgenticCommerceObjectStoreConfig.PROVIDER_S3,
                AgenticCommerceObjectStoreConfig.PROVIDER_RUSTFS));
        values.put("fields", Map.of(
                "provider", field("string", false, AgenticCommerceObjectStoreConfig.PROVIDER_OBJECT_STORE,
                        List.of("storageProvider", "storageKind", "kind", "type")),
                "endpoint", field("string", false, "", List.of("endpointUrl", "url")),
                "region", field("string", false, "", List.of("awsRegion")),
                "bucket", field("string", true, "", List.of("bucketName")),
                "keyPrefix", field("string", false, "", List.of("prefix", "root", "directory")),
                "attributes", field("object", false, Map.of(), List.of("metadata", "clientAttributes"))));
        return AgenticCommerceWayangMaps.copy(values);
    }

    private Map<String, Object> database() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("providers", List.of(
                AgenticCommerceDatabasePersistenceConfig.PROVIDER_DATABASE,
                AgenticCommerceDatabasePersistenceConfig.PROVIDER_JDBC,
                AgenticCommerceDatabasePersistenceConfig.PROVIDER_POSTGRES));
        values.put("fields", Map.of(
                "provider", field("string", false, AgenticCommerceDatabasePersistenceConfig.PROVIDER_DATABASE,
                        List.of("databaseProvider", "storageKind", "kind", "type", "driver")),
                "tableName", field("string", false, AgenticCommerceDatabasePersistenceConfig.DEFAULT_TABLE,
                        List.of("table", "documentsTable")),
                "namespace", field("string", false, AgenticCommerceDatabasePersistenceConfig.DEFAULT_NAMESPACE,
                        List.of("schema", "tenant", "directory", "prefix")),
                "attributes", field("object", false, Map.of(), List.of("metadata", "connectionAttributes"))));
        return AgenticCommerceWayangMaps.copy(values);
    }

    private Map<String, Object> validation() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("issueFields", List.of("code", "severity", "path", "message", "attributes"));
        values.put("reportFields", List.of(
                "valid",
                "storageKind",
                "issueCount",
                "errorCount",
                "warningCount",
                "errorCodes",
                "warningCodes",
                "remediationCount",
                "remediation",
                "patchCount",
                "patches",
                "target",
                "issues"));
        values.put("configIssueCodes", List.of(
                AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.UNSUPPORTED_AUDIT_STORAGE_KIND,
                AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_COUNT_INVALID,
                AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_BELOW_CONTRACT,
                AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_BYTE_LIMIT_INVALID,
                AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues
                        .AUDIT_RETENTION_BYTE_LIMIT_BELOW_CONTRACT,
                AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_STORAGE_DISABLED,
                AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_STORAGE_EPHEMERAL,
                AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_JOURNAL_PATH_MISSING,
                AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_JOURNAL_PATH_INVALID,
                AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_JOURNAL_NAME_MISSING,
                AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_JOURNAL_NAME_ABSOLUTE,
                AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_JOURNAL_NOT_JSONL));
        values.put("contractIssueCodes", List.of(
                AgenticCommerceWayangPersistenceTransferAuditContractIssues.SINK_BUILD_FAILED,
                AgenticCommerceWayangPersistenceTransferAuditContractIssues.RECORD_FAILED,
                AgenticCommerceWayangPersistenceTransferAuditContractIssues.QUERY_FAILED,
                AgenticCommerceWayangPersistenceTransferAuditContractIssues.RETAINED_TRAILS_MISMATCH,
                AgenticCommerceWayangPersistenceTransferAuditContractIssues.RETAINED_TRAIL_COUNT_MISMATCH,
                AgenticCommerceWayangPersistenceTransferAuditContractIssues.RETENTION_KEPT_OLDEST_TRAIL,
                AgenticCommerceWayangPersistenceTransferAuditContractIssues.LATEST_TRAIL_MISMATCH,
                AgenticCommerceWayangPersistenceTransferAuditContractIssues.RELOAD_TRAILS_MISMATCH,
                AgenticCommerceWayangPersistenceTransferAuditContractIssues.RELOAD_TRAIL_COUNT_MISMATCH,
                AgenticCommerceWayangPersistenceTransferAuditContractIssues.TYPE_QUERY_MISMATCH,
                AgenticCommerceWayangPersistenceTransferAuditContractIssues.OUTCOME_QUERY_MISMATCH,
                AgenticCommerceWayangPersistenceTransferAuditContractIssues.NEXT_ACTION_QUERY_MISMATCH));
        values.put("diagnosticWarningCodes", List.of(
                AgenticCommerceWayangPersistenceTransferAuditDiagnosticsIssues.AUDIT_CONTRACT_NOT_RUN,
                AgenticCommerceWayangPersistenceTransferAuditDiagnosticsIssues.AUDIT_RELOAD_NOT_CHECKED));
        return AgenticCommerceWayangMaps.copy(values);
    }

    private Map<String, Object> remediation() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("operations", List.of(
                AgenticCommerceWayangPersistenceTransferAuditConfigRemediation.OPERATION_REVIEW_VALIDATION_ISSUE,
                AgenticCommerceWayangPersistenceTransferAuditConfigRemediation.OPERATION_REPLACE_WITH_INTEGER,
                AgenticCommerceWayangPersistenceTransferAuditConfigRemediation.OPERATION_INCREASE_RETAINED_HISTORY,
                AgenticCommerceWayangPersistenceTransferAuditConfigRemediation.OPERATION_REPLACE_WITH_BYTE_SIZE,
                AgenticCommerceWayangPersistenceTransferAuditConfigRemediation.OPERATION_INCREASE_BYTE_LIMIT));
        values.put("fields", List.of(
                "code",
                "path",
                "message",
                "operation",
                "attributes",
                "patchCount",
                "patches"));
        values.put("patch", Map.of(
                "operations",
                List.of(AgenticCommerceWayangPersistenceTransferAuditConfigPatch.OPERATION_REPLACE),
                "fields",
                List.of("operation", "path", "value", "attributes"),
                "pathSyntax",
                "$.field[.nestedField]",
                "application",
                "copy-on-write"));
        values.put("patchApplicationReportFields", List.of(
                "patchable",
                "patchCount",
                "resolved",
                "improved",
                "beforeValid",
                "afterValid",
                "before",
                "after",
                "patches",
                "originalConfig",
                "patchedConfig"));
        values.put("patchApplicationSummaryFields", List.of(
                "patchable",
                "patchCount",
                "resolved",
                "improved",
                "beforeValid",
                "afterValid",
                "beforeErrorCodes",
                "afterErrorCodes",
                "beforeWarningCodes",
                "afterWarningCodes"));
        return AgenticCommerceWayangMaps.copy(values);
    }

    private static List<String> builtInStorageKinds() {
        return List.of(STORAGE_NOOP, STORAGE_IN_MEMORY, STORAGE_FILE, STORAGE_OBJECT_STORE, STORAGE_DATABASE,
                STORAGE_COMPOSITE);
    }

    private static List<String> retentionCountAliases() {
        return List.of("maxTrails", "maxEvents", "retention", "limit", "capacity");
    }

    private static List<String> retentionByteAliases() {
        return List.of("maxBytes", "byteLimit", "maxJournalBytes", "maxSizeBytes", "journalMaxBytes");
    }

    private static Map<String, Object> storageAliases() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(STORAGE_NOOP, List.of("none", "no-op", "disabled", "off"));
        values.put(STORAGE_IN_MEMORY, List.of("memory", "inmemory", "mem", "ephemeral", "transient"));
        values.put(STORAGE_FILE, List.of("files", "filesystem", "file-system", "local-file", "local", "jsonl"));
        values.put(STORAGE_COMPOSITE, List.of("multi", "fanout", "fan-out", "hybrid", "primary-fallback"));
        values.put(STORAGE_OBJECT_STORE, List.of("cloud", "cloud-storage", "object-storage", "objectstore",
                "s3", "aws-s3", "s3-compatible", "minio", "rustfs", "rust-fs"));
        values.put(STORAGE_DATABASE, List.of("db", "sql", "jdbc", "postgres", "postgresql", "pg"));
        return AgenticCommerceWayangMaps.copy(values);
    }

    private static Map<String, Object> field(
            String type,
            boolean required,
            Object defaultValue,
            List<String> aliases) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", type);
        values.put("required", required);
        values.put("defaultValue", defaultValue);
        values.put("aliases", aliases == null ? List.of() : aliases);
        return AgenticCommerceWayangMaps.copy(values);
    }
}
