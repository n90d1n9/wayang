package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Skill definition store with a primary backend and a fallback backend.
 *
 * <p>The primary can be a database-backed implementation, while the fallback
 * can be local files or S3-compatible object storage.
 */
public final class HybridSkillDefinitionStore implements SkillDefinitionStore {

    private final SkillDefinitionStore primary;
    private final SkillDefinitionStore fallback;

    public HybridSkillDefinitionStore(SkillDefinitionStore primary, SkillDefinitionStore fallback) {
        this.primary = Objects.requireNonNull(primary, "primary");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    public SkillDefinitionStore primary() {
        return primary;
    }

    public SkillDefinitionStore fallback() {
        return fallback;
    }

    @Override
    public Optional<SkillDefinition> getSkill(String skillId) {
        return HybridSkillStoreSupport.primaryOrFallback(
                () -> primary.getSkill(skillId),
                () -> fallback.getSkill(skillId),
                primary::registerSkill);
    }

    @Override
    public List<SkillDefinition> listSkills() {
        return HybridSkillStoreSupport.mergeFallbackThenPrimary(
                fallback::listSkills,
                primary::listSkills,
                SkillDefinition::id);
    }

    @Override
    public List<SkillDefinition> listByCategory(String category) {
        if (category == null || category.isBlank()) {
            return listSkills();
        }
        return listSkills().stream()
                .filter(skill -> category.equalsIgnoreCase(skill.category()))
                .toList();
    }

    @Override
    public void registerSkill(SkillDefinition skill) {
        primary.registerSkill(skill);
    }

    @Override
    public boolean unregisterSkill(String skillId) {
        return HybridSkillStoreSupport.removeFromBoth(
                () -> primary.unregisterSkill(skillId),
                () -> fallback.unregisterSkill(skillId));
    }
}
