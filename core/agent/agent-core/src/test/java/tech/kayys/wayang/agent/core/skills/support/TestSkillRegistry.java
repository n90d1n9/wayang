package tech.kayys.wayang.agent.core.skills.support;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillHealth;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.agent.spi.skills.SkillResult;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TestSkillRegistry implements SkillRegistry {

    private final Map<String, SkillDefinition> skills = new LinkedHashMap<>();
    private final Map<String, SkillResult> results = new LinkedHashMap<>();
    private final Map<String, RuntimeException> failures = new LinkedHashMap<>();

    public static TestSkillRegistry of(SkillDefinition... skills) {
        TestSkillRegistry registry = new TestSkillRegistry();
        Arrays.stream(skills).forEach(registry::registerSkill);
        return registry;
    }

    public static SkillDefinition skill(String id, String name, String description) {
        return SkillDefinition.builder()
                .id(id)
                .name(name)
                .description(description)
                .category("test")
                .systemPrompt("Execute " + id)
                .metadata(Map.of("version", "1.0.0"))
                .build();
    }

    public TestSkillRegistry result(String skillId, SkillResult result) {
        results.put(skillId, result);
        return this;
    }

    public TestSkillRegistry failExecution(String skillId, RuntimeException failure) {
        failures.put(skillId, failure);
        return this;
    }

    @Override
    public List<AgentSkill> listAll() {
        return List.of();
    }

    @Override
    public Optional<AgentSkill> find(String id) {
        return Optional.empty();
    }

    @Override
    public AgentSkill findOrThrow(String id) {
        throw new IllegalArgumentException("Skill not found: " + id);
    }

    @Override
    public void register(AgentSkill skill) {
        throw new UnsupportedOperationException("AgentSkill registration is not used by this fixture");
    }

    @Override
    public void unregister(String skillId) {
        unregisterSkill(skillId);
    }

    @Override
    public List<AgentSkill> findByCategory(SkillCategory category) {
        return List.of();
    }

    @Override
    public List<AgentSkill> listAllowed(String tenantId, Set<String> allowedIds) {
        return List.of();
    }

    @Override
    public boolean isRegistered(String skillId) {
        return skills.containsKey(skillId);
    }

    @Override
    public Map<String, SkillHealth> checkHealth() {
        Map<String, SkillHealth> health = new LinkedHashMap<>();
        skills.keySet().forEach(skillId -> health.put(skillId, SkillHealth.healthy(skillId)));
        return Map.copyOf(health);
    }

    @Override
    public int size() {
        return skills.size();
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
        return skills.values().stream()
                .filter(skill -> category == null || category.equals(skill.category()))
                .toList();
    }

    @Override
    public void registerSkill(SkillDefinition skill) {
        if (skill != null) {
            skills.put(skill.id(), skill);
        }
    }

    @Override
    public boolean unregisterSkill(String skillId) {
        return skills.remove(skillId) != null;
    }

    @Override
    public Uni<SkillResult> executeSkill(String skillId, Map<String, Object> input) {
        RuntimeException failure = failures.get(skillId);
        if (failure != null) {
            return Uni.createFrom().failure(failure);
        }
        return Uni.createFrom().item(results.getOrDefault(
                skillId,
                SkillResult.success(skillId, "Executed: " + skillId)));
    }
}
