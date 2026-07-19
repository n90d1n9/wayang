package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reconciles persisted lifecycle state with the configured skill definitions.
 */
public final class SkillLifecycleStateReconciler {

    public SkillLifecycleStateReconcileResult reconcile(
            SkillDefinitionStore definitionStore,
            SkillLifecycleStateStore stateStore,
            SkillLifecycleStateReconcileOptions options) {
        Objects.requireNonNull(definitionStore, "definitionStore");
        Objects.requireNonNull(stateStore, "stateStore");
        SkillLifecycleStateReconcileOptions resolved =
                options == null ? SkillLifecycleStateReconcileOptions.inspectOnly() : options;

        SkillLifecycleStateReconcilePlan plan = SkillLifecycleStateReconcilePlan.from(
                definitionIds(definitionStore),
                persistedStateIds(stateStore));
        List<String> createdStateIds = createMissingStates(stateStore, resolved, plan.missingStateSkillIds());
        List<String> removedStateIds = removeOrphanedStates(stateStore, resolved, plan.orphanedStateSkillIds());

        return plan.result(createdStateIds, removedStateIds);
    }

    private List<String> definitionIds(SkillDefinitionStore definitionStore) {
        List<String> ids = new ArrayList<>();
        definitionStore.listSkills().stream()
                .filter(Objects::nonNull)
                .map(SkillDefinition::id)
                .forEach(ids::add);
        return ids;
    }

    private List<String> persistedStateIds(SkillLifecycleStateStore stateStore) {
        List<String> ids = new ArrayList<>();
        stateStore.snapshot().entrySet().stream()
                .map(entry -> stateSkillId(entry.getKey(), entry.getValue()))
                .forEach(ids::add);
        return ids;
    }

    private List<String> createMissingStates(
            SkillLifecycleStateStore stateStore,
            SkillLifecycleStateReconcileOptions options,
            List<String> missingStateIds) {
        if (!options.createMissingStates()) {
            return List.of();
        }
        missingStateIds.forEach(skillId -> stateStore.save(SkillLifecycleState.created(skillId)));
        return missingStateIds;
    }

    private List<String> removeOrphanedStates(
            SkillLifecycleStateStore stateStore,
            SkillLifecycleStateReconcileOptions options,
            List<String> orphanedStateIds) {
        if (!options.removeOrphanedStates()) {
            return List.of();
        }
        return orphanedStateIds.stream()
                .filter(stateStore::remove)
                .toList();
    }

    private String stateSkillId(String key, SkillLifecycleState state) {
        if (state != null && hasText(state.skillId())) {
            return state.skillId();
        }
        return key;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
