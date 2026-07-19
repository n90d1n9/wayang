package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Optional;

/**
 * Persistence boundary for runtime skill definitions.
 */
public interface SkillDefinitionStore {

    Optional<SkillDefinition> getSkill(String skillId);

    List<SkillDefinition> listSkills();

    default List<SkillDefinition> listByCategory(String category) {
        if (category == null || category.isBlank()) {
            return listSkills();
        }
        return listSkills().stream()
                .filter(skill -> category.equalsIgnoreCase(skill.category()))
                .toList();
    }

    void registerSkill(SkillDefinition skill);

    boolean unregisterSkill(String skillId);
}
