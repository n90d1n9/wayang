package tech.kayys.wayang.agent.hermes;

import java.util.List;

/**
 * Stable identifiers for the Hermes learning lifecycle stages.
 */
public final class HermesLearningStageCatalog {

    public static final String SIGNAL_DETECTION = "signal-detection";
    public static final String ELIGIBILITY_ASSESSMENT = "eligibility-assessment";
    public static final String SKILL_DISTILLATION = "skill-distillation";
    public static final String CANDIDATE_VALIDATION = "candidate-validation";
    public static final String REUSE_MATCH = "reuse-match";
    public static final String PROMOTION_DECISION = "promotion-decision";
    public static final String SKILL_PERSISTENCE = "skill-persistence";
    public static final String PROMOTION_RECEIPT = "promotion-receipt";
    public static final String SKILL_INDEXING = "skill-indexing";

    public static final List<String> PLANNING_STAGES = List.of(
            SIGNAL_DETECTION,
            ELIGIBILITY_ASSESSMENT,
            SKILL_DISTILLATION,
            CANDIDATE_VALIDATION,
            REUSE_MATCH);

    public static final List<String> EXECUTION_STAGES = List.of(
            PROMOTION_DECISION,
            SKILL_PERSISTENCE,
            PROMOTION_RECEIPT);

    public static final List<String> OPTIONAL_STAGES = List.of(
            SKILL_INDEXING);

    public static final List<String> FULL_FLOW = List.of(
            SIGNAL_DETECTION,
            ELIGIBILITY_ASSESSMENT,
            SKILL_DISTILLATION,
            CANDIDATE_VALIDATION,
            REUSE_MATCH,
            PROMOTION_DECISION,
            SKILL_PERSISTENCE,
            PROMOTION_RECEIPT);

    public static final List<String> ALL_STAGES = List.of(
            SIGNAL_DETECTION,
            ELIGIBILITY_ASSESSMENT,
            SKILL_DISTILLATION,
            CANDIDATE_VALIDATION,
            REUSE_MATCH,
            PROMOTION_DECISION,
            SKILL_PERSISTENCE,
            PROMOTION_RECEIPT,
            SKILL_INDEXING);

    private HermesLearningStageCatalog() {
    }

    public static boolean contains(String stage) {
        return ALL_STAGES.contains(HermesDirectiveSupport.clean(stage, ""));
    }
}
