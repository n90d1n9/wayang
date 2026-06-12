package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Persistence-neutral decision for one Hermes learning pass.
 */
public record HermesLearningPlan(
        HermesLearningDecision decision,
        String reason,
        SkillDefinition skill,
        HermesLearningLifecycleReport lifecycleReport) {

    public HermesLearningPlan(
            HermesLearningDecision decision,
            String reason,
            SkillDefinition skill) {
        this(decision, reason, skill, null);
    }

    public HermesLearningPlan {
        if (decision == null) {
            throw new IllegalArgumentException("learning decision is required");
        }
        reason = HermesText.oneLineOr(reason, "");
        if ((decision == HermesLearningDecision.CREATED || decision == HermesLearningDecision.UPDATED)
                && skill == null) {
            throw new IllegalArgumentException("planned learned skill is required");
        }
        lifecycleReport = lifecycleReport == null || lifecycleReport.emptyReport()
                ? defaultLifecycle(decision, reason, skill)
                : lifecycleReport;
    }

    public Optional<SkillDefinition> skillDefinition() {
        return Optional.ofNullable(skill);
    }

    public boolean persistsSkill() {
        return decision == HermesLearningDecision.CREATED || decision == HermesLearningDecision.UPDATED;
    }

    public static HermesLearningPlan create(SkillDefinition skill) {
        return create(skill, null);
    }

    public static HermesLearningPlan update(SkillDefinition skill) {
        return update(skill, null);
    }

    public static HermesLearningPlan skipped(String reason) {
        return skipped(reason, null);
    }

    static HermesLearningPlan create(
            SkillDefinition skill,
            HermesLearningLifecycleReport lifecycleReport) {
        return new HermesLearningPlan(
                HermesLearningDecision.CREATED,
                "learned new procedural skill",
                skill,
                lifecycleReport);
    }

    static HermesLearningPlan update(
            SkillDefinition skill,
            HermesLearningLifecycleReport lifecycleReport) {
        return new HermesLearningPlan(
                HermesLearningDecision.UPDATED,
                "refined existing procedural skill",
                skill,
                lifecycleReport);
    }

    static HermesLearningPlan skipped(
            String reason,
            HermesLearningLifecycleReport lifecycleReport) {
        return new HermesLearningPlan(HermesLearningDecision.SKIPPED, reason, null, lifecycleReport);
    }

    private static HermesLearningLifecycleReport defaultLifecycle(
            HermesLearningDecision decision,
            String reason,
            SkillDefinition skill) {
        HermesLearningLifecycleReport lifecycle = HermesLearningLifecycleReport.fromStages(
                HermesLearningStageReport.completed(
                        HermesLearningStageCatalog.SIGNAL_DETECTION,
                        "learning signal available",
                        Map.of()),
                HermesLearningStageReport.completed(
                        HermesLearningStageCatalog.ELIGIBILITY_ASSESSMENT,
                        reason,
                        Map.of()));
        if (decision == HermesLearningDecision.SKIPPED) {
            return lifecycle
                    .withStage(HermesLearningStageReport.skipped(
                            HermesLearningStageCatalog.SKILL_DISTILLATION,
                            reason))
                    .withStage(HermesLearningStageReport.skipped(
                            HermesLearningStageCatalog.CANDIDATE_VALIDATION,
                            reason))
                    .withStage(HermesLearningStageReport.skipped(
                            HermesLearningStageCatalog.REUSE_MATCH,
                            reason));
        }
        return lifecycle
                .withStage(HermesLearningStageReport.completed(
                        HermesLearningStageCatalog.SKILL_DISTILLATION,
                        "skill candidate available",
                        skillMetadata(skill)))
                .withStage(HermesLearningStageReport.completed(
                        HermesLearningStageCatalog.CANDIDATE_VALIDATION,
                        "skill candidate accepted",
                        skillMetadata(skill)))
                .withStage(HermesLearningStageReport.completed(
                        HermesLearningStageCatalog.REUSE_MATCH,
                        decision == HermesLearningDecision.UPDATED
                                ? "existing or reusable skill selected"
                                : "no reusable skill matched",
                        Map.of("matched", decision == HermesLearningDecision.UPDATED)));
    }

    private static Map<String, Object> skillMetadata(SkillDefinition skill) {
        if (skill == null) {
            return Map.of();
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("skillId", skill.id());
        metadata.put("skillName", HermesText.oneLineOr(skill.name(), ""));
        metadata.put("skillCategory", HermesText.oneLineOr(skill.category(), ""));
        return Map.copyOf(metadata);
    }
}
