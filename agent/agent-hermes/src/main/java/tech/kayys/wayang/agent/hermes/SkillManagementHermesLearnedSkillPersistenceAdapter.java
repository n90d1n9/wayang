package tech.kayys.wayang.agent.hermes;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.skills.management.SkillArtifact;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillValidation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default learned-skill persistence adapter backed by SkillManagementService.
 */
public final class SkillManagementHermesLearnedSkillPersistenceAdapter
        implements HermesLearnedSkillPersistenceAdapter {

    private final SkillManagementService skillManagementService;
    private final HermesSkillPersistenceTargetPlan targetPlan;

    public SkillManagementHermesLearnedSkillPersistenceAdapter(
            SkillManagementService skillManagementService,
            HermesSkillPersistenceTargetPlan targetPlan) {
        this.skillManagementService = Objects.requireNonNull(skillManagementService, "skillManagementService");
        this.targetPlan = targetPlan == null
                ? HermesSkillPersistencePlan.from(null).targetPlan()
                : targetPlan;
    }

    public static SkillManagementHermesLearnedSkillPersistenceAdapter from(
            SkillManagementService skillManagementService,
            HermesSkillPersistenceTargetPlan targetPlan) {
        return new SkillManagementHermesLearnedSkillPersistenceAdapter(skillManagementService, targetPlan);
    }

    @Override
    public String adapterId() {
        return "skill-management";
    }

    @Override
    public HermesSkillPersistenceTargetPlan targetPlan() {
        return targetPlan;
    }

    @Override
    public Uni<Optional<SkillDefinition>> find(String skillId) {
        return skillManagementService.getSkill(skillId);
    }

    @Override
    public Uni<List<SkillDefinition>> listLearnedSkills() {
        return skillManagementService.listByCategory(HermesAgentMode.LEARNED_SKILL_CATEGORY);
    }

    @Override
    public SkillValidation validate(SkillDefinition skill) {
        return skillManagementService.validateSkill(skill);
    }

    @Override
    public Uni<SkillDefinition> create(SkillDefinition skill, SkillArtifact artifact) {
        return skillManagementService.putArtifact(artifact)
                .flatMap(ignored -> skillManagementService.createSkill(skill));
    }

    @Override
    public Uni<SkillDefinition> update(String skillId, SkillDefinition skill, SkillArtifact artifact) {
        return skillManagementService.putArtifact(artifact)
                .flatMap(ignored -> skillManagementService.updateSkill(skillId, skill));
    }
}
