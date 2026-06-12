package tech.kayys.wayang.agenticcommerce.wayang;

/**
 * Stable issue-code vocabulary for transfer audit sink/reader contract checks.
 */
public final class AgenticCommerceWayangPersistenceTransferAuditContractIssues {

    public static final String SINK_BUILD_FAILED = "sink_build_failed";
    public static final String RECORD_FAILED = "record_failed";
    public static final String QUERY_FAILED = "query_failed";
    public static final String RETAINED_TRAILS_MISMATCH = "retained_trails_mismatch";
    public static final String RETAINED_TRAIL_COUNT_MISMATCH = "retained_trail_count_mismatch";
    public static final String RETENTION_KEPT_OLDEST_TRAIL = "retention_kept_oldest_trail";
    public static final String LATEST_TRAIL_MISMATCH = "latest_trail_mismatch";
    public static final String RELOAD_TRAILS_MISMATCH = "reload_trails_mismatch";
    public static final String RELOAD_TRAIL_COUNT_MISMATCH = "reload_trail_count_mismatch";
    public static final String TYPE_QUERY_MISMATCH = "type_query_mismatch";
    public static final String OUTCOME_QUERY_MISMATCH = "outcome_query_mismatch";
    public static final String NEXT_ACTION_QUERY_MISMATCH = "next_action_query_mismatch";

    private AgenticCommerceWayangPersistenceTransferAuditContractIssues() {
    }
}
