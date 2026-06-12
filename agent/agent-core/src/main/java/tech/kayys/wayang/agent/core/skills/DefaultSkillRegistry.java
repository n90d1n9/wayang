package tech.kayys.wayang.agent.core.skills;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillHealth;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default CDI-managed implementation of {@link SkillRegistry}.
 *
 * <p>
 * Uses a {@link ConcurrentHashMap} for thread-safe skill storage.
 * Built-in skills are loaded on startup by {@link BuiltInSkillLoader}.
 */
@ApplicationScoped
public class DefaultSkillRegistry implements SkillRegistry {

    private static final Logger log = LoggerFactory.getLogger(DefaultSkillRegistry.class);

    private final Map<String, SkillDefinition> skills = new ConcurrentHashMap<>();
    private final Map<String, AgentSkill> agentSkills = new ConcurrentHashMap<>();

    @Override
    public List<AgentSkill> listAll() {
        return List.copyOf(agentSkills.values());
    }

    @Override
    public Optional<AgentSkill> find(String id) {
        return Optional.ofNullable(agentSkills.get(id));
    }

    @Override
    public AgentSkill findOrThrow(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Skill not registered: " + id));
    }

    @Override
    public void register(AgentSkill skill) {
        if (skill == null) {
            throw new IllegalArgumentException("Agent skill must not be null");
        }
        agentSkills.put(skill.id(), skill);
        log.info("Registered agent skill: {} ({})", skill.id(), skill.name());
    }

    @Override
    public void unregister(String skillId) {
        agentSkills.remove(skillId);
        unregisterSkill(skillId);
    }

    @Override
    public List<AgentSkill> findByCategory(SkillCategory category) {
        if (category == null) {
            return listAll();
        }
        return agentSkills.values().stream()
                .filter(skill -> category.name().equalsIgnoreCase(skill.category()))
                .toList();
    }

    @Override
    public List<AgentSkill> listAllowed(String tenantId, Set<String> allowedIds) {
        if (allowedIds == null || allowedIds.isEmpty()) {
            return listAll();
        }
        return agentSkills.values().stream()
                .filter(skill -> allowedIds.contains(skill.id()))
                .toList();
    }

    @Override
    public boolean isRegistered(String skillId) {
        return agentSkills.containsKey(skillId) || skills.containsKey(skillId);
    }

    @Override
    public Map<String, SkillHealth> checkHealth() {
        Map<String, SkillHealth> health = new ConcurrentHashMap<>();
        agentSkills.forEach((id, skill) -> health.put(id,
                skill.isHealthy() ? SkillHealth.healthy(id) : SkillHealth.unhealthy(id, "Skill reported unhealthy")));
        skills.keySet().forEach(id -> health.putIfAbsent(id, SkillHealth.healthy(id)));
        return Map.copyOf(health);
    }

    @Override
    public Optional<SkillDefinition> getSkill(String skillId) {
        if (skillId == null || skillId.isBlank()) {
            return Optional.empty();
        }
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
                .filter(s -> category.equalsIgnoreCase(s.category()))
                .toList();
    }

    @Override
    public void registerSkill(SkillDefinition skill) {
        if (skill == null) {
            throw new IllegalArgumentException("Skill definition must not be null");
        }
        SkillDefinition previous = skills.put(skill.id(), skill);
        if (previous != null) {
            log.info("Updated skill: {} ({})", skill.id(), skill.name());
        } else {
            log.info("Registered skill: {} ({})", skill.id(), skill.name());
        }
    }

    @Override
    public boolean unregisterSkill(String skillId) {
        SkillDefinition removed = skills.remove(skillId);
        if (removed != null) {
            log.info("Unregistered skill: {} ({})", removed.id(), removed.name());
            return true;
        }
        return false;
    }

    /**
     * Get the total number of registered skills.
     */
    public int size() {
        return skills.size() + agentSkills.size();
    }
}
