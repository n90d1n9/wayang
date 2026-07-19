package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Resolves lifecycle state while keeping read-only defaults separate from
 * explicit persistence.
 */
final class SkillLifecycleStateResolver {

    private final SkillLifecycleStateStore store;

    SkillLifecycleStateResolver(SkillLifecycleStateStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    SkillLifecycleState view(String skillId) {
        return store.get(skillId).orElseGet(() -> SkillLifecycleState.created(skillId));
    }
}
