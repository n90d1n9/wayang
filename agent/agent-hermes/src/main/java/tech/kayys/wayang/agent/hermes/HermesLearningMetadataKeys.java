package tech.kayys.wayang.agent.hermes;

import java.util.List;

/**
 * Stable metadata keys emitted by the Hermes learned-skill loop.
 */
public final class HermesLearningMetadataKeys {

    public static final String RESULT = "learningResult";
    public static final String LIFECYCLE = "learningLifecycle";
    public static final String PLANNING_LIFECYCLE = "planningLifecycle";
    public static final String PROMOTION = "promotion";
    public static final String PROMOTION_RECEIPT = "promotionReceipt";
    public static final String PROMOTION_RECEIPT_LEDGER = "promotionReceiptLedger";
    public static final String SKILL_INDEXING_RECEIPT = "skillIndexingReceipt";

    public static final String TERMINAL_STAGE = "terminalStage";
    public static final String COMPLETED_STAGES = "completedStages";
    public static final String SKIPPED_STAGES = "skippedStages";
    public static final String FAILED_STAGES = "failedStages";
    public static final String PENDING_STAGES = "pendingStages";
    public static final String STAGES = "stages";

    public static final String STAGE = "stage";
    public static final String STATUS = "status";
    public static final String REASON = "reason";
    public static final String METADATA = "metadata";

    public static final List<String> RESULT_FIELDS = List.of(
            PROMOTION,
            PROMOTION_RECEIPT,
            PROMOTION_RECEIPT_LEDGER,
            SKILL_INDEXING_RECEIPT,
            LIFECYCLE);

    public static final List<String> LIFECYCLE_FIELDS = List.of(
            TERMINAL_STAGE,
            COMPLETED_STAGES,
            SKIPPED_STAGES,
            FAILED_STAGES,
            PENDING_STAGES,
            STAGES);

    public static final List<String> STAGE_FIELDS = List.of(
            STAGE,
            STATUS,
            REASON,
            METADATA);

    private HermesLearningMetadataKeys() {
    }
}
