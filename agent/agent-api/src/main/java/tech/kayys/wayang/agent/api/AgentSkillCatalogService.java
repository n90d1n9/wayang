package tech.kayys.wayang.agent.api;

import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds API-facing views over runtime and persisted skills.
 */
final class AgentSkillCatalogService {

    List<AgentResource.SkillSummary> listSkills(SkillRegistry registry, AgentSkillsRequest request) {
        String category = request == null ? null : request.category();
        List<AgentResource.SkillSummary> summaries = new ArrayList<>();
        registry.listAll().stream()
                .filter(skill -> matchesCategory(skill.category(), category))
                .map(this::toSummary)
                .forEach(summaries::add);
        registry.listSkills().stream()
                .filter(skill -> matchesCategory(skill.category(), category))
                .map(this::toSummary)
                .forEach(summaries::add);
        return summaries.stream()
                .sorted(Comparator.comparing(AgentResource.SkillSummary::id))
                .toList();
    }

    Optional<AgentResource.SkillSummary> getSkill(SkillRegistry registry, String skillId) {
        return registry.find(skillId)
                .map(this::toSummary)
                .or(() -> registry.getSkill(skillId).map(this::toSummary));
    }

    AgentResource.AgentHealthResponse health(SkillRegistry registry) {
        Map<String, Boolean> healthBySkill = new LinkedHashMap<>();
        registry.listAll().forEach(skill -> healthBySkill.put(skill.id(), skill.isHealthy()));
        registry.listSkills().forEach(skill -> healthBySkill.putIfAbsent(skill.id(), true));
        long healthy = healthBySkill.values().stream().filter(Boolean::booleanValue).count();
        return new AgentResource.AgentHealthResponse(
                "UP",
                healthBySkill.size(),
                (int) healthy,
                healthBySkill.size() - (int) healthy);
    }

    private AgentResource.SkillSummary toSummary(AgentSkill skill) {
        return new AgentResource.SkillSummary(
                skill.id(),
                skill.name(),
                skill.description(),
                skill.category(),
                skill.version(),
                skill.priority(),
                skill.isHealthy(),
                true);
    }

    private AgentResource.SkillSummary toSummary(SkillDefinition skill) {
        Map<String, Object> metadata = skill.metadata() == null ? Map.of() : skill.metadata();
        return new AgentResource.SkillSummary(
                skill.id(),
                skill.name(),
                skill.description(),
                skill.category(),
                String.valueOf(metadata.getOrDefault("version", "1.0.0")),
                intMetadata(metadata.get("priority"), 100),
                true,
                false);
    }

    private boolean matchesCategory(String actual, String expected) {
        return expected == null || expected.isBlank()
                || (actual != null && actual.equalsIgnoreCase(expected));
    }

    private int intMetadata(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        return fallback;
    }
}
