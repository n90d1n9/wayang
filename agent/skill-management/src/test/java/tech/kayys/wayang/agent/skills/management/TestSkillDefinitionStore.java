package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Small package-local definition store for skill-management component tests.
 */
class TestSkillDefinitionStore implements SkillDefinitionStore {

    private final Map<String, SkillDefinition> skills = new LinkedHashMap<>();

    static TestSkillDefinitionStore failingList(String message) {
        return new TestSkillDefinitionStore() {
            @Override
            public List<SkillDefinition> listSkills() {
                throw new IllegalStateException(message);
            }
        };
    }

    @Override
    public Optional<SkillDefinition> getSkill(String skillId) {
        return Optional.ofNullable(skills.get(skillId));
    }

    @Override
    public List<SkillDefinition> listSkills() {
        return List.copyOf(skills.values());
    }

    @Override
    public List<SkillDefinition> listByCategory(String category) {
        if (category == null || category.isBlank()) {
            return listSkills();
        }
        return skills.values().stream()
                .filter(skill -> category.equalsIgnoreCase(skill.category()))
                .toList();
    }

    @Override
    public void registerSkill(SkillDefinition skill) {
        skills.put(skill.id(), skill);
    }

    @Override
    public boolean unregisterSkill(String skillId) {
        return skills.remove(skillId) != null;
    }
}
