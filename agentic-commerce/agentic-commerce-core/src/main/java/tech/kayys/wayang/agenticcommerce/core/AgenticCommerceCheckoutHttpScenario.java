package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ordered checkout HTTP harness scenario.
 */
public record AgenticCommerceCheckoutHttpScenario(
        String id,
        String name,
        List<AgenticCommerceCheckoutHttpScenarioStep> steps,
        Map<String, Object> metadata) {

    public AgenticCommerceCheckoutHttpScenario {
        id = AgenticCommerceValues.textValue(id);
        if (id.isBlank()) {
            throw new IllegalArgumentException("Agentic Commerce scenario id must not be blank");
        }
        name = AgenticCommerceValues.textValue(name);
        steps = steps == null
                ? List.of()
                : steps.stream()
                        .filter(java.util.Objects::nonNull)
                        .toList();
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public int stepCount() {
        return steps.size();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        AgenticCommerceValues.putText(values, "name", name);
        values.put("stepCount", stepCount());
        values.put("steps", steps.stream()
                .map(AgenticCommerceCheckoutHttpScenarioStep::toMap)
                .toList());
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}
