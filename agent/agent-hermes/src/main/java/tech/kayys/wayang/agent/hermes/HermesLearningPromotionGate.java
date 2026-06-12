package tech.kayys.wayang.agent.hermes;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.Objects;
import java.util.Optional;

/**
 * Final validation gate before a planned skill enters learned-skill persistence.
 */
public final class HermesLearningPromotionGate {

    private final HermesLearnedSkillRepository learnedSkills;

    public HermesLearningPromotionGate(HermesLearnedSkillRepository learnedSkills) {
        this.learnedSkills = Objects.requireNonNull(learnedSkills, "learnedSkills");
    }

    public Uni<HermesLearningPromotion> promote(HermesLearningPlan plan) {
        HermesLearningPlan resolved = Objects.requireNonNull(plan, "plan");
        if (!resolved.persistsSkill()) {
            return Uni.createFrom().item(HermesLearningPromotion.skipped(resolved));
        }
        SkillDefinition skill = resolved.skillDefinition().orElseThrow();
        Optional<HermesLearningResult> validation = learnedSkills.validate(skill, promotionLabel(resolved));
        return Uni.createFrom().item(validation
                .map(result -> HermesLearningPromotion.rejected(resolved, result.reason()))
                .orElseGet(() -> HermesLearningPromotion.approved(resolved)));
    }

    private String promotionLabel(HermesLearningPlan plan) {
        return switch (plan.decision()) {
            case CREATED -> "learned skill promotion";
            case UPDATED -> "learned skill refinement promotion";
            case SKIPPED -> "learned skill promotion";
        };
    }
}
