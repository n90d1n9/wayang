package tech.kayys.wayang.agenticcommerce.wayang;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.AgentSkill;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Dispatches checkout skill invocations by Wayang skill id or protocol operation.
 */
public final class AgenticCommerceCheckoutSkillDispatcher {

    private final Map<String, AgentSkill> skillsById;
    private final Map<String, AgentSkill> skillsByOperation;

    public AgenticCommerceCheckoutSkillDispatcher(AgenticCommerceCheckoutService service) {
        this(AgenticCommerceCheckoutAgentSkills.skills(Objects.requireNonNull(service, "service")));
    }

    public AgenticCommerceCheckoutSkillDispatcher(List<AgentSkill> skills) {
        List<AgentSkill> resolved = skills == null ? List.of() : skills.stream()
                .filter(Objects::nonNull)
                .toList();
        Map<String, AgentSkill> byId = new LinkedHashMap<>();
        Map<String, AgentSkill> byOperation = new LinkedHashMap<>();
        for (AgentSkill skill : resolved) {
            byId.put(skill.id(), skill);
            skill.aliases().stream()
                    .map(AgenticCommerceWayangMaps::text)
                    .filter(alias -> !alias.isBlank())
                    .forEach(alias -> byOperation.put(alias, skill));
        }
        this.skillsById = java.util.Collections.unmodifiableMap(byId);
        this.skillsByOperation = java.util.Collections.unmodifiableMap(byOperation);
    }

    public static AgenticCommerceCheckoutSkillDispatcher of(AgenticCommerceCheckoutService service) {
        return new AgenticCommerceCheckoutSkillDispatcher(service);
    }

    public List<String> skillIds() {
        return List.copyOf(skillsById.keySet());
    }

    public List<String> operations() {
        return List.copyOf(skillsByOperation.keySet());
    }

    public List<AgentSkill> skills() {
        return List.copyOf(skillsById.values());
    }

    public Optional<AgentSkill> skillForId(String skillId) {
        return Optional.ofNullable(skillsById.get(AgenticCommerceWayangMaps.text(skillId)));
    }

    public Optional<AgentSkill> skillForOperation(String operation) {
        return Optional.ofNullable(skillsByOperation.get(AgenticCommerceWayangMaps.text(operation)));
    }

    public boolean canHandle(Map<String, Object> context) {
        String skillId = AgenticCommerceWayangMaps.firstText(context, "skillId", "skill");
        String operation = AgenticCommerceWayangMaps.firstText(context, "operation");
        return (!skillId.isBlank() && skillsById.containsKey(skillId))
                || (!operation.isBlank() && skillsByOperation.containsKey(operation));
    }

    public Uni<Map<String, Object>> execute(Map<String, Object> context) {
        Map<String, Object> resolved = context == null ? Map.of() : context;
        String skillId = AgenticCommerceWayangMaps.firstText(resolved, "skillId", "skill");
        if (!skillId.isBlank()) {
            return executeBySkillId(skillId, resolved);
        }
        String operation = AgenticCommerceWayangMaps.firstText(resolved, "operation");
        if (!operation.isBlank()) {
            return executeByOperation(operation, resolved);
        }
        return Uni.createFrom().item(error(
                "",
                "",
                "Agentic Commerce checkout dispatch requires skillId, skill, or operation."));
    }

    public Uni<Map<String, Object>> executeBySkillId(String skillId, Map<String, Object> context) {
        String normalized = AgenticCommerceWayangMaps.text(skillId);
        AgentSkill skill = skillsById.get(normalized);
        if (skill == null) {
            return Uni.createFrom().item(error(
                    normalized,
                    "",
                    "Unknown Agentic Commerce checkout skill id: " + normalized));
        }
        return skill.execute(context == null ? Map.of() : context);
    }

    public Uni<Map<String, Object>> executeByOperation(String operation, Map<String, Object> context) {
        String normalized = AgenticCommerceWayangMaps.text(operation);
        AgentSkill skill = skillsByOperation.get(normalized);
        if (skill == null) {
            return Uni.createFrom().item(error(
                    "",
                    normalized,
                    "Unknown Agentic Commerce checkout operation: " + normalized));
        }
        return skill.execute(context == null ? Map.of() : context);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", AgenticCommerceWayang.PROTOCOL_ID);
        values.put("skillCount", skillsById.size());
        values.put("operationCount", skillsByOperation.size());
        values.put("skillIds", skillIds());
        values.put("operations", operations());
        return Map.copyOf(values);
    }

    private static Map<String, Object> error(String skillId, String operation, String observation) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("success", false);
        values.put("status", "ERROR");
        values.put("observation", observation);
        if (!AgenticCommerceWayangMaps.text(skillId).isBlank()) {
            values.put("skill_id", skillId);
        }
        if (!AgenticCommerceWayangMaps.text(operation).isBlank()) {
            values.put("operation", operation);
        }
        values.put("error", IllegalArgumentException.class.getName());
        return Map.copyOf(values);
    }
}
