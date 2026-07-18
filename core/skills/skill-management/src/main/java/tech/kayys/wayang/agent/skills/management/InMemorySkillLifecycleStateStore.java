package tech.kayys.wayang.agent.skills.management;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory lifecycle state store used by default and tests.
 */
public final class InMemorySkillLifecycleStateStore implements SkillLifecycleStateStore {

    private final ConcurrentMap<String, SkillLifecycleState> states = new ConcurrentHashMap<>();

    @Override
    public Optional<SkillLifecycleState> get(String skillId) {
        if (SkillManagementSkillIds.isBlank(skillId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(states.get(skillId));
    }

    @Override
    public SkillLifecycleState save(SkillLifecycleState state) {
        Objects.requireNonNull(state, "state");
        states.put(state.skillId(), state);
        return state;
    }

    @Override
    public boolean remove(String skillId) {
        if (SkillManagementSkillIds.isBlank(skillId)) {
            return false;
        }
        return states.remove(skillId) != null;
    }

    @Override
    public Map<String, SkillLifecycleState> snapshot() {
        return Map.copyOf(states);
    }
}
