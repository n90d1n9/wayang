package tech.kayys.wayang.agent.skills.management;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Lifecycle state store with a primary backend and a fallback backend.
 */
public final class HybridSkillLifecycleStateStore implements SkillLifecycleStateStore {

    private final SkillLifecycleStateStore primary;
    private final SkillLifecycleStateStore fallback;

    public HybridSkillLifecycleStateStore(
            SkillLifecycleStateStore primary,
            SkillLifecycleStateStore fallback) {
        this.primary = Objects.requireNonNull(primary, "primary");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    public SkillLifecycleStateStore primary() {
        return primary;
    }

    public SkillLifecycleStateStore fallback() {
        return fallback;
    }

    @Override
    public Optional<SkillLifecycleState> get(String skillId) {
        return HybridSkillStoreSupport.primaryOrFallback(
                () -> primary.get(skillId),
                () -> fallback.get(skillId),
                primary::save);
    }

    @Override
    public SkillLifecycleState save(SkillLifecycleState state) {
        return primary.save(state);
    }

    @Override
    public boolean remove(String skillId) {
        return HybridSkillStoreSupport.removeFromBoth(
                () -> primary.remove(skillId),
                () -> fallback.remove(skillId));
    }

    @Override
    public Map<String, SkillLifecycleState> snapshot() {
        return HybridSkillStoreSupport.mergeFallbackThenPrimary(
                fallback::snapshot,
                primary::snapshot);
    }
}
