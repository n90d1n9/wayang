package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillHealth;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class InMemoryCliSkillRegistry implements SkillRegistry {

    private final Map<String, AgentSkill> runtimeSkills = new LinkedHashMap<>();
    private final Map<String, SkillDefinition> definitions = new LinkedHashMap<>();

    @Override
    public List<AgentSkill> listAll() {
        return List.copyOf(runtimeSkills.values());
    }

    @Override
    public Optional<AgentSkill> find(String id) {
        return Optional.ofNullable(runtimeSkills.get(id));
    }

    @Override
    public AgentSkill findOrThrow(String id) {
        return find(id).orElseThrow();
    }

    @Override
    public void register(AgentSkill skill) {
        runtimeSkills.put(skill.id(), skill);
    }

    @Override
    public void unregister(String skillId) {
        runtimeSkills.remove(skillId);
        definitions.remove(skillId);
    }

    @Override
    public List<AgentSkill> findByCategory(SkillCategory category) {
        return runtimeSkills.values().stream()
                .filter(skill -> category.name().equalsIgnoreCase(skill.category()))
                .toList();
    }

    @Override
    public List<AgentSkill> listAllowed(String tenantId, Set<String> allowedIds) {
        return runtimeSkills.values().stream()
                .filter(skill -> allowedIds == null || allowedIds.isEmpty() || allowedIds.contains(skill.id()))
                .toList();
    }

    @Override
    public boolean isRegistered(String skillId) {
        return runtimeSkills.containsKey(skillId) || definitions.containsKey(skillId);
    }

    @Override
    public Map<String, SkillHealth> checkHealth() {
        return Map.of();
    }

    @Override
    public int size() {
        Set<String> ids = new LinkedHashSet<>(runtimeSkills.keySet());
        ids.addAll(definitions.keySet());
        return ids.size();
    }

    @Override
    public Optional<SkillDefinition> getSkill(String skillId) {
        return Optional.ofNullable(definitions.get(skillId));
    }

    @Override
    public List<SkillDefinition> listSkills() {
        return List.copyOf(definitions.values());
    }

    @Override
    public List<SkillDefinition> listByCategory(String category) {
        if (category == null || category.isBlank()) {
            return listSkills();
        }
        return definitions.values().stream()
                .filter(skill -> category.equalsIgnoreCase(skill.category()))
                .toList();
    }

    @Override
    public void registerSkill(SkillDefinition skill) {
        definitions.put(skill.id(), skill);
    }

    @Override
    public boolean unregisterSkill(String skillId) {
        return definitions.remove(skillId) != null;
    }
}
