package tech.kayys.wayang.agent.hermes;

import io.smallrye.mutiny.Uni;

import java.util.Objects;

/**
 * Executes a planned Hermes learning decision against learned-skill persistence.
 */
public final class HermesLearningPlanExecutor {

    private final HermesLearnedSkillRepository learnedSkills;
    private final HermesLearningPromotionGate promotionGate;
    private final HermesLearningPromotionRecorder promotionRecorder;
    private final HermesLearningIndexingCoordinator indexingCoordinator;

    public HermesLearningPlanExecutor(HermesLearnedSkillRepository learnedSkills) {
        this(learnedSkills, null, null);
    }

    public HermesLearningPlanExecutor(
            HermesLearnedSkillRepository learnedSkills,
            HermesLearningPromotionRecorder promotionRecorder) {
        this(learnedSkills, promotionRecorder, null);
    }

    public HermesLearningPlanExecutor(
            HermesLearnedSkillRepository learnedSkills,
            HermesLearningPromotionRecorder promotionRecorder,
            HermesLearnedSkillIndexer learnedSkillIndexer) {
        this.learnedSkills = Objects.requireNonNull(learnedSkills, "learnedSkills");
        this.promotionGate = new HermesLearningPromotionGate(this.learnedSkills);
        this.promotionRecorder = promotionRecorder == null
                ? new HermesLearningPromotionRecorder(this.learnedSkills)
                : promotionRecorder;
        this.indexingCoordinator = new HermesLearningIndexingCoordinator(learnedSkillIndexer);
    }

    public Uni<HermesLearningResult> execute(
            HermesLearningPlan plan,
            HermesLearningSignal signal) {
        HermesLearningPlan resolved = Objects.requireNonNull(plan, "plan");
        return promotionGate.promote(resolved)
                .flatMap(promotion -> switch (promotion.decision()) {
                    case CREATED -> learnedSkills.create(promotion.skillDefinition().orElseThrow(), signal)
                            .flatMap(result -> recordAndIndex(result, promotion, signal));
                    case UPDATED -> learnedSkills.update(promotion.skillDefinition().orElseThrow(), signal)
                            .flatMap(result -> recordAndIndex(result, promotion, signal));
                    case SKIPPED -> recordAndIndex(
                            HermesLearningResult.skipped(promotion.reason()),
                            promotion,
                            signal);
                });
    }

    private Uni<HermesLearningResult> recordAndIndex(
            HermesLearningResult result,
            HermesLearningPromotion promotion,
            HermesLearningSignal signal) {
        HermesLearningResult recorded = promotionRecorder.record(result, promotion);
        return indexingCoordinator.index(recorded, promotion, signal);
    }
}
