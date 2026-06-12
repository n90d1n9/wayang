package tech.kayys.wayang.agenticcommerce.wayang;

/**
 * Stable issue-code vocabulary for transfer audit storage configuration validation.
 */
public final class AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues {

    public static final String UNSUPPORTED_AUDIT_STORAGE_KIND = "unsupported_audit_storage_kind";
    public static final String AUDIT_RETENTION_COUNT_INVALID = "audit_retention_count_invalid";
    public static final String AUDIT_RETENTION_BELOW_CONTRACT = "audit_retention_below_contract";
    public static final String AUDIT_RETENTION_BYTE_LIMIT_INVALID = "audit_retention_byte_limit_invalid";
    public static final String AUDIT_RETENTION_BYTE_LIMIT_BELOW_CONTRACT =
            "audit_retention_byte_limit_below_contract";
    public static final String AUDIT_STORAGE_DISABLED = "audit_storage_disabled";
    public static final String AUDIT_STORAGE_EPHEMERAL = "audit_storage_ephemeral";
    public static final String AUDIT_OBJECT_STORE_PROVIDER_GENERIC = "audit_object_store_provider_generic";
    public static final String AUDIT_OBJECT_STORE_PROVIDER_UNKNOWN = "audit_object_store_provider_unknown";
    public static final String AUDIT_OBJECT_STORE_ENDPOINT_MISSING = "audit_object_store_endpoint_missing";
    public static final String AUDIT_DATABASE_PROVIDER_GENERIC = "audit_database_provider_generic";
    public static final String AUDIT_DATABASE_PROVIDER_UNKNOWN = "audit_database_provider_unknown";
    public static final String AUDIT_JOURNAL_PATH_MISSING = "audit_journal_path_missing";
    public static final String AUDIT_JOURNAL_PATH_INVALID = "audit_journal_path_invalid";
    public static final String AUDIT_JOURNAL_NAME_MISSING = "audit_journal_name_missing";
    public static final String AUDIT_JOURNAL_NAME_ABSOLUTE = "audit_journal_name_absolute";
    public static final String AUDIT_JOURNAL_NOT_JSONL = "audit_journal_not_jsonl";
    public static final String AUDIT_COMPOSITE_WITHOUT_DURABLE_CHILD =
            "audit_composite_without_durable_child";
    public static final String AUDIT_COMPOSITE_FIRST_CHILD_NOT_DURABLE =
            "audit_composite_first_child_not_durable";
    public static final String AUDIT_COMPOSITE_DUPLICATE_TARGETS = "audit_composite_duplicate_targets";

    private AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues() {
    }
}
