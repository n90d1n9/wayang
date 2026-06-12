package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of installing Agentic Commerce checkout skills into a Wayang registry.
 */
public record AgenticCommerceSkillRegistration(
        List<String> requestedSkillIds,
        List<String> registeredDefinitionIds,
        List<String> registeredRuntimeSkillIds,
        List<String> missingSkillIds,
        Map<String, Object> metadata) {

    public AgenticCommerceSkillRegistration {
        requestedSkillIds = strings(requestedSkillIds);
        registeredDefinitionIds = strings(registeredDefinitionIds);
        registeredRuntimeSkillIds = strings(registeredRuntimeSkillIds);
        missingSkillIds = strings(missingSkillIds);
        metadata = AgenticCommerceWayangMaps.copy(metadata);
    }

    public int requestedCount() {
        return requestedSkillIds.size();
    }

    public int definitionCount() {
        return registeredDefinitionIds.size();
    }

    public int runtimeSkillCount() {
        return registeredRuntimeSkillIds.size();
    }

    public int missingCount() {
        return missingSkillIds.size();
    }

    public boolean successful() {
        return missingSkillIds.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", AgenticCommerceWayang.PROTOCOL_ID);
        values.put("specVersion", AgenticCommerceProtocol.SPEC_VERSION);
        values.put("successful", successful());
        values.put("requestedCount", requestedCount());
        values.put("definitionCount", definitionCount());
        values.put("runtimeSkillCount", runtimeSkillCount());
        values.put("missingCount", missingCount());
        values.put("requestedSkillIds", requestedSkillIds);
        values.put("registeredDefinitionIds", registeredDefinitionIds);
        values.put("registeredRuntimeSkillIds", registeredRuntimeSkillIds);
        values.put("missingSkillIds", missingSkillIds);
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }

    private static List<String> strings(List<String> values) {
        return values == null
                ? List.of()
                : values.stream()
                        .map(AgenticCommerceWayangMaps::text)
                        .filter(value -> !value.isBlank())
                        .distinct()
                        .toList();
    }
}
