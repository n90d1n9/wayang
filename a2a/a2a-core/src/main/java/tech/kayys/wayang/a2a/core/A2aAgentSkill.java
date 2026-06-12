package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Discoverable skill advertised by an A2A agent.
 */
public record A2aAgentSkill(
        String id,
        String name,
        String description,
        List<String> tags,
        List<String> examples,
        List<String> inputModes,
        List<String> outputModes,
        List<Map<String, Object>> securityRequirements) {

    public A2aAgentSkill {
        id = A2aValues.required(id, "id");
        name = A2aValues.required(name, "name");
        description = A2aValues.required(description, "description");
        tags = A2aValues.stringList(tags, "tags");
        if (tags.isEmpty()) {
            throw new IllegalArgumentException("A2A skill requires at least one tag");
        }
        examples = A2aValues.stringList(examples, "examples");
        inputModes = A2aValues.stringList(inputModes, "inputModes");
        outputModes = A2aValues.stringList(outputModes, "outputModes");
        securityRequirements = securityRequirements == null || securityRequirements.isEmpty()
                ? List.of()
                : securityRequirements.stream().map(A2aValues::copyMap).toList();
    }

    public static A2aAgentSkill of(String id, String name, String description, List<String> tags) {
        return new A2aAgentSkill(id, name, description, tags, List.of(), List.of(), List.of(), List.of());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id);
        payload.put("name", name);
        payload.put("description", description);
        payload.put("tags", tags);
        A2aValues.putOptional(payload, "examples", examples);
        A2aValues.putOptional(payload, "inputModes", inputModes);
        A2aValues.putOptional(payload, "outputModes", outputModes);
        A2aValues.putOptional(payload, "securityRequirements", securityRequirements);
        return A2aValues.copyMap(payload);
    }

    public static A2aAgentSkill fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        return new A2aAgentSkill(
                A2aValues.string(source, "id"),
                A2aValues.string(source, "name"),
                A2aValues.string(source, "description"),
                A2aValues.stringList(source.get("tags"), "tags"),
                A2aValues.stringList(source.get("examples"), "examples"),
                A2aValues.stringList(source.get("inputModes"), "inputModes"),
                A2aValues.stringList(source.get("outputModes"), "outputModes"),
                A2aValues.objectList(source.get("securityRequirements"), "securityRequirements"));
    }
}
