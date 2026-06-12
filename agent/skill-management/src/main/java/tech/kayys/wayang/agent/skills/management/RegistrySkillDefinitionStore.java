package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Skill definition store backed by the active runtime registry.
 */
public final class RegistrySkillDefinitionStore implements SkillDefinitionStore {

    private final SkillRegistry registry;

    public RegistrySkillDefinitionStore(SkillRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public Optional<SkillDefinition> getSkill(String skillId) {
        return registry.getSkill(skillId);
    }

    @Override
    public List<SkillDefinition> listSkills() {
        return registry.listSkills();
    }

    @Override
    public List<SkillDefinition> listByCategory(String category) {
        return registry.listByCategory(category);
    }

    @Override
    public void registerSkill(SkillDefinition skill) {
        registry.registerSkill(skill);
    }

    @Override
    public boolean unregisterSkill(String skillId) {
        return registry.unregisterSkill(skillId);
    }
}
