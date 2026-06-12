package tech.kayys.wayang.agent.skills.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.Objects;
import java.util.Optional;

/**
 * Runs definition mutations with lifecycle-state consistency and event recording.
 */
final class SkillManagementDefinitionMutationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SkillManagementDefinitionMutationRunner.class);

    private final SkillDefinitionStore definitionStore;
    private final SkillLifecycleStateStore lifecycleStateStore;
    private final SkillManagementEventRecorder eventRecorder;
    private final SkillManagementDefinitionMutationRollback rollback;
    private final SkillManagementDefinitionWriteFailure writeFailure;

    SkillManagementDefinitionMutationRunner(
            SkillDefinitionStore definitionStore,
            SkillLifecycleStateStore lifecycleStateStore,
            SkillManagementEventRecorder eventRecorder) {
        this.definitionStore = Objects.requireNonNull(definitionStore, "definitionStore");
        this.lifecycleStateStore = Objects.requireNonNull(lifecycleStateStore, "lifecycleStateStore");
        this.eventRecorder = Objects.requireNonNull(eventRecorder, "eventRecorder");
        this.rollback = new SkillManagementDefinitionMutationRollback(
                this.definitionStore,
                this.lifecycleStateStore);
        this.writeFailure = new SkillManagementDefinitionWriteFailure(this.eventRecorder);
    }

    SkillDefinition create(SkillDefinition skill) {
        Objects.requireNonNull(skill, "skill");
        if (definitionStore.getSkill(skill.id()).isPresent()) {
            throw new IllegalStateException("Skill already exists: " + skill.id());
        }
        SkillManagementOperationContext context = SkillManagementOperationContext.root();
        try {
            definitionStore.registerSkill(skill);
        } catch (RuntimeException error) {
            throw writeFailure.record(
                    SkillManagementEventOperation.CREATE_SKILL,
                    "create",
                    skill.id(),
                    error,
                    context);
        }
        try {
            lifecycleStateStore.save(SkillLifecycleState.created(skill.id()));
        } catch (RuntimeException error) {
            throw writeFailure.record(
                    SkillManagementEventOperation.CREATE_SKILL,
                    "create",
                    skill.id(),
                    rollback.created(skill.id(), error),
                    context);
        }
        LOG.info("Created skill definition: {}", skill.id());
        eventRecorder.success(SkillManagementEventOperation.CREATE_SKILL, skill.id(), context);
        return skill;
    }

    SkillDefinition update(String skillId, SkillDefinition skill) {
        Objects.requireNonNull(skill, "skill");
        if (!Objects.equals(skillId, skill.id())) {
            throw new IllegalArgumentException("Skill id mismatch: " + skillId + " != " + skill.id());
        }
        Optional<SkillDefinition> previousDefinition = definitionStore.getSkill(skillId);
        if (previousDefinition.isEmpty()) {
            throw new IllegalStateException("Skill not found: " + skillId);
        }
        Optional<SkillLifecycleState> previousState = lifecycleStateStore.get(skillId);
        SkillManagementOperationContext context = SkillManagementOperationContext.root();
        try {
            definitionStore.registerSkill(skill);
        } catch (RuntimeException error) {
            throw writeFailure.record(
                    SkillManagementEventOperation.UPDATE_SKILL,
                    "update",
                    skillId,
                    error,
                    context);
        }
        try {
            SkillLifecycleState next = nextRevision(skillId, previousState);
            lifecycleStateStore.save(next);
            eventRecorder.success(
                    SkillManagementEventOperation.UPDATE_SKILL,
                    skillId,
                    SkillManagementEventAttributes.skillRevision(next),
                    context);
        } catch (RuntimeException error) {
            throw writeFailure.record(
                    SkillManagementEventOperation.UPDATE_SKILL,
                    "update",
                    skillId,
                    rollback.updated(skillId, previousDefinition.orElseThrow(), previousState, error),
                    context);
        }
        LOG.info("Updated skill definition: {}", skillId);
        return skill;
    }

    boolean delete(String skillId) {
        Optional<SkillDefinition> previousDefinition = definitionStore.getSkill(skillId);
        Optional<SkillLifecycleState> previousState = lifecycleStateStore.get(skillId);
        SkillManagementOperationContext context = SkillManagementOperationContext.root();
        if (previousDefinition.isEmpty()) {
            try {
                lifecycleStateStore.remove(skillId);
            } catch (RuntimeException error) {
                throw writeFailure.record(
                        SkillManagementEventOperation.DELETE_SKILL,
                        "delete",
                        skillId,
                        error,
                        context);
            }
            return false;
        }
        boolean removed;
        try {
            removed = definitionStore.unregisterSkill(skillId);
        } catch (RuntimeException error) {
            throw writeFailure.record(
                    SkillManagementEventOperation.DELETE_SKILL,
                    "delete",
                    skillId,
                    error,
                    context);
        }
        if (!removed) {
            return false;
        }
        try {
            lifecycleStateStore.remove(skillId);
        } catch (RuntimeException error) {
            throw writeFailure.record(
                    SkillManagementEventOperation.DELETE_SKILL,
                    "delete",
                    skillId,
                    rollback.deleted(skillId, previousDefinition.orElseThrow(), previousState, error),
                    context);
        }
        LOG.info("Deleted skill definition: {}", skillId);
        eventRecorder.success(SkillManagementEventOperation.DELETE_SKILL, skillId, context);
        return true;
    }

    private SkillLifecycleState nextRevision(String skillId, Optional<SkillLifecycleState> previousState) {
        return previousState.orElseGet(() -> SkillLifecycleState.created(skillId)).nextRevision();
    }
}
