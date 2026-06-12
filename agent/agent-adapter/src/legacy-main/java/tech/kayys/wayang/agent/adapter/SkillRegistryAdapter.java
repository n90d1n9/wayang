package tech.kayys.wayang.agent.adapter;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.spi.spi.SkillRegistry as GollekSkillRegistry;
import tech.kayys.wayang.agent.spi.skills.registry.SkillRegistry;
import tech.kayys.wayang.agent.spi.skills.registry.SkillDefinition;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter that wraps gollek SkillRegistry for backward compatibility.
 * 
 * @deprecated Use {@link GollekSkillRegistry} directly
 */
@ApplicationScoped
@Deprecated
public class SkillRegistryAdapter implements SkillRegistry {

    @Inject
    GollekSkillRegistry gollekSkillRegistry;

    @Override
    public void register(SkillDefinition skill) {
        // Convert wayang SkillDefinition to gollek format
        gollekSkillRegistry.register(convertToGollekSkill(skill));
    }

    @Override
    public Optional<SkillDefinition> find(String skillId) {
        return gollekSkillRegistry.find(skillId)
                .map(this::convertToWayangSkill);
    }

    @Override
    public boolean hasSkill(String skillId) {
        return gollekSkillRegistry.find(skillId).isPresent();
    }

    @Override
    public List<SkillDefinition> findAll() {
        return gollekSkillRegistry.findAll().stream()
                .map(this::convertToWayangSkill)
                .toList();
    }

    @Override
    public List<SkillDefinition> findByCategory(String category) {
        return gollekSkillRegistry.findAll().stream()
                .filter(skill -> skill.category().name().equals(category))
                .map(this::convertToWayangSkill)
                .toList();
    }

    private tech.kayys.wayang.agent.core.spi.SkillDefinition convertToGollekSkill(SkillDefinition wayangSkill) {
        // Conversion logic - in production use proper mapping
        return null; // Placeholder
    }

    private SkillDefinition convertToWayangSkill(tech.kayys.wayang.agent.core.spi.SkillDefinition gollekSkill) {
        // Conversion logic - in production use proper mapping
        return null; // Placeholder
    }
}
