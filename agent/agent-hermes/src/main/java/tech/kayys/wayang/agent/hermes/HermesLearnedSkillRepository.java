package tech.kayys.wayang.agent.hermes;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.skills.management.SkillArtifact;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillValidation;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Persistence boundary for Hermes-learned skills.
 */
public final class HermesLearnedSkillRepository {

    private final HermesLearnedSkillPersistenceAdapter persistenceAdapter;
    private final HermesSkillMarkdownRenderer markdownRenderer;

    public HermesLearnedSkillRepository(
            SkillManagementService skillManagementService,
            HermesSkillMarkdownRenderer markdownRenderer) {
        this(skillManagementService, markdownRenderer, HermesSkillPersistencePlan.from(null).targetPlan());
    }

    public HermesLearnedSkillRepository(
            SkillManagementService skillManagementService,
            HermesSkillMarkdownRenderer markdownRenderer,
            HermesSkillPersistenceTargetPlan targetPlan) {
        this(HermesLearnedSkillPersistenceAdapterResolver.resolve(
                skillManagementService,
                targetPlan), markdownRenderer);
    }

    public HermesLearnedSkillRepository(
            HermesLearnedSkillPersistenceAdapter persistenceAdapter,
            HermesSkillMarkdownRenderer markdownRenderer) {
        this.persistenceAdapter = Objects.requireNonNull(persistenceAdapter, "persistenceAdapter");
        this.markdownRenderer = markdownRenderer == null ? new HermesSkillMarkdownRenderer() : markdownRenderer;
    }

    public HermesSkillPersistenceTargetPlan targetPlan() {
        return persistenceAdapter.targetPlan();
    }

    public Map<String, Object> persistenceMetadata() {
        return persistenceAdapter.toMetadata();
    }

    public Uni<Optional<SkillDefinition>> find(String skillId) {
        return persistenceAdapter.find(skillId);
    }

    public Uni<Optional<HermesSkillReuseMatch>> findReusable(
            SkillDefinition candidate,
            HermesSkillReusePolicy reusePolicy) {
        HermesSkillReusePolicy effectivePolicy = reusePolicy == null ? new HermesSkillReusePolicy() : reusePolicy;
        return persistenceAdapter.listLearnedSkills()
                .map(existingSkills -> effectivePolicy.findReusable(candidate, existingSkills));
    }

    public Uni<HermesSkillLineageView> lineage(String skillId) {
        return persistenceAdapter.find(skillId)
                .flatMap(skill -> persistenceAdapter.listLearnedSkills()
                        .map(learnedSkills -> HermesSkillLineageView.from(skillId, skill, learnedSkills)));
    }

    public Uni<HermesSkillLineageCatalog> lineageCatalog() {
        return persistenceAdapter.listLearnedSkills()
                .map(HermesSkillLineageCatalog::from);
    }

    public Optional<HermesLearningResult> validate(SkillDefinition skill, String label) {
        SkillValidation validation = persistenceAdapter.validate(skill);
        if (validation.valid()) {
            return Optional.empty();
        }
        return Optional.of(HermesLearningResult.skipped(
                label + " failed validation: " + String.join("; ", validation.errors())));
    }

    public Uni<HermesLearningResult> create(SkillDefinition skill, HermesLearningSignal signal) {
        SkillArtifact artifact = markdownRenderer.render(skill, signal);
        return persistenceAdapter.create(skill, artifact)
                .replaceWith(HermesLearningResult.created(skill));
    }

    public Uni<HermesLearningResult> update(SkillDefinition skill, HermesLearningSignal signal) {
        SkillArtifact artifact = markdownRenderer.render(skill, signal);
        return persistenceAdapter.update(skill.id(), skill, artifact)
                .replaceWith(HermesLearningResult.updated(skill));
    }
}
