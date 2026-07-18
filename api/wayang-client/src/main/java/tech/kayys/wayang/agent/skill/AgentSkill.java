package tech.kayys.wayang.agent.skill;

import java.util.List;

public interface AgentSkill {

    AgentSkillDescriptor descriptor();

    default AgentSkillState state() {
        return AgentSkillState.ACTIVE;
    }

    default List<String> aliases() {
        return List.of();
    }

    default String id() {
        return descriptor().id();
    }

    default boolean supportsSurface(String surfaceId) {
        return descriptor().supportsSurface(surfaceId);
    }

    default boolean availableForRuns() {
        return state().availableForRuns();
    }
}
