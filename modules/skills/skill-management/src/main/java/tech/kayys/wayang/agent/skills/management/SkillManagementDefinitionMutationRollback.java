package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.Objects;
import java.util.Optional;

/**
 * Restores definition and lifecycle stores after a partially-applied mutation.
 */
final class SkillManagementDefinitionMutationRollback {

    private final SkillDefinitionStore definitionStore;
    private final SkillLifecycleStateStore lifecycleStateStore;

    SkillManagementDefinitionMutationRollback(
            SkillDefinitionStore definitionStore,
            SkillLifecycleStateStore lifecycleStateStore) {
        this.definitionStore = Objects.requireNonNull(definitionStore, "definitionStore");
        this.lifecycleStateStore = Objects.requireNonNull(lifecycleStateStore, "lifecycleStateStore");
    }

    RuntimeException created(String skillId, RuntimeException error) {
        Objects.requireNonNull(skillId, "skillId");
        RuntimeException resolved = Objects.requireNonNull(error, "error");
        suppressRollbackError(resolved, () -> definitionStore.unregisterSkill(skillId));
        return resolved;
    }

    RuntimeException updated(
            String skillId,
            SkillDefinition previousDefinition,
            Optional<SkillLifecycleState> previousState,
            RuntimeException error) {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(previousDefinition, "previousDefinition");
        RuntimeException resolved = Objects.requireNonNull(error, "error");
        suppressRollbackError(resolved, () -> definitionStore.registerSkill(previousDefinition));
        restoreLifecycleState(skillId, previousState, resolved);
        return resolved;
    }

    RuntimeException deleted(
            String skillId,
            SkillDefinition previousDefinition,
            Optional<SkillLifecycleState> previousState,
            RuntimeException error) {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(previousDefinition, "previousDefinition");
        RuntimeException resolved = Objects.requireNonNull(error, "error");
        suppressRollbackError(resolved, () -> definitionStore.registerSkill(previousDefinition));
        restoreLifecycleState(skillId, previousState, resolved);
        return resolved;
    }

    private void restoreLifecycleState(
            String skillId,
            Optional<SkillLifecycleState> previousState,
            RuntimeException error) {
        Optional<SkillLifecycleState> resolvedPreviousState =
                previousState == null ? Optional.empty() : previousState;
        suppressRollbackError(error, () -> {
            if (resolvedPreviousState.isPresent()) {
                lifecycleStateStore.save(resolvedPreviousState.orElseThrow());
            } else {
                lifecycleStateStore.remove(skillId);
            }
        });
    }

    private void suppressRollbackError(RuntimeException error, Runnable rollback) {
        try {
            rollback.run();
        } catch (RuntimeException rollbackError) {
            error.addSuppressed(rollbackError);
        }
    }
}
