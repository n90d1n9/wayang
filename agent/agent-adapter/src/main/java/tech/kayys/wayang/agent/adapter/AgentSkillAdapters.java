package tech.kayys.wayang.agent.adapter;

import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillDescriptor;
import tech.kayys.wayang.agent.spi.skills.SkillResult;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Conversion helpers between the active runtime {@link AgentSkill} contract and
 * data-oriented skill records.
 */
public final class AgentSkillAdapters {

    private static final List<String> CONTRACT_KEYS = List.of(
            "success",
            "status",
            "observation",
            "error");

    private AgentSkillAdapters() {
    }

    public static SkillDefinition toDefinition(AgentSkill skill) {
        if (skill == null) {
            throw new IllegalArgumentException("Agent skill must not be null");
        }

        SkillDescriptor descriptor = skill.getClass().getAnnotation(SkillDescriptor.class);
        String id = descriptor != null && !descriptor.id().isBlank() ? descriptor.id() : skill.id();
        String name = descriptor != null ? descriptor.name() : skill.name();
        String description = descriptor != null ? descriptor.description() : skill.description();
        String category = descriptor != null ? descriptor.category().name() : skill.category();
        String version = descriptor != null ? descriptor.version() : skill.version();
        int priority = descriptor != null ? descriptor.priority() : skill.priority();
        List<String> aliases = descriptor != null
                ? Arrays.stream(descriptor.aliases()).filter(alias -> !alias.isBlank()).toList()
                : skill.aliases();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("runtimeSkill", true);
        metadata.put("version", version);
        metadata.put("priority", priority);
        metadata.put("aliases", aliases);
        if (descriptor != null) {
            metadata.put("triggers", List.of(descriptor.triggers()));
            metadata.put("inputs", Arrays.stream(descriptor.inputs()).map(AgentSkillAdapters::inputMetadata).toList());
            metadata.put("outputs", Arrays.stream(descriptor.outputs()).map(AgentSkillAdapters::outputMetadata).toList());
        }

        return SkillDefinition.builder()
                .id(id)
                .name(name)
                .description(description)
                .category(category)
                .systemPrompt("Runtime skill adapter for " + name + ". " + description)
                .metadata(metadata)
                .build();
    }

    public static SkillResult toSkillResult(String skillId, Map<String, Object> result, long durationMs) {
        Map<String, Object> outputs = result == null ? Map.of() : result;
        SkillResult.Builder builder = SkillResult.builder()
                .skillId(skillId)
                .status(status(outputs))
                .observation(observation(outputs))
                .durationMs(durationMs);

        outputs.forEach((key, value) -> {
            if (!CONTRACT_KEYS.contains(key)) {
                builder.output(key, value);
            }
        });
        return builder.build();
    }

    private static SkillResult.Status status(Map<String, Object> result) {
        Object status = result.get("status");
        if (status != null) {
            try {
                return SkillResult.Status.valueOf(status.toString().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        Object success = result.get("success");
        return Boolean.FALSE.equals(success) ? SkillResult.Status.FAILURE : SkillResult.Status.SUCCESS;
    }

    private static String observation(Map<String, Object> result) {
        Object observation = result.get("observation");
        return observation == null ? "" : observation.toString();
    }

    private static Map<String, Object> inputMetadata(SkillDescriptor.Input input) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", input.name());
        metadata.put("type", input.type());
        metadata.put("required", input.required());
        metadata.put("description", input.description());
        return metadata;
    }

    private static Map<String, Object> outputMetadata(SkillDescriptor.Output output) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", output.name());
        metadata.put("type", output.type());
        metadata.put("description", output.description());
        return metadata;
    }
}
