package tech.kayys.wayang.agent.skills.management;

import java.util.Map;
import java.util.Optional;

/**
 * Persistence boundary for skill lifecycle state.
 */
public interface SkillLifecycleStateStore {

    Optional<SkillLifecycleState> get(String skillId);

    SkillLifecycleState save(SkillLifecycleState state);

    boolean remove(String skillId);

    Map<String, SkillLifecycleState> snapshot();
}
