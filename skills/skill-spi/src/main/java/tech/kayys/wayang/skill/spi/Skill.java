package tech.kayys.wayang.skill.spi;

import java.util.List;
import java.util.Map;

/**
 * Runtime contract for a dynamic skill.
 */
public interface Skill {
    String id();

    String name();

    String description();

    default List<String> aliases() {
        return List.of();
    }

    default String version() {
        return "1.0.0";
    }

    default SkillCategory category() {
        return SkillCategory.GENERAL;
    }

    default int priority() {
        return 100;
    }

    default void initialize(Map<String, Object> config) {
    }

    default void shutdown() {
    }

    SkillResult execute(SkillContext context);

    default boolean canHandle(Map<String, Object> inputs) {
        return true;
    }

    default boolean isHealthy() {
        return true;
    }
}
