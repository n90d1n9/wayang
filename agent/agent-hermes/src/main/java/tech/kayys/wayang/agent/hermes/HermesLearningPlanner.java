package tech.kayys.wayang.agent.hermes;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Plans whether an eligible Hermes learning signal should create, refine, or skip a skill.
 */
public final class HermesLearningPlanner {

    private final HermesLearnedSkillRepository learnedSkills;
    private final HermesSkillDistiller distiller;
    private final HermesAgentModeConfig config;
    private final HermesLearningPolicy learningPolicy;
    private final HermesSkillReusePolicy reusePolicy;

    public HermesLearningPlanner(
            HermesLearnedSkillRepository learnedSkills,
            HermesSkillDistiller distiller,
            HermesAgentModeConfig config,
            HermesSkillReusePolicy reusePolicy) {
        this.learnedSkills = Objects.requireNonNull(learnedSkills, "learnedSkills");
        this.distiller = distiller == null ? new HermesSkillDistiller() : distiller;
        this.config = config == null ? HermesAgentModeConfig.defaults() : config;
        this.learningPolicy = new HermesLearningPolicy(this.config);
        this.reusePolicy = reusePolicy == null ? new HermesSkillReusePolicy() : reusePolicy;
    }

    public Uni<HermesLearningPlan> plan(HermesLearningSignal signal) {
        HermesLearningAssessment assessment = learningPolicy.assess(signal);
        HermesLearningLifecycleReport lifecycle = assessmentLifecycle(assessment);
        if (!assessment.eligible()) {
            return Uni.createFrom().item(HermesLearningPlan.skipped(
                    assessment.reason(),
                    skippedPlanning(lifecycle, assessment.reason())));
        }
        SkillDefinition candidate = distiller.distill(signal, config);
        HermesLearningLifecycleReport distilledLifecycle = lifecycle
                .withStage(distillationStage(candidate, "distilled learned-skill candidate"));
        Optional<HermesLearningPlan> invalidCandidate =
                invalidPlan(candidate, "distilled skill", distilledLifecycle);
        if (invalidCandidate.isPresent()) {
            return Uni.createFrom().item(invalidCandidate.orElseThrow());
        }
        HermesLearningLifecycleReport validatedLifecycle = distilledLifecycle
                .withStage(validationStage(candidate, "distilled skill accepted"));
        return learnedSkills.find(candidate.id())
                .flatMap(existing -> existing
                        .map(skill -> refinementPlan(
                                skill,
                                candidate,
                                signal,
                                "skill already exists",
                                reuseMatchStage(validatedLifecycle, skill, "exact")))
                        .orElseGet(() -> reusableOrCreatePlan(candidate, signal, validatedLifecycle)));
    }

    private Uni<HermesLearningPlan> reusableOrCreatePlan(
            SkillDefinition candidate,
            HermesLearningSignal signal,
            HermesLearningLifecycleReport lifecycle) {
        return learnedSkills.findReusable(candidate, reusePolicy)
                .flatMap(match -> match
                        .map(reusable -> refinementPlan(
                                reusable.skill(),
                                candidate,
                                signal,
                                "similar skill already exists: "
                                        + reusable.skill().id()
                                        + " (" + reusable.reason() + ")",
                                reuseMatchStage(lifecycle, reusable.skill(), reusable.reason())))
                        .orElseGet(() -> Uni.createFrom().item(HermesLearningPlan.create(
                                candidate,
                                lifecycle.withStage(HermesLearningStageReport.completed(
                                        HermesLearningStageCatalog.REUSE_MATCH,
                                        "no reusable skill matched",
                                        Map.of("matched", false)))))));
    }

    private Uni<HermesLearningPlan> refinementPlan(
            SkillDefinition existing,
            SkillDefinition candidate,
            HermesLearningSignal signal,
            String skipReason,
            HermesLearningLifecycleReport lifecycle) {
        if (!config.skillSelfImprovementEnabled()) {
            return Uni.createFrom().item(HermesLearningPlan.skipped(skipReason, lifecycle));
        }
        SkillDefinition refined = distiller.refine(existing, candidate, signal, skipReason);
        HermesLearningLifecycleReport refinedLifecycle = lifecycle
                .withStage(distillationStage(refined, "refined learned-skill candidate"));
        Optional<HermesLearningPlan> invalidRefinement =
                invalidPlan(refined, "refined skill", refinedLifecycle);
        if (invalidRefinement.isPresent()) {
            return Uni.createFrom().item(invalidRefinement.orElseThrow());
        }
        return Uni.createFrom().item(HermesLearningPlan.update(
                refined,
                refinedLifecycle.withStage(validationStage(refined, "refined skill accepted"))));
    }

    private Optional<HermesLearningPlan> invalidPlan(
            SkillDefinition skill,
            String label,
            HermesLearningLifecycleReport lifecycle) {
        return learnedSkills.validate(skill, label)
                .map(result -> HermesLearningPlan.skipped(
                        result.reason(),
                        lifecycle.withStage(HermesLearningStageReport.failed(
                                        HermesLearningStageCatalog.CANDIDATE_VALIDATION,
                                        result.reason(),
                                        skillMetadata(skill)))
                                .withStage(HermesLearningStageReport.skipped(
                                        HermesLearningStageCatalog.REUSE_MATCH,
                                        result.reason()))));
    }

    private static HermesLearningLifecycleReport assessmentLifecycle(HermesLearningAssessment assessment) {
        HermesLearningAssessment resolved = assessment == null
                ? HermesLearningAssessment.skipped("learning assessment missing")
                : assessment;
        return HermesLearningLifecycleReport.fromStages(
                HermesLearningStageReport.completed(
                        HermesLearningStageCatalog.SIGNAL_DETECTION,
                        "learning signal available",
                        Map.of()),
                HermesLearningStageReport.completed(
                        HermesLearningStageCatalog.ELIGIBILITY_ASSESSMENT,
                        resolved.reason(),
                        Map.of("assessment", resolved.toMetadata())));
    }

    private static HermesLearningLifecycleReport skippedPlanning(
            HermesLearningLifecycleReport lifecycle,
            String reason) {
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

    private static HermesLearningLifecycleReport reuseMatchStage(
            HermesLearningLifecycleReport lifecycle,
            SkillDefinition skill,
            String matchType) {
        Map<String, Object> metadata = new LinkedHashMap<>(skillMetadata(skill));
        metadata.put("matched", true);
        metadata.put("matchType", HermesText.oneLineOr(matchType, "existing"));
        return lifecycle.withStage(HermesLearningStageReport.completed(
                HermesLearningStageCatalog.REUSE_MATCH,
                "reusable skill selected",
                metadata));
    }

    private static HermesLearningStageReport distillationStage(
            SkillDefinition skill,
            String reason) {
        return HermesLearningStageReport.completed(
                HermesLearningStageCatalog.SKILL_DISTILLATION,
                reason,
                skillMetadata(skill));
    }

    private static HermesLearningStageReport validationStage(
            SkillDefinition skill,
            String reason) {
        return HermesLearningStageReport.completed(
                HermesLearningStageCatalog.CANDIDATE_VALIDATION,
                reason,
                skillMetadata(skill));
    }

    private static Map<String, Object> skillMetadata(SkillDefinition skill) {
        if (skill == null) {
            return Map.of();
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("skillId", HermesText.trimOr(skill.id(), ""));
        metadata.put("skillName", HermesText.oneLineOr(skill.name(), ""));
        metadata.put("skillCategory", HermesText.oneLineOr(skill.category(), ""));
        metadata.put("revision", HermesText.oneLineOr(
                String.valueOf(skill.metadata().getOrDefault("hermes.revision", "")),
                ""));
        return Map.copyOf(metadata);
    }
}
